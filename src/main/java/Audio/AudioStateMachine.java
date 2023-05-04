package Audio;

import UI.SongDurationTracker;
import Utils.Transcriber;

public class AudioStateMachine implements IAudioStateMachine {

    private IPlaybackWrapper playbackWrapper;

    private AudioKeyPlaylist soundboardList;
    private AudioKeyPlaylist jukeboxDefaultList;
    private AudioKeyPlaylist jukeboxQueueList;
    private AudioKey jukeboxCurrentlyPlayingSong;

    private AudioStateMachineStatus myCurrentStatus;
    private CurrentActiveStreamState currentActiveStreamState;
    private boolean loopingJukebox;

    private AudioStateMachine(IPlaybackWrapper playbackWrapper) {
        this.playbackWrapper = playbackWrapper;

        soundboardList = new AudioKeyPlaylist("SOUNDBOARD");
        jukeboxDefaultList = new AudioKeyPlaylist("JUKEBOX DFL");
        jukeboxQueueList = new AudioKeyPlaylist("JUKEBOX QUEUE");

        myCurrentStatus = AudioStateMachineStatus.INACTIVE;
        currentActiveStreamState = CurrentActiveStreamState.NEITHER;

        loopingJukebox = false;
    }

    /**
     * Should be a pass-through to an IPlaybackWrapper loadTracks() useful for retrieving tracks from local/internet
     * to instant-play soundboard sounds, import tracks into jukebox default list, or enqueue jukebox songs
     *
     * @param uri                     The URI to load the track from
     * @param output                  AudioKeyPlaylist to populate with load results
     * @param trackLoadResultHandler  Handles what occurs after the AudioKeyPlaylist is populated
     * @param storeLoadedTrackObjects Whether to store the loaded track object on each generated AudioKey
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean loadTracks(String uri, AudioKeyPlaylist output, ITrackLoadResultHandler trackLoadResultHandler, boolean storeLoadedTrackObjects) {
        return playbackWrapper.loadTracks(uri, output, trackLoadResultHandler, storeLoadedTrackObjects);
    }

    /**
     * Interrupts jukebox if it was playing and plays a sound
     *
     * @param key Sound to play
     */
    @Override
    public void playSoundboardSound(AudioKey key) {
        // Play sound
        if (key.getAbstractedLoadedTrack() == null) {
            // We need to load up the track
            AudioKeyPlaylist temp = new AudioKeyPlaylist("TEMP");
            if (!playbackWrapper.loadTracks(key.getUrl(), temp, (output, successful) -> {
                if (successful) {
                    // Recurse through playSoundboardSound with an AudioKey with a track loaded onto it
                    playSoundboardSound(output.getKey(0));
                } else {
                    Transcriber.printTimestamped("Loading for key %s failed!", key);
                }
            }, true)) {
                Transcriber.printTimestamped("Loading for key %s failed!", key);
            }
        } else {
            // If track is loaded play it
            switch (myCurrentStatus) {
                case JUKEBOX_PLAYING:
                    playbackWrapper.pausePlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                    setCurrentState(AudioStateMachineStatus.SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED);
                    break;
                case JUKEBOX_PAUSED:
                    setCurrentState(AudioStateMachineStatus.SOUNDBOARD_PLAYING_JUKEBOX_PAUSED);
                    break;
                case SOUNDBOARD_PLAYING:
                case SOUNDBOARD_PLAYING_JUKEBOX_READY:
                case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                case SOUNDBOARD_PLAYING_JUKEBOX_PAUSED:
                    // Don't care about current playing soundboard sound. End it and start playing the new one.
                    playbackWrapper.endPlayback(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD);
                    // Retain current state as it allows it exit state back to jukebox
                    break;
                case INACTIVE:
                    setCurrentState(AudioStateMachineStatus.SOUNDBOARD_PLAYING);
                    break;
                default:
                    // Caught in a bad state
                    Transcriber.printTimestamped("AudioStateMachine fell into a bad state!");
                    resetStateMachine();
                    return;
            }
            // Update state machine
            changeActiveStream(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD);
            // Play sound
            if (!playbackWrapper.startPlayback(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD, key.getAbstractedLoadedTrack())) {
                Transcriber.printTimestamped("Playback for key %s failed!", key);
                stopSoundboard();
            }
        }
        // Returning playback to jukebox is not handled here (stopSoundboard() will eventually be called)
    }

