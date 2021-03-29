package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import net.dv8tion.jda.api.JDA;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class SkipCommand extends Command {

    @Override public String getCommandKeyword() {
        return "skip";
    }

    @Override public String getHelpDescription() {
        return "Skips the current song in the Jukebox";
    }

    @Override public String getSpecificHelpDescription() {
        return "Skips the currently playing song and fetches the next one:\n\n* It will play the next song in the queue if there is one.\n* It will play a random song from the default list if the queue is empty.";
    }

    @Override public void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        audioMaster.jukeboxSkipToNextSong(true);
    }
}
