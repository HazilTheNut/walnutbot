package Commands;

import Audio.AudioMaster;
import Utils.BotManager;

public class ListQueueCommand extends Command {

    @Override public String getCommandKeyword() {
        return "list";
    }

    @Override String getHelpArgs() {
        return "<page>";
    }

    @Override public String getHelpDescription() {
        return "Lists the songs in the Jukebox Queue";
    }

    @Override String getSpecificHelpDescription() {
        return String.format("%1$s\n\npage - The page of the list you would like to view", getHelpDescription());
    }

    @Override public void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        StringBuilder message = new StringBuilder();
            message.append("*Jukebox Queue:*\n```\n");
        if (audioMaster.getCurrentlyPlayingSong() != null)
            message.append("Now Playing: ").append(audioMaster.getCurrentlyPlayingSong().getTrackName()).append("\n===\n");
        if (audioMaster.getJukeboxQueueList().getAudioKeys().size() == 0)
            message.append("No songs are currently queued; playing random songs from default playlist.");
        else {
            if (args.length < 1)
                message.append(PlaylistLister.listItems(audioMaster.getJukeboxQueueList(), null, getHelpCommandUsage()));
            else
                message.append(PlaylistLister.listItems(audioMaster.getJukeboxQueueList(), args[0], getHelpCommandUsage()));
        }
        message.append("\n```");
        feedbackHandler.sendMessage(message.toString());
    }
}
