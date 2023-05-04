package Commands;

import Audio.AudioMaster;
import Utils.IBotManager;
import Utils.Transcriber;

public class VolumeCommand extends Command {

    @Override String getCommandKeyword() {
        return "vol";
    }

    @Override String getHelpArgs() {
        return "<channel> <volume>";
    }

    @Override public String getHelpDescription() {
        return "Changes the volume settings.";
    }

    @Override String getSpecificHelpDescription() {
        return getHelpDescription().concat("\n\n"
            + "channel - Either \"main\", \"sb\", or \"jb\" for the Main, Soundboard, and Jukebox audio channels respectively\n"
            + "volume - An integer ranging from 0-100 as a volume percentage.");
    }

    @Override void onRunCommand(IBotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 2, feedbackHandler))
            return;
        int vol;
        switch (args[0]){
            case "main":
                vol = getVolumeAmount(args[1], audioMaster.getMainVolume());
                audioMaster.setMainVolume(vol);
                Transcriber.printAndPost(feedbackHandler, "Main Volume changed to `%1$d`", vol);
                break;
            case "sb":
                vol = getVolumeAmount(args[1], audioMaster.getSoundboardVolume());
                audioMaster.setSoundboardVolume(vol);
                Transcriber.printAndPost(feedbackHandler, "Soundboard Volume changed to `%1$d`", vol);
                break;
            case "jb":
                vol = getVolumeAmount(args[1], audioMaster.getJukeboxVolume());
                audioMaster.setJukeboxVolume(vol);
                Transcriber.printAndPost(feedbackHandler, "Jukebox Volume changed to `%1$d`", vol);
                break;
            default:
                Transcriber.printAndPost(feedbackHandler, "**ERROR:** `%1$s` is not a channel. Channels: `main`, `sb`, `jb`");
        }
    }

    private int getVolumeAmount(String volumeStr, int prevValue){
        try {
            return Math.max(0, Math.min(Integer.valueOf(volumeStr), 100));
        } catch (NumberFormatException e){
            return prevValue;
        }
    }
}
