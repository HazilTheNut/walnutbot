package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.BotManager;
import Utils.FileIO;
import Utils.Transcriber;

public class SoundboardModifyCommand extends Command {

    @Override String getCommandKeyword() {
        return "modify";
    }

    @Override String getHelpArgs() {
        return "<sound name> [args..]";
    }

    @Override public String getHelpDescription() {
        return "Modifies the properties of a Soundboard sound";
    }

    @Override String getSpecificHelpDescription() {
        return "Modifies the properties of a name-matching Soundboard sound\n\n"
            + "sound name - The name of the Soundboard sound to modify\n"
            + "args.. - Additional arguments taken in the form of \"-<flag> <value>\", as described below:\n"
            + "    -name : Renames the Soundboard sound to the new name\n"
            + "    -url  : Assigns a nwe url to the Soundboard sound";
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (args.length < 1){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** Not enough arguments. Usage: `%1$s`", getHelpCommandUsage());
            return;
        }
        int index = 1;
        AudioKey newData = new AudioKey(null, null);
        while (index + 1 < args.length){
            if (args[index].equals("-name"))
                newData.setName(args[index+1]);
            else if (args[index].equals("-url")) {
                String expandedURI = FileIO.expandURIMacros(args[0]);
                if (sanitizeLocalAccess(expandedURI, feedbackHandler, permissions))
                    newData.setUrl(expandedURI);
                else
                    return;
            }
            index += 2;
        }
        if (audioMaster.modifySoundboardSound(args[0], newData))
            Transcriber.printAndPost(feedbackHandler, "Sound `%1$s` successfully modified.", args[0]);
        else
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** Sound `%1$s` not found!", args[0]);
    }
}