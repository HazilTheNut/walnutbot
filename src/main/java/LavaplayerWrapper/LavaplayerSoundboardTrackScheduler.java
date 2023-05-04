package LavaplayerWrapper;

import Audio.IAudioStateMachine;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class LavaplayerSoundboardTrackScheduler extends AudioEventAdapter {

    private final IAudioStateMachine audioStateMachine;

    public LavaplayerSoundboardTrackScheduler(IAudioStateMachine audioStateMachine) {
        this.audioStateMachine = audioStateMachine;
    }

    public void onPlayerPause(AudioPlayer player) {
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     */
    public void onPlayerResume(AudioPlayer player) {
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track that started
     */
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Transcriber.printTimestamped("Soundboard track '%s' (%s) started", LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track that ended
     * @param endReason The reason why the track stopped playing
     */
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Transcriber.printTimestamped("Soundboard track '%s' (%s) ended", LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
        audioStateMachine.stopSoundboard();
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track where the exception occurred
     * @param exception The exception that occurred
     */
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Transcriber.printTimestamped("Soundboard track '%s' (%s) hit an exception!", LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
        exception.printStackTrace();
        audioStateMachine.stopSoundboard();
        // Adapter dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track where the exception occurred
     * @param thresholdMs The wait threshold that was exceeded for this event to trigger
     */
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        Transcriber.printTimestamped("Soundboard track '%s' (%s) got stuck!", LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
        audioStateMachine.stopSoundboard();
        // Adapter dummy method
    }
}
