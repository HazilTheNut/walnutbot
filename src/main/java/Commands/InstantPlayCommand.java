package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.FileIO;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class InstantPlayCommand extends Command {

    @Override public String getCommandKeyword() {
        return "url";
    }

    @Override public String getHelpArgs() {
        return "<url>";
    }

    @Override public String getHelpDescription() {
        return "Play audio from a url as a Soundboard sound";
    }

    @Override public String getSpecificHelpDescription() {
        return "Plays a sound loaded from the input url.\nSupports Youtube, Soundcloud, Bandcamp, http urls.\n\nurl - The website URL / file path to the desired song.";
    }

    @Override public void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (args.length > 0) {
            String expandedURI = FileIO.expandURIMacros(args[0]);
            if (sanitizeLocalAccess(expandedURI, feedbackHandler, permissions))
                audioMaster.playSoundboardSound(expandedURI);
        }
    }
}
