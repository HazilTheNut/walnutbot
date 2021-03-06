package Audio;

import Commands.CommandInterpreter;
import UI.AudioKeyPlaylistLoader;
import UI.JukeboxListener;
import UI.PlayerTrackListener;
import Utils.*;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioMaster{

    private AudioPlayerManager playerManager;

    private CommandInterpreter commandInterpreter;

    private VoiceChannel connectedChannel;

    //Soundboard
    private AudioPlayer soundboardPlayer;
    private GenericTrackScheduler genericTrackScheduler;
    private AudioKeyPlaylist soundboardList;
    private AtomicBoolean soundboardActive;

    private static final String SOUNDBOARD_LOC = "~~/data/soundboard.playlist";

    //Jukebox
    private AudioPlayer jukeboxPlayer;
    private JukeboxTrackScheduler jukeboxTrackScheduler;
    private ArrayList<JukeboxSongRequest> jukeboxSongsToRequest;
    private AtomicBoolean jukeboxSongIsProcessing;
    private AudioKeyPlaylist jukeboxQueueList; //The list of requested songs to exhaust through first
    private AudioKeyPlaylist jukeboxDefaultList; //The list of songs to randomly select when the request queue is exhausted
    private AudioKey currentlyPlayingSong;
    private JukeboxListener jukeboxListener;

    private AudioKeyPlaylistListener jukeboxDefaultListListener;
    private boolean loopingCurrentSong = false;
    private boolean jukeboxPaused = false; //The "true" state of the jukebox controlled via UI, commands, etc.
    private boolean jukeboxDefaultListIsLocalFile = false;
    private SongDurationTracker songDurationTracker;

    //Volumes are 0-100, scaled 0-1000 internally
    private int mainVolume;
    private int soundboardVolume;
    private int jukeboxVolume;
    public static final int VOLUME_DEFAULT = 50;
    private static final int VOLUME_MAX = 150;
    private VolumeChangeListener volumeChangeListener;

    public AudioMaster(){

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        soundboardPlayer = playerManager.createPlayer();
        genericTrackScheduler = new GenericTrackScheduler();
        genericTrackScheduler.addPlayerTrackListener(new SoundboardPlayerListener()); //This allows the AudioMaster to listen for when the soundboard sounds, in order to switch audio stream back to the jukebox.
        soundboardPlayer.addListener(genericTrackScheduler);

        soundboardList = new AudioKeyPlaylist(new File(FileIO.expandURIMacros(SOUNDBOARD_LOC)), false);
        soundboardList.printPlaylist();

        soundboardActive = new AtomicBoolean(false);

        mainVolume = getSettingsVolume("mainVolume");
        soundboardVolume = getSettingsVolume("soundboardVolume");
        jukeboxVolume = getSettingsVolume("jukeboxVolume");

        jukeboxPlayer = playerManager.createPlayer();
        jukeboxTrackScheduler = new JukeboxTrackScheduler(this);
        jukeboxPlayer.addListener(jukeboxTrackScheduler);

        updatePlayerVolumes();

        jukeboxQueueList = new AudioKeyPlaylist("queue");

        jukeboxSongsToRequest = new ArrayList<>();
        jukeboxSongIsProcessing = new AtomicBoolean(false);

        //Set tp thread to process jukebox songs
        Thread jukeboxRequestThread = new Thread(() -> {
            while (true) {
                processJukeboxSongToRequest();
                do {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (jukeboxSongIsProcessing.get());
            }
        });
        jukeboxRequestThread.start();
    }

    private void setActiveStream(AudioPlayer player){
        if (connectedChannel != null) {
            AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
            audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
        }
    }

    public void resumeJukebox(){
        soundboardPlayer.stopTrack();
        setActiveStream(jukeboxPlayer);
        jukeboxPlayer.setPaused(jukeboxPaused);
    }

    public void playSoundboardSound(String url){
        if (getConnectedChannel() == null) {
            Transcriber.printRaw("Warning! This bot is currently not connected to any channel!");
            return;
        }
        jukeboxPlayer.setPaused(true);
        soundboardPlayer.stopTrack();
        setActiveStream(soundboardPlayer);
        soundboardActive.set(true);
        playerManager.loadItem(url, new GenericLoadResultHandler(soundboardPlayer));
    }
    
    /*
    The following are the three ways a new song is queued up to play:
    
    A - The Current Song Ends
    1: Get the next song (from the queue or from default if queue is empty) and load it up.
    2: Play loaded song
    
    B - Skip via either UI or Discord
    1: Get the next song (from the queue or from default if queue is empty) and load it up.
    2: Play loaded song
    
    C - Add a new song to queue via either UI or Discord
    1: If queue is empty, do (a), otherwise do (b)
      a: Load up and play song
      b: Add song to queue
      
    
    For cases A and B, do jukeboxSkipToNextSong()
    For case C, do queueJukeboxSong()
    
     */

    public synchronized void queueJukeboxSong(String uri, PostSongRequestAction ifSuccess, PostSongRequestAction ifError){
        ArrayList<AudioKey> keys = AudioKeyPlaylistLoader.grabKeysFromPlaylist(uri);
        for (AudioKey key : keys)
            queueJukeboxSong(key, ifSuccess, ifError);
//        queueJukeboxSong(new AudioKey("Requested", uri), ifSuccess, ifError);
    }

    public synchronized void queueJukeboxSong(AudioKey audioKey, PostSongRequestAction ifSuccess, PostSongRequestAction ifError){
        jukeboxSongsToRequest.add(new JukeboxSongRequest(audioKey, ifSuccess, ifError, RequestType.ADD_TO_QUEUE));
    }

    private void processJukeboxSongToRequest(){
        if (jukeboxSongsToRequest.size() > 0){
            jukeboxSongIsProcessing.set(true);
            JukeboxSongRequest request = jukeboxSongsToRequest.remove(0);
            if (request.requestType == RequestType.ADD_TO_QUEUE) { //If this request is to add it to the queue
                //Set up audio stream
                if (getConnectedChannel() == null)
                    Transcriber.printRaw("Warning! This bot is currently not connected to any channel!");
                else
                    setActiveStream(jukeboxPlayer);
                //Fetch song and queue it up
                if (request.getAudioKey().getLoadedTrack() == null) // If requested audio key already has a loaded track, don't need to fetch it from the internet again
                    playerManager.loadItem(request.getAudioKey().getUrl(),
                        new JukeboxQueueResultHandler(jukeboxPlayer, request.getIfSuccess(), request.getIfError()));
                else { // Fetch song from the internet
                    addTrackToJukeboxQueue(request.getAudioKey().getLoadedTrack());
                    request.getIfSuccess().doAction();
                    jukeboxSongIsProcessing.set(false);
                }
            }
        }
    }

    public boolean isProcessingJukeboxRequests(){
        return jukeboxSongsToRequest.size() > 0 || jukeboxSongIsProcessing.get();
    }

    void addTrackToJukeboxQueue(AudioTrack track){
        if (currentlyPlayingSong == null && connectedChannel != null) { //If no song is currently playing and this bot is ready to play music
            currentlyPlayingSong = new AudioKey(track);
            playCurrentSong();
        }
        else
            jukeboxQueueList.addAudioKey(new AudioKey(track));
    }

    public void jukeboxSkipToNextSong() { jukeboxSkipToNextSong(false); }

    public void jukeboxSkipToNextSong(boolean forceskip){
        //Loop the current playing song if that feature is enabled and is not being forcibly skipped (i.e. the "skip" button on the UI).
        if (!forceskip && isLoopingCurrentSong() && currentlyPlayingSong != null && currentlyPlayingSong.getLoadedTrack() != null){
            currentlyPlayingSong = new AudioKey(currentlyPlayingSong.getLoadedTrack().makeClone());
            playCurrentSong();
            return;
        }
        AudioKey keyToPlay = null;
        if (jukeboxQueueList.isEmpty()){
            if (jukeboxDefaultList != null && !jukeboxDefaultList.isEmpty())
                keyToPlay = jukeboxDefaultList.getRandomAudioKey();
        } else {
            keyToPlay = jukeboxQueueList.removeAudioKey(0);
        }
        currentlyPlayingSong = keyToPlay; //This can be null. This is fine - currentlyPlayingSong being null means no song is being played.
        playCurrentSong();
    }
    
    private void playCurrentSong(){
        setJukeboxTruePause(true);
        if (currentlyPlayingSong != null){
            setActiveStream(jukeboxPlayer);
            if (currentlyPlayingSong.getLoadedTrack() != null)
                jukeboxPlayer.startTrack(currentlyPlayingSong.getLoadedTrack(), false);
            else
                playerManager.loadItem(currentlyPlayingSong.getUrl(), new JukeboxPlayResultHandler(jukeboxPlayer, () -> {}, () -> {
                    Transcriber.printRaw("WARNING: Song url \"%1$s\" is invalid!", currentlyPlayingSong.getUrl());
                    jukeboxSkipToNextSong();
                }));
        } else {
            // If the jukebox player is playing a song, but we want the current song to be null, we need to make sure the jukebox player stops.
            jukeboxPlayer.stopTrack();
            if (songDurationTracker != null) songDurationTracker.reset();
        }
        setJukeboxTruePause(false);
    }

    public void unpauseCurrentSong(){
        if (currentlyPlayingSong == null)
            jukeboxSkipToNextSong(true);
        else
            setJukeboxTruePause(false);
    }

    public void clearJukeboxQueue(){
        jukeboxQueueList.clearPlaylist();
    }

    public void shuffleJukeboxQueue(){
        jukeboxQueueList.shuffle();
    }

    public AudioPlayer getSoundboardPlayer() {
        return soundboardPlayer;
    }

    public AudioPlayer getJukeboxPlayer() {
        return jukeboxPlayer;
    }

    public VoiceChannel getConnectedChannel() {
        return connectedChannel;
    }

    public void setConnectedChannel(VoiceChannel connectedChannel) {
        this.connectedChannel = connectedChannel;
    }

    public AudioKeyPlaylist getSoundboardList() {
        return soundboardList;
    }

    public boolean isJukeboxDefaultListIsLocalFile() {
        return jukeboxDefaultListIsLocalFile;
    }

    public void saveSoundboard(){
        soundboardList.saveToFile(new File(FileIO.expandURIMacros(SOUNDBOARD_LOC)));
    }

    public void saveJukeboxDefault(){
        if (jukeboxDefaultListIsLocalFile && jukeboxDefaultList != null)
            jukeboxDefaultList.saveToFile(new File(jukeboxDefaultList.getUrl()));
    }

    public void createNewJukeboxDefaultList(String path){
        File file = new File(enforceFileExtension(path));
        loadJukeboxPlaylist(new AudioKeyPlaylist(file, false), true);
    }

    private String enforceFileExtension(String path){
        String ext = ".playlist";
        int extIndex = path.lastIndexOf('.');
        if (extIndex < 0)
            return path.concat(ext);
        else
            return path.substring(0, extIndex).concat(ext);
    }

    public void emptyJukeboxPlaylist(){
        loadJukeboxPlaylist(null, false);
    }

    public void loadJukeboxPlaylist(AudioKeyPlaylist playlist, boolean jukeboxDefaultListIsLocalFile){
        jukeboxDefaultList = playlist;
        this.jukeboxDefaultListIsLocalFile = jukeboxDefaultListIsLocalFile;
        if (playlist != null) {
            Transcriber.printRaw("Current playlist: %1$s", jukeboxDefaultList.toString());
            jukeboxDefaultList.addAudioKeyPlaylistListener(jukeboxDefaultListListener);
            if (jukeboxListener != null) jukeboxListener.onDefaultListChange(this);
        } else {
            if (jukeboxDefaultListListener != null) jukeboxDefaultListListener.onClear();
            if (jukeboxListener != null) jukeboxListener.onDefaultListChange(this);
            Transcriber.printRaw("Playlist set to an empty one.");
        }
    }

    public void stopAllAudio() {
        soundboardPlayer.setPaused(true);
        setJukeboxTruePause(true);
    }

    /**
     * Performs a more permanent form of pausing of the jukebox.
     * When the soundboard plays a sound, it pauses the jukebox player while the sound is playing, but that is not truly "pausing it".
     * When the soundboard finishes playing a sound, it returns the jukebox to its previous status, which must be stored as a field.
     *
     * This method both assigns the jukebox playing state and it's previous state to return to after playing a sound through the soundboard.
     *
     * @param paused Whether or not the jukebox is truly "paused"
     */
    public void setJukeboxTruePause(boolean paused){
        jukeboxPlayer.setPaused(paused);
        jukeboxPaused = paused;
    }

    public void setMainVolume(int mainVolume) {
        this.mainVolume = mainVolume;
        SettingsLoader.modifySettingsValue("mainVolume", String.valueOf(mainVolume));
        SettingsLoader.writeSettingsFile();
        volumeChangeListener.onMainVolumeChange(mainVolume, this);
        updatePlayerVolumes();
    }

    public void setSoundboardVolume(int soundboardVolume) {
        this.soundboardVolume = soundboardVolume;
        SettingsLoader.modifySettingsValue("soundboardVolume", String.valueOf(soundboardVolume));
        SettingsLoader.writeSettingsFile();
        if (volumeChangeListener != null) volumeChangeListener.onSoundboardVolumeChange(soundboardVolume, this);
        updatePlayerVolumes();
    }

    public void setJukeboxVolume(int jukeboxVolume) {
        this.jukeboxVolume = jukeboxVolume;
        SettingsLoader.modifySettingsValue("jukeboxVolume", String.valueOf(jukeboxVolume));
        SettingsLoader.writeSettingsFile();
        if (volumeChangeListener != null) volumeChangeListener.onJukeboxVolumeChange(jukeboxVolume, this);
        updatePlayerVolumes();
    }

    public int getMainVolume() {
        return mainVolume;
    }

    public int getSoundboardVolume() {
        return soundboardVolume;
    }

    public int getJukeboxVolume() {
        return jukeboxVolume;
    }

    private void updatePlayerVolumes(){
        double mainVolPercent = (double)mainVolume / VOLUME_MAX;
        double soundboardVolPercent = (double)soundboardVolume / VOLUME_MAX;
        double jukeboxVolPercent = (double)jukeboxVolume / VOLUME_MAX;
        soundboardPlayer.setVolume((int)(VOLUME_MAX * mainVolPercent * soundboardVolPercent));
        jukeboxPlayer.setVolume((int)(VOLUME_MAX * mainVolPercent * jukeboxVolPercent));
    }

    private int getSettingsVolume(String setting){
        try {
            return Integer.valueOf(SettingsLoader.getSettingsValue(setting, String.valueOf(VOLUME_DEFAULT)));
        } catch (NumberFormatException e){
            return VOLUME_DEFAULT;
        }
    }

    public SongDurationTracker getSongDurationTracker() {
        return songDurationTracker;
    }

    public void setSongDurationTracker(SongDurationTracker songDurationTracker) {
        this.songDurationTracker = songDurationTracker;
    }

    public GenericTrackScheduler getGenericTrackScheduler() {
        return genericTrackScheduler;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public AudioKeyPlaylist getJukeboxQueueList() {
        return jukeboxQueueList;
    }

    public AudioKeyPlaylist getJukeboxDefaultList() {
        return jukeboxDefaultList;
    }

    public String getJukeboxDefaultListName() {
        if (jukeboxDefaultList == null) {
            return "None";
        } else
            return jukeboxDefaultList.getName();
    }

    public AudioKey getCurrentlyPlayingSong() {
        return currentlyPlayingSong;
    }

    public boolean isLoopingCurrentSong() {
        return loopingCurrentSong;
    }

    public void setLoopingCurrentSong(boolean loopingCurrentSong) {
        this.loopingCurrentSong = loopingCurrentSong;
        if (jukeboxListener != null) jukeboxListener.onJukeboxChangeLoopState(loopingCurrentSong);
    }

    public void setJukeboxListener(JukeboxListener jukeboxListener) {
        this.jukeboxListener = jukeboxListener;
    }

    public CommandInterpreter getCommandInterpreter() {
        return commandInterpreter;
    }

    public void setCommandInterpreter(CommandInterpreter commandInterpreter) {
        this.commandInterpreter = commandInterpreter;
    }

    public void setDiscordBotManager(BotManager botManager) {
        if (botManager instanceof DiscordBotManager)
            jukeboxPlayer.addListener((DiscordBotManager)botManager);
    }

    public void sortSoundboardList(){
        getSoundboardList().sort();
        //Transcriber.printTimestamped("Sorting: data complete");
        saveSoundboard();
        //Transcriber.printTimestamped("Sorting: disk write complete");
    }

    public void addSoundboardSound(AudioKey key){
        soundboardList.addAudioKey(key);
        saveSoundboard();
    }

    public AudioKey removeSoundboardSound(String id){
        AudioKey removed = null;
        // Try to evaluate String as an integer and remove from a particular position
        try {
            removed = soundboardList.removeAudioKey(Integer.valueOf(id));
        } catch (NumberFormatException ignored){}
        // If id is not an integer, search by name-matching
        if (removed == null)
            removed = soundboardList.removeAudioKey(id);
        if (removed != null)
            saveSoundboard();
        return removed;
    }

    public boolean modifySoundboardSound(String soundName, AudioKey newData){
        if (soundboardList.modifyAudioKey(soundName, newData)){
            saveSoundboard();
            return true;
        }
        return false;
    }

    public void setJukeboxDefaultListListener(AudioKeyPlaylistListener jukeboxDefaultListListener) {
        this.jukeboxDefaultListListener = jukeboxDefaultListListener;
        if (jukeboxDefaultList != null) {
            jukeboxDefaultList.addAudioKeyPlaylistListener(jukeboxDefaultListListener);
        }
    }

    public void setVolumeChangeListener(VolumeChangeListener volumeChangeListener) {
        this.volumeChangeListener = volumeChangeListener;
    }

    public boolean isSoundboardActive(){
        return soundboardActive.get();
    }

    private class SoundboardPlayerListener implements PlayerTrackListener{
        @Override public void onTrackStart() {

        }

        @Override public void onTrackStop() {
            soundboardActive.set(false);
            resumeJukebox();
        }

        @Override public void onTrackError() {
            soundboardActive.set(false);
            resumeJukebox();
        }
    }

    public interface PostSongRequestAction {
        void doAction();
    }

    private class JukeboxLoadResultHandler implements AudioLoadResultHandler{

        PostSongRequestAction ifSuccess;
        PostSongRequestAction ifError;
        AudioPlayer audioPlayer;

        JukeboxLoadResultHandler(AudioPlayer audioPlayer, PostSongRequestAction ifSuccess, PostSongRequestAction ifError) {
            this.ifError = ifError;
            this.ifSuccess = ifSuccess;
            this.audioPlayer = audioPlayer;
        }

        @Override public void trackLoaded(AudioTrack track) {
            ifSuccess.doAction();
        }

        @Override public void playlistLoaded(AudioPlaylist playlist) {
            ifSuccess.doAction();
        }

        @Override public void noMatches() {
            ifError.doAction();
        }

        @Override public void loadFailed(FriendlyException throwable) {
            ifError.doAction();
        }
    }

    private class JukeboxQueueResultHandler extends JukeboxLoadResultHandler {

        public JukeboxQueueResultHandler(AudioPlayer audioPlayer, PostSongRequestAction ifSuccess, PostSongRequestAction ifError) {
            super(audioPlayer, ifSuccess, ifError);
        }

        @Override public void trackLoaded(AudioTrack track) {
            addTrackToJukeboxQueue(track);
            super.trackLoaded(track);
            jukeboxSongIsProcessing.set(false);
        }

        @Override public void playlistLoaded(AudioPlaylist playlist) {
            for (AudioTrack track : playlist.getTracks())
                addTrackToJukeboxQueue(track);
            super.playlistLoaded(playlist);
            jukeboxSongIsProcessing.set(false);
        }

        @Override public void noMatches() {
            super.noMatches();
            jukeboxSongIsProcessing.set(false);
        }

        @Override public void loadFailed(FriendlyException throwable) {
            super.loadFailed(throwable);
            jukeboxSongIsProcessing.set(false);
        }
    }

    private class JukeboxDefaultResultHandler extends JukeboxLoadResultHandler {

        JukeboxDefaultResultHandler(AudioPlayer audioPlayer, PostSongRequestAction ifSuccess,
            PostSongRequestAction ifError) {
            super(audioPlayer, ifSuccess, ifError);
        }

        @Override public void trackLoaded(AudioTrack track) {
            jukeboxDefaultList.addAudioKey(new AudioKey(track));
            super.trackLoaded(track);
            jukeboxSongIsProcessing.set(false);
        }
    }

    private class JukeboxPlayResultHandler extends JukeboxLoadResultHandler {

        public JukeboxPlayResultHandler(AudioPlayer audioPlayer, PostSongRequestAction ifSuccess,
            PostSongRequestAction ifError) {
            super(audioPlayer, ifSuccess, ifError);
        }

        @Override public void trackLoaded(AudioTrack track) {
            Transcriber.printTimestamped("Track \'%1$s\' loaded! (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
            currentlyPlayingSong = new AudioKey(track);
            if (!audioPlayer.startTrack(track, false))
                Transcriber.printTimestamped("Track \'%1$s\' failed to start (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
            super.trackLoaded(track);
        }
    }

    public enum RequestType {
        ADD_TO_DEFAULT, //Behavior not supported. May need to add later if synchronous requests are needed to ensure songs load properly.
        ADD_TO_QUEUE
    }

    private class JukeboxSongRequest {

        private AudioKey audioKey;
        private PostSongRequestAction ifSuccess;
        private PostSongRequestAction ifError;
        private RequestType requestType;

        public JukeboxSongRequest(AudioKey audioKey, PostSongRequestAction ifSuccess,
            PostSongRequestAction ifError, RequestType requestType) {
            this.audioKey = audioKey;
            this.ifSuccess = ifSuccess;
            this.ifError = ifError;
            this.requestType = requestType;
        }

        public AudioKey getAudioKey() {
            return audioKey;
        }

        public PostSongRequestAction getIfSuccess() {
            return ifSuccess;
        }

        public PostSongRequestAction getIfError() {
            return ifError;
        }

        public RequestType getRequestType() {
            return requestType;
        }
    }
}
