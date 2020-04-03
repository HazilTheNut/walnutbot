package Commands;

import Audio.AudioMaster;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Command {

    String getCommandName();

    String getHelpName();

    String getHelpDescription();

    String getSpecificHelpDescription();

    void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args);
}
