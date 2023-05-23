package LavaplayerWrapper;

import Audio.*;
import Utils.FileIO;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LavaplayerWrapper implements IPlaybackWrapper {

    private final AudioPlayerManager audioPlayerManager;
    private final ILavaplayerBotBridge lavaplayerBotBridge;
    private final AudioPlayer soundboardPlayer;
    private final AudioPlayer jukeboxPlayer;
    private boolean isConnectedToVoiceChannel;

    private AtomicInteger loadRequestsProcessing;

    private static final int MAX_VOLUME = 150;

    private int mainVolumePercent;
    private int jukeboxVolumePercent;
    private int soundboardVolumePercent;

    public LavaplayerWrapper(ILavaplayerBotBridge lavaplayerBotBridge){
        this.lavaplayerBotBridge = lavaplayerBotBridge;
        lavaplayerBotBridge.assignLavaplayerWrapper(this);

        audioPlayerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);

        soundboardPlayer = audioPlayerManager.createPlayer();
        jukeboxPlayer = audioPlayerManager.createPlayer();

        isConnectedToVoiceChannel = false;

        loadRequestsProcessing = new AtomicInteger(0);
    }

    @Override
    public void assignAudioStateMachine(IAudioStateMachine stateMachine) {
        jukeboxPlayer.addListener(new LavaplayerJukeboxTrackScheduler(stateMachine));
        soundboardPlayer.addListener(new LavaplayerSoundboardTrackScheduler(stateMachine));
    }

    /**
     * Given a URI (can be local file or web address), populates an AudioKeyPlaylist with the tracks found at that location.
     * The URI can point to an audio file (.mp3, .mp4, .wav, etc.), a Walnutbot .playlist file, or a web address such as <a href="https://www.bandcamp.com">...</a>
     * This is a nonblocking operation and spins up its own thread.
     *
     * @param uri                     The URI to load the track from
     * @param output                  AudioKeyPlaylist to populate with load results
     * @param trackLoadResultHandler  Handles what occurs after the AudioKeyPlaylist is populated
     * @param storeLoadedTrackObjects Whether to store the loaded track object on each generated AudioKey
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean loadTracks(String uri, AudioKeyPlaylistTSWrapper output, ITrackLoadResultHandler trackLoadResultHandler, boolean storeLoadedTrackObjects) {
        // Check to see if it is a .playlist file
        loadRequestsProcessing.incrementAndGet();
        if (FileIO.isPlaylistFile(uri)) {
            AudioKeyPlaylist fromFile = new AudioKeyPlaylist(new File(uri));
            // Put this on its own thread to retain this method being nonblocking
            Thread playlistFileImportThread = new Thread(() -> {
                if (fromFile.isURLValid()) {
                    if (storeLoadedTrackObjects) {
                        // AudioKeys from playlist file do not have tracks loaded on them.
                        loadTracksPlaylistFile(fromFile, output, trackLoadResultHandler);
                    } else {
                        // Don't need to load the tracks, just add them in
                        output.accessAudioKeyPlaylist(playlist -> {
                            for (AudioKey key : fromFile.getAudioKeys()) {
                                playlist.addAudioKey(key);
                            }
                        });
                    }
                    trackLoadResultHandler.onTracksLoaded(output, true);
                } else
                    trackLoadResultHandler.onTracksLoaded(output, false);
                loadRequestsProcessing.decrementAndGet();
            });
            playlistFileImportThread.start();
            return fromFile.isURLValid();
        }
        // Otherwise use lavaplayer to load tracks
        audioPlayerManager.loadItem(uri, new LavaplayerTrackLoadResultHandler(output, trackLoadResultHandler, loadRequestsProcessing, storeLoadedTrackObjects));
        return true;
    }

    private void loadTracksPlaylistFile(AudioKeyPlaylist fromFile, AudioKeyPlaylistTSWrapper output, ITrackLoadResultHandler trackLoadResultHandler) {
        loadTracksPlaylistFile(fromFile, output, trackLoadResultHandler, new TrackLoadCounter(), new LinkedList<>());
    }

    /**
     * Helper function for loading AudioKeys with loaded tracks from playlist files and recursively evaluating Walnutbot playlists.
     *
     * @param fromFile AudioKeyPlaylist loaded from a playlist file
     * @param output AudioKeyPlaylistTSWrapper to add AudioKeys with loaded tracks to
     * @param trackLoadResultHandler  Handles what occurs after the AudioKeyPlaylist is populated
     * @param counter Counter used to keep the track loading ordered
     * @param openedPlaylistFileURIs List of previously-opened file URIs to prevent recursive loops
     */
    private void loadTracksPlaylistFile(AudioKeyPlaylist fromFile, AudioKeyPlaylistTSWrapper output, ITrackLoadResultHandler trackLoadResultHandler, TrackLoadCounter counter, List<String> openedPlaylistFileURIs) {
        for (AudioKey keyFromFile : fromFile.getAudioKeys()) {
            String filepath = keyFromFile.getUrl();
            if (FileIO.isPlaylistFile(filepath)){
                AudioKeyPlaylist nestedPlaylistFromFile = new AudioKeyPlaylist(filepath);
                if (nestedPlaylistFromFile.isURLValid() && !openedPlaylistFileURIs.contains(filepath)) {
                    openedPlaylistFileURIs.add(filepath);
                    loadTracksPlaylistFile(nestedPlaylistFromFile, output, trackLoadResultHandler,counter, openedPlaylistFileURIs);
                }
            } else {
                counter.increment();
                audioPlayerManager.loadItemOrdered(counter.getCount(), keyFromFile.getUrl(), new LavaplayerTrackLoadResultHandler(output, trackLoadResultHandler, loadRequestsProcessing,true, keyFromFile.getName()));
            }
        }
    }

    /**
     * Sets which stream of audio, either the Soundboard or the Jukebox, should be playing through the bot.
     *
     * @param playbackStreamType Which stream of audio to set as the active stream
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean setActiveStream(PlaybackStreamType playbackStreamType) {
        switch (playbackStreamType) {
            case JUKEBOX:
                return lavaplayerBotBridge.setActiveStream(jukeboxPlayer);
            case SOUNDBOARD:
                return lavaplayerBotBridge.setActiveStream(soundboardPlayer);
            default:
                return false;
        }
    }

    /**
     * Resets the loaded track on the AudioKey to be played back again, returning a new AudioKey.
     *
     * @param key AudioKey to refresh
     * @return A refreshed version of the input AudioKey able to start playback from the beginning. Returns null if the AudioKey's loaded track object is malformed or null.
     */
    @Override
    public AudioKey refreshAudioKey(AudioKey key) {
        if (key.getAbstractedLoadedTrack() == null)
            return null;
        if (!(key.getAbstractedLoadedTrack() instanceof AudioTrack))
            return null;
        AudioTrack track = (AudioTrack) key.getAbstractedLoadedTrack();
        return new AudioKey(key.getName(), key.getUrl(), track.makeClone());
    }

    /**
     * Starts playback of a loaded track, which can later be paused or ended.
     *
     * @param playbackStreamType Which playback stream to start playing a new track
     * @param loadedTrackObject Object that should be an instance of AudioTrack that is ready for playback
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean startPlayback(PlaybackStreamType playbackStreamType, Object loadedTrackObject) {
        if (loadedTrackObject == null)
            return false;
        if (!(loadedTrackObject instanceof AudioTrack))
            return false;
        AudioTrack track = (AudioTrack) loadedTrackObject;
        if (isDisconnectedFromVoiceChannel())
            return false;
        switch (playbackStreamType) {
            case JUKEBOX:
                jukeboxPlayer.startTrack(track, false);
                break;
            case SOUNDBOARD:
                soundboardPlayer.startTrack(track, false);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Ends playback, which cannot later be resumed.
     *
     * @param playbackStreamType Which playback stream to end its current track
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean endPlayback(PlaybackStreamType playbackStreamType) {
        if (isDisconnectedFromVoiceChannel())
            return false;
        switch (playbackStreamType) {
            case JUKEBOX:
                jukeboxPlayer.stopTrack();
                break;
            case SOUNDBOARD:
                soundboardPlayer.stopTrack();
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Pauses playback, which can later be resumed or ended.
     *
     * @param playbackStreamType Which playback stream to pause its current track
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean pausePlayback(PlaybackStreamType playbackStreamType) {
        if (isDisconnectedFromVoiceChannel())
            return false;
        switch (playbackStreamType) {
            case JUKEBOX:
                jukeboxPlayer.setPaused(true);
                break;
            case SOUNDBOARD:
                soundboardPlayer.setPaused(true);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Resumes playback, which can later be paused or ended.
     *
     * @param playbackStreamType Which playback stream to pause its current track
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean resumePlayback(PlaybackStreamType playbackStreamType) {
        if (isDisconnectedFromVoiceChannel())
            return false;
        switch (playbackStreamType) {
            case JUKEBOX:
                jukeboxPlayer.setPaused(false);
                break;
            case SOUNDBOARD:
                soundboardPlayer.setPaused(false);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Sets volume of an audio stream.
     *
     * @param playbackStreamType Which playback stream to modify its volume
     * @param volume             0-100 scale volume
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean setVolume(PlaybackStreamType playbackStreamType, int volume) {
        // Bounds check
        if (volume < 0 || volume > 100)
            return false;
        // Modify internal percentage values
        switch (playbackStreamType) {
            case JUKEBOX:
                jukeboxVolumePercent = volume;
                break;
            case SOUNDBOARD:
                soundboardVolumePercent = volume;
                break;
            case BOTH:
                mainVolumePercent = volume;
                break;
            default:
                return false;
        }
        // Recalculate volume levels
        double mainVol = (double)(mainVolumePercent) / 100d;
        double jukeboxVol = (double)(jukeboxVolumePercent) / 100d;
        double soundboardVol = (double)(soundboardVolumePercent) / 100d;

        jukeboxPlayer.setVolume((int)(MAX_VOLUME * jukeboxVol * mainVol));
        soundboardPlayer.setVolume((int)(MAX_VOLUME * soundboardVol * mainVol));

        return true;
    }

    /**
     * Gest the volume of a playback stream, or the main volume that controls both.
     *
     * @param playbackStreamType Which playback stream to read its volume
     * @return The volume level of the specified playback stream
     */
    @Override
    public int getVolume(PlaybackStreamType playbackStreamType) {
        switch (playbackStreamType) {
            case JUKEBOX:
                return jukeboxVolumePercent;
            case SOUNDBOARD:
                return soundboardVolumePercent;
            case BOTH:
                return mainVolumePercent;
            default:
                return 0;
        }
    }

    @Override
    public boolean isProcessingLoadRequests() {
        return loadRequestsProcessing.get() <= 0;
    }

    private boolean isDisconnectedFromVoiceChannel() {
        return !isConnectedToVoiceChannel;
    }

    protected void setConnectedToVoiceChannel(boolean connectedToVoiceChannel) {
        isConnectedToVoiceChannel = connectedToVoiceChannel;
    }

    private static class LavaplayerTrackLoadResultHandler implements AudioLoadResultHandler {

        private final AudioKeyPlaylistTSWrapper audioKeyPlaylistTSWrapper;
        private final ITrackLoadResultHandler trackLoadResultHandler;
        private final AtomicInteger loadRequestsProcessing;
        @Nullable
        private final String nameOverride;
        boolean storeLoadedTrackObjects;

        public LavaplayerTrackLoadResultHandler(AudioKeyPlaylistTSWrapper audioKeyPlaylistTSWrapper, ITrackLoadResultHandler trackLoadResultHandler, AtomicInteger loadRequestsProcessing, boolean storeLoadedTrackObjects) {
            this(audioKeyPlaylistTSWrapper, trackLoadResultHandler, loadRequestsProcessing, storeLoadedTrackObjects, null);
        }

        public LavaplayerTrackLoadResultHandler(AudioKeyPlaylistTSWrapper audioKeyPlaylistTSWrapper, ITrackLoadResultHandler trackLoadResultHandler, AtomicInteger loadRequestsProcessing, boolean storeLoadedTrackObjects, String nameOverride) {
            this.audioKeyPlaylistTSWrapper = audioKeyPlaylistTSWrapper;
            this.trackLoadResultHandler = trackLoadResultHandler;
            this.loadRequestsProcessing = loadRequestsProcessing;
            this.storeLoadedTrackObjects = storeLoadedTrackObjects;
            this.nameOverride = nameOverride;
        }

        /**
         * Called when the requested item is a track and it was successfully loaded.
         *
         * @param track The loaded track
         */
        @Override
        public void trackLoaded(AudioTrack track) {
            audioKeyPlaylistTSWrapper.accessAudioKeyPlaylist(playlist -> insertTrack(playlist, track));
            trackLoadResultHandler.onTracksLoaded(audioKeyPlaylistTSWrapper, true);
            loadRequestsProcessing.decrementAndGet();
        }

        /**
         * Called when the requested item is a playlist and it was successfully loaded.
         *
         * @param playlist The loaded playlist
         */
        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            audioKeyPlaylistTSWrapper.accessAudioKeyPlaylist(playlist1 -> {
                for (AudioTrack track : playlist.getTracks()){
                    insertTrack(playlist1, track);
                }
            });
            trackLoadResultHandler.onTracksLoaded(audioKeyPlaylistTSWrapper, true);
            loadRequestsProcessing.decrementAndGet();
        }

        private void insertTrack(AudioKeyPlaylist playlist, AudioTrack track) {
            AudioKey key;
            if (nameOverride != null)
                key = new AudioKey(nameOverride, track.getIdentifier(), track);
            else if (storeLoadedTrackObjects)
                key = new AudioKey(LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier(), track);
            else
                key = new AudioKey(LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
            playlist.addAudioKey(key);
        }

        /**
         * Called when there were no items found by the specified identifier.
         */
        @Override
        public void noMatches() {
            trackLoadResultHandler.onTracksLoaded(audioKeyPlaylistTSWrapper, false);
            loadRequestsProcessing.decrementAndGet();
        }

        /**
         * Called when loading an item failed with an exception.
         *
         * @param exception The exception that was thrown
         */
        @Override
        public void loadFailed(FriendlyException exception) {
            trackLoadResultHandler.onTracksLoaded(audioKeyPlaylistTSWrapper, false);
            loadRequestsProcessing.decrementAndGet();
        }
    }

    private static class TrackLoadCounter {
        private int count = 0;

        void increment() {
            count++;
        }

        int getCount() {
            return count;
        }
    }
}
