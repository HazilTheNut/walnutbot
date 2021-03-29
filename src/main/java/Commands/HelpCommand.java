package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.SettingsLoader;

import java.util.Comparator;
import java.util.List;

public class HelpCommand extends Command {

    CommandInterpreter commandInterpreter;

    public HelpCommand(CommandInterpreter commandInterpreter) {
        this.commandInterpreter = commandInterpreter;
    }

    @Override public String getCommandKeyword() {
        return "help";
    }

    @Override public String getHelpArgs() {
        return "[command]";
    }

    @Override public String getHelpDescription() {
        return "Displays the help message";
    }

    @Override public String getSpecificHelpDescription() {
        return String.format("Lists the commands you can give to this bot.\n%shelp <command> for detailed info on a specific command",
            SettingsLoader.getBotConfigValue("command_char"));
    }

    @Override public void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        //Build message
        String message;
        Command cmd = getSubCommandFromArgs(args);
        if (cmd != null) { //Getting help on a specific command
            message = String.format("```   %3$s%1$s\n-------------\n%2$s```",
                cmd.getHelpCommandUsage(),
                cmd.getSpecificHelpDescription(),
                SettingsLoader.getBotConfigValue("command_char"));
        } else { //The general help info (or if asking specific help on a command that doesn't exist
            StringBuilder builder = new StringBuilder("**Available Commands:**\n```\n");
            int longestLength = commandInterpreter.getLongestCommandHelpName();
            List<Command> commandList = commandInterpreter.getExpandedCommandList();
            commandList.sort(Comparator.comparing(Command::getCommandTreeStr));
            for (Command command : commandList) {
                String commandHelpName = command.getHelpCommandUsage();
                String commandDescription = command.getHelpDescription();
                builder.append(String.format("%4$s%1$s%3$s : %2$s\n", commandHelpName, commandDescription,
                    createCommandNameSpacing(longestLength - commandHelpName.length()), SettingsLoader.getBotConfigValue("command_char")));
            }
            builder.append("```");
            message = builder.toString();
        }
        //Display message
        feedbackHandler.sendAuthorPM(message);
    }

    private String createCommandNameSpacing(int length){
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) builder.append(" ");
        return builder.toString();
    }

    private Command getSubCommandFromArgs(String[] args){
        if (args.length <= 0)
            return null;
        StringBuilder fullCommand = new StringBuilder();
        for (String arg : args) fullCommand.append(arg).append(' ');
        String commandStr = fullCommand.toString().trim();
        for (Command command : commandInterpreter.getExpandedCommandList())
            if (command.getCommandTreeStr().equals(commandStr)) return command;
        return null;
    }
}
