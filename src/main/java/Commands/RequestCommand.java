package Commands;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Audio.AudioMaster;
import Main.WalnutbotEnvironment;
import Utils.FileIO;
import Utils.Transcriber;

public class RequestCommand extends Command {

    public RequestCommand(){
        addSubCommand(new GenericCommand("clearqueue", "Clears the Jukebox Queue", (environment, feedbackHandler) -> {
            environment.getAudioStateMachine().getJukeboxQueue().accessAudioKeyPlaylist(AudioKeyPlaylist::clearPlaylist);
            Transcriber.printAndPost(feedbackHandler, "Jukebox Queue cleared.");
        }));
        addSubCommand(new JukeboxDefaultListCommand());
        addSubCommand(new JukeboxDequeCommand());
        addSubCommand(new ListQueueCommand());
        addSubCommand(new JukeboxLoopCommand());
        addSubCommand(new GenericCommand("pause", "Pauses the Jukebox player", ((environment, feedbackHandler) -> environment.getAudioStateMachine().pauseJukebox())));
        addSubCommand(new GenericCommand("play", "Starts / unpauses the Jukebox player", ((environment, feedbackHandler) -> environment.getAudioStateMachine().resumeJukebox())));
        addSubCommand(new JukeboxPostponeCommand());
        addSubCommand(new GenericCommand("shuffle", "Shuffles the Jukebox Queue", (environment, feedbackHandler) -> {
            environment.getAudioStateMachine().getJukeboxQueue().accessAudioKeyPlaylist(AudioKeyPlaylist::shuffle);
            Transcriber.printAndPost(feedbackHandler, "Jukebox Queue shuffled.");
        }));
        addSubCommand(new SkipCommand());
    }

    @Override public String getCommandKeyword() {
        return "jb";
    }

    @Override public String getHelpArgs() {
        return "<url>";
    }

    @Override public String getHelpDescription() {
        return "Requests a song, putting it on the Jukebox Queue";
    }

    @Override public String getSpecificHelpDescription() {
        return "Places a song fetched from the url onto the Jukebox queue.\n\nurl - The website URL / file path to the desired song.";
    }

    @Override public void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        //Input Sanitation
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        String expandedURI = FileIO.expandURIMacros(args[0]);
        if (sanitizeLocalAccess(expandedURI, feedbackHandler, permissions))
            environment.getAudioStateMachine().enqueueJukeboxSong(new AudioKey("TEMP", expandedURI));
    }

    private void postQueueStatus(AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler){
        int numberofSongs = audioMaster.getJukeboxQueueList().getAudioKeys().size();
        //if (audioMaster.getCurrentlyPlayingSong() != null) numberofSongs++;
        Transcriber.printAndPost(feedbackHandler, "Track(s) loaded! (%1$d in queue)", numberofSongs);
    }

    private void postErrorStatus(CommandFeedbackHandler feedbackHandler, String url){
        Transcriber.printAndPost(feedbackHandler, "**WARNING:** This bot threw an error parsing this url: \"%1$s\"", url);
    }
}
