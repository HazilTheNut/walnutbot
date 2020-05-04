package Audio;

import UI.JukeboxUIWrapper;
import UI.PlayerTrackListener;
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

public class AudioMaster{

    private AudioPlayerManager playerManager;

    //Soundboard
    private AudioPlayer soundboardPlayer;
    private GenericTrackScheduler genericTrackScheduler;
    private AudioKeyPlaylist soundboardList;

    private VoiceChannel connectedChannel;

    //Jukebox
    private AudioPlayer jukeboxPlayer;
    private JukeboxTrackScheduler jukeboxTrackScheduler;
    private AudioKeyPlaylist jukeboxQueueList; //The list of requested songs to exhaust through first
    private AudioKeyPlaylist jukeboxDefaultList; //The list of songs to randomly select when the request queue is exhausted
    private AudioKey currentlyPlayingSong;
    private JukeboxUIWrapper jukeboxUIWrapper;
    private boolean loopingCurrentSong = false;

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

        soundboardList = new AudioKeyPlaylist(new File(FileIO.getRootFilePath() + "soundboard.playlist"));
        soundboardList.printPlaylist();

        masterVolume = VOLUME_DEFAULT;
        soundboardVolume = VOLUME_DEFAULT;
        jukeboxVolume = VOLUME_DEFAULT;

        jukeboxPlayer = playerManager.createPlayer();
        jukeboxTrackScheduler = new JukeboxTrackScheduler(this);
        jukeboxPlayer.addListener(jukeboxTrackScheduler);

        jukeboxQueueList = new AudioKeyPlaylist("queue");
    }

    private void setActiveStream(AudioPlayer player){
        AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
        audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
    }

    public void resumeJukebox(){
        soundboardPlayer.stopTrack();
        setActiveStream(jukeboxPlayer);
        jukeboxPlayer.setPaused(false);
    }

    public void playSoundboardSound(String url){
        if (getConnectedChannel() == null) {
            Transcriber.print("Warning! This bot is currently not connected to any channel!");
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

    public void queueJukeboxSong(AudioKey audioKey, PostSongRequestAction ifSuccess, PostSongRequestAction ifError){
        //Set up audio stream
        if (getConnectedChannel() == null)
            Transcriber.print("Warning! This bot is currently not connected to any channel!");
        else
            setActiveStream(jukeboxPlayer);
        //Fetch song and queue it up
        if (audioKey.getLoadedTrack() == null)
            playerManager.loadItem(audioKey.getUrl(), new JukeboxQueueResultHandler(jukeboxPlayer, ifSuccess, ifError));
        else {
            addTrackToJukeboxQueue(audioKey.getLoadedTrack());
            ifSuccess.doAction();
        }
    }

    void addTrackToJukeboxQueue(AudioTrack track){
        if (currentlyPlayingSong == null && connectedChannel != null) { //If no song is currently playing and this bot is ready to play music
            currentlyPlayingSong = new AudioKey(track);
            playCurrentSong();
        }
        else
            jukeboxQueueList.addAudioKey(new AudioKey(track));
        jukeboxUIWrapper.refreshQueueList(this);
    }

    public void jukeboxSkipToNextSong() { jukeboxSkipToNextSong(false); }

    public void jukeboxSkipToNextSong(boolean forceskip){
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
        currentlyPlayingSong = keyToPlay;
        playCurrentSong();
        jukeboxUIWrapper.refreshQueueList(this);
    }
    
    private void playCurrentSong(){
        jukeboxPlayer.setPaused(true);
        if (currentlyPlayingSong != null){
            setActiveStream(jukeboxPlayer);
            if (currentlyPlayingSong.getLoadedTrack() != null)
                jukeboxPlayer.startTrack(currentlyPlayingSong.getLoadedTrack(), false);
            else
                playerManager.loadItem(currentlyPlayingSong.getUrl(), new JukeboxPlayResultHandler(jukeboxPlayer, () -> {}, () -> {
                    Transcriber.print("WARNING: Song url \"%1$s\" is invalid!", currentlyPlayingSong.getUrl());
                    jukeboxSkipToNextSong();
                }));
        }
        jukeboxPlayer.setPaused(false);
    }

    public void clearJukeboxQueue(){
        jukeboxQueueList.getAudioKeys().clear();
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
        if (jukeboxDefaultList != null)
            jukeboxDefaultList.saveToFile(new File(jukeboxDefaultList.getUrl()));
    }

    public void createNewJukeboxPlaylist(JukeboxUIWrapper uiWrapper){
        JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Walnutbot Playlist", "playlist"));
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File file = new File(enforceFileExtension(fileChooser.getSelectedFile().getAbsolutePath()));
            jukeboxDefaultList = new AudioKeyPlaylist(file);
            uiWrapper.refreshDefaultList(this);
            uiWrapper.updateDefaultPlaylistLabel(jukeboxDefaultList.getName());
            Transcriber.print("Current playlist: %1$s", jukeboxDefaultList.toString());
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
            AudioKeyPlaylist playlist = new AudioKeyPlaylist(fileChooser.getSelectedFile());
            if (playlist.isURLValid()) {
                jukeboxDefaultList = playlist;
                uiWrapper.refreshDefaultList(this);
                uiWrapper.updateDefaultPlaylistLabel(jukeboxDefaultList.getName());
                Transcriber.print("Current playlist: %1$s", jukeboxDefaultList.toString());
            }
        }
    }

    public void stopAllAudio() {
        soundboardPlayer.setPaused(true);
        jukeboxPlayer.setPaused(true);
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

    private class SoundboardPlayerListener implements PlayerTrackListener{
        @Override public void onTrackStart() {

        }

        @Override public void onTrackStop() {
            setActiveStream(jukeboxPlayer);
            jukeboxPlayer.setPaused(false);
        }

        @Override public void onTrackError() {
            setActiveStream(jukeboxPlayer);
            jukeboxPlayer.setPaused(false);
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
        }

        @Override public void playlistLoaded(AudioPlaylist playlist) {
            for (AudioTrack track : playlist.getTracks())
                addTrackToJukeboxQueue(track);
            super.playlistLoaded(playlist);
        }
    }

    private class JukeboxPlayResultHandler extends JukeboxLoadResultHandler {

        public JukeboxPlayResultHandler(AudioPlayer audioPlayer, PostSongRequestAction ifSuccess,
            PostSongRequestAction ifError) {
            super(audioPlayer, ifSuccess, ifError);
        }

        @Override public void trackLoaded(AudioTrack track) {
            Transcriber.print("Track \'%1$s\' loaded! (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
            currentlyPlayingSong = new AudioKey(track);
            if (!audioPlayer.startTrack(track, false))
                Transcriber.print("Track \'%1$s\' failed to start (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
            super.trackLoaded(track);
        }
    }
}
