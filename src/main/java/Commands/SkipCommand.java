package Commands;

import Audio.AudioMaster;
import Utils.IBotManager;

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

    @Override public void onRunCommand(IBotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        audioMaster.jukeboxSkipToNextSong(true);
    }
}
