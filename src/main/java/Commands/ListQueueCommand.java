package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import net.dv8tion.jda.api.JDA;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ListQueueCommand extends Command {
    @Override public String getCommandKeyword() {
        return "list";
    }

    @Override public String getHelpDescription() {
        return "Lists the songs currently queued in the Jukebox";
    }

    @Override public String getSpecificHelpDescription() {
        return "Prints a list of songs in the Jukebox queue.";
    }

    @Override public void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        StringBuilder message = new StringBuilder("*Jukebox Queue:*\n```\n");
        if (audioMaster.getCurrentlyPlayingSong() != null)
            message.append("Now Playing: ").append(audioMaster.getCurrentlyPlayingSong().getTrackName()).append("\n===\n");
        for (int i = 0; i < audioMaster.getJukeboxQueueList().getAudioKeys().size(); i++) {
            message.append('[').append(i).append("] ").append(audioMaster.getJukeboxQueueList().getAudioKeys().get(i).getTrackName()).append('\n');
        }
        if (audioMaster.getJukeboxQueueList().getAudioKeys().size() == 0)
            message.append("No songs are currently queued; playing random songs from default playlist.");
        message.append("\n```");
        feedbackHandler.sendMessage(message.toString());
    }
}
