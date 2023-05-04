package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.IBotManager;
import Utils.SettingsLoader;

public class SoundboardCommand extends Command {

    public SoundboardCommand(){
        addSubCommand(new SoundboardAddCommand());
        addSubCommand(new SoundboardListCommand());
        addSubCommand(new SoundboardModifyCommand());
        addSubCommand(new SoundboardRemoveCommand());
        addSubCommand(new SoundboardSortCommand());
        addSubCommand(new GenericCommand("stop", "Forcibly stops the Soundboard player", ((audioMaster, feedbackHandler) -> audioMaster.resumeJukebox())));
        addSubCommand(new InstantPlayCommand());
    }

    @Override public String getCommandKeyword() {
        return "sb";
    }

    @Override public String getHelpArgs() {
        return "<sound name>";
    }

    @Override public String getHelpDescription() {
        return "Plays a sound from the Soundboard";
    }

    @Override public String getSpecificHelpDescription() {
        return "Plays a sound from the soundboard.\nDo \"" + SettingsLoader.getBotConfigValue("command_char") + "sb list\" for a list of the available sounds.\n\nsound name - The name of the sound from the soundboard.";
    }

    @Override public void onRunCommand(IBotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        //Input sanitation
        if (args[0] == null) return;
        for (AudioKey audioKey : audioMaster.getSoundboardList().getAudioKeys()){
            if (audioKey.getName().equals(args[0])){
                audioMaster.playSoundboardSound(audioKey.getUrl());
                return;
            }
        }
    }
}
