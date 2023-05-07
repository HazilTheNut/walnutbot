package Commands;

import Main.WalnutbotEnvironment;

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

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        commandAction.doAction(environment, feedbackHandler);
    }

    interface CommandAction {
        void doAction(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler);
    }
}
