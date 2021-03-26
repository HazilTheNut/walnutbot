package Commands;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EchoCommand implements Command {

    CommandInterpreter commandInterpreter;

    public EchoCommand(CommandInterpreter commandInterpreter) {
        this.commandInterpreter = commandInterpreter;
    }

    @Override public String getCommandName() {
        return "echo";
    }

    @Override public String getHelpName() {
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

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args) {
        if (event.getChannel().getType().isGuild())
            (event.getChannel().sendMessage(String.format("<@%1$s> I have sent a PM to you.", event.getAuthor().getId()))).queue();
        //Build message
        String message = "".concat(SettingsLoader.getBotConfigValue("command_char")).concat("help");
        //Display message
        User author = event.getAuthor();
        author.openPrivateChannel().complete().sendMessage(message).queue();
    }
}