    /**
     * Stops the soundboard playback if it was playing and returns playback to jukebox if it was playing
     */
    @Override
    public void stopSoundboard() {
        // Stop playback
        playbackWrapper.endPlayback(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD);
        switch (myCurrentStatus) {
            case INACTIVE:
            case JUKEBOX_PLAYING:
            case JUKEBOX_PAUSED:
                // Do nothing
                break;
            case SOUNDBOARD_PLAYING:
                setCurrentState(AudioStateMachineStatus.INACTIVE);
            case SOUNDBOARD_PLAYING_JUKEBOX_READY:
                // Jukebox ready to begin playback
                changeActiveStream(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                if (playbackWrapper.startPlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX, jukeboxCurrentlyPlayingSong.getAbstractedLoadedTrack())) {
                    // Update state machine
                    setCurrentState(AudioStateMachineStatus.JUKEBOX_PLAYING);
                } else {
                    // Unsuccessfully resumed
                    Transcriber.printTimestamped("Could not start playback of jukebox after completion of soundboard sound!");
                    setCurrentState(AudioStateMachineStatus.INACTIVE);
                    return;
                }
                break;
            case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                // Jukebox to resume playback
                changeActiveStream(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                if (playbackWrapper.resumePlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX)){
                    // Update state machine
                    setCurrentState(AudioStateMachineStatus.JUKEBOX_PLAYING);
                } else {
                    // Unsuccessfully resumed
                    Transcriber.printTimestamped("Could not resume playback of jukebox after completion of soundboard sound!");
                    setCurrentState(AudioStateMachineStatus.INACTIVE);
                    return;
                }
                break;
            case SOUNDBOARD_PLAYING_JUKEBOX_PAUSED:
                changeActiveStream(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                setCurrentState(AudioStateMachineStatus.JUKEBOX_PAUSED);
                break;
        }
    }

    /**
     * Enqueues an AudioKey onto the jukebox queue
     *
     * @param key Song to play
     */
    @Override
    public void enqueueJukeboxSong(AudioKey key) {
        // Populate the queue with tracks loaded from key
        if (key.getAbstractedLoadedTrack() == null) {
            // Need to load the track(s)
            AudioKeyPlaylist temp = new AudioKeyPlaylist("TEMP");
            if (!playbackWrapper.loadTracks(key.getUrl(), temp, (output, successful) -> {
                if (successful) {
                    for (AudioKey outputKey : output.getAudioKeys()) {
                        if (outputKey.getAbstractedLoadedTrack() != null)
                            enqueueJukeboxSong(outputKey);
                    }
                } else {
                    Transcriber.printTimestamped("Loading for key %s failed!", key);
                }
            }, true)) {
                Transcriber.printTimestamped("Loading for key %s failed!", key);
            }
        } else {
            switch (myCurrentStatus) {
                case INACTIVE:
                    // Start playing music
                    setJukeboxCurrentlyPlayingSong(key);
                    changeActiveStream(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                    if (playbackWrapper.startPlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX, jukeboxCurrentlyPlayingSong.getAbstractedLoadedTrack())) {
                        // Update state machine
                        setCurrentState(AudioStateMachineStatus.JUKEBOX_PLAYING);
                    } else {
                        Transcriber.printTimestamped("Playback for key %s failed!", jukeboxCurrentlyPlayingSong);
                    }
                    break;
                case JUKEBOX_PLAYING:
                case JUKEBOX_PAUSED:
                case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                case SOUNDBOARD_PLAYING_JUKEBOX_READY:
                case SOUNDBOARD_PLAYING_JUKEBOX_PAUSED:
                    // Jukebox already has a song, queue it up
                    jukeboxQueueList.addAudioKey(key);
                    break;
                case SOUNDBOARD_PLAYING:
                    // Soundboard is playing, get ready to play when it is finished
                    setJukeboxCurrentlyPlayingSong(key);
                    setCurrentState(AudioStateMachineStatus.SOUNDBOARD_PLAYING_JUKEBOX_READY);
                    break;
                default:
                    // Caught in a bad state
                    Transcriber.printTimestamped("AudioStateMachine fell into a bad state!");
                    resetStateMachine();
            }
        }
    }

    /**
     * Starts the next song in the jukebox queue. If the queue is empty, it pulls at random from the jukebox default list.
     * If queue and default list are both empty, jukebox stops playing music.
     *
     * @param ignoreLooping Ignore the looping status and play a new song
     */
    @Override
    public void startNextJukeboxSong(boolean ignoreLooping) {
        // End playback
        playbackWrapper.endPlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);

        // Get next song
        if (!ignoreLooping && loopingJukebox) {
            // Loop current song if looping
            setJukeboxCurrentlyPlayingSong(playbackWrapper.refreshAudioKey(jukeboxCurrentlyPlayingSong));
        } else if (!jukeboxQueueList.isEmpty()) {
            // If something is in the queue, dequeue it
            setJukeboxCurrentlyPlayingSong(jukeboxQueueList.removeAudioKey(0));
        } else if (!jukeboxDefaultList.isEmpty()) {
            // If default list is nonempty, get a random song
            setJukeboxCurrentlyPlayingSong(jukeboxDefaultList.getRandomAudioKey());
        } else {
            // Nothing to play, queue and default list are both empty
            setJukeboxCurrentlyPlayingSong(null);
            // Update state machine
            switch (myCurrentStatus) {
                case JUKEBOX_PLAYING:
                case JUKEBOX_PAUSED:
                    setCurrentState(AudioStateMachineStatus.INACTIVE);
                    break;
                case SOUNDBOARD_PLAYING:
                case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                case SOUNDBOARD_PLAYING_JUKEBOX_READY:
                    // Nothing to play when soundboard finishes
                    setCurrentState(AudioStateMachineStatus.SOUNDBOARD_PLAYING);
                    break;
                case INACTIVE:
                    // Do nothing
                    break;
                default:
                    // Caught in a bad state
                    Transcriber.printTimestamped("AudioStateMachine fell into a bad state!");
                    resetStateMachine();
                    break;
            }
        }

        if (jukeboxCurrentlyPlayingSong != null) {
            // Begin playing song
            switch (myCurrentStatus) {
                case INACTIVE:
                case JUKEBOX_PLAYING:
                case JUKEBOX_PAUSED:
                    // Begin playback
                    changeActiveStream(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                    if (playbackWrapper.startPlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX, jukeboxCurrentlyPlayingSong.getAbstractedLoadedTrack())) {
                        // Update state machine
                        setCurrentState(AudioStateMachineStatus.JUKEBOX_PLAYING);
                    } else {
                        Transcriber.printTimestamped("Playback for key %s failed!", jukeboxCurrentlyPlayingSong);
                    }
                    break;
                case SOUNDBOARD_PLAYING:
                case SOUNDBOARD_PLAYING_JUKEBOX_READY:
                case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                    // Ready the next song
                    setCurrentState(AudioStateMachineStatus.SOUNDBOARD_PLAYING_JUKEBOX_READY);
                    break;
                default:
                    // Caught in a bad state
                    Transcriber.printTimestamped("AudioStateMachine fell into a bad state!");
                    resetStateMachine();
                    break;
            }
        }
    }

