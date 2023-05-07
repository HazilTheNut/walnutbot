package LavaplayerWrapper;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

public interface ILavaplayerBotBridge {

    void assignLavaplayerWrapper(LavaplayerWrapper lavaplayerWrapper);

    boolean setActiveStream(AudioPlayer player);

}
