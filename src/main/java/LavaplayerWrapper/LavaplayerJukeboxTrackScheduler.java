package LavaplayerWrapper;

import Audio.IAudioStateMachine;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class LavaplayerJukeboxTrackScheduler extends AudioEventAdapter {

    private final IAudioStateMachine audioStateMachine;

    public LavaplayerJukeboxTrackScheduler(IAudioStateMachine audioStateMachine) {
        this.audioStateMachine = audioStateMachine;
    }

    public void onPlayerPause(AudioPlayer player) {
        audioStateMachine.songDurationTrackersNotifySongPause();
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     */
    public void onPlayerResume(AudioPlayer player) {
        audioStateMachine.songDurationTrackersNotifySongResume();
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track that started
     */
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Transcriber.printTimestamped("Jukebox track '%s' (%s) started", LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
        audioStateMachine.songDurationTrackersNotifySongBegun(track.getDuration());
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track that ended
     * @param endReason The reason why the track stopped playing
     */
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Transcriber.printTimestamped("Jukebox track '%s' (%s) ended", LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
        audioStateMachine.songDurationTrackersNotifySongEnd();
        if (endReason.mayStartNext)
            audioStateMachine.startNextJukeboxSong(false);
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track where the exception occurred
     * @param exception The exception that occurred
     */
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Transcriber.printTimestamped("Jukebox track '%s' (%s) hit an exception!", LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
        exception.printStackTrace();
        audioStateMachine.songDurationTrackersNotifySongEnd();
        audioStateMachine.startNextJukeboxSong(true);
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track where the exception occurred
     * @param thresholdMs The wait threshold that was exceeded for this event to trigger
     */
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        Transcriber.printTimestamped("Jukebox track '%s' (%s) got stuck!", LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
        audioStateMachine.songDurationTrackersNotifySongEnd();
        audioStateMachine.startNextJukeboxSong(true);
        // Adapter dummy method
    }
}
