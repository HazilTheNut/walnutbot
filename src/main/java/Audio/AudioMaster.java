package Audio;

import Commands.Command;
import Commands.CommandInterpreter;
import UI.AudioKeyPlaylistLoader;
import UI.JukeboxUIWrapper;
import UI.PlayerTrackListener;
import UI.SoundboardUIWrapper;
import Utils.BotManager;
import Utils.DiscordBotManager;
import Utils.FileIO;
import Utils.Transcriber;
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

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioMaster{

    private AudioPlayerManager playerManager;

    private CommandInterpreter commandInterpreter;

    private VoiceChannel connectedChannel;

    //Soundboard
    private AudioPlayer soundboardPlayer;
    private GenericTrackScheduler genericTrackScheduler;
    private AudioKeyPlaylist soundboardList;
    private SoundboardUIWrapper soundboardUIWrapper;

    //Jukebox
    private AudioPlayer jukeboxPlayer;
    private JukeboxTrackScheduler jukeboxTrackScheduler;
    private ArrayList<JukeboxSongRequest> jukeboxSongsToRequest;
    private AtomicBoolean jukeboxSongIsProcessing;
    private AudioKeyPlaylist jukeboxQueueList; //The list of requested songs to exhaust through first
    private AudioKeyPlaylist jukeboxDefaultList; //The list of songs to randomly select when the request queue is exhausted
    private AudioKey currentlyPlayingSong;
    private JukeboxUIWrapper jukeboxUIWrapper;

    private AudioKeyPlaylistListener jukeboxDefaultListListener;
    private boolean loopingCurrentSong = false;
    private boolean jukeboxPaused = false; //The "true" state of the jukebox controlled via UI, commands, etc.
    private boolean jukeboxDefaultListIsLocalFile = false;
    private SongDurationTracker songDurationTracker;

    //Volumes are 0-1, scaled 0-1000 internally
    private double masterVolume;
    private double soundboardVolume;
    private double jukeboxVolume;
    public static final double VOLUME_DEFAULT = 0.5d;
    private static final int VOLUME_MAX = 150;

    public AudioMaster(){

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        soundboardPlayer = playerManager.createPlayer();
        genericTrackScheduler = new GenericTrackScheduler();
        genericTrackScheduler.addPlayerTrackListener(new SoundboardPlayerListener()); //This allows the AudioMaster to listen for when the soundboard sounds, in order to switch audio stream back to the jukebox.
        soundboardPlayer.addListener(genericTrackScheduler);

        soundboardList = new AudioKeyPlaylist(new File(FileIO.getRootFilePath() + "soundboard.playlist"), false);
        soundboardList.printPlaylist();

        masterVolume = VOLUME_DEFAULT;
        soundboardVolume = VOLUME_DEFAULT;
        jukeboxVolume = VOLUME_DEFAULT;

        jukeboxPlayer = playerManager.createPlayer();
        jukeboxTrackScheduler = new JukeboxTrackScheduler(this);
        jukeboxPlayer.addListener(jukeboxTrackScheduler);

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
        AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
        audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
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
        setActiveStream(soundboardPlayer);
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

    void addTrackToJukeboxQueue(AudioTrack track){
        if (currentlyPlayingSong == null && connectedChannel != null) { //If no song is currently playing and this bot is ready to play music
            currentlyPlayingSong = new AudioKey(track);
            playCurrentSong();
        }
        else
            jukeboxQueueList.addAudioKey(new AudioKey(track));
        if (jukeboxUIWrapper != null)
            jukeboxUIWrapper.refreshQueueList(this);
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
        currentlyPlayingSong = keyToPlay; //This can be null, as can discerned above. This is fine - currentlyPlayingSong being null means no song is being played.
        playCurrentSong();
        if (jukeboxUIWrapper != null)
            jukeboxUIWrapper.refreshQueueList(this);
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
        } else
            songDurationTracker.reset();
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
        if (jukeboxUIWrapper != null)
            jukeboxUIWrapper.refreshQueueList(this);
    }

    public void shuffleJukeboxQueue(){
        jukeboxQueueList.shuffle();
        if (jukeboxUIWrapper != null)
            jukeboxUIWrapper.refreshQueueList(this);
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

    public void saveSoundboard(){
        soundboardList.saveToFile(new File(FileIO.getRootFilePath() + "soundboard.playlist"));
    }

    public void saveJukeboxDefault(){
        if (jukeboxDefaultListIsLocalFile && jukeboxDefaultList != null)
            jukeboxDefaultList.saveToFile(new File(jukeboxDefaultList.getUrl()));
    }

    public void createNewJukeboxPlaylist(JukeboxUIWrapper uiWrapper){
        JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Walnutbot Playlist", "playlist"));
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File file = new File(enforceFileExtension(fileChooser.getSelectedFile().getAbsolutePath()));
            jukeboxDefaultList = new AudioKeyPlaylist(file, false);
            //jukeboxDefaultList.getAudioKeys().clear(); //The Playlist will see that the file DNE and insert an AudioKey in there. This removes that to create a clean, totally-new playlist.
            uiWrapper.refreshDefaultList(this);
            uiWrapper.updateDefaultPlaylistLabel(jukeboxDefaultList.getName());
            Transcriber.printRaw("Current playlist: %1$s", jukeboxDefaultList.toString());
        }
    }

    private String enforceFileExtension(String path){
        String ext = ".playlist";
        int extIndex = path.lastIndexOf('.');
        if (extIndex < 0)
            return path.concat(ext);
        else
            return path.substring(0, extIndex).concat(ext);
    }

    public void openJukeboxPlaylist(JukeboxUIWrapper uiWrapper){
        JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Walnutbot Playlist", "playlist"));
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            AudioKeyPlaylist playlist = new AudioKeyPlaylist(fileChooser.getSelectedFile(), true);
            if (playlist.isURLValid()) {
                loadJukeboxPlaylist(playlist, uiWrapper);
            }
        }
    }

    public void emptyJukeboxPlaylist(JukeboxUIWrapper uiWrapper){
        loadJukeboxPlaylist(null, uiWrapper);
    }

    public void loadJukeBoxPlaylist(AudioKeyPlaylist playlist, boolean jukeboxDefaultListIsLocalFile){
        loadJukeboxPlaylist(playlist, jukeboxUIWrapper);
        this.jukeboxDefaultListIsLocalFile = jukeboxDefaultListIsLocalFile;
    }

    private void loadJukeboxPlaylist(AudioKeyPlaylist playlist, JukeboxUIWrapper uiWrapper){
        jukeboxDefaultList = playlist;
        uiWrapper.refreshDefaultList(this);
        uiWrapper.updateDefaultPlaylistLabel(jukeboxDefaultList.getName());
        if (playlist != null) {
            Transcriber.printRaw("Current playlist: %1$s", jukeboxDefaultList.toString());
            jukeboxDefaultList.addAudioKeyPlaylistListener(jukeboxDefaultListListener);
        }
        else
            Transcriber.printRaw("Playlist set to an empty one.");
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

    public void setMasterVolume(double masterVolume) {
        this.masterVolume = masterVolume;
        updatePlayerVolumes();
    }

    public void setSoundboardVolume(double soundboardVolume) {
        this.soundboardVolume = soundboardVolume;
        updatePlayerVolumes();
    }

    public void setJukeboxVolume(double jukeboxVolume) {
        this.jukeboxVolume = jukeboxVolume;
        updatePlayerVolumes();
    }

    private void updatePlayerVolumes(){
        soundboardPlayer.setVolume((int)(VOLUME_MAX * masterVolume * soundboardVolume));
        jukeboxPlayer.setVolume((int)(VOLUME_MAX * masterVolume * jukeboxVolume));
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

    public AudioKey getCurrentlyPlayingSong() {
        return currentlyPlayingSong;
    }

    public boolean isLoopingCurrentSong() {
        return loopingCurrentSong;
    }

    public void setLoopingCurrentSong(boolean loopingCurrentSong) {
        this.loopingCurrentSong = loopingCurrentSong;
    }

    public void setJukeboxUIWrapper(JukeboxUIWrapper jukeboxUIWrapper) {
        this.jukeboxUIWrapper = jukeboxUIWrapper;
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

    public void setSoundboardUIWrapper(SoundboardUIWrapper soundboardUIWrapper) {
        this.soundboardUIWrapper = soundboardUIWrapper;
    }

    public void addSoundboardSound(AudioKey key){
        soundboardList.addAudioKey(key);
        if (soundboardUIWrapper != null)
            soundboardUIWrapper.updateSoundboardSoundsList(this);
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
        if (removed != null && soundboardUIWrapper != null) {
            soundboardUIWrapper.updateSoundboardSoundsList(this);
            saveSoundboard();
        }
        return removed;
    }

    public boolean modifySoundboardSound(String soundName, AudioKey newData){
        if (soundboardList.modifyAudioKey(soundName, newData)){
            if (soundboardUIWrapper != null)
                soundboardUIWrapper.updateSoundboardSoundsList(this);
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

    private class SoundboardPlayerListener implements PlayerTrackListener{
        @Override public void onTrackStart() {

        }

        @Override public void onTrackStop() {
            resumeJukebox();
        }

        @Override public void onTrackError() {
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
