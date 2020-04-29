package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Audio.AudioMaster;
import Utils.ButtonMaker;
import sun.tools.jps.Jps;

import javax.swing.*;
import java.awt.*;

public class JukeboxPanel extends JPanel implements JukeboxUIWrapper{

    private TrackListingTable defaultListTable;
    private TrackListingTable queueTable;
    private JLabel playlistLabel;

    private JButton addSongButton;
    private JButton addPlaylistButton;
    private JLabel currentPlayingSongLabel;
    private static final String noSongText = "Song currently not playing";

    public JukeboxPanel(AudioMaster audioMaster){
        audioMaster.setJukeboxUIWrapper(this);

        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, createDefaultListPanel(audioMaster), createQueuePanel(audioMaster));

        add(splitPane, BorderLayout.CENTER);
        validate();
    }

    private JPanel createDefaultListPanel(AudioMaster audioMaster){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        defaultListTable = new TrackListingTable(audioMaster, false);

        JScrollPane scrollPane = new JScrollPane(defaultListTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createDefaultListControlsPanel(audioMaster, defaultListTable), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createDefaultListControlsPanel(AudioMaster audioMaster, PlaylistUIWrapper playlistUIWrapper){
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

        addSongButton = new JButton("Add Song");
        addSongButton.addActionListener(e -> new ModifyAudioKeyFrame(audioMaster, playlistUIWrapper, null, audioMaster.getJukeboxDefaultList(), audioMaster::saveJukeboxDefault));
        addSongButton.setEnabled(false);
        mainPanel.add(addSongButton);

        addPlaylistButton = new JButton("Import Playlist");
        addPlaylistButton.addActionListener(e -> new AddPlaylistFrame(audioMaster, playlistUIWrapper));
        addPlaylistButton.setEnabled(false);
        mainPanel.add(addPlaylistButton);

        return mainPanel;
    }

    private JPanel createQueuePanel(AudioMaster audioMaster){
        JPanel panel = new JPanel(new BorderLayout());

        queueTable = new TrackListingTable(audioMaster, true);

        JScrollPane scrollPane = new JScrollPane(queueTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createQueueControlsPanel(audioMaster), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createQueueControlsPanel(AudioMaster audioMaster){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
    
        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", 4);
        playButton.addActionListener(e -> audioMaster.getJukeboxPlayer().setPaused(false));
        panel.add(playButton);

        JButton pauseButton = ButtonMaker.createIconButton("icons/stop.png", "Pause", 4);
        pauseButton.addActionListener(e -> audioMaster.getJukeboxPlayer().setPaused(true));
        panel.add(pauseButton);
        
        JButton nextButton = ButtonMaker.createIconButton("icons/next.png", "Skip", 4);
        nextButton.addActionListener(e -> audioMaster.progressJukeboxQueue());
        panel.add(nextButton);

        currentPlayingSongLabel = new JLabel(noSongText);
        panel.add(currentPlayingSongLabel);

        return panel;
    }

    @Override public void refreshDefaultList(AudioMaster audioMaster) {
        defaultListTable.pullAudioKeyList(audioMaster.getJukeboxDefaultList());
        boolean listValid = audioMaster.getJukeboxDefaultList() != null;
        addSongButton.setEnabled(listValid);
        addPlaylistButton.setEnabled(listValid);
    }

    @Override public void refreshQueueList(AudioMaster audioMaster) {
        queueTable.pullAudioKeyList(audioMaster.getJukeboxQueueList());
        if (audioMaster.getCurrentlyPlayingSong() != null)
            currentPlayingSongLabel.setText(String.format("Currently Playing: %1$s", audioMaster.getCurrentlyPlayingSong().getName()));
        else
            currentPlayingSongLabel.setText(noSongText);
        currentPlayingSongLabel.repaint();
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
            repaint();
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
                removeButton.addActionListener(e -> {
                    int pos = trackLister.getAudioKeyID(this);
                    trackLister.removeAudioKey(pos);
                    audioMaster.getJukeboxQueueList().removeAudioKey(pos);
                });
                add(removeButton);
                buttonToMeasure = removeButton;
            } else {
                //Queue Button
                JButton queueButton = ButtonMaker.createIconButton("icons/queue.png", "Queue", 4);
                queueButton.addActionListener(e -> audioMaster.queueJukeboxSong(audioKey));
                add(queueButton);
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
