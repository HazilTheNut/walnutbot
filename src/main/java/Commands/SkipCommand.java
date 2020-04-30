package Commands;

import Audio.AudioMaster;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SkipCommand implements Command {

    @Override public String getCommandName() {
        return "skip";
    }

    @Override public String getHelpName() {
        return "skip";
    }

    @Override public String getHelpDescription() {
        return "skips the current song in the Jukebox";
    }

    @Override public String getSpecificHelpDescription() {
        return "Skips the currently playing song and fetches the next one:\n\n* It will play the next song in the queue if there is one.\n* It will play a random song from the default list if the queue is empty.";
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args) {
        audioMaster.progressJukeboxQueue();
    }
}
