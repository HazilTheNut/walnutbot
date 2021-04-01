package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.Transcriber;

public class ConnectCommand extends Command {

    public ConnectCommand(){
        addSubCommand(new ConnectListCommand());
    }

    @Override String getCommandKeyword() { return "connect"; }

    @Override String getHelpArgs() {
        return "<server> <channel>";
    }

    @Override public String getHelpDescription() {
        return "Makes the bot connect to a particular voice channel";
    }

    @Override String getSpecificHelpDescription() {
        return "Makes the bot connect to a particular voice channel."
            + "\n\nserver - The name of the server to connect to. For server names with spaces in them, wrap the server name with quotation marks ('\"')."
            + "\n\nchannel - The name of the channel to connect to. For channel names with spaces in them, wrap the channel name with quotation marks ('\"').";
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (args.length < 2){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** Too few arguments. Usage: `%s`", getHelpCommandUsage());
        } else {
            String server   = args[0];
            String channel  = args[1];
            if (botManager.connectToVoiceChannel(server, channel))
                Transcriber.printAndPost(feedbackHandler, "Connected to channel `%1$s` on server `%2$s`", channel, server);
            else
                Transcriber.printAndPost(feedbackHandler, "**ERROR:** Channel `%1$s` does not exist on server `%2$s`", channel, server);
        }
    }
}
