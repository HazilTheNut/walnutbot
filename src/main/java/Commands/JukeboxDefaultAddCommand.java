package Commands;

import Audio.*;
import Main.WalnutbotEnvironment;
import Utils.FileIO;
import Utils.Transcriber;

public class JukeboxDefaultAddCommand extends Command {

    @Override String getCommandKeyword() {
        return "add";
    }

    @Override String getHelpArgs() {
        return "<url>";
    }

    @Override public String getHelpDescription() {
        return "Adds a song to the Jukebox's Default List";
    }

    @Override String getSpecificHelpDescription() {
        return "Adds a song to the Jukebox's Default List\n\n"
            + "url - The URL of the song to add to the default list";
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        if (environment.getAudioStateMachine().getJukeboxDefaultListLoadState() == IAudioStateMachine.JukeboxDefaultListLoadState.UNLOADED){
            Transcriber.printAndPost(feedbackHandler, "**WARNING:** No Default List loaded!");
            return;
        }
        if (sanitizeLocalAccess(args[0], feedbackHandler, permissions)) {
            environment.getAudioStateMachine().loadTracks(FileIO.expandURIMacros(args[0]), environment.getAudioStateMachine().getJukeboxDefaultList(), new LoadJobSettings(false, false), (loadResult, successful) -> {
                if (!successful)
                    Transcriber.printAndPost(feedbackHandler, "Failed to load URI: %s", args[0]);
            });
            Transcriber.printAndPost(feedbackHandler, "Songs added to Jukebox Default List");
        }
    }
}
