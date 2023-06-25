package Audio;

import UI.SongDurationTracker;
import Utils.FileIO;
import Utils.SettingsLoader;
import Utils.Transcriber;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

public class AudioStateMachine implements IAudioStateMachine {

    private final IPlaybackWrapper playbackWrapper;

    private final AudioKeyPlaylistTSWrapper soundboardList;
    private final AudioKeyPlaylistTSWrapper jukeboxDefaultList;
    private final AudioKeyPlaylistTSWrapper jukeboxQueueList;
    private final Semaphore stateMachineMutex;
    private AudioKey jukeboxCurrentlyPlayingSong;

    private AudioStateMachineStatus myCurrentStatus;
    private CurrentActiveStreamState currentActiveStreamState;
    private JukeboxDefaultListLoadState jukeboxDefaultListLoadState;
    private boolean loopingJukebox;

    private final List<SongDurationTracker> songDurationTrackers;
    private final List<IAudioStateMachineListener> audioStateMachineListeners;
    private final List<IVolumeChangeListener> volumeChangeListeners;

    private final ConcurrentHashMap<UUID, AudioTrackLoadJob> loadingJobs;

    private final ConcurrentLinkedDeque<INotifiableObject> soundboardCompleteNotifyList;
    private final ConcurrentLinkedDeque<INotifiableObject> loadingCompleteNotifyList;

    public static final int VOLUME_DEFAULT = 50;

    public AudioStateMachine(IPlaybackWrapper playbackWrapper) {
        this.playbackWrapper = playbackWrapper;
        playbackWrapper.assignAudioStateMachine(this);

        // Init state machine
        myCurrentStatus = AudioStateMachineStatus.INACTIVE;
        currentActiveStreamState = CurrentActiveStreamState.NEITHER;
        jukeboxDefaultListLoadState = JukeboxDefaultListLoadState.UNLOADED;

        loadingJobs = new ConcurrentHashMap<>();
        soundboardCompleteNotifyList = new ConcurrentLinkedDeque<>();
        loadingCompleteNotifyList = new ConcurrentLinkedDeque<>();

        // Init soundboard list
        AudioKeyPlaylist soundboardListRaw = new AudioKeyPlaylist("SOUNDBOARD");
        soundboardListRaw.addAudioKeyPlaylistListener(((playlist, event) -> {
            // Auto-save
            if (event.getEventType() == AudioKeyPlaylistEvent.AudioKeyPlaylistEventType.EVENT_QUEUE_END) {
                Transcriber.printRaw("saving soundboard");
                playlist.saveToFile(FileIO.getSoundboardFile());
            }
        }));
        soundboardList = new AudioKeyPlaylistTSWrapper(soundboardListRaw);
        loadTracks(FileIO.getSoundboardFile().getAbsolutePath(), soundboardList, new LoadJobSettings(true, false), (playlist, successful) -> {});

        // Init jukebox default list
        AudioKeyPlaylist jukeboxDefaultListRaw = new AudioKeyPlaylist("JUKEBOX DFL");
        jukeboxDefaultListRaw.addAudioKeyPlaylistListener((playlist, event) -> {
            // Auto-save
            if (event.getEventType() == AudioKeyPlaylistEvent.AudioKeyPlaylistEventType.EVENT_QUEUE_END) {
                if (jukeboxDefaultListLoadState == JukeboxDefaultListLoadState.LOCAL_FILE) {
                    Transcriber.printRaw("saving jukebox default list to %s", jukeboxDefaultListRaw.getUrl());
                    jukeboxDefaultListRaw.saveToFile(new File(jukeboxDefaultListRaw.getUrl()));
                }
            }
        });
        jukeboxDefaultList = new AudioKeyPlaylistTSWrapper(jukeboxDefaultListRaw);

        // Init jukebox queue
        jukeboxQueueList = new AudioKeyPlaylistTSWrapper(new AudioKeyPlaylist("JUKEBOX QUEUE"));

        loopingJukebox = false;

        stateMachineMutex = new Semaphore(1, true);

        songDurationTrackers = new LinkedList<>();
        audioStateMachineListeners = new LinkedList<>();
        volumeChangeListeners = new LinkedList<>();

        setMainVolume(getSettingsVolume("mainVolume"));
        setSoundboardVolume(getSettingsVolume("soundboardVolume"));
        setJukeboxVolume(getSettingsVolume("jukeboxVolume"));
    }

