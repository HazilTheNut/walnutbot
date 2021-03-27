package Commands;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class InstantPlayCommand extends Command {

    @Override public String getCommandName() {
        return "url";
    }

    @Override public String getHelpArgs() {
        return "<url>";
    }

    @Override public String getHelpDescription() {
        return "Play audio from url as a soundboard sound";
    }

    @Override public String getSpecificHelpDescription() {
        return "Plays a sound loaded from the input url.\nSupports Youtube, Soundcloud, Bandcamp, http urls.\n\nurl - The website URL / file path to the desired song.";
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (args.length > 0) {
            //Permissions Check
            if (!Boolean.valueOf(SettingsLoader.getSettingsValue("discordAllowLocalAccess", "false")) && args[0].indexOf("http") != 0){
                feedbackHandler.sendMessage("**WARNING:** This bot's admin has blocked access to local files.");
                return;
            }
            //Play Sound
            audioMaster.playSoundboardSound(args[0]);
        }
    }
}
