package Commands;

import Audio.AudioKey;
import Main.WalnutbotEnvironment;
import Utils.Transcriber;

public class SoundboardRemoveCommand extends Command {

    @Override String getCommandKeyword() {
        return "remove";
    }

    @Override public String getHelpDescription() {
        return "Removes a sound from the Soundboard";
    }

    @Override String getSpecificHelpDescription() {
        return "Removes a sound from the Soundboard\n\n"
            + "sound name - The name of the sound in the Soundboard.";
    }

    @Override String getHelpArgs() {
        return "<sound name>";
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        environment.getAudioStateMachine().getSoundboardList().accessAudioKeyPlaylist(playlist -> {
            AudioKey removed = playlist.removeAudioKey(args[0]);
            if (removed != null)
                Transcriber.printAndPost(feedbackHandler, "Sound `%1$s` removed from the Soundboard.", removed.getName());
            else
                Transcriber.printAndPost(feedbackHandler, "**ERROR:** Sound `%1$s` doesn't exist in the Soundboard.", args[0]);
        });
    }
}
