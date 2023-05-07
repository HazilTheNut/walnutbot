package Commands;

import Main.WalnutbotEnvironment;
import Utils.FileIO;

import java.io.File;

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

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        if (sanitizeLocalAccess("dummy", feedbackHandler, permissions)) {
            File file = new File(FileIO.appendPlaylistFileExtension(FileIO.expandURIMacros(args[0])));
            environment.getAudioStateMachine().loadJukeboxDefaultList(file.getAbsolutePath());
        }
    }
}
