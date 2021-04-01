package Audio;

import UI.AudioKeyPlaylistLoader;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;

public class AudioKeyPlaylistScraper {

    private AudioMaster audioMaster;

    public AudioKeyPlaylistScraper(AudioMaster audioMaster) {
        this.audioMaster = audioMaster;
    }

    public void populateAudioKeyPlaylist(String url, AudioKeyPlaylist playlist, PostPopulateAction postPopulateAction){
        Transcriber.printTimestamped("Loading playlist from %1$s", url);
        ArrayList<AudioKey> loadedFromFile = AudioKeyPlaylistLoader.grabKeysFromPlaylist(url);
        if (loadedFromFile.size() > 0){
            for (AudioKey audioKey : loadedFromFile){
                audioMaster.getPlayerManager().loadItem(audioKey.getUrl(), new PlaylistScraper(postPopulateAction, playlist));
            }
        } else
            audioMaster.getPlayerManager().loadItem(url, new PlaylistScraper(postPopulateAction, playlist));
    }

    public interface PostPopulateAction {
        void doAction();
    }

    private class PlaylistScraper implements AudioLoadResultHandler {

        private PostPopulateAction postPopulateAction;
        private AudioKeyPlaylist target;

        public PlaylistScraper(PostPopulateAction postPopulateAction, AudioKeyPlaylist target) {
            this.postPopulateAction = postPopulateAction;
            this.target = target;
        }

        /**
         * Called when the requested item is a track and it was successfully loaded.
         *
         * @param track The loaded track
         */
        @Override public void trackLoaded(AudioTrack track) {
            AudioKey audioKey = new AudioKey(track);
            target.addAudioKey(audioKey);
            postPopulateAction.doAction();
        }

        /**
         * Called when the requested item is a playlist and it was successfully loaded.
         *
         * @param playlist The loaded playlist
         */
        @Override public void playlistLoaded(AudioPlaylist playlist) {
            for (AudioTrack track : playlist.getTracks()){
                AudioKey audioKey = new AudioKey(track);
                target.addAudioKey(audioKey);
            }
            audioMaster.saveJukeboxDefault();
        }

        /**
         * Called when there were no items found by the specified identifier.
         */
        @Override public void noMatches() {

        }

        /**
         * Called when loading an item failed with an exception.
         *
         * @param exception The exception that was thrown
         */
        @Override public void loadFailed(FriendlyException exception) {

        }
    }
}
