package Commands;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import Utils.Transcriber;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RequestCommand implements Command {

    @Override public String getCommandName() {
        return "req";
    }

    @Override public String getHelpName() {
        return "req <url>";
    }

    @Override public String getHelpDescription() {
        return "Requests a song, putting it on the Jukebox queue";
    }

    @Override public String getSpecificHelpDescription() {
        return "Places a song fetched from the url onto the Jukebox queue.\n\nurl - The website URL / file path to the desired song.";
    }

    @Override public void onRunCommand(JDA jda, AudioMaster audioMaster, MessageReceivedEvent event, String[] args) {
        //Input Sanitation
        if (args.length <= 0) return;
        //Check for Permissions
        if (!Boolean.valueOf(SettingsLoader.getSettingsValue("discordAllowLocalAccess", "false")) && args[0].indexOf("http") != 0){
            (event.getChannel().sendMessage("**WARNING:** This bot's admin has blocked access to local files.")).queue();
            return;
        }
        //Make Request
        audioMaster.queueJukeboxSong(args[0], () -> postQueueStatus(audioMaster, event), () -> postErrorStatus(event, args[0]));
    }

    private void postQueueStatus(AudioMaster audioMaster, MessageReceivedEvent event){
        int numberofSongs = audioMaster.getJukeboxQueueList().getAudioKeys().size();
        //if (audioMaster.getCurrentlyPlayingSong() != null) numberofSongs++;
        Transcriber.printAndPost(event.getChannel(), "Track(s) loaded! (%1$d in queue)", numberofSongs);
    }

    private void postErrorStatus(MessageReceivedEvent event, String url){
        Transcriber.printAndPost(event.getChannel(), "**WARNING:** This bot threw an error parsing this url: \"%1$s\"", url);
    }
}
