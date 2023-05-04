package Audio;

import UI.SongDurationTracker;

public interface IAudioStateMachine {

    enum AudioStateMachineStatus {
        INACTIVE,                               // Neither stream is playing
        JUKEBOX_PLAYING,                        // Jukebox is not paused and is playing music
        JUKEBOX_PAUSED,                         // Jukebox has a song currently active but is paused
        SOUNDBOARD_PLAYING,                     // Soundboard is playing something and jukebox is inactive
        SOUNDBOARD_PLAYING_JUKEBOX_READY,       // Soundboard is playing something and jukebox started a new song while the sound is playing
        SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED,   // Soundboard is playing something and jukebox was playing a song when the sound played
        SOUNDBOARD_PLAYING_JUKEBOX_PAUSED       // Soundboard is playing something and jukebox is paused
    }

    enum CurrentActiveStreamState {
        NEITHER,
        JUKEBOX_ACTIVE,
        SOUNDBOARD_ACTIVE
    }

    // State machine operation
    /**
     * Should be a pass-through to an IPlaybackWrapper loadTracks() useful for retrieving tracks from local/internet
     *  to instant-play soundboard sounds, import tracks into jukebox default list, or enqueue jukebox songs
     *
     * @param uri The URI to load the track from
     * @param output AudioKeyPlaylist to populate with load results
     * @param trackLoadResultHandler Handles what occurs after the AudioKeyPlaylist is populated
     * @param storeLoadedTrackObjects Whether to store the loaded track object on each generated AudioKey
     * @return true if the operation was successful, and false otherwise
     */
    boolean loadTracks(String uri, AudioKeyPlaylist output, ITrackLoadResultHandler trackLoadResultHandler, boolean storeLoadedTrackObjects);

    /**
     * Interrupts jukebox if it was playing and plays a sound
     *
     * @param key Sound to play
     */
    void playSoundboardSound(AudioKey key);

    /**
     * Stops the soundboard playback if it was playing and returns playback to jukebox if it was playing
     */
    void stopSoundboard();

    /**
     * Enqueues an AudioKey onto the jukebox queue
     *
     * @param key Song to play
     */
    void enqueueJukeboxSong(AudioKey key);

    /**
     * Starts the next song in the jukebox queue. If the queue is empty, it pulls at random from the jukebox default list
     * If queue and default list are both empty, jukebox stops playing music.
     *
     * @param ignoreLooping Ignore the looping status and play a new song
     */
    void startNextJukeboxSong(boolean ignoreLooping);

    /**
     * Pause the jukebox
     */
    void pauseJukebox();

    /**
     * Resume the jukebox if a song is loaded
     */
    void resumeJukebox();

    // State machine information

    /**
     * Gets the current status of the state machine
     *
     * @return the current status of the state machine
     */
    AudioStateMachineStatus getCurrentStatus();

    /**
     * Gets the current status of which stream (jukebox or soundboard) is active
     *
     * @return the current status of which stream is active
     */
    CurrentActiveStreamState getCurrentActiveStream();

    // AudioKeyPlaylists

    /**
     * Blocking access to this IAudioStateMachine's AudioKeyPlaylist of soundboard sounds.
     *
     * @param accessHandle What is to be done when access is acquired. After this is run, all listeners
     *                     to the AudioKeyPlaylist are notified if any changes are made.
     */
    void accessSoundboardSoundsList(IAudioKeyPlaylistAccessHandle accessHandle);

    /**
     * Blocking access to this IAudioStateMachine's AudioKeyPlaylist of the jukebox default list.
     *
     * @param accessHandle What is to be done when access is acquired. After this is run, all listeners
     *                     to the AudioKeyPlaylist are notified if any changes are made.
     */
    void accessJukeboxDefaultList(IAudioKeyPlaylistAccessHandle accessHandle);

    /**
     * Blocking access to this IAudioStateMachine's AudioKeyPlaylist of the jukebox queue.
     *
     * @param accessHandle What is to be done when access is acquired. After this is run, all listeners
     *                     to the AudioKeyPlaylist are notified if any changes are made.
     */
    void accessJukeboxQueue(IAudioKeyPlaylistAccessHandle accessHandle);

    /**
     * Gets the currently playing song in the jukebox
     *
     * @return The currently playing song in the jukebox
     */
    AudioKey getJukeboxCurrentlyPlayingSong();

    // Volume control

    /**
     * Gets the main volume which controls the volume of both playback streams
     *
     * @return the main volume which controls the volume of both playback streams
     */
    int getMainVolume();

    /**
     * Gets the volume of the jukebox playback stream
     *
     * @return the volume of the jukebox playback stream
     */
    int getJukeboxVolume();

    /**
     * Gets the volume of the soundboard playback stream
     *
     * @return the volume of the soundboard playback stream
     */
    int getSoundboardVolume();

    /**
     * Sets the main volume which controls the volume of both playback streams
     *
     * @param volume The volume to set the main volume to.
     * @return true if the operation was successful, and false otherwise
     */
    boolean setMainVolume(int volume);

    /**
     * Sets the volume of the jukebox playback stream
     *
     * @param volume The volume to set the jukebox volume to.
     * @return true if the operation was successful, and false otherwise
     */
    boolean setJukeboxVolume(int volume);

    /**
     * Sets the volume of the soundboard playback stream
     *
     * @param volume The volume to set the soundboard volume to.
     * @return true if the operation was successful, and false otherwise
     */
    boolean setSoundboardVolume(int volume);

    /**
     * Adds a VolumeChangeListener to monitor this IAudioStateMachine when its volume settings change
     *
     * @param volumeChangeListener VolumeChangeListener to listen to this IAudioStateMachine
     */
    void addVolumeChangeListener(VolumeChangeListener volumeChangeListener);

    // Looping current playing song

    /**
     * Returns whether the jukebox is looping the currently playing song
     *
     * @return whether the jukebox is looping the currently playing song
     */
    boolean getLoopingStatus();

    /**
     * Sets whether the jukebox should loop the currently playing song
     *
     * @param looping whether the jukebox should loop the currently playing song
     */
    void setLoopingStatus(boolean looping);

    // Song duration tracker

    /**
     * Adds a VolumeChangeListener to monitor the duration of jukebox songs played through this IAudioStateMachine
     *
     * @param songDurationTracker VolumeChangeListener to monitor the duration of jukebox songs played through this IAudioStateMachine
     */
    void addSongDurationTracker(SongDurationTracker songDurationTracker);

    /**
     * Notifies all song duration trackers that a jukebox song has begun
     *
     * @param durationMilliseconds Duration of song in milliseconds
     */
    void songDurationTrackersNotifySongBegun(long durationMilliseconds);

    /**
     * Notifies all song duration trackers that a jukebox song has paused
     */
    void songDurationTrackersNotifySongPause();

    /**
     * Notifies all song duration trackers that a jukebox song has resumed
     */
    void songDurationTrackersNotifySongResume();

    /**
     * Notifies all song duration trackers that a jukebox song has ended
     */
    void songDurationTrackersNotifySongEnd();
}
