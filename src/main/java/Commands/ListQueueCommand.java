package Commands;

import Main.WalnutbotEnvironment;

public class ListQueueCommand extends Command {

    @Override public String getCommandKeyword() {
        return "list";
    }

    @Override String getHelpArgs() {
        return "[page]";
    }

    @Override public String getHelpDescription() {
        return "Lists the songs in the Jukebox Queue";
    }

    @Override String getSpecificHelpDescription() {
        return String.format("%1$s\n\npage - The page of the list you would like to view", getHelpDescription());
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        environment.getAudioStateMachine().getJukeboxQueue().accessAudioKeyPlaylist(playlist -> {
            StringBuilder message = new StringBuilder();
            message.append("*JJukebox Queue:*\n```\n");
            if (environment.getAudioStateMachine().getJukeboxCurrentlyPlayingSong() != null)
                message.append("Now Playing: ").append(environment.getAudioStateMachine().getJukeboxCurrentlyPlayingSong().getName()).append("\n===\n");
            if (playlist.isEmpty()) {
                message.append("No songs are currently queued; playing random songs from default playlist.");
            } else {
                if (args.length < 1)
                    message.append(PlaylistLister.listItems(playlist, null, getHelpCommandUsage(), feedbackHandler.getListPageSize(
                            CommandFeedbackHandler.CommandType.QUEUE)));
                else
                    message.append(PlaylistLister.listItems(playlist, args[0], getHelpCommandUsage(), feedbackHandler.getListPageSize(
                            CommandFeedbackHandler.CommandType.QUEUE)));
            }
            message.append("\n```");
            feedbackHandler.sendMessage(message.toString(), false);
        });
    }
}
