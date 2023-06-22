package Commands;

import Audio.IAudioStateMachine;
import Main.WalnutbotEnvironment;

public class JukeboxDefaultListCommand extends Command {

    public JukeboxDefaultListCommand(){
        addSubCommand(new JukeboxDefaultAddCommand());
        addSubCommand(new GenericCommand("disable", "Sets the Default List to nothing, disabling it", (environment, feedbackHandler) -> environment.getAudioStateMachine().clearJukeboxDefaultList()));
        addSubCommand(new JukeboxDefaultLoadCommand());
        addSubCommand(new JukeboxDefaultNewCommand());
        addSubCommand(new JukeboxDefaultModifyCommand());
        addSubCommand(new JukeboxDefaultRemoveCommand());
    }

    @Override public String getCommandKeyword() {
        return "dfl";
    }

    @Override String getHelpArgs() {
        return "[page]";
    }

    @Override public String getHelpDescription() {
        return "Lists the songs in the Jukebox Default List";
    }

    @Override String getSpecificHelpDescription() {
        return String.format("%1$s\n\npage - The page of the list you would like to view", getHelpDescription());
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        environment.getAudioStateMachine().getJukeboxDefaultList().accessAudioKeyPlaylist(playlist -> {
            StringBuilder message = new StringBuilder();
            message.append("*Jukebox Default List:*\n```\n");
            if (environment.getAudioStateMachine().getJukeboxDefaultListLoadState() == IAudioStateMachine.JukeboxDefaultListLoadState.UNLOADED){
                message.append("No active Default List.");
            } else {
                if (args.length < 1)
                    message.append(PlaylistLister.listItems(playlist, null, getHelpCommandUsage(), feedbackHandler.getListPageSize(
                            CommandFeedbackHandler.CommandType.DEFAULT)));
                else
                    message.append(PlaylistLister.listItems(playlist, args[0], getHelpCommandUsage(), feedbackHandler.getListPageSize(
                            CommandFeedbackHandler.CommandType.DEFAULT)));
            }
            message.append("\n```");
            feedbackHandler.sendMessage(message.toString(), false);
        });
    }
}
