package Commands;

import Main.WalnutbotEnvironment;
import Utils.FileIO;
import Utils.Transcriber;

public class JukeboxDefaultLoadCommand extends Command {

    @Override String getCommandKeyword() {
        return "load";
    }

    @Override String getHelpArgs() {
        return "<uri>";
    }

    @Override public String getHelpDescription() {
        return "Loads a playlist, either online or a local file, as the Default List";
    }

    @Override String getSpecificHelpDescription() {
        return getHelpDescription().concat("\n\n"
            + "uri - The URI of the playlist");
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        if (sanitizeLocalAccess(args[0], feedbackHandler, permissions)) {
            String expanded = FileIO.expandURIMacros(args[0]);
            Transcriber.printRaw("JukeboxDefaultLoadCommand.onRunCommand expanded: %s", expanded);
            environment.getAudioStateMachine().loadJukeboxDefaultList(FileIO.expandURIMacros(expanded));
        }
    }
}
