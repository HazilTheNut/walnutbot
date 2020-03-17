import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class AudioMaster {

    private AudioPlayer soundboardPlayer;

    private VoiceChannel connectedChannel;

    public AudioMaster(){

        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);



    }


    public VoiceChannel getConnectedChannel() {
        return connectedChannel;
    }

    public void setConnectedChannel(VoiceChannel connectedChannel) {
        this.connectedChannel = connectedChannel;
    }


}
