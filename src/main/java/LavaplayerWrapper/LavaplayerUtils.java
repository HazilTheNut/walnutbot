package LavaplayerWrapper;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class LavaplayerUtils {

    static String printAuthorAndTitle(AudioTrack track) {
        return String.format("%s - %s", track.getInfo().author, track.getInfo().title);
    }
}
