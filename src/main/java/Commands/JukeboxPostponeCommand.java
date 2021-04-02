package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
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

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        int pos = -1;
        try {
            pos = Integer.valueOf(args[0]);
        } catch (NumberFormatException e){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** `%1$s` is not an integer value.", args[0]);
        }
        if (pos >= 0 && pos < audioMaster.getJukeboxQueueList().getAudioKeys().size()){
            audioMaster.queueJukeboxSong(audioMaster.getJukeboxQueueList().removeAudioKey(pos), () -> Transcriber.printAndPost(feedbackHandler, "Song postponed!"), () -> {});
        }
    }
}
