package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.BotManager;
import Utils.Transcriber;

public class JukeboxDefaultModifyCommand extends Command {

    @Override String getCommandKeyword() {
        return "modify";
    }

    @Override String getHelpArgs() {
        return "<pos> [args..]";
    }

    @Override public String getHelpDescription() {
        return "Modifies the properties of a Soundboard sound";
    }

    @Override String getSpecificHelpDescription() {
        return "Modifies the properties of a name-matching Soundboard sound\n\n"
            + "pos - the position of the song in the Default List to modify\n"
            + "args.. - Additional arguments taken in the form of \"-<flag> <value>\", as described below:\n"
            + "    -name : Renames the Soundboard sound to the new name\n"
            + "    -url  : Assigns a nwe url to the Soundboard sound";
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        int index = 1;
        AudioKey newData = new AudioKey(null, null);
        while (index + 1 < args.length){
            if (args[index].equals("-name"))
                newData.setName(args[index+1]);
            else if (args[index].equals("-url"))
                if (sanitizeLocalAccess(args[index+1], feedbackHandler, permissions))
                    newData.setUrl(args[index+1]);
                else return;
            index += 2;
        }
        try {
            int pos = Integer.valueOf(args[0]);
            String originalName = audioMaster.getJukeboxDefaultList().getKey(pos).getName();
            audioMaster.getJukeboxDefaultList().modifyAudioKey(pos, newData);
            Transcriber.printAndPost(feedbackHandler, "Song `%1$s` successfully modified to \"%1$s\".", originalName, audioMaster.getJukeboxDefaultList().getKey(pos).toString());
            audioMaster.saveJukeboxDefault();
        } catch (NumberFormatException e){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** `pos` was not an integer.");
        }
    }
}
