package Audio;

import java.util.List;

public interface IPlaybackWrapper {


    enum PlaybackStreamType {
        SOUNDBOARD,
        JUKEBOX,
        // Only use BOTH for stuff like the main volume. Fails for many operations such as starting or stopping tracks
        BOTH
    }

    void assignAudioStateMachine (IAudioStateMachine stateMachine);

    /**
     * Given a URI (can be local file or web address), populates an AudioKeyPlaylist with the tracks found at that location.
     * The URI can point to an audio file (.mp3, .mp4, .wav, etc.) or a web address such as <a href="https://www.bandcamp.com">...</a>.
     * The URI cannot be a Walnutbot playlist file (.playlist, .wbp)
     * This is a nonblocking operation and spins up its own thread.
     *
     * @param source                  The URI to load the track from
     * @param output                  AudioKeyPlaylist to populate with load results
     * @param storeLoadedTrackObjects Whether to store the loaded track object on each generated AudioKey
     * @param loadResultHandler       Handles what occurs after the AudioKeyPlaylist is populated
     */
    void loadItem(AudioKey source, List<AudioKey> output, LoadJobSettings storeLoadedTrackObjects, IPlaybackWrapperLoadResultHandler loadResultHandler);

    /**
     * Sets which stream of audio, either the Soundboard or the Jukebox, should be playing through the bot.
     *
     * @param playbackStreamType Which stream of audio to set as the active stream
     * @return true if the operation was successful, and false otherwise
     */
    boolean setActiveStream(PlaybackStreamType playbackStreamType);

    /**
     * Resets the loaded track on the AudioKey to be played back again, returning a new AudioKey.
     *
     * @param key AudioKey to refresh
     * @return A refreshed version of the input AudioKey able to start playback from the beginning.
     */
    AudioKey refreshAudioKey(AudioKey key);

    /**
     * Starts playback of a loaded track, which can later be paused or ended.
     *
     * @param playbackStreamType Which playback stream to start playing a new track
     * @return true if the operation was successful, and false otherwise
     */
    boolean startPlayback(PlaybackStreamType playbackStreamType, Object loadedTrackObject);

    /**
     * Ends playback, which cannot later be resumed.
     *
     * @param playbackStreamType Which playback stream to end its current track
     * @return true if the operation was successful, and false otherwise
     */
    boolean endPlayback(PlaybackStreamType playbackStreamType);

    /**
     * Pauses playback, which can later be resumed or ended.
     *
     * @param playbackStreamType Which playback stream to pause its current track
     * @return true if the operation was successful, and false otherwise
     */
    boolean pausePlayback(PlaybackStreamType playbackStreamType);

    /**
     * Resumes playback, which can later be paused or ended.
     *
     * @param playbackStreamType Which playback stream to pause its current track
     * @return true if the operation was successful, and false otherwise
     */
    boolean resumePlayback(PlaybackStreamType playbackStreamType);

    /**
     * Sets volume of a playback stream.
     *
     * @param playbackStreamType Which playback stream to modify its volume
     * @param volume 0-100 scale volume
     * @return true if the operation was successful, and false otherwise
     */
    boolean setVolume(PlaybackStreamType playbackStreamType, int volume);

    /**
     * Gest the volume of a playback stream, or the main volume that controls both.
     *
     * @param playbackStreamType Which playback stream to read its volume
     * @return The volume level of the specified playback stream
     */
    int getVolume(PlaybackStreamType playbackStreamType);

    boolean isProcessingLoadRequests();

    boolean isDisconnectedFromVoiceChannel();
}