package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.BotManager;
import Utils.FileIO;
import Utils.Transcriber;

public class SoundboardAddCommand extends Command {

    @Override String getCommandKeyword() {
        return "add";
    }

    @Override public String getHelpDescription() {
        return "Adds a sound to the Soundboard";
    }

    @Override String getSpecificHelpDescription() {
        return "Adds a sound to the Soundboard.\n\nsound name - The name of the Soundboard sound.\nurl - The URL of the Soundboard sound.";
    }

    @Override String getHelpArgs() {
        return "<sound name> <url>";
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (args.length < 2)
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** Not enough arguments. Usage: `%1$s`", getHelpCommandUsage());
        else {
            audioMaster.addSoundboardSound(new AudioKey(args[0], FileIO.expandURIMacros(args[1])));
            Transcriber.printAndPost(feedbackHandler, "Sound `%1$s` (%2$s) added to the Soundboard.", args[0], args[1]);
        }
    }
}
