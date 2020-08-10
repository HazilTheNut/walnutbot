package Commands;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class InstantPlayCommand implements Command {

    @Override public String getCommandName() {
        return "sbip";
    }

    @Override public String getHelpName() {
        return "sbip <url>";
    }

    @Override public String getHelpDescription() {
        return "Soundboard Instant Play";
    }

    @Override public String getSpecificHelpDescription() {
        return "Plays a sound loaded from the input url.\nSupports Youtube, Soundcloud, Bandcamp, http urls.\n\nurl - The website URL / file path to the desired song.";
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args) {
        if (args.length > 0) {
            //Permissions Check
            if (!Boolean.valueOf(SettingsLoader.getSettingsValue("discordAllowLocalAccess", "false")) && args[0].indexOf("http") != 0){
                (event.getChannel().sendMessage("**WARNING:** This bot's admin has blocked access to local files.")).queue();
                return;
            }
            //Play Sound
            audioMaster.playSoundboardSound(args[0]);
        }
    }
}