    /**
     * Pause the jukebox
     */
    @Override
    public void pauseJukebox() {
        switch (myCurrentStatus){
            case JUKEBOX_PLAYING:
                playbackWrapper.pausePlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                setCurrentState(AudioStateMachineStatus.JUKEBOX_PAUSED);
                break;
            case INACTIVE:
            case JUKEBOX_PAUSED:
            case SOUNDBOARD_PLAYING:
            case SOUNDBOARD_PLAYING_JUKEBOX_PAUSED:
                // Do nothing
                break;
            case SOUNDBOARD_PLAYING_JUKEBOX_READY:
            case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                playbackWrapper.pausePlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                setCurrentState(AudioStateMachineStatus.SOUNDBOARD_PLAYING_JUKEBOX_PAUSED);
                break;
        }
    }

    /**
     * Resume the jukebox if a song is loaded or start playing music if not.
     */
    @Override
    public void resumeJukebox() {
        switch (myCurrentStatus){
            case INACTIVE:
            case SOUNDBOARD_PLAYING:
                // Begin playing music
                startNextJukeboxSong(true);
                break;
            case JUKEBOX_PAUSED:
                playbackWrapper.resumePlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                setCurrentState(AudioStateMachineStatus.JUKEBOX_PLAYING);
                break;
            case JUKEBOX_PLAYING:
            case SOUNDBOARD_PLAYING_JUKEBOX_READY:
            case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                // Do nothing
                break;
            case SOUNDBOARD_PLAYING_JUKEBOX_PAUSED:
                // Resume song when soundboard is finished
                setCurrentState(AudioStateMachineStatus.SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED);
                break;
        }
    }

