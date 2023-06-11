package LavaplayerWrapper;

import Audio.*;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jetbrains.annotations.Nullable;

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
     * The URI can point to an audio file (.mp3, .mp4, .wav, etc.) or a web address such as <a href="https://www.bandcamp.com">...</a>.
     * The URI cannot be a Walnutbot playlist file (.playlist, .wbp)
     * This is a nonblocking operation and spins up its own thread.
     *
     * @param source                  The URI to load the track from
     * @param output                  AudioKeyPlaylist to populate with load results
     * @param loadJobSettings Whether to store the loaded track object on each generated AudioKey
     * @param loadResultHandler       Handles what occurs after the AudioKeyPlaylist is populated
     */
    @Override
    public void loadItem(AudioKey source, List<AudioKey> output, LoadJobSettings loadJobSettings, IPlaybackWrapperLoadResultHandler loadResultHandler) {
        loadRequestsProcessing.incrementAndGet();
        if (source.getName() != null)
            audioPlayerManager.loadItem(source.getUrl(), new LavaplayerTrackLoadResultHandler(output, loadResultHandler, loadRequestsProcessing, loadJobSettings.storeLoadedTrackObjects(), source.getName()));
        else
            audioPlayerManager.loadItem(source.getUrl(), new LavaplayerTrackLoadResultHandler(output, loadResultHandler, loadRequestsProcessing, loadJobSettings.storeLoadedTrackObjects(), null));
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
        Transcriber.printRaw("startPlayback: %s %s", playbackStreamType.name(), LavaplayerUtils.printAuthorAndTitle(track));
        if (isDisconnectedFromVoiceChannel())
            return false;
        switch (playbackStreamType) {
            case JUKEBOX:
                jukeboxPlayer.startTrack(track, false);
                jukeboxPlayer.setPaused(false);
                break;
            case SOUNDBOARD:
                soundboardPlayer.startTrack(track, false);
                soundboardPlayer.setPaused(false);
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
        Transcriber.printRaw("endPlayback: %s", playbackStreamType.name());
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
        Transcriber.printRaw("pausePlayback: %s", playbackStreamType.name());
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
        Transcriber.printRaw("resumePlayback: %s", playbackStreamType.name());
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

    @Override
    public boolean isDisconnectedFromVoiceChannel() {
        return !isConnectedToVoiceChannel;
    }

    protected void setConnectedToVoiceChannel(boolean connectedToVoiceChannel) {
        isConnectedToVoiceChannel = connectedToVoiceChannel;
    }

    private static class LavaplayerTrackLoadResultHandler implements AudioLoadResultHandler {
        private final List<AudioKey> outputList;
        @Nullable
        private final IPlaybackWrapperLoadResultHandler trackLoadResultHandler;
        private final AtomicInteger loadRequestsProcessing;
        @Nullable
        private final String nameOverride;
        boolean storeLoadedTrackObjects;

        public LavaplayerTrackLoadResultHandler(List<AudioKey> outputList, @Nullable IPlaybackWrapperLoadResultHandler trackLoadResultHandler, AtomicInteger loadRequestsProcessing, boolean storeLoadedTrackObjects, @Nullable String nameOverride) {
            this.outputList = outputList;
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
            insertTrack(track);
            if (trackLoadResultHandler != null)
                trackLoadResultHandler.onLoadComplete(true);
            loadRequestsProcessing.decrementAndGet();
        }

        /**
         * Called when the requested item is a playlist and it was successfully loaded.
         *
         * @param playlist The loaded playlist
         */
        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            for (AudioTrack track : playlist.getTracks()){
                insertTrack(track);
            }
            if (trackLoadResultHandler != null)
                trackLoadResultHandler.onLoadComplete(true);
            loadRequestsProcessing.decrementAndGet();
        }

        private void insertTrack(AudioTrack track) {
            AudioKey key;
            if (nameOverride != null)
                key = new AudioKey(nameOverride, track.getIdentifier(), track);
            else if (storeLoadedTrackObjects)
                key = new AudioKey(LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier(), track);
            else
                key = new AudioKey(LavaplayerUtils.printAuthorAndTitle(track), track.getIdentifier());
            outputList.add(key);
        }

        /**
         * Called when there were no items found by the specified identifier.
         */
        @Override
        public void noMatches() {
            if (trackLoadResultHandler != null)
                trackLoadResultHandler.onLoadComplete(false);
            loadRequestsProcessing.decrementAndGet();
        }

        /**
         * Called when loading an item failed with an exception.
         *
         * @param exception The exception that was thrown
         */
        @Override
        public void loadFailed(FriendlyException exception) {
            if (trackLoadResultHandler != null)
                trackLoadResultHandler.onLoadComplete(false);
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
