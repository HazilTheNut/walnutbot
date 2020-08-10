package Commands;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SoundboardCommand implements Command {

    @Override public String getCommandName() {
        return "sb";
    }

    @Override public String getHelpName() {
        return "sb <sound name>";
    }

    @Override public String getHelpDescription() {
        return "Plays a sound from the Soundboard";
    }

    @Override public String getSpecificHelpDescription() {
        return "Plays a sound from the soundboard.\nDo " + SettingsLoader.getBotConfigValue("command_char") + "sblist for a list of the available sounds.\n\nsound name - The name of the sound from the soundboard.";
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args) {
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
