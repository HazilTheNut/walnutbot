package Audio;

import UI.JukeboxUIWrapper;
import UI.PlayerTrackListener;
import Utils.FileIO;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
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

    public void playSoundboardSound(String url){
        if (getConnectedChannel() == null) {
            Transcriber.print("Warning! This bot is currently not connected to any channel!");
            return;
        }
        jukeboxPlayer.setPaused(true);
        setActiveStream(soundboardPlayer);
        playerManager.loadItem(url, new GenericLoadResultHandler(soundboardPlayer));
    }

    public void queueJukeboxSong(AudioKey audioKey, PostSongRequestAction action){
        if (getConnectedChannel() == null) {
            Transcriber.print("Warning! This bot is currently not connected to any channel!");
            return;
        }
        setActiveStream(jukeboxPlayer);
        playerManager.loadItem(audioKey.getUrl(), new JukeboxLoadResultHandler(this, action));
    }

    void addTrackToJukeboxQueue(AudioTrack track){
        jukeboxQueueList.addAudioKey(new AudioKey(track));
        if (currentlyPlayingSong == null) {//If no song is currently playing
            progressJukeboxQueue(); //Plays the song immediately if the queue was empty.
        }
        else //Since progressJukeboxQueue() should also start a refresh of the UI.
            jukeboxUIWrapper.refreshQueueList(this);
    }

    public void progressJukeboxQueue(){
        //Get the next song to play
        AudioKey keyToPlay = null;
        if (jukeboxQueueList.isEmpty()){
            if (jukeboxDefaultList != null && !jukeboxDefaultList.isEmpty())
                keyToPlay = jukeboxDefaultList.getRandomAudioKey();
        } else {
            keyToPlay = jukeboxQueueList.removeAudioKey(0);
        }
        //If successfully retrieved next song, play it.
        if (keyToPlay != null){
            setActiveStream(jukeboxPlayer);
            if (keyToPlay.getLoadedTrack() != null)
                jukeboxPlayer.startTrack(keyToPlay.getLoadedTrack(), false);
            else
                playerManager.loadItem(keyToPlay.getUrl(), new GenericLoadResultHandler(jukeboxPlayer));
        }
        currentlyPlayingSong = keyToPlay;
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
}
