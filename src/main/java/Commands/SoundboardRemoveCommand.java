package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import CommuncationPlatform.ICommunicationPlatformManager;
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

    @Override void onRunCommand(ICommunicationPlatformManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (args.length < 1)
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** Not enough arguments. Usage: `%1$s`", getHelpCommandUsage());
        else {
            AudioKey removed = audioMaster.removeSoundboardSound(args[0]);
            if (removed != null)
                Transcriber.printAndPost(feedbackHandler, "Sound `%1$s` removed from the Soundboard.", removed.getName());
            else
                Transcriber.printAndPost(feedbackHandler, "**ERROR:** Sound `%1$s` doesn't exist in the Soundboard.", args[0]);
        }
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
