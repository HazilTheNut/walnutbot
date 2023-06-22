package LavaplayerWrapper;

import CommuncationPlatform.IDiscordPlaybackSystemBridge;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class LavaplayerDiscordBridge implements ILavaplayerBotBridge, IDiscordPlaybackSystemBridge {

    private VoiceChannel connectedVoiceChannel;
    private AudioPlayer activeStream;
    private LavaplayerWrapper lavaplayerWrapper;

    @Override
    public void assignLavaplayerWrapper(LavaplayerWrapper lavaplayerWrapper) {
        this.lavaplayerWrapper = lavaplayerWrapper;
    }

    @Override
    public boolean setActiveStream(AudioPlayer player) {
        if (connectedVoiceChannel == null)
            return false;
        if (activeStream != player)
            connectedVoiceChannel.getGuild().getAudioManager().setSendingHandler(new LavaplayerDiscordSendHandler(player));
        activeStream = player;
        return true;
    }

    public void setConnectedVoiceChannel(VoiceChannel connectedVoiceChannel) {
        this.connectedVoiceChannel = connectedVoiceChannel;
        lavaplayerWrapper.setConnectedToVoiceChannel(true);
        // Reestablish audio streaming with new voice channel
        if (activeStream != null)
            setActiveStream(activeStream);
    }

    @Override
    public VoiceChannel getConnectedVoiceChannel() {
        return connectedVoiceChannel;
    }

    @Override
    public void onDisconnect() {
        activeStream = null;
        lavaplayerWrapper.setConnectedToVoiceChannel(false);
    }
}
