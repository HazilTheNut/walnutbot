package Audio;

import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class JukeboxTrackScheduler extends AudioEventAdapter {

    private AudioMaster audioMaster;

    public JukeboxTrackScheduler(AudioMaster audioMaster) {
        this.audioMaster = audioMaster;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        if (audioMaster.getSongDurationTracker() != null)
            audioMaster.getSongDurationTracker().onSongPause();
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        if (audioMaster.getSongDurationTracker() != null)
            audioMaster.getSongDurationTracker().onSongResume();
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Transcriber.printTimestamped("Track \'%1$s\' starting (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
        if (audioMaster.getSongDurationTracker() != null)
            audioMaster.getSongDurationTracker().onSongStart(track.getDuration(), track.getInfo().title);
        // A track started playing
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Transcriber.printTimestamped("Track \'%1$s\' ended (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
        if (endReason.mayStartNext) {
            audioMaster.jukeboxSkipToNextSong();
        }
        if (audioMaster.getSongDurationTracker() != null)
            audioMaster.getSongDurationTracker().onSongEnd();

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
        Transcriber.printTimestamped(exception.getMessage());
        exception.printStackTrace();
        if (audioMaster.getSongDurationTracker() != null)
            audioMaster.getSongDurationTracker().onSongEnd();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        Transcriber.printTimestamped("Track \'%1$s\' got stuck (Path: %2$s)", track.getInfo().title, track.getInfo().uri);
        // Audio track has been unable to provide us any audio, might want to just start a new track
        audioMaster.jukeboxSkipToNextSong();
        if (audioMaster.getSongDurationTracker() != null)
            audioMaster.getSongDurationTracker().onSongEnd();
    }
}
