package UI;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.ButtonMaker;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;

public class ConnectionPanel extends JPanel {
    
    public ConnectionPanel(BotManager botManager, AudioMaster audioMaster){

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JComboBox<VoiceChannelOption> channelSelect = new JComboBox<>();
        JButton listButton = new JButton("List Channels...");
        listButton.addActionListener(e -> generateVoiceChannelOptions(channelSelect, botManager));

        add(listButton);
        add(channelSelect);

        JButton connectButton = ButtonMaker.createIconButton("icons/connect.png", "Connect", 10);
        connectButton.addActionListener(e -> {
            VoiceChannelOption selected = (VoiceChannelOption)channelSelect.getSelectedItem();
            if (selected != null)
                botManager.connectToVoiceChannel(selected.getServerName(), selected.getChannelName());
        });

        JButton disconnectButton = ButtonMaker.createIconButton("icons/disconnect.png", "Disconnect", 10);
        disconnectButton.addActionListener(e -> botManager.disconnectFromVoiceChannel());

        add(connectButton);
        add(disconnectButton);

        setBorder(BorderFactory.createTitledBorder("Connection"));
    }

    private void generateVoiceChannelOptions(JComboBox<VoiceChannelOption> selectionBox, BotManager botManager){
        selectionBox.removeAllItems();
        List<String> channelList = botManager.getListOfVoiceChannels();
        for (String channelInfo : channelList){
            String[] parts = channelInfo.split(":");
            selectionBox.addItem(new VoiceChannelOption(parts[0].trim(), parts[1].trim()));
        }
    }

    private class VoiceChannelOption{
        private String serverName;
        private String channelName;

        public VoiceChannelOption(String serverName, String channelName) {
            this.serverName = serverName;
            this.channelName = channelName;
        }

        public String getServerName() {
            return serverName;
        }

        public String getChannelName() {
            return channelName;
        }

        @Override public String toString() {
            return String.format("%1$s : %2$s", serverName, channelName);
        }
    }
    
}
