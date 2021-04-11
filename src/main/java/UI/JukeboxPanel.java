package UI;

import Audio.*;
import Commands.Command;
import Commands.CommandInterpreter;
import Utils.FileIO;
import Utils.Transcriber;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class JukeboxPanel extends JPanel implements JukeboxListener {

    private TrackListingTable defaultListTable;
    private TrackListingTable queueTable;
    private JLabel playlistLabel;

    private JCheckBox loopingBox;
    private JButton addPlaylistButton;
    private JLabel currentPlayingSongLabel;
    private static final String noSongText = "Song currently not playing";

    public JukeboxPanel(AudioMaster audioMaster, CommandInterpreter commandInterpreter, UIFrame uiFrame){
        audioMaster.setJukeboxListener(this);

        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, createDefaultListPanel(audioMaster, commandInterpreter, uiFrame), createQueuePanel(audioMaster, uiFrame, commandInterpreter));
        splitPane.setDividerLocation(75);

        add(splitPane, BorderLayout.CENTER);
        validate();
    }

    private JPanel createDefaultListPanel(AudioMaster audioMaster, CommandInterpreter commandInterpreter, UIFrame uiFrame){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        defaultListTable = new TrackListingTable(audioMaster, uiFrame, commandInterpreter, false);
        audioMaster.setJukeboxDefaultListListener(defaultListTable);

        JScrollPane scrollPane = new JScrollPane(defaultListTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        int unitIncrement = 0;
        for (Component component : scrollPane.getComponents())
            unitIncrement = Math.max(unitIncrement, (int)component.getMinimumSize().getHeight());

        scrollPane.getVerticalScrollBar().setUnitIncrement(unitIncrement);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createDefaultListControlsPanel(commandInterpreter, uiFrame), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createDefaultListControlsPanel(CommandInterpreter commandInterpreter, UIFrame uiFrame){
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        
        JButton openButton = ButtonMaker.createIconButton("icons/open.png", "Open", 8);
        openButton.addActionListener(e -> openJukeboxPlaylist(commandInterpreter));
        mainPanel.add(openButton);

        JButton newButton = ButtonMaker.createIconButton("icons/new.png", "New", 8);
        newButton.addActionListener(e -> createNewJukeboxPlaylist(commandInterpreter));
        mainPanel.add(newButton);

        JButton emptyButton = ButtonMaker.createIconButton("icons/empty.png", "Empty Playlist", 8);
        emptyButton.addActionListener(e -> commandInterpreter.evaluateCommand("jb dfl disable",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        mainPanel.add(emptyButton);

        JButton quickLoadButton = ButtonMaker.createIconButton("icons/quick_menu.png", "Quick Load", 8);
        quickLoadButton.addActionListener(e -> openQuickLoadMenu(quickLoadButton, commandInterpreter));
        mainPanel.add(quickLoadButton);

        JButton remotePlaylistButton = ButtonMaker.createIconButton("icons/internet.png", "Remote Playlist", 8);
        remotePlaylistButton.addActionListener(e -> new MakeRequestFrame("jb dfl load ", "Remote Playlist", commandInterpreter, uiFrame, false));
        mainPanel.add(remotePlaylistButton);

        mainPanel.add(Box.createHorizontalStrut(5));

        playlistLabel = new JLabel();
        mainPanel.add(playlistLabel);

        mainPanel.add(Box.createHorizontalGlue());

        addPlaylistButton = new JButton("Import Music");
        addPlaylistButton.addActionListener(e -> new MakeRequestFrame("jb dfl add ", "Import Music", commandInterpreter, uiFrame));
        addPlaylistButton.setEnabled(false);
        mainPanel.add(addPlaylistButton);

        return mainPanel;
    }

    public void openJukeboxPlaylist(CommandInterpreter commandInterpreter){
        JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Walnutbot Playlist", "playlist"));
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File chosen = fileChooser.getSelectedFile();
            commandInterpreter.evaluateCommand("jb dfl load ".concat(chosen.getPath().replace("\\","\\\\")),
                Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        }
    }

    public void createNewJukeboxPlaylist(CommandInterpreter commandInterpreter){
        JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Walnutbot Playlist", "playlist"));
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File chosen = fileChooser.getSelectedFile();
            commandInterpreter.evaluateCommand("jb dfl new ".concat(chosen.getPath().replace("\\","\\\\")),
                Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        }
    }

    private JPanel createQueuePanel(AudioMaster audioMaster, UIFrame uiFrame, CommandInterpreter commandInterpreter){
        JPanel panel = new JPanel(new BorderLayout());

        queueTable = new TrackListingTable(audioMaster, uiFrame, commandInterpreter, true);
        queueTable.pullAudioKeyList(audioMaster.getJukeboxQueueList());
        audioMaster.getJukeboxQueueList().addAudioKeyPlaylistListener(queueTable);

        JScrollPane scrollPane = new JScrollPane(queueTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createQueueControlsPanel(audioMaster, uiFrame, commandInterpreter), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createQueueControlsPanel(AudioMaster audioMaster, JFrame uiFrame, CommandInterpreter commandInterpreter){
        JPanel masterPanel = new JPanel(new BorderLayout());
        
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.LINE_AXIS));

        int BUTTON_MARGIN = 8;

        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", BUTTON_MARGIN);
        playButton.addActionListener(e -> commandInterpreter.evaluateCommand("jb play",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        controlsPanel.add(playButton);

        JButton pauseButton = ButtonMaker.createIconButton("icons/stop.png", "Pause", BUTTON_MARGIN);
        pauseButton.addActionListener(e -> commandInterpreter.evaluateCommand("jb pause",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        controlsPanel.add(pauseButton);
        
        JButton nextButton = ButtonMaker.createIconButton("icons/next.png", "Skip", BUTTON_MARGIN);
        nextButton.addActionListener(e -> commandInterpreter.evaluateCommand("jb skip",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        controlsPanel.add(nextButton);

        currentPlayingSongLabel = new JLabel(noSongText);
        controlsPanel.add(currentPlayingSongLabel);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.LINE_AXIS));

        optionsPanel.add(Box.createHorizontalStrut(10));

        JLabel songTimer = new JLabel("");
        audioMaster.setSongDurationTracker(new SongDurationTracker(songTimer, currentPlayingSongLabel, noSongText));
        optionsPanel.add(songTimer);

        loopingBox = new JCheckBox("Loop", audioMaster.isLoopingCurrentSong());
        loopingBox.addItemListener(e -> {
            if (audioMaster.isLoopingCurrentSong() != loopingBox.isSelected())
                commandInterpreter.evaluateCommand(String.format("jb loop %1$b", loopingBox.isSelected()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        });
        optionsPanel.add(loopingBox);

        JButton requestButton = ButtonMaker.createIconButton("icons/add.png", "Make Request", 11);
        requestButton.addActionListener(e -> new MakeRequestFrame("jb ", "Request Songs", commandInterpreter, uiFrame));
        optionsPanel.add(requestButton);

        JButton shuffleButton = ButtonMaker.createIconButton("icons/shuffle.png", "Shuffle", 10);
        shuffleButton.addActionListener(e -> commandInterpreter.evaluateCommand("jb shuffle", Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        optionsPanel.add(shuffleButton);

        JButton clearButton = ButtonMaker.createIconButton("icons/empty.png", "Clear", 3);
        clearButton.addActionListener(e -> commandInterpreter.evaluateCommand("jb clearqueue", Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        optionsPanel.add(clearButton);

        masterPanel.add(controlsPanel, BorderLayout.CENTER);
        masterPanel.add(optionsPanel, BorderLayout.LINE_END);

        return masterPanel;
    }

    private void updateDefaultPlaylistLabel(AudioMaster audioMaster) {
        if (audioMaster.getJukeboxDefaultList() == null) {
            playlistLabel.setIcon(null);
            playlistLabel.setText("");
            playlistLabel.setToolTipText("");
        } else {
            playlistLabel.setText(audioMaster.getJukeboxDefaultList().getName());
            if (audioMaster.isJukeboxDefaultListIsLocalFile()) {
                playlistLabel.setIcon(new ImageIcon(FileIO.getRootFilePath().concat(ButtonMaker.convertIconPath("icons/save.png"))));
                playlistLabel.setToolTipText("This playlist is a local file; Auto-Save is enabled.");
            } else {
                playlistLabel.setIcon(new ImageIcon(FileIO.getRootFilePath().concat(ButtonMaker.convertIconPath("icons/internet.png"))));
                playlistLabel.setToolTipText("This playlist was loaded from the Internet; Auto-Save is disabled.");
            }
            playlistLabel.repaint();
        }
    }

    private void openQuickLoadMenu(Component invoker, CommandInterpreter commandInterpreter){
        JPopupMenu popupMenu = new JPopupMenu("./playlists/");
        for (File file : FileIO.getFilesInDirectory(FileIO.getRootFilePath().concat("playlists/"))) {
            JMenuItem item = new JMenuItem(file.getName());
            item.addActionListener(e -> commandInterpreter.evaluateCommand("jb dfl load ".concat(FileIO.sanitizeURIForCommand(file.getPath())),
                Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
            popupMenu.add(item);
        }
        popupMenu.show(invoker, 0, 0);
    }

    @Override public void onDefaultListChange(AudioMaster audioMaster) {
        defaultListTable.pullAudioKeyList(audioMaster.getJukeboxDefaultList());
        boolean listValid = audioMaster.getJukeboxDefaultList() != null;
        addPlaylistButton.setEnabled(listValid);
        updateDefaultPlaylistLabel(audioMaster);
    }

    @Override public void onJukeboxChangeLoopState(boolean isLoopingSong) {
        loopingBox.setSelected(isLoopingSong);
    }

    private class TrackListingTable extends JPanel implements AudioKeyPlaylistListener {

        AudioMaster audioMaster;
        UIFrame uiFrame;
        CommandInterpreter commandInterpreter;
        boolean isQueueList;

        JLabel endOfQueueLabel = new JLabel("");

        private TrackListingTable(AudioMaster audioMaster, UIFrame uiFrame, CommandInterpreter commandInterpreter, boolean isQueueList){
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            this.audioMaster = audioMaster;
            this.isQueueList = isQueueList;
            this.commandInterpreter = commandInterpreter;
            this.uiFrame = uiFrame;
        }

        private void pullAudioKeyList(AudioKeyPlaylist playlist){
            removeAll();
            if (playlist != null) {
                for (int i = 0; i < playlist.getAudioKeys().size(); i++) {
                    add(new TrackListing(playlist.getKey(i), audioMaster, commandInterpreter,
                        isQueueList));
                }
                if (isQueueList && playlist.isEmpty()) {
                    endOfQueueLabel = new JLabel("Queue Empty - Playing random songs from Default List (above)");
                    Font italic = new Font(endOfQueueLabel.getFont().getName(), Font.ITALIC,
                        endOfQueueLabel.getFont().getSize());
                    endOfQueueLabel.setFont(italic);
                    add(endOfQueueLabel);
                }
            }
            validate();
            repaint();
        }

        @Override public void onAddition(AudioKeyPlaylistEvent event) {
            remove(endOfQueueLabel);
            add(new TrackListing(event.getKey(), audioMaster, commandInterpreter, isQueueList));
            revalidate();
            repaint();
        }

        @Override public void onRemoval(AudioKeyPlaylistEvent event) {
            remove(event.getPos());
            if (getComponents().length <= 0)
                add(endOfQueueLabel);
            revalidate();
            repaint();
        }

        /**
         * Called when an element of the AudioKeyPlaylist has been modified.
         * This method is called after the element has been modified.
         *
         * @param event The AudioKeyPlaylistEvent describing the details of the event.
         */
        @Override public void onModification(AudioKeyPlaylistEvent event) {
            if (getComponent(event.getPos()) instanceof TrackListing) {
                TrackListing component = (TrackListing) getComponent(event.getPos());
                component.setData(event.getKey());
                revalidate();
                repaint();
            }
        }

        /**
         * Called when the AudioKeyPlaylist has been cleared.
         */
        @Override public void onClear() {
            removeAll();
            add(endOfQueueLabel);
            revalidate();
            repaint();
        }

        private void refresh(){
            if (isQueueList)
                pullAudioKeyList(audioMaster.getJukeboxQueueList());
            else
                pullAudioKeyList(audioMaster.getJukeboxDefaultList());
        }

        /**
         * Called when the AudioKeyPlaylist has been shuffled.
         */
        @Override public void onShuffle() {
            refresh();
        }

        /**
         * Called when the AudioKeyPlaylist has been sorted.
         */
        @Override public void onSort() {
            refresh();
        }

        /**
         * Called when the AudioKeyPlaylist changes for a new one.
         */
        @Override public void onNewPlaylist() {
            refresh();
        }
    }

    private class TrackListing extends JPanel implements AudioKeyUIWrapper {

        private AudioKey audioKey;
        private JTextField nameText;
        private JTextField urlText;

        private java.util.UUID UUID;

        private int findMyPosition(){
            for (int i = 0; i < getParent().getComponents().length; i++) {
                if (getParent().getComponents()[i] instanceof TrackListing) {
                    TrackListing trackListing = (TrackListing) getParent().getComponents()[i];
                    if (trackListing.getUUID().equals(UUID))
                        return i;
                }
            }
            return -1;
        }

        java.util.UUID getUUID() {
            return UUID;
        }

        private TrackListing(AudioKey audioKey, AudioMaster audioMaster, CommandInterpreter commandInterpreter,
            boolean isInQueueList){
            UUID = java.util.UUID.randomUUID();
            this.audioKey = audioKey;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            JButton buttonToMeasure;
            if (isInQueueList){
                JButton postponeButton = ButtonMaker.createIconButton("icons/queue.png", "Postpone", 3);
                postponeButton.addActionListener(e -> commandInterpreter.evaluateCommand(String.format("jb postpone %1$d", findMyPosition()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
                add(postponeButton);
                JButton removeButton = ButtonMaker.createIconButton("icons/cancel.png", "Remove", 3);
                removeButton.addActionListener(e -> commandInterpreter.evaluateCommand(String.format("jb deque %1$d", findMyPosition()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
                add(removeButton);
                buttonToMeasure = removeButton;
            } else {
                //Queue Button
                JButton queueButton = ButtonMaker.createIconButton("icons/queue.png", "Queue", 4);
                queueButton.addActionListener(e -> commandInterpreter.evaluateCommand("jb ".concat(FileIO.sanitizeURIForCommand(audioKey.getUrl())),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
                add(queueButton);
                //Edit Button
                JButton editButton = ButtonMaker.createIconButton("icons/menu.png", "Edit", 4);
                editButton.addActionListener(
                    e -> new ModifyAudioKeyFrame(audioMaster, audioKey, findMyPosition(), commandInterpreter, ModifyAudioKeyFrame.ModificationType.MODIFY, ModifyAudioKeyFrame.TargetList.JUKEBOX_DEFAULT));
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
