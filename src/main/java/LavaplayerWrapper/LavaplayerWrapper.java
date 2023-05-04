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

import java.io.File;

public class LavaplayerWrapper implements IPlaybackWrapper {

    private final AudioPlayerManager audioPlayerManager;
    private final ILavaplayerBotBridge lavaplayerBotBridge;
    private final AudioPlayer soundboardPlayer;
    private final AudioPlayer jukeboxPlayer;
    private IAudioStateMachine audioStateMachine;

    private static final int MAX_VOLUME = 150;

    private int mainVolumePercent;
    private int jukeboxVolumePercent;
    private int soundboardVolumePercent;

    public LavaplayerWrapper(ILavaplayerBotBridge lavaplayerBotBridge, IAudioStateMachine audioStateMachine){
        this.lavaplayerBotBridge = lavaplayerBotBridge;

        audioPlayerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);

        soundboardPlayer = audioPlayerManager.createPlayer();
        jukeboxPlayer = audioPlayerManager.createPlayer();

        jukeboxPlayer.addListener(new LavaplayerJukeboxTrackScheduler(audioStateMachine));
        soundboardPlayer.addListener(new LavaplayerSoundboardTrackScheduler(audioStateMachine));
    }

    /**
     * Given a URI (can be local file or web address), populates an AudioKeyPlaylist with the tracks found at that location.
     * The URI can point to an audio file (.mp3, .mp4, .wav, etc.), a Walnutbot .playlist file, or a web address such as <a href="https://www.bandcamp.com">...</a>
     *
     * @param uri                     The URI to load the track from
     * @param output                  AudioKeyPlaylist to populate with load results
     * @param trackLoadResultHandler  Handles what occurs after the AudioKeyPlaylist is populated
     * @param storeLoadedTrackObjects Whether to store the loaded track object on each generated AudioKey
     * @return true if the operation was successful, and false otherwise
     */
    @Override
    public boolean loadTracks(String uri, AudioKeyPlaylist output, ITrackLoadResultHandler trackLoadResultHandler, boolean storeLoadedTrackObjects) {
        // Check to see if it is a .playlist file
        if (!FileIO.isWebsiteURL(uri) && FileIO.getFileExtension(uri).equals("playlist")) {
            AudioKeyPlaylist fromFile = new AudioKeyPlaylist(new File(uri));
            for (AudioKey key : fromFile.getAudioKeys()){
                output.addAudioKey(key);
            }
            trackLoadResultHandler.onTracksLoaded(output, true);
            return true;
        }
        final boolean[] successful = {true};
        // Otherwise use lavaplayer to load tracks
        audioPlayerManager.loadItem(uri, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                AudioKey key = new AudioKey(String.format("%s - %s", track.getInfo().author, track.getInfo().title), track.getIdentifier());
                if (storeLoadedTrackObjects)
                    key.setAbstractedLoadedTrack(track);
                output.addAudioKey(key);
            }
            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    trackLoaded(track);
                }
            }
            @Override
            public void noMatches() {
                successful[0] = false;
            }
            @Override
            public void loadFailed(FriendlyException exception) {
                successful[0] = false;
            }
        });
        trackLoadResultHandler.onTracksLoaded(output, successful[0]);
        return successful[0];
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
        switch (playbackStreamType) {
            case JUKEBOX:
                jukeboxPlayer.playTrack(track);
                break;
            case SOUNDBOARD:
                soundboardPlayer.playTrack(track);
                break;
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
}
