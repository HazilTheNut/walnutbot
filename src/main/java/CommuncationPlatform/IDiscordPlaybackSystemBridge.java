package CommuncationPlatform;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public interface IDiscordPlaybackSystemBridge {

    void setConnectedVoiceChannel(VoiceChannel voiceChannel);

    VoiceChannel getConnectedVoiceChannel();

    void onDisconnect();

}
