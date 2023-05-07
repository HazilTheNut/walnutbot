package Commands;

import Main.WalnutbotEnvironment;
import Utils.SettingsLoader;

import java.util.Objects;

public class EchoCommand extends Command {

    CommandInterpreter commandInterpreter;

    public EchoCommand(CommandInterpreter commandInterpreter) {
        this.commandInterpreter = commandInterpreter;
    }

    @Override public String getCommandKeyword() {
        return "echo";
    }

    @Override public String getHelpDescription() {
        return String.format("Prints \"%shelp\"",
            SettingsLoader.getBotConfigValue("command_char"));
    }

    @Override public String getSpecificHelpDescription() {
        return String.format("Prints \"%shelp\"",
            SettingsLoader.getBotConfigValue("command_char"));
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        //Build message
        String message = "".concat(Objects.requireNonNull(SettingsLoader.getBotConfigValue("command_char"))).concat("help");
        //Display message
        feedbackHandler.sendAuthorPM(message, false);
    }
}
