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
            + "url - The URL of the song to add to the default list. A URL of \"blank\" inserts an empty key.";
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        if (environment.getAudioStateMachine().getJukeboxDefaultListLoadState() == IAudioStateMachine.JukeboxDefaultListLoadState.UNLOADED){
            Transcriber.printAndPost(feedbackHandler, "**WARNING:** No Default List loaded!");
            return;
        }
        if (args[0].equals("blank")) {
            environment.getAudioStateMachine().getJukeboxDefaultList().accessAudioKeyPlaylist(playlist -> playlist.addAudioKey(new AudioKey("blank", "blank")));
            return;
        }
        if (sanitizeLocalAccess(args[0], feedbackHandler, permissions)) {
            String expandedURI = FileIO.expandURIMacros(args[0]);
            Transcriber.printRaw("JukeboxDefaultAddCommand.onRunCommand expanded URI: %s", expandedURI);
            environment.getAudioStateMachine().loadTracks(expandedURI, environment.getAudioStateMachine().getJukeboxDefaultList(), new LoadJobSettings(false, false), (loadResult, successful) -> {
                if (!successful)
                    Transcriber.printAndPost(feedbackHandler, "Failed to load URI: %s", args[0]);
            });
            Transcriber.printAndPost(feedbackHandler, "Songs added to Jukebox Default List");
        }
    }
}
