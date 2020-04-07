package Audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class JukeboxLoadResultHandler implements AudioLoadResultHandler {

    AudioMaster audioMaster;

    public JukeboxLoadResultHandler(AudioMaster audioMaster) {
        this.audioMaster = audioMaster;
    }

    /**
     * Called when the requested item is a track and it was successfully loaded.
     *
     * @param track The loaded track
     */
    @Override public void trackLoaded(AudioTrack track) {
        audioMaster.addTrackToJukeboxQueue(track);
    }

    /**
     * Called when the requested item is a playlist and it was successfully loaded.
     *
     * @param playlist The loaded playlist
     */
    @Override public void playlistLoaded(AudioPlaylist playlist) {
        for (AudioTrack track : playlist.getTracks())
            audioMaster.addTrackToJukeboxQueue(track);
    }

    /**
     * Called when there were no items found by the specified identifier.
     */
    @Override public void noMatches() {

    }

    /**
     * Called when loading an item failed with an exception.
     *
     * @param exception The exception that was thrown
     */
    @Override public void loadFailed(FriendlyException exception) {

    }
}
