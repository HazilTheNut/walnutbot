package Audio;

import UI.PlayerTrackListener;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.Arrays;

public class GenericTrackScheduler extends AudioEventAdapter {

    private ArrayList<PlayerTrackListener> listeners;

    public GenericTrackScheduler() {
        this.listeners = new ArrayList<>();
    }

    public void addPlayerTrackListener(PlayerTrackListener listener){
        listeners.add(listener);
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        for (PlayerTrackListener listener : listeners)
            listener.onTrackStop();
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        for (PlayerTrackListener listener : listeners)
            listener.onTrackStart();
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        for (PlayerTrackListener listener : listeners)
            listener.onTrackStart();
        Transcriber.print("Track \'%1$s\' starting (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
        // A track started playing
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        for (PlayerTrackListener listener : listeners)
            listener.onTrackStop();
        Transcriber.print("Track \'%1$s\' ended (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
        if (endReason.mayStartNext) {
            // Start next track
        }

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        for (PlayerTrackListener listener : listeners)
            listener.onTrackError();
        Transcriber.print(exception.getMessage());
        exception.printStackTrace();
        // An already playing track threw an exception (track end event will still be received separately)
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        for (PlayerTrackListener listener : listeners)
            listener.onTrackError();
        Transcriber.print("Track \'%1$s\' got stuck (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
        // Audio track has been unable to provide us any audio, might want to just start a new track
    }
}
