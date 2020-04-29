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
    private ArrayList<AudioKey> jukeboxQueueList; //The list of requested songs to exhaust through first
    private AudioKeyPlaylist jukeboxDefaultList; //The list of songs to randomly select when the request queue is exhausted

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

        jukeboxQueueList = new ArrayList<>();
    }

    public void playSoundboardSound(String url){
        if (getConnectedChannel() == null) {
            Transcriber.print("Warning! This bot is currently not connected to any channel!");
            return;
        }
        jukeboxPlayer.setPaused(true);
        AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
        audioManager.setSendingHandler(new AudioPlayerSendHandler(soundboardPlayer));
        playerManager.loadItem(url, new GenericLoadResultHandler(soundboardPlayer));
    }

    public void queueJukeboxSong(AudioKey audioKey){
        if (getConnectedChannel() == null) {
            Transcriber.print("Warning! This bot is currently not connected to any channel!");
            return;
        }
        AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
        audioManager.setSendingHandler(new AudioPlayerSendHandler(jukeboxPlayer));
        playerManager.loadItem(audioKey.getUrl(), new JukeboxLoadResultHandler(this));
    }

    void addTrackToJukeboxQueue(AudioTrack track){
        boolean queueIsEmpty = jukeboxQueueList.isEmpty();
        jukeboxQueueList.add(new AudioKey(track));
        if (queueIsEmpty)
            jukeboxPlayer.startTrack(track, false);
    }

    void progressJukeboxQueue(){
        if (jukeboxQueueList.isEmpty())
            return;
        jukeboxQueueList.remove(0);
        jukeboxPlayer.startTrack(jukeboxQueueList.get(0).getLoadedTrack(), false);
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

    public ArrayList<AudioKey> getJukeboxQueueList() {
        return jukeboxQueueList;
    }

    public AudioKeyPlaylist getJukeboxDefaultList() {
        return jukeboxDefaultList;
    }

    private class SoundboardPlayerListener implements PlayerTrackListener{
        @Override public void onTrackStart() {

        }

        @Override public void onTrackStop() {
            AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
            audioManager.setSendingHandler(new AudioPlayerSendHandler(jukeboxPlayer));
            jukeboxPlayer.setPaused(false);
        }

        @Override public void onTrackError() {
            AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
            audioManager.setSendingHandler(new AudioPlayerSendHandler(jukeboxPlayer));
            jukeboxPlayer.setPaused(false);
        }
    }
}
