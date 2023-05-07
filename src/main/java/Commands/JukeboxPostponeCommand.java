package Commands;

import Audio.AudioKey;
import Main.WalnutbotEnvironment;
import Utils.Transcriber;

public class JukeboxPostponeCommand extends Command {

    @Override String getCommandKeyword() {
        return "postpone";
    }

    @Override String getHelpArgs() {
        return "<pos>";
    }

    @Override public String getHelpDescription() {
        return "Moves queued song at pos to the end of the Jukebox Queue";
    }

    @Override String getSpecificHelpDescription() {
        return getHelpDescription().concat("\n\npos - The position in the Jukebox Queue, the song at which you intend to postpone");
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        int pos = -1;
        try {
            pos = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** `%1$s` is not an integer value.", args[0]);
        }
        // Acquire song
        AudioKey[] key = new AudioKey[1];
        int finalPos = pos;
        environment.getAudioStateMachine().getJukeboxQueue().accessAudioKeyPlaylist(playlist -> key[0] = playlist.removeAudioKey(finalPos));
        // Requeue it
        if (key[0] != null) {
            environment.getAudioStateMachine().enqueueJukeboxSong(key[0]);
        }
    }
}
