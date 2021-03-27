package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class SoundboardCommand extends Command {

    public SoundboardCommand(){
        addSubCommand(new SoundboardListCommand());
        addSubCommand(new InstantPlayCommand());
    }

    @Override public String getCommandName() {
        return "sb";
    }

    @Override public String getHelpArgs() {
        return "<sound name>";
    }

    @Override public String getHelpDescription() {
        return "Plays a sound from the Soundboard";
    }

    @Override public String getSpecificHelpDescription() {
        return "Plays a sound from the soundboard.\nDo " + SettingsLoader.getBotConfigValue("command_char") + "sblist for a list of the available sounds.\n\nsound name - The name of the sound from the soundboard.";
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
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
