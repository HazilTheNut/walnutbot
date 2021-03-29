package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Audio.AudioMaster;
import Audio.SongDurationTracker;
import Commands.CommandInterpreter;
import Utils.ButtonMaker;
import Utils.Transcriber;

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

    public JukeboxPanel(AudioMaster audioMaster, CommandInterpreter commandInterpreter, JFrame uiFrame){
        audioMaster.setJukeboxUIWrapper(this);

        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, createDefaultListPanel(audioMaster), createQueuePanel(audioMaster, uiFrame));
        splitPane.setDividerLocation(75);

        add(splitPane, BorderLayout.CENTER);
        validate();
    }

    private JPanel createDefaultListPanel(AudioMaster audioMaster){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        defaultListTable = new TrackListingTable(audioMaster, false);

        JScrollPane scrollPane = new JScrollPane(defaultListTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        int unitIncrement = 0;
        for (Component component : scrollPane.getComponents())
            unitIncrement = Math.max(unitIncrement, (int)component.getMinimumSize().getHeight());

        scrollPane.getVerticalScrollBar().setUnitIncrement(unitIncrement);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createDefaultListControlsPanel(audioMaster, defaultListTable), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createDefaultListControlsPanel(AudioMaster audioMaster, PlaylistUIWrapper playlistUIWrapper){
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        
        JButton openButton = ButtonMaker.createIconButton("icons/open.png", "Open", 8);
        openButton.addActionListener(e -> audioMaster.openJukeboxPlaylist(this));
        mainPanel.add(openButton);

        JButton newButton = ButtonMaker.createIconButton("icons/new.png", "New", 8);
        newButton.addActionListener(e -> audioMaster.createNewJukeboxPlaylist(this));
        mainPanel.add(newButton);

        JButton emptyButton = ButtonMaker.createIconButton("icons/empty.png", "Empty Playlist", 8);
        emptyButton.addActionListener(e -> audioMaster.emptyJukeboxPlaylist(this));
        mainPanel.add(emptyButton);

        mainPanel.add(Box.createHorizontalStrut(5));

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

    private JPanel createQueuePanel(AudioMaster audioMaster, JFrame uiFrame){
        JPanel panel = new JPanel(new BorderLayout());

        queueTable = new TrackListingTable(audioMaster, true);
        queueTable.pullAudioKeyList(audioMaster.getJukeboxQueueList());

        JScrollPane scrollPane = new JScrollPane(queueTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createQueueControlsPanel(audioMaster, uiFrame), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createQueueControlsPanel(AudioMaster audioMaster, JFrame uiFrame){
        JPanel masterPanel = new JPanel(new BorderLayout());
        
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.LINE_AXIS));

        int BUTTON_MARGIN = 8;

        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", BUTTON_MARGIN);
        playButton.addActionListener(e -> audioMaster.unpauseCurrentSong());
        controlsPanel.add(playButton);

        JButton pauseButton = ButtonMaker.createIconButton("icons/stop.png", "Pause", BUTTON_MARGIN);
        pauseButton.addActionListener(e -> audioMaster.setJukeboxTruePause(true));
        controlsPanel.add(pauseButton);
        
        JButton nextButton = ButtonMaker.createIconButton("icons/next.png", "Skip", BUTTON_MARGIN);
        nextButton.addActionListener(e -> audioMaster.jukeboxSkipToNextSong(true));
        controlsPanel.add(nextButton);

        currentPlayingSongLabel = new JLabel(noSongText);
        controlsPanel.add(currentPlayingSongLabel);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.LINE_AXIS));

        optionsPanel.add(Box.createHorizontalStrut(10));

        JLabel songTimer = new JLabel("");
        audioMaster.setSongDurationTracker(new SongDurationTracker(songTimer));
        optionsPanel.add(songTimer);

        JCheckBox loopingBox = new JCheckBox("Loop", audioMaster.isLoopingCurrentSong());
        loopingBox.addChangeListener(e -> audioMaster.setLoopingCurrentSong(loopingBox.isSelected()));
        optionsPanel.add(loopingBox);

        JButton requestButton = ButtonMaker.createIconButton("icons/add.png", "Make Request", 11);
        requestButton.addActionListener(e -> new MakeRequestFrame(audioMaster, uiFrame));
        optionsPanel.add(requestButton);

        JButton shuffleButton = ButtonMaker.createIconButton("icons/shuffle.png", "Shuffle", 10);
        shuffleButton.addActionListener(e -> audioMaster.shuffleJukeboxQueue());
        optionsPanel.add(shuffleButton);

        JButton clearButton = ButtonMaker.createIconButton("icons/empty.png", "Clear", 3);
        clearButton.addActionListener(e -> audioMaster.clearJukeboxQueue());
        optionsPanel.add(clearButton);

        masterPanel.add(controlsPanel, BorderLayout.CENTER);
        masterPanel.add(optionsPanel, BorderLayout.LINE_END);

        return masterPanel;
    }

    @Override public void refreshDefaultList(AudioMaster audioMaster) {
        defaultListTable.pullAudioKeyList(audioMaster.getJukeboxDefaultList());
        boolean listValid = audioMaster.getJukeboxDefaultList() != null;
        addSongButton.setEnabled(listValid);
        addPlaylistButton.setEnabled(listValid);
    }

    @Override public void refreshQueueList(AudioMaster audioMaster) {
        queueTable.pullAudioKeyList(audioMaster.getJukeboxQueueList());
        if (audioMaster.getCurrentlyPlayingSong() != null) {
            String preface = (audioMaster.getJukeboxPlayer().isPaused()) ? "Paused" : "Now Playing";
            currentPlayingSongLabel.setText(String.format("%2$s: %1$s", audioMaster.getCurrentlyPlayingSong().getTrackName(), preface));
            String tooltip = String.format("%1$s : %2$s", audioMaster.getCurrentlyPlayingSong().getName(), audioMaster.getCurrentlyPlayingSong().getUrl());
            currentPlayingSongLabel.setToolTipText(tooltip);
        }
        else {
            currentPlayingSongLabel.setText(noSongText);
            currentPlayingSongLabel.setToolTipText("");
        }
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
            if (playlist != null) {
                for (AudioKey audioKey : playlist.getAudioKeys()) {
                    add(new TrackListing(audioKey, audioMaster, this, isQueueList));
                }
                if (isQueueList && playlist.isEmpty()) {
                    JLabel endOfQueueLabel = new JLabel("Queue Empty - Playing random songs from Default List (above)");
                    Font italic = new Font(endOfQueueLabel.getFont().getName(), Font.ITALIC,
                        endOfQueueLabel.getFont().getSize());
                    endOfQueueLabel.setFont(italic);
                    add(endOfQueueLabel);
                }
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
                JButton postponeButton = ButtonMaker.createIconButton("icons/queue.png", "Postpone", 3);
                postponeButton.addActionListener(e -> {
                    int pos = trackLister.getAudioKeyID(this);
                    trackLister.removeAudioKey(pos);
                    audioMaster.getJukeboxQueueList().removeAudioKey(pos);
                    audioMaster.queueJukeboxSong(audioKey, () -> {}, () -> {});
                });
                add(postponeButton);
                JButton removeButton = ButtonMaker.createIconButton("icons/cancel.png", "Remove", 3);
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
                queueButton.addActionListener(e -> audioMaster.queueJukeboxSong(audioKey, () -> {}, () -> Transcriber.print("ERROR: Audio key is invalid: %1$s", audioKey.toString())));
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

            nameText.setText(" " + audioKey.getTrackName());
            urlText.setText(audioKey.getUrl());

            repaint();
        }

        @Override public AudioKey getData() {
            return audioKey;
        }
    }
}
