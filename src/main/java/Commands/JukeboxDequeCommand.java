package Commands;

import Audio.AudioMaster;
import Utils.IBotManager;
import Utils.Transcriber;

public class JukeboxDequeCommand extends Command {

    @Override String getCommandKeyword() {
        return "deque";
    }

    @Override String getHelpArgs() {
        return "<pos>";
    }

    @Override public String getHelpDescription() {
        return "Removes the song at pos in the Jukebox Queue";
    }

    @Override String getSpecificHelpDescription() {
        return getHelpDescription().concat("\n\npos - The position in the Jukebox Queue, the song at which you intend to remove");
    }

    @Override void onRunCommand(IBotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        int pos = -1;
        try {
            pos = Integer.valueOf(args[0]);
        } catch (NumberFormatException e){
            Transcriber
                .printAndPost(feedbackHandler, "**WARNING:** `%1$s` is not an integer value!", args[0]);
        }
        if (pos >= 0 && pos < audioMaster.getJukeboxQueueList().getAudioKeys().size()){
            Transcriber.printAndPost(feedbackHandler, "Song `%1$s` removed from the queue.", audioMaster.getJukeboxQueueList().removeAudioKey(pos));
        }
    }
}
