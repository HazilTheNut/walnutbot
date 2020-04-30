package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RequestCommand implements Command {

    @Override public String getCommandName() {
        return "req";
    }

    @Override public String getHelpName() {
        return "req <url>";
    }

    @Override public String getHelpDescription() {
        return "Requests a song, putting it on the Jukebox queue";
    }

    @Override public String getSpecificHelpDescription() {
        return "Places a song fetched from the url onto the Jukebox queue.\n\nurl - The website URL / file path to the desired song.";
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args) {
        if (args.length <= 0) return;
        audioMaster.getPlayerManager().loadItem(args[0], new AudioLoadResultHandler() {
            @Override public void trackLoaded(AudioTrack track) {
                AudioKey song = new AudioKey(track);
                audioMaster.queueJukeboxSong(song, () -> postQueueStatus(audioMaster, event, song), () -> postErrorStatus(event, song));
            }

            @Override public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    AudioKey song = new AudioKey(track);
                    audioMaster.queueJukeboxSong(song, () -> {}, () -> postErrorStatus(event, song));
                }
                postQueueStatus(audioMaster, event, new AudioKey(playlist.getName(), args[0]));
            }

            @Override public void noMatches() {

            }

            @Override public void loadFailed(FriendlyException exception) {

            }

            private void postQueueStatus(AudioMaster audioMaster, MessageReceivedEvent event, AudioKey song){
                int numberofSongs = audioMaster.getJukeboxQueueList().getAudioKeys().size();
                //if (audioMaster.getCurrentlyPlayingSong() != null) numberofSongs++;
                Transcriber.printAndPost(event.getChannel(), "Track(s) \"%1$s\" loaded! (%2$d in queue)", song.getName(), numberofSongs);
            }

            private void postErrorStatus(MessageReceivedEvent event, AudioKey song){
                Transcriber.printAndPost(event.getChannel(), "**WARNING:** URL \"%1$s\" is invalid!", song.getUrl());
            }
        });
    }
}
