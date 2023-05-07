package Commands;

import Audio.IAudioStateMachine;
import Main.WalnutbotEnvironment;
import Utils.Transcriber;

public class JukeboxDefaultRemoveCommand extends Command {

    @Override String getCommandKeyword() {
        return "remove";
    }

    @Override String getHelpArgs() {
        return "<pos>";
    }

    @Override public String getHelpDescription() {
        return "Remove a song from the Default List";
    }

    @Override String getSpecificHelpDescription() {
        return String.format("%1$s\n\npos - The position in the list you wish to remove from.", getHelpDescription());
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        if (environment.getAudioStateMachine().getJukeboxDefaultListLoadState() == IAudioStateMachine.JukeboxDefaultListLoadState.UNLOADED) {
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** No active Default List.");
        } else try {
            int pos = Integer.parseInt(args[0]);
            environment.getAudioStateMachine().getJukeboxDefaultList().accessAudioKeyPlaylist(playlist -> playlist.removeAudioKey(pos));
        } catch (NumberFormatException e){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** `pos` is not an integer value.");
        }
    }
}
