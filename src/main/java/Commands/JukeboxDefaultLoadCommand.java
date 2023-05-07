package Commands;

import Audio.AudioKeyPlaylist;
import Audio.AudioKeyPlaylistScraper;
import Audio.AudioMaster;
import CommuncationPlatform.ICommunicationPlatformManager;
import Main.WalnutbotEnvironment;
import Utils.FileIO;
import Utils.Transcriber;

import java.io.File;

public class JukeboxDefaultLoadCommand extends Command {

    @Override String getCommandKeyword() {
        return "load";
    }

    @Override String getHelpArgs() {
        return "<uri>";
    }

    @Override public String getHelpDescription() {
        return "Loads a playlist, either online or a local file, as the Default List";
    }

    @Override String getSpecificHelpDescription() {
        return getHelpDescription().concat("\n\n"
            + "uri - The URI of the playlist");
    }

    @Override void onRunCommand(ICommunicationPlatformManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        File file = new File(FileIO.expandURIMacros(args[0]));
        if (file.isFile()){
            audioMaster.loadJukeboxPlaylist(new AudioKeyPlaylist(file), true);
            Transcriber.printAndPost(feedbackHandler, "Playlist at location `%1$s` loaded!", file.getAbsolutePath());
        } else {
            AudioKeyPlaylist playlist = new AudioKeyPlaylist(args[0], args[0]);
            audioMaster.loadJukeboxPlaylist(playlist, false);
            AudioKeyPlaylistScraper scraper = new AudioKeyPlaylistScraper(audioMaster);
            scraper.populateAudioKeyPlaylist(args[0], playlist, () -> {});
        }
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        if (sanitizeLocalAccess(args[0], feedbackHandler, permissions))
            environment.getAudioStateMachine().loadJukeboxDefaultList(args[0]);
    }
}
