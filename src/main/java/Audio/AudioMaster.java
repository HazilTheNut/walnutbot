package Audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

public class AudioMaster {

    private AudioPlayerManager playerManager;

    private AudioPlayer soundboardPlayer;
    private SoundboardTrackScheduler soundboardTrackScheduler;

    private VoiceChannel connectedChannel;

    public AudioMaster(){

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        soundboardPlayer = playerManager.createPlayer();
        soundboardTrackScheduler = new SoundboardTrackScheduler();
        soundboardPlayer.addListener(soundboardTrackScheduler);
    }

    public void playAudio(String url, AudioPlayer player){
        if (getConnectedChannel() == null) {
            System.out.println("Warning! This bot is currently not connected to any channel!");
            return;
        }
        AudioManager audioManager = connectedChannel.getGuild().getAudioManager();
        audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
        playerManager.loadItem(url, new GenericLoadResultHandler(player));
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

    public void stopAllAudio(){
        //TODO: Add functionality
    }

}
