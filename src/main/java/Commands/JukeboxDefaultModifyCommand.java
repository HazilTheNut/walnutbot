package Commands;

import Audio.AudioKey;
import Main.WalnutbotEnvironment;
import Utils.FileIO;
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

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        environment.getAudioStateMachine().getJukeboxDefaultList().accessAudioKeyPlaylist(playlist -> {
            int index = 1;
            AudioKey newData = new AudioKey(null, null);
            while (index + 1 < args.length){
                if (args[index].equals("-name"))
                    newData.setName(args[index+1]);
                else if (args[index].equals("-url")) {
                    String expandedURI = FileIO.expandURIMacros(args[index + 1]);
                    if (sanitizeLocalAccess(expandedURI, feedbackHandler, permissions))
                        newData.setUrl(expandedURI);
                    else
                        return;
                }
                index += 2;
            }
            try {
                int pos = Integer.parseInt(args[0]);
                String originalName = playlist.getKey(pos).getName();
                playlist.modifyAudioKey(pos, newData);
                Transcriber.printAndPost(feedbackHandler, "Song `%1$s` successfully modified to \"%1$s\".", originalName, playlist.getKey(pos).toString());
            } catch (NumberFormatException e){
                Transcriber.printAndPost(feedbackHandler, "**ERROR:** `pos` is not an integer value");
            }
        });
    }
}
