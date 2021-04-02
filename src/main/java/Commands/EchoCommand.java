package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

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

    @Override public void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        //Build message
        String message = "".concat(SettingsLoader.getBotConfigValue("command_char")).concat("help");
        //Display message
        feedbackHandler.sendAuthorPM(message, false);
    }

}
