package Commands;

import Main.WalnutbotEnvironment;
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

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        int pos = -1;
        try {
            pos = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            Transcriber
                    .printAndPost(feedbackHandler, "**WARNING:** `%1$s` is not an integer value!", args[0]);
        }
        int finalPos = pos;
        environment.getAudioStateMachine().getJukeboxQueue().accessAudioKeyPlaylist(playlist -> {
            if (finalPos >= 0 && finalPos < playlist.getAudioKeys().size()){
                Transcriber.printAndPost(feedbackHandler, "Song `%1$s` removed from the queue.", playlist.removeAudioKey(finalPos));
            }
        });
    }
}
