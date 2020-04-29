package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Audio.AudioMaster;
import Utils.ButtonMaker;

import javax.swing.*;
import java.awt.*;

public class JukeboxPanel extends JPanel implements JukeboxUIWrapper{

    private TrackListingTable defaultListTable;
    private TrackListingTable queueTable;
    private JLabel playlistLabel;

    public JukeboxPanel(AudioMaster audioMaster){

        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, createUpperPanel(audioMaster), new JTextArea());

        add(splitPane, BorderLayout.CENTER);
        validate();
    }

    private JPanel createUpperPanel(AudioMaster audioMaster){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        defaultListTable = new TrackListingTable(audioMaster, false);

        JScrollPane scrollPane = new JScrollPane(defaultListTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createUpperBanner(audioMaster, defaultListTable), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createUpperBanner(AudioMaster audioMaster, PlaylistUIWrapper playlistUIWrapper){
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        
        JButton openButton = ButtonMaker.createIconButton("icons/open.png", "Open", 4);
        openButton.addActionListener(e -> audioMaster.openJukeboxPlaylist(this));
        mainPanel.add(openButton);

        JButton newButton = ButtonMaker.createIconButton("icons/new.png", "New", 4);
        newButton.addActionListener(e -> audioMaster.createNewJukeboxPlaylist(this));
        mainPanel.add(newButton);

        playlistLabel = new JLabel();
        mainPanel.add(playlistLabel);

        mainPanel.add(Box.createHorizontalGlue());

        JButton addSongButton = new JButton("Add Song");
        addSongButton.addActionListener(e -> new ModifyAudioKeyFrame(audioMaster, playlistUIWrapper, null, audioMaster.getJukeboxDefaultList(), audioMaster::saveJukeboxDefault));
        mainPanel.add(addSongButton);

        JButton addPlaylistButton = new JButton("Import Playlist");
        addPlaylistButton.addActionListener(e -> new AddPlaylistFrame(audioMaster, playlistUIWrapper));
        mainPanel.add(addPlaylistButton);

        return mainPanel;
    }

    @Override public void refreshDefaultList(AudioMaster audioMaster) {
        defaultListTable.pullAudioKeyList(audioMaster.getJukeboxDefaultList());
    }

    @Override public void refreshQueueList(AudioMaster audioMaster) {

    }

    @Override public void updateDefaultPlaylistLabel(String playlistName) {
        playlistLabel.setText(playlistName);
        playlistLabel.repaint();
    }

    private class TrackListingTable extends JPanel implements PlaylistUIWrapper{

        AudioMaster audioMaster;
        boolean isQueueList;

        private TrackListingTable(AudioMaster audioMaster, boolean isQueueList){
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            this.audioMaster = audioMaster;
            this.isQueueList = isQueueList;
        }

        private void pullAudioKeyList(AudioKeyPlaylist playlist){
            removeAll();
            for (AudioKey audioKey : playlist.getAudioKeys()){
                add(new TrackListing(audioKey, audioMaster, this, isQueueList));
            }
            validate();
        }

        @Override public void addAudioKey(AudioKey key) {
            add(new TrackListing(key, audioMaster, this, isQueueList));
            revalidate();
            repaint();
        }

        @Override public int getAudioKeyID(AudioKeyUIWrapper keyUIWrapper) {
            if (keyUIWrapper == null) return -1;
            for (int i = 0; i < getComponents().length; i++) {
                if (getComponent(i) instanceof AudioKeyUIWrapper) {
                    AudioKeyUIWrapper uiWrapper = (AudioKeyUIWrapper) getComponent(i);
                    if (uiWrapper.getData().equals(keyUIWrapper.getData()))
                        return i;
                }
            }
            return -1;
        }

        @Override public void modifyAudioKey(int keyID, AudioKey newData) {
            Component c = getComponent(keyID);
            if (c instanceof AudioKeyUIWrapper) {
                AudioKeyUIWrapper audioKeyUIWrapper = (AudioKeyUIWrapper) c;
                audioKeyUIWrapper.setData(newData);
            }
        }

        @Override public void removeAudioKey(int keyID) {
            remove(keyID);
            revalidate();
            repaint();
        }
    }

    private class TrackListing extends JPanel implements AudioKeyUIWrapper {

        private AudioKey audioKey;
        private JTextField nameText;
        private JTextField urlText;

        private TrackListing(AudioKey audioKey, AudioMaster audioMaster, TrackListingTable trackLister, boolean isInQueueList){
            this.audioKey = audioKey;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            JButton buttonToMeasure;
            if (isInQueueList){
                JButton removeButton = ButtonMaker.createIconButton("icons/cancel.png", "Remove", 4);
                removeButton.addActionListener(e -> trackLister.removeAudioKey(trackLister.getAudioKeyID(this)));
                add(removeButton);
                buttonToMeasure = removeButton;
            } else {
                //Queue Button
                add(ButtonMaker.createIconButton("icons/queue.png", "Queue", 4));
                //Edit Button
                JButton editButton = ButtonMaker.createIconButton("icons/menu.png", "Edit", 4);
                editButton.addActionListener(
                    e -> new ModifyAudioKeyFrame(audioMaster, trackLister, this, audioMaster.getJukeboxDefaultList(), audioMaster::saveJukeboxDefault));
                add(editButton);
                buttonToMeasure = editButton;
            }
            setMaximumSize(new Dimension(9001, buttonToMeasure.getPreferredSize().height));
            //Text
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new GridLayout(1, 2));
            //Name field
            nameText = new JTextField();
            nameText.setBorder(BorderFactory.createEmptyBorder());
            nameText.setEditable(false);
            textPanel.add(nameText);
            //Url field
            urlText = new JTextField();
            urlText.setBorder(BorderFactory.createEmptyBorder());
            urlText.setEditable(false);
            textPanel.add(urlText);

            setData(audioKey);

            add(textPanel);
        }

        @Override public void setData(AudioKey data) {
            audioKey.setName(data.getName());
            audioKey.setUrl(data.getUrl());

            nameText.setText(" " + audioKey.getName());
            urlText.setText(audioKey.getUrl());

            repaint();
        }

        @Override public AudioKey getData() {
            return audioKey;
        }
    }
}
