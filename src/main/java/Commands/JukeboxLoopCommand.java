package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.Transcriber;

public class JukeboxLoopCommand extends Command {

    @Override String getCommandKeyword() {
        return "loop";
    }

    @Override String getHelpArgs() {
        return "<true|false>";
    }

    @Override public String getHelpDescription() {
        return "Sets whether the Jukebox should loop the currently-playing song";
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        boolean newLoopingState = Boolean.valueOf(args[0]);
        audioMaster.setLoopingCurrentSong(newLoopingState);
        if (newLoopingState)
            Transcriber.printAndPost(feedbackHandler, "The Jukebox will now loop the current song.");
        else
            Transcriber.printAndPost(feedbackHandler, "The Jukebox will now play new songs when the current one finishes.");
    }
}
