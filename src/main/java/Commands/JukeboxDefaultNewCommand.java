package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.FileIO;
import Utils.Transcriber;

public class JukeboxDefaultNewCommand extends Command {

    @Override String getCommandKeyword() {
        return "new";
    }

    @Override String getHelpArgs() {
        return "<path>";
    }

    @Override public String getHelpDescription() {
        return "Creates a new Jukebox playlist and sets the Default List to that new list";
    }

    @Override String getSpecificHelpDescription() {
        return getHelpDescription().concat("\n\npath - The file path to where the list should be saved");
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (sanitizeLocalAccess("dummy", feedbackHandler, permissions)){
            if (argsInsufficient(args, 1, feedbackHandler))
                return;
            audioMaster.createNewJukeboxDefaultList(FileIO.expandURIMacros(args[0]));
        } else
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** You do not have permission to run this command.");
    }
}
