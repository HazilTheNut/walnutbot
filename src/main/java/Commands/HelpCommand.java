package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.SettingsLoader;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
            message = String.format("```   %3$s%1$s\n---------------------------\n%2$s```",
                cmd.getHelpCommandUsage(),
                cmd.getSpecificHelpDescription(),
                SettingsLoader.getBotConfigValue("command_char"));
        } else { //The general help info (or if asking specific help on a command that doesn't exist
            StringBuilder builder = new StringBuilder("**Available Commands:**\n```\n");
            List<Command> commandList = getAvailableCommands(permissions);
            commandList.sort(Comparator.comparing(Command::getCommandTreeStr));
            builder.append(generateGeneralHelpList(args, commandList, feedbackHandler.getListPageSize(CommandFeedbackHandler.CommandType.HELP)));
            builder.append("```");
            message = builder.toString();
        }
        //Display message
        feedbackHandler.sendAuthorPM(message, false);
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

    private List<Command> getAvailableCommands(byte permission){
        LinkedList<Command> availableCommands = new LinkedList<>();
        for (Command command : commandInterpreter.getExpandedCommandList()){
            if (command.isPermissionSufficient(permission)) availableCommands.add(command);
        }
        return availableCommands;
    }

    private int getLongestCommandHelpName(List<Command> commands){
        int max = 0;
        for (Command command : commands)
            max = Math.max(max, command.getHelpCommandUsage().length());
        return max;
    }

    private String generateGeneralHelpList(String[] args, List<Command> commands, int pageSize){
        int page = 1;
        if (args.length >= 1) {
            try {
                page = Math.max(1, Integer.valueOf(args[0]));
            } catch (NumberFormatException ignored) { }
        }
        StringBuilder list = new StringBuilder();

        // Get number of pages
        int pagecount = (int) Math.ceil((float) commands.size() / pageSize);
        if (pagecount > 1)
            list.append(String.format("Page %1$d of %2$d:\n", page, pagecount));

        // List elements
        int baseAddr = (page - 1) * pageSize;
        int longestLength = getLongestCommandHelpName(commands);
        ListIterator<Command> iterator = commands.listIterator(baseAddr);
        for (int i = 0; i < pageSize; i++) {
            int addr = baseAddr + i;
            if (addr >= commands.size())
                break;
            Command command = iterator.next();
            String commandHelpName = command.getHelpCommandUsage();
            String commandDescription = command.getHelpDescription();
            list.append(String.format("%4$s%1$s%3$s : %2$s\n", commandHelpName, commandDescription,
                createCommandNameSpacing(longestLength - commandHelpName.length()), SettingsLoader.getBotConfigValue("command_char")));
        }

        // Remind usage of command to get more pages
        if (pagecount > 1)
            list.append("\nDo ").append(SettingsLoader.getBotConfigValue("command_char"))
                .append("help <page> to see further into the list.");
        // Return
        return list.toString();
    }
}