    /**
     * Helper function that switches the current active stream and records the new state.
     *
     * @param playbackStreamType Which stream to set as active.
     */
    private void changeActiveStream(IPlaybackWrapper.PlaybackStreamType playbackStreamType){
        switch (playbackStreamType) {
            case JUKEBOX:
                currentActiveStreamState = CurrentActiveStreamState.JUKEBOX_ACTIVE;
                playbackWrapper.setActiveStream(playbackStreamType);
                break;
            case SOUNDBOARD:
                currentActiveStreamState = CurrentActiveStreamState.SOUNDBOARD_ACTIVE;
                playbackWrapper.setActiveStream(playbackStreamType);
                break;
            default:
                currentActiveStreamState = CurrentActiveStreamState.NEITHER;
                break;
        }
    }

    /**
     * Sets the currently playing song. Wrapped in a set method to notify listeners to this.
     *
     * @param song AudioKey to track as the current playing song.
     */
    private void setJukeboxCurrentlyPlayingSong(AudioKey song) {
        jukeboxCurrentlyPlayingSong = song;
    }

    /**
     * Sets our new state. Wrapped in a set method to notify listeners to this.
     *
     * @param newStatus State to transition to
     */
    private void setCurrentState(AudioStateMachineStatus newStatus) {
        myCurrentStatus = newStatus;
        Transcriber.printTimestamped("AudioStateMachine new state: %s", myCurrentStatus.name());
    }

