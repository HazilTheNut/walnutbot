package Audio;

import Utils.FileIO;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;

public class AudioMaster {

    private AudioPlayerManager playerManager;

    private AudioPlayer soundboardPlayer;
    private SoundboardTrackScheduler soundboardTrackScheduler;

    private Playlist soundboardList;

    private VoiceChannel connectedChannel;

    public AudioMaster(){

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        soundboardPlayer = playerManager.createPlayer();
        soundboardTrackScheduler = new SoundboardTrackScheduler();
        soundboardPlayer.addListener(soundboardTrackScheduler);

        soundboardList = new Playlist(new File(FileIO.getRootFilePath() + "soundboard.playlist"));
        soundboardList.printPlaylist();
    }

    public void playSoundboardSound(String url){
        if (getConnectedChannel() == null) {
            System.out.println("Warning! This bot is currently not connected to any channel!");
            return;
        }
        AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
        audioManager.setSendingHandler(new AudioPlayerSendHandler(soundboardPlayer));
        playerManager.loadItem(url, new GenericLoadResultHandler(soundboardPlayer));
    }

    public AudioPlayer getSoundboardPlayer() {
        return soundboardPlayer;
    }

    public VoiceChannel getConnectedChannel() {
        return connectedChannel;
    }

    public void setConnectedChannel(VoiceChannel connectedChannel) {
        this.connectedChannel = connectedChannel;
    }

    public Playlist getSoundboardList() {
        return soundboardList;
    }

    public void saveSoundboard(){
        soundboardList.saveToFile(new File(FileIO.getRootFilePath() + "soundboard.playlist"));
    }

    public void stopAllAudio() {
        soundboardPlayer.setPaused(true);
    }
}
