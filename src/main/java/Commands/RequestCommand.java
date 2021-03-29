package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.SettingsLoader;
import Utils.Transcriber;
import net.dv8tion.jda.api.JDA;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class RequestCommand extends Command {

    public RequestCommand(){
        addSubCommand(new ListQueueCommand());
        addSubCommand(new SkipCommand());
    }

    @Override public String getCommandKeyword() {
        return "jb";
    }

    @Override public String getHelpArgs() {
        return "<url>";
    }

    @Override public String getHelpDescription() {
        return "Requests a song, putting it on the Jukebox queue";
    }

    @Override public String getSpecificHelpDescription() {
        return "Places a song fetched from the url onto the Jukebox queue.\n\nurl - The website URL / file path to the desired song.";
    }

    @Override public void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        //Input Sanitation
        if (args.length <= 0) return;
        if (sanitizeLocalAccess(args[0], feedbackHandler, permissions))
            audioMaster.queueJukeboxSong(args[0], () -> postQueueStatus(audioMaster, feedbackHandler), () -> postErrorStatus(feedbackHandler, args[0]));
    }

    private void postQueueStatus(AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler){
        int numberofSongs = audioMaster.getJukeboxQueueList().getAudioKeys().size();
        //if (audioMaster.getCurrentlyPlayingSong() != null) numberofSongs++;
        Transcriber.printAndPost(feedbackHandler, "Track(s) loaded! (%1$d in queue)", numberofSongs);
    }

    private void postErrorStatus(CommandFeedbackHandler feedbackHandler, String url){
        Transcriber.printAndPost(feedbackHandler, "**WARNING:** This bot threw an error parsing this url: \"%1$s\"", url);
    }
}