    /**
     * Use when something bad happens. Returns state machine back to a good state.
     */
    private void resetStateMachine(){
        playbackWrapper.endPlayback(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD);
        playbackWrapper.endPlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
        changeActiveStream(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
        setJukeboxCurrentlyPlayingSong(null);
        setCurrentState(AudioStateMachineStatus.INACTIVE);
    }

    /**
     * Gets the current status of the state machine
     *
     * @return the current status of the state machine
     */
    @Override
    public AudioStateMachineStatus getCurrentStatus() {
        return myCurrentStatus;
    }

    /**
     * Gets the current status of which stream (jukebox or soundboard) is active
     *
     * @return the current status of which stream is active
     */
    @Override
    public CurrentActiveStreamState getCurrentActiveStream() {
        return currentActiveStreamState;
    }

    /**
     * Blocking access to this IAudioStateMachine's AudioKeyPlaylist of soundboard sounds.
     *
     * @param accessHandle What is to be done when access is acquired. After this is run, all listeners
     *                     to the AudioKeyPlaylist are notified if any changes are made.
     */
    @Override
    public void accessSoundboardSoundsList(IAudioKeyPlaylistAccessHandle accessHandle) {
        accessHandle.onGainAccess(soundboardList);
    }

    /**
     * Blocking access to this IAudioStateMachine's AudioKeyPlaylist of the jukebox default list.
     *
     * @param accessHandle What is to be done when access is acquired. After this is run, all listeners
     *                     to the AudioKeyPlaylist are notified if any changes are made.
     */
    @Override
    public void accessJukeboxDefaultList(IAudioKeyPlaylistAccessHandle accessHandle) {
        accessHandle.onGainAccess(jukeboxDefaultList);
    }

    /**
     * Blocking access to this IAudioStateMachine's AudioKeyPlaylist of the jukebox queue.
     *
     * @param accessHandle What is to be done when access is acquired. After this is run, all listeners
     *                     to the AudioKeyPlaylist are notified if any changes are made.
     */
    @Override
    public void accessJukeboxQueue(IAudioKeyPlaylistAccessHandle accessHandle) {
        accessHandle.onGainAccess(jukeboxQueueList);
    }

    /**
     * Gets the currently playing song in the jukebox
     *
     * @return The currently playing song in the jukebox
     */
    public AudioKey getJukeboxCurrentlyPlayingSong() {
        return jukeboxCurrentlyPlayingSong;
    }

    /**
     * Gets the main volume which controls the volume of both playback streams
     *
     * @return the main volume which controls the volume of both playback streams
     */
    @Override
    public int getMainVolume() {
        return playbackWrapper.getVolume(IPlaybackWrapper.PlaybackStreamType.BOTH);
    }

    /**
     * Gets the volume of the jukebox playback stream
     *
     * @return the volume of the jukebox playback stream
     */
    @Override
    public int getJukeboxVolume() {
        return playbackWrapper.getVolume(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
    }

    /**
     * Gets the volume of the soundboard playback stream
     *
     * @return the volume of the soundboard playback stream
     */
    @Override
    public int getSoundboardVolume() {
        return playbackWrapper.getVolume(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD);
    }

    /**
     * Sets the main volume which controls the volume of both playback streams
     *
     * @param volume The volume to set the main volume to.
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean setMainVolume(int volume) {
        return playbackWrapper.setVolume(IPlaybackWrapper.PlaybackStreamType.BOTH, volume);
    }

    /**
     * Sets the volume of the jukebox playback stream
     *
     * @param volume The volume to set the jukebox volume to.
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean setJukeboxVolume(int volume) {
        return playbackWrapper.setVolume(IPlaybackWrapper.PlaybackStreamType.JUKEBOX, volume);
    }

    /**
     * Sets the volume of the soundboard playback stream
     *
     * @param volume The volume to set the soundboard volume to.
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean setSoundboardVolume(int volume) {
        return playbackWrapper.setVolume(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD, volume);
    }

    /**
     * Adds a VolumeChangeListener to monitor this IAudioStateMachine when its volume settings change
     *
     * @param volumeChangeListener VolumeChangeListener to listen to this IAudioStateMachine
     */
    @Override
    public void addVolumeChangeListener(VolumeChangeListener volumeChangeListener) {

    }

    /**
     * Returns whether the jukebox is looping the currently playing song
     *
     * @return whether the jukebox is looping the currently playing song
     */
    @Override
    public boolean getLoopingStatus() {
        return false;
    }

    /**
     * Sets whether the jukebox should loop the currently playing song
     *
     * @param looping whether the jukebox should loop the currently playing song
     */
    @Override
    public void setLoopingStatus(boolean looping) {

    }

    /**
     * Adds a VolumeChangeListener to monitor the duration of jukebox songs played through this IAudioStateMachine
     *
     * @param songDurationTracker VolumeChangeListener to monitor the duration of jukebox songs played through this IAudioStateMachine
     */
    @Override
    public void addSongDurationTracker(SongDurationTracker songDurationTracker) {

    }

    /**
     * Notifies all song duration trackers that a jukebox song has begun
     *
     * @param durationMilliseconds Duration of song in milliseconds
     */
    @Override
    public void songDurationTrackersNotifySongBegun(long durationMilliseconds) {

    }

    /**
     * Notifies all song duration trackers that a jukebox song has paused
     */
    @Override
    public void songDurationTrackersNotifySongPause() {

    }

    /**
     * Notifies all song duration trackers that a jukebox song has resumed
     */
    @Override
    public void songDurationTrackersNotifySongResume() {

    }

    /**
     * Notifies all song duration trackers that a jukebox song has ended
     */
    @Override
    public void songDurationTrackersNotifySongEnd() {

    }
}
