package UI;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

import javax.swing.*;

public class ConnectionPanel extends JPanel {
    
    public ConnectionPanel(JDA jda){

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JComboBox<VoiceChannelOption> channelSelect = new JComboBox<>();
        JButton listButton = new JButton("List Channels...");
        listButton.addActionListener(e -> generateVoiceChannelOptions(channelSelect, jda));

        add(listButton);
        add(channelSelect);

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            VoiceChannelOption selected = (VoiceChannelOption)channelSelect.getSelectedItem();
            if (selected != null) {
                selected.getVoiceChannel().getGuild().getAudioManager().openAudioConnection(selected.getVoiceChannel());
            }
        });

        JButton disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> {
            VoiceChannelOption selected = (VoiceChannelOption)channelSelect.getSelectedItem();
            if (selected != null) {
                selected.getVoiceChannel().getGuild().getAudioManager().closeAudioConnection();
            }
        });

        add(connectButton);
        add(disconnectButton);
        
    }

    private void generateVoiceChannelOptions(JComboBox<VoiceChannelOption> selectionBox, JDA jda){
        selectionBox.removeAllItems();
        for (VoiceChannel channel : jda.getVoiceChannels())
            selectionBox.addItem(new VoiceChannelOption(channel, String.format("%1$s : %2$s", channel.getGuild().getName(), channel.getName())));
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
