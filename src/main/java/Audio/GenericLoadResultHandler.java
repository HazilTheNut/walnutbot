package Audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class GenericLoadResultHandler implements AudioLoadResultHandler {

    private AudioPlayer audioPlayer;

    public GenericLoadResultHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        System.out.printf("Track \'%1$s\' loaded! (Path: %2$s)\n", track.getInfo().title, track.getInfo().uri);
        if (!audioPlayer.startTrack(track, false))
            System.out.printf("Track \'%1$s\' failed to start (Path: %2$s)\n", track.getInfo().title, track.getInfo().uri);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        /*for (AudioTrack track : playlist.getTracks()) {
            trackScheduler.queue(track);
        }*/
    }

    @Override
    public void noMatches() {
        System.out.println("URL passed in got no matches!");
        // Notify the user that we've got nothing
    }

    @Override
    public void loadFailed(FriendlyException throwable) {
        System.out.println("Failure to load track!");
        throwable.printStackTrace();
        // Notify the user that everything exploded
    }
}
