package Utils;

import Audio.AudioMaster;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DiscordBotManager implements AudioEventListener, BotManager {

    private boolean useDefaultStatus;
    private boolean showCurrentPlayingSong;

    private JDA jda;
    private AudioMaster audioMaster;

    public DiscordBotManager(JDA jda, AudioMaster audioMaster){
        this.jda = jda;
        this.audioMaster = audioMaster;
        useDefaultStatus         = Boolean.valueOf(SettingsLoader.getBotConfigValue("status_use_default"));
        showCurrentPlayingSong   = Boolean.valueOf(SettingsLoader.getBotConfigValue("status_show_current_song"));
    }

    @Override
    public void updateStatus(){
        updateStatus(useDefaultStatus);
    }

    private void updateStatus(boolean useDefault){
        if (showCurrentPlayingSong && audioMaster.getCurrentlyPlayingSong() != null){ // Status based on currently-playing song (currentlyPlayingSong is null when no song is playing)
            String message = String.format("%s - %s",
                audioMaster.getCurrentlyPlayingSong().getLoadedTrack().getInfo().title,
                audioMaster.getCurrentlyPlayingSong().getLoadedTrack().getInfo().author);
            jda.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT, message));
        } else if (useDefault){ // Status based on default configuration
            jda.getPresence().setActivity(Activity
                .of(Activity.ActivityType.DEFAULT, "sounds / type " + SettingsLoader.getBotConfigValue("command_char") + "help"));
        } else { // Status based on user-defined configuration
            try {
                Activity.ActivityType type;
                switch (SettingsLoader.getBotConfigValue("status_type").toUpperCase()){
                    case "WATCHING":
                        type = Activity.ActivityType.WATCHING;
                        break;
                    case "LISTENING":
                        type = Activity.ActivityType.LISTENING;
                        break;
                    case "EMPTY":
                        return;
                    default:
                    case "PLAYING":
                        type = Activity.ActivityType.DEFAULT;
                        break;
                }
                String message = SettingsLoader.getBotConfigValue("status_message");
                message = message.replaceAll("%help%", SettingsLoader.getBotConfigValue("command_char").concat("help"));
                jda.getPresence().setActivity(Activity.of(type, message));
            } catch (NullPointerException | IllegalArgumentException e){
                e.printStackTrace();
                updateStatus(true);
            }
        }
    }



    /**
     * @param event The event
     */
    @Override public void onEvent(AudioEvent event) {
        updateStatus();
    }

    @Override public boolean connectToVoiceChannel(String serverName, String channelName) {
        for (VoiceChannel voiceChannel : jda.getVoiceChannels()){
            if (voiceChannel.getGuild().getName().equals(serverName) && voiceChannel.getName().equals(channelName)){
                voiceChannel.getGuild().getAudioManager().openAudioConnection(voiceChannel);
                audioMaster.setConnectedChannel(voiceChannel);
                return true;
            }
        }
        return false;
    }

    @Override public void disconnectFromVoiceChannel() {
        for (AudioManager audioManager : jda.getAudioManagers()){
            audioManager.closeAudioConnection();
            audioMaster.setConnectedChannel(null);
            audioMaster.stopAllAudio();
        }
    }

    @Override public List<String> getListOfVoiceChannels() {
        ArrayList<String> list = new ArrayList<>();
        for (VoiceChannel voiceChannel : jda.getVoiceChannels())
            list.add(formatVoiceChannel(voiceChannel));
        list.sort(String::compareTo);
        return list;
    }

    private String formatVoiceChannel(VoiceChannel voiceChannel){
        return String.format("%1$s : %2$s", voiceChannel.getGuild().getName(), voiceChannel.getName());
    }

    @Override public String getBotName() {
        return jda.getSelfUser().getAsTag();
    }
}