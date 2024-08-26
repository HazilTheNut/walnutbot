package Commands;

import Main.WalnutbotEnvironment;
import Utils.FileIO;
import Utils.Transcriber;

import java.io.File;
import java.io.IOException;

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
            String expandedURI = FileIO.appendPlaylistFileExtension(FileIO.expandURIMacros(args[0]));
            Transcriber.printRaw("JukeboxDefaultNewCommand.onRunCommand:\n\tinput URI: %s\n\texpanded URI: %s\n", args[0], expandedURI);
            File file = new File(expandedURI);
            if (!file.exists()) {
                try {
                    if (!file.createNewFile())
                        Transcriber.printAndPost(feedbackHandler, "Failed to create new file at uri '%1$s'", expandedURI);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            environment.getAudioStateMachine().loadJukeboxDefaultList(file.getAbsolutePath());
        }
    }
}
