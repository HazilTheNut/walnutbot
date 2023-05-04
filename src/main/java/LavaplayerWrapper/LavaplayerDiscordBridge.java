package LavaplayerWrapper;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class LavaplayerDiscordBridge implements ILavaplayerBotBridge{

    private VoiceChannel connectedVoiceChannel;

    @Override
    public boolean setActiveStream(AudioPlayer player) {
        if (connectedVoiceChannel == null)
            return false;
        connectedVoiceChannel.getGuild().getAudioManager().setSendingHandler(new LavaplayerDiscordSendHandler(player));
        return true;
    }

    public VoiceChannel getConnectedVoiceChannel() {
        return connectedVoiceChannel;
    }

    public void setConnectedVoiceChannel(VoiceChannel connectedVoiceChannel) {
        this.connectedVoiceChannel = connectedVoiceChannel;
    }
}