    private int getSettingsVolume(String setting){
        try {
            return Integer.parseInt(SettingsLoader.getSettingsValue(setting, String.valueOf(VOLUME_DEFAULT)));
        } catch (NumberFormatException e){
            return VOLUME_DEFAULT;
        }
    }

    /**
     * Should be a pass-through to an IPlaybackWrapper loadTracks() useful for retrieving tracks from local/internet
     * to instant-play soundboard sounds, import tracks into jukebox default list, or enqueue jukebox songs
     *
     * @param uri                    The URI to load the track from
     * @param output                 AudioKeyPlaylist to populate with load results
     * @param loadJobSettings        Whether to store the loaded track object on each generated AudioKey
     * @param trackLoadResultHandler Handles what occurs after the AudioKeyPlaylist is populated
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean loadTracks(String uri, AudioKeyPlaylistTSWrapper output, LoadJobSettings loadJobSettings, ITrackLoadResultHandler trackLoadResultHandler) {
        Transcriber.printRaw("loadTracks: %s %s", uri, loadJobSettings);
        return loadTracks_internal(uri, output, loadJobSettings, trackLoadResultHandler);
    }

    private synchronized boolean loadTracks_internal(String uri, AudioKeyPlaylistTSWrapper output, LoadJobSettings loadJobSettings, ITrackLoadResultHandler trackLoadResultHandler){
        AudioTrackLoadJob job = new AudioTrackLoadJob();
        loadingJobs.put(job.getId(), job);
        return job.loadItem(uri, output, playbackWrapper, (result, successful) -> {
            trackLoadResultHandler.onTracksLoaded(result, successful);
            removeCompletedLoadingJob(job);
        }, loadJobSettings);
    }

    private synchronized void removeCompletedLoadingJob(AudioTrackLoadJob job) {
        loadingJobs.remove(job.getId());
        // If set is empty, notify objects waiting for load to finish
        if (loadingJobs.isEmpty()) {
            while (!loadingCompleteNotifyList.isEmpty())
                loadingCompleteNotifyList.removeFirst().awaken();
        }
    }

    /**
     * Interrupts jukebox if it was playing and plays a sound
     *
     * @param key Sound to play
     */
    @Override
    public void playSoundboardSound(AudioKey key) {
        try {
            stateMachineMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            playSoundboardSound_internal(key);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        stateMachineMutex.release();
    }

    private void playSoundboardSound_internal(AudioKey key) {
        Transcriber.printRaw("playSoundboardSound_internal");
        // Play sound
        if (key.getAbstractedLoadedTrack() == null) {
            // We need to load up the track
            AudioKeyPlaylistTSWrapper temp = new AudioKeyPlaylistTSWrapper(new AudioKeyPlaylist("TEMP"));
            if (!loadTracks(key.getUrl(), temp, new LoadJobSettings(true, true), (output, successful) -> {
                if (successful) {
                    // Recurse through playSoundboardSound with an AudioKey with a track loaded onto it
                    // Should call playSoundboardSound since loadTracks spins up its own thread
                    output.accessAudioKeyPlaylist(playlist -> playSoundboardSound(playlist.getKey(0)));
                } else {
                    Transcriber.printTimestamped("Loading for key %s failed!", key);
                }
            })) {
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
            AudioKey refreshedKey = playbackWrapper.refreshAudioKey(key);
            if (!playbackWrapper.startPlayback(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD, refreshedKey.getAbstractedLoadedTrack())) {
                Transcriber.printTimestamped("Playback for key %s failed!", refreshedKey);
                stopSoundboard_internal();
            }
        }
        // Returning playback to jukebox is not handled here (stopSoundboard() will eventually be called)
    }

    /**
     * Stops the soundboard playback if it was playing and returns playback to jukebox if it was playing
     */
    @Override
    public void stopSoundboard() {
        try {
            stateMachineMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            stopSoundboard_internal();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        stateMachineMutex.release();
    }

    private void stopSoundboard_internal() {
        Transcriber.printRaw("stopSoundboard_internal");
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
                break;
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
        // Notify objects waiting for soundboard to finish
        while (!soundboardCompleteNotifyList.isEmpty())
            soundboardCompleteNotifyList.removeFirst().awaken();
    }

    /**
     * Enqueues an AudioKey onto the jukebox queue
     *
     * @param key Song to play
     */
    @Override
    public void enqueueJukeboxSong(AudioKey key) {
        try {
            stateMachineMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            enqueueJukeboxSong_internal(key);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        stateMachineMutex.release();
    }

    private void enqueueJukeboxSong_internal(AudioKey key) {
        // Populate the queue with tracks loaded from key
        if (key.getAbstractedLoadedTrack() == null) {
            Transcriber.printRaw("enqueueJukeboxSong_internal: need to load");
            // Need to load the track(s)
            AudioKeyPlaylistTSWrapper temp = new AudioKeyPlaylistTSWrapper(new AudioKeyPlaylist("TEMP"));
            if (!loadTracks(key.getUrl(), temp, new LoadJobSettings(true, true), (output, successful) -> {
                if (successful) {
                    output.accessAudioKeyPlaylist(playlist -> {
                        for (AudioKey outputKey : playlist.getAudioKeys()) {
                            if (outputKey.getAbstractedLoadedTrack() != null)
                                // Should call enqueueJukeboxSong since loadTracks spins up its own thread
                                enqueueJukeboxSong(outputKey);
                        }
                    });
                } else {
                    Transcriber.printTimestamped("Loading for key %s failed!", key);
                }
            })) {
                Transcriber.printTimestamped("Loading for key %s failed!", key);
            }
        } else {
            Transcriber.printRaw("enqueueJukeboxSong_internal: loaded");
            switch (myCurrentStatus) {
                case INACTIVE:
                    if (playbackWrapper.isDisconnectedFromVoiceChannel()) {
                        // If not connected to a voice channel, queue it up rather than start playback
                        jukeboxQueueList.accessAudioKeyPlaylist(playlist -> playlist.addAudioKey(key));
                        break;
                    }
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
                    jukeboxQueueList.accessAudioKeyPlaylist(playlist -> playlist.addAudioKey(key));
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
        try {
            stateMachineMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            startNextJukeboxSong_internal(ignoreLooping);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stateMachineMutex.release();
    }

    private void startNextJukeboxSong_internal(boolean ignoreLooping) {
        Transcriber.printRaw("startNextJukeboxSong_internal");
        // End playback
        playbackWrapper.endPlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);

        // Get next song
        if (!ignoreLooping && loopingJukebox) {
            // Loop current song if looping
            setJukeboxCurrentlyPlayingSong(playbackWrapper.refreshAudioKey(jukeboxCurrentlyPlayingSong));
        } else if (jukeboxQueueList.isNonEmpty()) {
            // If something is in the queue, dequeue it
            jukeboxQueueList.accessAudioKeyPlaylist(playlist -> setJukeboxCurrentlyPlayingSong(playlist.removeAudioKey(0)));
        } else if (jukeboxDefaultList.isNonEmpty()) {
            // If default list is nonempty, get a random song
            jukeboxDefaultList.accessAudioKeyPlaylist(playlist -> setJukeboxCurrentlyPlayingSong(playlist.getRandomAudioKey().shallowCopy()));
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
            return;
        }

        if (jukeboxCurrentlyPlayingSong.getAbstractedLoadedTrack() != null) {
            // Track was preloaded and ready to play
            beginPlaybackOfCurrentSong();
        } else {
            AudioKeyPlaylistTSWrapper temp = new AudioKeyPlaylistTSWrapper(new AudioKeyPlaylist("TEMP"));
            loadTracks(jukeboxCurrentlyPlayingSong.getUrl(), temp, new LoadJobSettings(true, true), (loadedTemp, successful) -> {
                if (successful) {
                    loadedTemp.accessAudioKeyPlaylist(playlist -> jukeboxCurrentlyPlayingSong = playlist.getKey(0));
                    beginPlaybackOfCurrentSong();
                } else {
                    // Got a bad track, try getting another one
                    Transcriber.printRaw("Could not resolve URI for `%s`, starting next song", jukeboxCurrentlyPlayingSong.getUrl());
                    startNextJukeboxSong(ignoreLooping);
                }
            });
        }
    }

    private void beginPlaybackOfCurrentSong() {
        Transcriber.printRaw("beginPlaybackOfCurrentSong");
        if (jukeboxCurrentlyPlayingSong != null) {
            if (jukeboxCurrentlyPlayingSong.getAbstractedLoadedTrack() == null) {
                Transcriber.printTimestamped("Cannot play Key %s without a track loaded onto it", jukeboxCurrentlyPlayingSong);
                return;
            }
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
        try {
            stateMachineMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            pauseJukebox_internal();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stateMachineMutex.release();
    }

    private void pauseJukebox_internal() {
        Transcriber.printRaw("pauseJukebox_internal");
        switch (myCurrentStatus){
            case JUKEBOX_PLAYING:
            case JUKEBOX_PAUSED:
                playbackWrapper.pausePlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX);
                setCurrentState(AudioStateMachineStatus.JUKEBOX_PAUSED);
                break;
            case INACTIVE:
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
        try {
            stateMachineMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            resumeJukebox_internal();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stateMachineMutex.release();
    }

    private void resumeJukebox_internal() {
        Transcriber.printRaw("resumeJukebox_internal");
        switch (myCurrentStatus){
            case INACTIVE:
            case SOUNDBOARD_PLAYING:
                // Begin playing music
                startNextJukeboxSong(true);
                break;
            case JUKEBOX_PAUSED:
            case JUKEBOX_PLAYING:
                if (playbackWrapper.resumePlayback(IPlaybackWrapper.PlaybackStreamType.JUKEBOX))
                    setCurrentState(AudioStateMachineStatus.JUKEBOX_PLAYING);
                break;
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
     * Clears the jukebox default list and loads one in from the provided URI
     *
     * @param uri Where to find the jukebox default list
     */
    @Override
    public void loadJukeboxDefaultList(String uri) {
        try {
            stateMachineMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            loadJukeboxDefaultList_internal(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stateMachineMutex.release();
    }

    private void loadJukeboxDefaultList_internal(String uri) {
        Transcriber.printRaw("loadJukeboxDefaultList_internal: %s", uri);
        jukeboxDefaultList.accessAudioKeyPlaylist(playlist -> {
            // Unload previous playlist
            Transcriber.printRaw("loadJukeboxDefaultList_internal: prev playlist unloading: %s", playlist.getUrl());
            playlist.clearPlaylist();
            jukeboxDefaultListLoadState = JukeboxDefaultListLoadState.UNLOADED; // Don't do setJukeboxDefaultListLoadState as we should only notify listeners once
            // Begin loading new playlist
            loadTracks(uri, jukeboxDefaultList, new LoadJobSettings(false,false), (loadResult, successful) -> {
                Transcriber.printRaw("loadJukeboxDefaultList_internal: new playlist loaded: %s", uri);
                if (!successful) {
                    setJukeboxDefaultListLoadState(JukeboxDefaultListLoadState.UNLOADED);
                } else if (FileIO.isWebsiteURL(uri))
                    setJukeboxDefaultListLoadState(JukeboxDefaultListLoadState.REMOTE);
                else if (FileIO.isPlaylistFile(uri))
                    setJukeboxDefaultListLoadState(JukeboxDefaultListLoadState.LOCAL_FILE);
                else {
                    setJukeboxDefaultListLoadState(JukeboxDefaultListLoadState.UNLOADED);
                }
                // loadTracks spins up its own thread so this should be okay
                loadResult.accessAudioKeyPlaylist(accessedLoadResult -> {
                    // Update the URL
                    switch (jukeboxDefaultListLoadState) {
                        case UNLOADED:
                            accessedLoadResult.setUrl("(Nothing Loaded)");
                            break;
                        case REMOTE:
                        case LOCAL_FILE:
                            accessedLoadResult.setUrl(uri);
                            break;
                        default:
                            accessedLoadResult.setUrl("ERROR");
                            break;
                    }
                    Transcriber.printRaw("loadJukeboxDefaultList_internal: assigned uri: %s", accessedLoadResult.getUrl());
                });
            });
        });
    }

    /**
     * Clears the jukebox default list
     */
    @Override
    public void clearJukeboxDefaultList() {
        try {
            stateMachineMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            clearJukeboxDefaultList_internal();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stateMachineMutex.release();
    }

    public void clearJukeboxDefaultList_internal() {
        Transcriber.printRaw("unloadJukeboxDefaultList_internal");
        jukeboxDefaultList.accessAudioKeyPlaylist(playlist -> {
            playlist.clearPlaylist();
            setJukeboxDefaultListLoadState(JukeboxDefaultListLoadState.UNLOADED);
        });
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
        Thread listenerUpdateThread = new Thread(() -> {
            for (IAudioStateMachineListener listener : audioStateMachineListeners){
                listener.onAudioStateMachineUpdateStatus(myCurrentStatus);
            }
        });
        listenerUpdateThread.start();
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

    private void setJukeboxDefaultListLoadState(JukeboxDefaultListLoadState loadState){
        jukeboxDefaultListLoadState = loadState;
        Thread listenerUpdateThread = new Thread(() -> {
            for (IAudioStateMachineListener listener : audioStateMachineListeners){
                listener.onJukeboxDefaultListLoadStateUpdate(loadState, this);
            }
        });
        listenerUpdateThread.start();
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
     * Gets the load state of the jukebox default list (whether it is unloaded, from a local file, or from the internet)
     *
     * @return the load state of the jukebox default list
     */
    @Override
    public JukeboxDefaultListLoadState getJukeboxDefaultListLoadState() {
        return jukeboxDefaultListLoadState;
    }

    /**
     * Subscribes a listener to this IAudioStateMachine to detect when changes occur
     *
     * @param listener Subscribing listener
     */
    @Override
    public void addAudioStateMachineListener(IAudioStateMachineListener listener) {
        audioStateMachineListeners.add(listener);
    }

    /**
     * Gets the threadsafe wrapper for the soundboard list
     *
     * @return the threadsafe wrapper for the soundboard list
     */
    @Override
    public AudioKeyPlaylistTSWrapper getSoundboardList() {
        return soundboardList;
    }

    /**
     * Gets the threadsafe wrapper for the jukebox default list
     *
     * @return the threadsafe wrapper for the jukebox default list
     */
    @Override
    public AudioKeyPlaylistTSWrapper getJukeboxDefaultList() {
        return jukeboxDefaultList;
    }

    /**
     * Gets the threadsafe wrapper for the jukebox queue
     *
     * @return the threadsafe wrapper for the jukebox queue
     */
    @Override
    public AudioKeyPlaylistTSWrapper getJukeboxQueue() {
        return jukeboxQueueList;
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
        if (playbackWrapper.setVolume(IPlaybackWrapper.PlaybackStreamType.BOTH, volume)) {
            for (IVolumeChangeListener volumeChangeListener : volumeChangeListeners)
                volumeChangeListener.onMainVolumeChange(volume, this);
            return true;
        }
        return false;
    }

    /**
     * Sets the volume of the jukebox playback stream
     *
     * @param volume The volume to set the jukebox volume to.
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean setJukeboxVolume(int volume) {
        if (playbackWrapper.setVolume(IPlaybackWrapper.PlaybackStreamType.JUKEBOX, volume)) {
            for (IVolumeChangeListener volumeChangeListener : volumeChangeListeners)
                volumeChangeListener.onJukeboxVolumeChange(volume, this);
            return true;
        }
        return false;
    }

    /**
     * Sets the volume of the soundboard playback stream
     *
     * @param volume The volume to set the soundboard volume to.
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean setSoundboardVolume(int volume) {
        if (playbackWrapper.setVolume(IPlaybackWrapper.PlaybackStreamType.SOUNDBOARD, volume)) {
            for (IVolumeChangeListener volumeChangeListener : volumeChangeListeners)
                volumeChangeListener.onSoundboardVolumeChange(volume, this);
            return true;
        }
        return false;
    }

    /**
     * Adds a VolumeChangeListener to monitor this IAudioStateMachine when its volume settings change
     *
     * @param volumeChangeListener VolumeChangeListener to listen to this IAudioStateMachine
     */
    @Override
    public void addVolumeChangeListener(IVolumeChangeListener volumeChangeListener) {
        volumeChangeListeners.add(volumeChangeListener);
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
        loopingJukebox = looping;
        Thread listenerUpdateThread = new Thread(() -> {
            for (IAudioStateMachineListener listener : audioStateMachineListeners){
                listener.onJukeboxLoopingStatusUpdate(looping);
            }
        });
        listenerUpdateThread.start();
    }

    /**
     * Adds a VolumeChangeListener to monitor the duration of jukebox songs played through this IAudioStateMachine
     *
     * @param songDurationTracker VolumeChangeListener to monitor the duration of jukebox songs played through this IAudioStateMachine
     */
    @Override
    public void addSongDurationTracker(SongDurationTracker songDurationTracker) {
        songDurationTrackers.add(songDurationTracker);
    }

    /**
     * Notifies all song duration trackers that a jukebox song has begun
     *
     * @param durationMilliseconds Duration of song in milliseconds
     */
    @Override
    public void songDurationTrackersNotifySongBegun(long durationMilliseconds) {
        Thread listenerUpdateThread = new Thread(() -> {
            for (SongDurationTracker songDurationTracker : songDurationTrackers){
                songDurationTracker.onSongStart(durationMilliseconds, jukeboxCurrentlyPlayingSong.getTrackName());
            }
        });
        listenerUpdateThread.start();
    }

    /**
     * Notifies all song duration trackers that a jukebox song has paused
     */
    @Override
    public void songDurationTrackersNotifySongPause() {
        Thread listenerUpdateThread = new Thread(() -> {
            for (SongDurationTracker songDurationTracker : songDurationTrackers){
                songDurationTracker.onSongPause();
            }
        });
        listenerUpdateThread.start();
    }

    /**
     * Notifies all song duration trackers that a jukebox song has resumed
     */
    @Override
    public void songDurationTrackersNotifySongResume() {
        Thread listenerUpdateThread = new Thread(() -> {
            for (SongDurationTracker songDurationTracker : songDurationTrackers){
                songDurationTracker.onSongResume();
            }
        });
        listenerUpdateThread.start();
    }

    /**
     * Notifies all song duration trackers that a jukebox song has ended
     */
    @Override
    public void songDurationTrackersNotifySongEnd() {
        Thread listenerUpdateThread = new Thread(() -> {
            for (SongDurationTracker songDurationTracker : songDurationTrackers){
                songDurationTracker.onSongEnd();
            }
        });
        listenerUpdateThread.start();
    }

    /**
     * Notifies object when the soundboard finishes playing.
     *
     * @param obj The object to notify
     * @return true if the soundboard is currently playing and the caller should wait
     */
    @Override
    public boolean notifyWhenSoundboardCompletes(INotifiableObject obj) {
        switch (myCurrentStatus) {
            case SOUNDBOARD_PLAYING:
            case SOUNDBOARD_PLAYING_JUKEBOX_PAUSED:
            case SOUNDBOARD_PLAYING_JUKEBOX_READY:
            case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                soundboardCompleteNotifyList.add(obj);
                return true;
            case INACTIVE:
            case JUKEBOX_PAUSED:
            case JUKEBOX_PLAYING:
            default:
                return false;
        }
    }

    /**
     * Notifies object when the soundboard finishes playing.
     *
     * @param obj The object to notify
     * @return true if the soundboard is currently playing and the caller should wait
     */
    @Override
    public boolean notifyWhenAudioLoadingCompletes(INotifiableObject obj) {
        if (!loadingJobs.isEmpty()) {
            loadingCompleteNotifyList.add(obj);
            return true;
        }
        return false;
    }
}
