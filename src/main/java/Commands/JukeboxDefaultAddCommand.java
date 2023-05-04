package Commands;

import Audio.AudioKeyPlaylistScraper;
import Audio.AudioMaster;
import Utils.IBotManager;
import Utils.FileIO;
import Utils.Transcriber;

public class JukeboxDefaultAddCommand extends Command {

    @Override String getCommandKeyword() {
        return "add";
    }

    @Override String getHelpArgs() {
        return "<url>";
    }

    @Override public String getHelpDescription() {
        return "Adds a song to the Jukebox's Default List";
    }

    @Override String getSpecificHelpDescription() {
        return "Adds a song to the Jukebox's Default List\n\n"
            + "url - The URL of the song to add to the default list";
    }

    @Override void onRunCommand(IBotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (args.length < 1){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** Too few arguments. Usage: `%1$s`", getHelpCommandUsage());
        }
        if (audioMaster.getJukeboxDefaultList() == null){
            Transcriber.printAndPost(feedbackHandler, "**WARNING:** No Default List loaded!");
            return;
        }
        AudioKeyPlaylistScraper scraper = new AudioKeyPlaylistScraper(audioMaster);
        scraper.populateAudioKeyPlaylist(FileIO.expandURIMacros(args[0]), audioMaster.getJukeboxDefaultList(), audioMaster::saveJukeboxDefault);
        Transcriber.printAndPost(feedbackHandler, "Songs added to Jukebox Default List");
    }
}
