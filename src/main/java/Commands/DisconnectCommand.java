package Commands;

import Audio.AudioMaster;
import Utils.BotManager;

public class DisconnectCommand extends Command {

    @Override String getCommandKeyword() { return "disconnect"; }

    @Override String getHelpArgs() {
        return "";
    }

    @Override public String getHelpDescription() {
        return "Makes the bot disconnect from its connected voice channel.";
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        botManager.disconnectFromVoiceChannel();
    }
}
