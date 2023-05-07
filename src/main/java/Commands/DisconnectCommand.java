package Commands;

import Main.WalnutbotEnvironment;

public class DisconnectCommand extends Command {

    @Override String getCommandKeyword() { return "disconnect"; }

    @Override String getHelpArgs() {
        return "";
    }

    @Override public String getHelpDescription() {
        return "Makes the bot disconnect from its connected voice channel";
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        environment.getCommunicationPlatformManager().disconnectFromVoiceChannel();
    }
}
