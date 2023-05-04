package Commands;

import Audio.AudioMaster;
import Utils.IBotManager;

public class GenericCommand extends Command {

    private String keyword;
    private String desc;
    private CommandAction commandAction;

    public GenericCommand(String keyword, String desc, CommandAction commandAction) {
        this.keyword = keyword;
        this.desc = desc;
        this.commandAction = commandAction;
    }

    @Override String getCommandKeyword() {
        return keyword;
    }

    @Override public String getHelpDescription() {
        return desc;
    }

    @Override void onRunCommand(IBotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        commandAction.doAction(audioMaster, feedbackHandler);
    }

    interface CommandAction {
        void doAction(AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler);
    }
}
