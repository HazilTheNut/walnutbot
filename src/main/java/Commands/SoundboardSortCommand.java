package Commands;

import Audio.AudioMaster;
import Utils.BotManager;

public class SoundboardSortCommand extends Command {

    @Override String getCommandKeyword() {
        return "sort";
    }

    @Override public String getHelpDescription() {
        return "Sorts the Soundboard list with an A-Z ordering";
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        audioMaster.sortSoundboardList();
    }
}
