package Commands;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HelpCommand implements Command {

    CommandInterpreter commandInterpreter;

    public HelpCommand(CommandInterpreter commandInterpreter) {
        this.commandInterpreter = commandInterpreter;
    }

    @Override public String getCommandName() {
        return "help";
    }

    @Override public String getHelpName() {
        return "help [command]";
    }

    @Override public String getHelpDescription() {
        return "Displays this message";
    }

    @Override public String getSpecificHelpDescription() {
        return String.format("Lists the commands you can give to this bot.\n%shelp <command> for detailed info on a specific command",
            SettingsLoader.getBotConfigValue("command_char"));
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args) {
        if (event.getChannel().getType().isGuild())
            (event.getChannel().sendMessage(String.format("<@%1$s> I have sent a PM to you.", event.getAuthor().getId()))).queue();
        //Build message
        String message;
        if (args.length > 0 && commandInterpreter.getCommandMap().containsKey(args[0])) { //Getting help on a specific command
            message = String.format("```   %3$s%1$s\n-------------\n%2$s```", commandInterpreter.getCommandMap().get(args[0]).getHelpName(),
                commandInterpreter.getCommandMap().get(args[0]).getSpecificHelpDescription(), SettingsLoader.getBotConfigValue("command_char"));
        } else { //The general help info (or if asking specific help on a command that doesn't exist
            StringBuilder builder = new StringBuilder("**Available Commands:**\n```\n");
            int longestLength = commandInterpreter.getLongestCommandHelpName();
            for (String command : commandInterpreter.getCommandMap().keySet()) {
                String commandHelpName = commandInterpreter.getCommandMap().get(command).getHelpName();
                String commandDescription = commandInterpreter.getCommandMap().get(command).getHelpDescription();
                builder.append(String.format("%4$s%1$s%3$s : %2$s\n", commandHelpName, commandDescription,
                    createCommandNameSpacing(longestLength - commandHelpName.length()), SettingsLoader.getBotConfigValue("command_char")));
            }
            builder.append("```");
            message = builder.toString();
        }
        //Display message
        User author = event.getAuthor();
        author.openPrivateChannel().complete().sendMessage(message).queue();
    }

    private String createCommandNameSpacing(int length){
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) builder.append(" ");
        return builder.toString();
    }
}
