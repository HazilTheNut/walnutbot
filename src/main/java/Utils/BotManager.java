package Utils;

import java.util.List;

public interface BotManager {

    /**
     * Tells the bot to connect to a particular voice channel of a particular server.
     *
     * @param serverName The name of the server to connect to.
     * @param channelName The name of the channel to connect to.
     * @return True if the server-channel pair exists and the bot has access to it, and false otherwise
     */
    boolean connectToVoiceChannel(String serverName, String channelName);

    void disconnectFromVoiceChannel();

    List<String> getListOfVoiceChannels();

    void updateStatus();

    String getBotName();

}
