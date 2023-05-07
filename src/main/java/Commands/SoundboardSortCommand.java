package Commands;

import Audio.AudioKeyPlaylist;
import Audio.AudioMaster;
import CommuncationPlatform.ICommunicationPlatformManager;
import Main.WalnutbotEnvironment;

public class SoundboardSortCommand extends Command {

    @Override String getCommandKeyword() {
        return "sort";
    }

    @Override public String getHelpDescription() {
        return "Sorts the Soundboard list with an A-Z ordering";
    }

    @Override void onRunCommand(ICommunicationPlatformManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        audioMaster.sortSoundboardList();
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        environment.getAudioStateMachine().getSoundboardList().accessAudioKeyPlaylist(AudioKeyPlaylist::sort);
    }
}
