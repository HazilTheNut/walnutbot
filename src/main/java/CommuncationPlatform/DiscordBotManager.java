package CommuncationPlatform;

import Audio.IAudioStateMachine;
import Audio.IAudioStateMachineListener;
import Utils.SettingsLoader;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.ArrayList;
import java.util.List;

public class DiscordBotManager implements AudioEventListener, ICommunicationPlatformManager, IAudioStateMachineListener {

    private final boolean useDefaultStatus;
    private final boolean showCurrentPlayingSong;

    private final JDA jda;
    private final IAudioStateMachine audioStateMachine;
    private final IDiscordPlaybackSystemBridge playbackSystemBridge;

    public DiscordBotManager(JDA jda, IAudioStateMachine audioStateMachine, IDiscordPlaybackSystemBridge playbackSystemBridge){
        this.jda = jda;
        this.audioStateMachine = audioStateMachine;
        audioStateMachine.addAudioStateMachineListener(this);
        this.playbackSystemBridge = playbackSystemBridge;
        useDefaultStatus         = Boolean.parseBoolean(SettingsLoader.getBotConfigValue("status_use_default"));
        showCurrentPlayingSong   = Boolean.parseBoolean(SettingsLoader.getBotConfigValue("status_show_current_song"));
    }

    @Override
    public void updateStatus(){
        updateStatus(useDefaultStatus);
    }

    private void updateStatus(boolean useDefault){
        if (showCurrentPlayingSong && audioStateMachine.getJukeboxCurrentlyPlayingSong() != null){ // Status based on currently-playing song (currentlyPlayingSong is null when no song is playing)
            String message = audioStateMachine.getJukeboxCurrentlyPlayingSong().getName();
            jda.getPresence().setActivity(Activity.playing(message));
        } else if (useDefault){ // Status based on default configuration
            jda.getPresence().setActivity(Activity
                .playing("sounds / type " + SettingsLoader.getBotConfigValue("command_char") + "help"));
        } else { // Status based on user-defined configuration
            try {
                String message = SettingsLoader.getBotConfigValue("status_message");
                assert message != null;
                message = message.replaceAll("%help%", SettingsLoader.getBotConfigValue("command_char").concat("help"));
                Activity.ActivityType type;
                switch (SettingsLoader.getBotConfigValue("status_type").toUpperCase()){
                    case "WATCHING":
                        jda.getPresence().setActivity(Activity.watching(message));
                        break;
                    case "LISTENING":
                        jda.getPresence().setActivity(Activity.listening(message));
                        break;
                    case "EMPTY":
                        return;
                    default:
                    case "PLAYING":
                        jda.getPresence().setActivity(Activity.playing(message));
                        break;
                }
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
        for (Guild guild : jda.getGuilds()){
            List<VoiceChannel> voiceChannels = guild.getVoiceChannelsByName(channelName, true);
            if (voiceChannels.size() == 0)
                continue;
            playbackSystemBridge.setConnectedVoiceChannel(voiceChannels.get(0));
            return true;
        }
        return false;
    }

    @Override public void disconnectFromVoiceChannel() {
        for (AudioManager audioManager : jda.getAudioManagers()){
            audioManager.closeAudioConnection();
            audioStateMachine.stopSoundboard();
            audioStateMachine.pauseJukebox();
        }
    }

    @Override public List<String> getListOfVoiceChannels() {
        ArrayList<String> list = new ArrayList<>();
        for (VoiceChannel voiceChannel : jda.getVoiceChannels())
            list.add(formatVoiceChannel(voiceChannel));
        list.sort(String::compareTo);
        return list;
    }

    @Override
    public String connectedVoiceChannelToString() {
        if (playbackSystemBridge.getConnectedVoiceChannel() != null) {
            return formatVoiceChannel(playbackSystemBridge.getConnectedVoiceChannel());
        }
        return "disconnected";
    }

    private String formatVoiceChannel(VoiceChannel voiceChannel){
        return String.format("%1$s : %2$s", voiceChannel.getGuild().getName(), voiceChannel.getName());
    }

    @Override public String getBotName() {
        return jda.getSelfUser().getAsTag();
    }

    @Override
    public void onAudioStateMachineUpdateStatus(IAudioStateMachine.AudioStateMachineStatus status) {
        updateStatus();
    }

    @Override
    public void onJukeboxDefaultListLoadStateUpdate(IAudioStateMachine.JukeboxDefaultListLoadState loadState) {

    }
}
