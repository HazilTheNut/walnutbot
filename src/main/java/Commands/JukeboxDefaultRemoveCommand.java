package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
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
        return String.format("%1$s\n\n pos - The position in the list you wish to remove from.", getHelpDescription());
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        if (audioMaster.getJukeboxDefaultList() == null) {
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** No active Default List.");
        } else try {
            int pos = Integer.valueOf(args[0]);
            audioMaster.getJukeboxDefaultList().removeAudioKey(pos);
            audioMaster.saveJukeboxDefault();
        } catch (NumberFormatException e){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** `pos` was not an integer.");
        }
    }
}
