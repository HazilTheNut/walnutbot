package UI;

import Audio.AudioMaster;
import Utils.ButtonMaker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConnectionPanel extends JPanel {
    
    public ConnectionPanel(JDA jda, AudioMaster audioMaster){

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JComboBox<VoiceChannelOption> channelSelect = new JComboBox<>();
        JButton listButton = new JButton("List Channels...");
        listButton.addActionListener(e -> generateVoiceChannelOptions(channelSelect, jda));

        add(listButton);
        add(channelSelect);

        JButton connectButton = ButtonMaker.createIconButton("icons/connect.png", "Connect", 10);
        connectButton.addActionListener(e -> {
            VoiceChannelOption selected = (VoiceChannelOption)channelSelect.getSelectedItem();
            if (selected != null) {
                selected.getVoiceChannel().getGuild().getAudioManager().openAudioConnection(selected.getVoiceChannel());
                audioMaster.setConnectedChannel(selected.getVoiceChannel());
            }
        });

        JButton disconnectButton = ButtonMaker.createIconButton("icons/disconnect.png", "Disconnect", 10);
        disconnectButton.addActionListener(e -> {
            VoiceChannelOption selected = (VoiceChannelOption)channelSelect.getSelectedItem();
            if (selected != null) {
                selected.getVoiceChannel().getGuild().getAudioManager().closeAudioConnection();
                audioMaster.setConnectedChannel(null);
                audioMaster.stopAllAudio();
            }
        });

        add(connectButton);
        add(disconnectButton);

        setBorder(BorderFactory.createTitledBorder("Connection"));
    }

    private void generateVoiceChannelOptions(JComboBox<VoiceChannelOption> selectionBox, JDA jda){
        selectionBox.removeAllItems();
        ArrayList<VoiceChannel> voiceChannels = new ArrayList<>(jda.getVoiceChannels());
        voiceChannels.sort(new Comparator<VoiceChannel>() {
            @Override public int compare(VoiceChannel o1, VoiceChannel o2) {
                return representVoiceChannel(o1).compareTo(representVoiceChannel(o2));
            }
        });
        for (VoiceChannel channel : voiceChannels)
            selectionBox.addItem(new VoiceChannelOption(channel, representVoiceChannel(channel)));
    }

    private String representVoiceChannel(VoiceChannel channel) {
        return String.format("%1$s : %2$s", channel.getGuild().getName(), channel.getName());
    }

    private class VoiceChannelOption{
        private VoiceChannel vc;
        private String name;

        public VoiceChannelOption(VoiceChannel vc, String name) {
            this.vc = vc;
            this.name = name;
        }
        public VoiceChannel getVoiceChannel() {
            return vc;
        }
        public String getName() {
            return name;
        }

        @Override public String toString() {
            return getName();
        }
    }
    
}
