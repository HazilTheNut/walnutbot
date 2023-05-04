package Commands;

import Audio.AudioMaster;
import Utils.IBotManager;

public class SoundboardSortCommand extends Command {

    @Override String getCommandKeyword() {
        return "sort";
    }

    @Override public String getHelpDescription() {
        return "Sorts the Soundboard list with an A-Z ordering";
    }

    @Override void onRunCommand(IBotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        audioMaster.sortSoundboardList();
    }
}
