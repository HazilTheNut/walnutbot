package UI;

import Audio.*;
import Commands.Command;
import Commands.CommandInterpreter;
import Main.WalnutbotEnvironment;
import Utils.FileIO;
import Utils.Transcriber;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;

public class JukeboxPanel extends JPanel implements IAudioStateMachineListener {

    private TrackListingTable defaultListTable;
    private TrackListingTable queueTable;
    private JLabel playlistLabel;

    private JCheckBox loopingBox;
    private JButton addPlaylistButton;

    private final AudioKeyPlaylist recentPlaylistsHistory;
    private static final String HISTORY_LOC = "~~/data/jbhistory.playlist";

    private static final String noSongText = "Song currently not playing";

    public JukeboxPanel(WalnutbotEnvironment environment, UIFrame uiFrame){
        environment.getAudioStateMachine().addAudioStateMachineListener(this);

        recentPlaylistsHistory = new AudioKeyPlaylist(new File(FileIO.expandURIMacros(HISTORY_LOC)), false);

        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, createDefaultListPanel(environment, uiFrame), createQueuePanel(environment, uiFrame));
        splitPane.setDividerLocation(75);

        add(splitPane, BorderLayout.CENTER);
        validate();
    }

    private JPanel createDefaultListPanel(WalnutbotEnvironment environment, UIFrame uiFrame){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        defaultListTable = new TrackListingTable(environment, uiFrame, false);
        environment.getAudioStateMachine().getJukeboxDefaultList().accessAudioKeyPlaylist(playlist -> playlist.addAudioKeyPlaylistListener(defaultListTable));

        JScrollPane scrollPane = new JScrollPane(defaultListTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        int unitIncrement = 0;
        for (Component component : scrollPane.getComponents())
            unitIncrement = Math.max(unitIncrement, (int)component.getMinimumSize().getHeight());

        scrollPane.getVerticalScrollBar().setUnitIncrement(unitIncrement);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createDefaultListControlsPanel(environment.getCommandInterpreter(), uiFrame), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createDefaultListControlsPanel(CommandInterpreter commandInterpreter, UIFrame uiFrame){
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));

        /*
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
        */

        JButton quickLoadButton = ButtonMaker.createIconButton("icons/quick_menu.png", "Playlist", 16);
        quickLoadButton.addActionListener(e -> openQuickLoadMenu(quickLoadButton, commandInterpreter, uiFrame));
        mainPanel.add(quickLoadButton);

        /*
        JButton remotePlaylistButton = ButtonMaker.createIconButton("icons/internet.png", "Remote Playlist", 8);
        remotePlaylistButton.addActionListener(e -> new MakeRequestFrame("jb dfl load ", "Remote Playlist", commandInterpreter, uiFrame, false));
        mainPanel.add(remotePlaylistButton);
        */

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
        fileChooser.setFileFilter(new FileNameExtensionFilter("Walnutbot Playlist (.playlist)", FileIO.getValidPlaylistFileExtensions()));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setDialogTitle("Open Walnutbot Playlist");
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File chosen = fileChooser.getSelectedFile();
            if (chosen.getName().endsWith(".playlist")) {
                commandInterpreter
                    .evaluateCommand("jb dfl load ".concat(chosen.getPath().replace("\\", "\\\\")),
                        Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
            } else
                JOptionPane.showMessageDialog(new JFrame(), "ERROR: This is not a Walnutbot Playlist!");
        }
    }

    public void createNewJukeboxPlaylist(CommandInterpreter commandInterpreter){
        JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Walnutbot Playlist", FileIO.getValidPlaylistFileExtensions()));
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION){
            File chosen = fileChooser.getSelectedFile();
            commandInterpreter.evaluateCommand("jb dfl new ".concat(chosen.getPath().replace("\\","\\\\")),
                Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        }
    }

    private JPanel createQueuePanel(WalnutbotEnvironment environment, UIFrame uiFrame){
        JPanel panel = new JPanel(new BorderLayout());

        queueTable = new TrackListingTable(environment, uiFrame, true);
        environment.getAudioStateMachine().getJukeboxQueue().accessAudioKeyPlaylist(playlist -> {
            playlist.addAudioKeyPlaylistListener(queueTable);
        });

        JScrollPane scrollPane = new JScrollPane(queueTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createQueueControlsPanel(environment, uiFrame), BorderLayout.PAGE_START);

        return panel;
    }

    private JPanel createQueueControlsPanel(WalnutbotEnvironment environment, JFrame uiFrame){
        JPanel masterPanel = new JPanel(new BorderLayout());

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.LINE_AXIS));

        int BUTTON_MARGIN = 8;

        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", BUTTON_MARGIN);
        playButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand("jb play",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        controlsPanel.add(playButton);

        JButton pauseButton = ButtonMaker.createIconButton("icons/stop.png", "Pause", BUTTON_MARGIN);
        pauseButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand("jb pause",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        controlsPanel.add(pauseButton);

        JButton nextButton = ButtonMaker.createIconButton("icons/next.png", "Skip", BUTTON_MARGIN);
        nextButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand("jb skip",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        controlsPanel.add(nextButton);

        JLabel currentPlayingSongLabel = new JLabel(noSongText);
        controlsPanel.add(currentPlayingSongLabel);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.LINE_AXIS));

        optionsPanel.add(Box.createHorizontalStrut(10));

        JLabel songTimer = new JLabel("");
        environment.getAudioStateMachine().addSongDurationTracker(new SongDurationTracker(songTimer, currentPlayingSongLabel, noSongText));
        optionsPanel.add(songTimer);

        loopingBox = new JCheckBox("Loop", environment.getAudioStateMachine().getLoopingStatus());
        loopingBox.addItemListener(e -> {
            if (environment.getAudioStateMachine().getLoopingStatus() != loopingBox.isSelected())
                environment.getCommandInterpreter().evaluateCommand(String.format("jb loop %1$b", loopingBox.isSelected()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        });
        optionsPanel.add(loopingBox);

        JButton requestButton = ButtonMaker.createIconButton("icons/add.png", "Make Request", 11);
        requestButton.addActionListener(e -> new MakeRequestFrame("jb ", "Request Songs", environment.getCommandInterpreter(), uiFrame));
        optionsPanel.add(requestButton);

        JButton shuffleButton = ButtonMaker.createIconButton("icons/shuffle.png", "Shuffle", 10);
        shuffleButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand("jb shuffle", Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        optionsPanel.add(shuffleButton);

        JButton clearButton = ButtonMaker.createIconButton("icons/empty.png", "Clear", 3);
        clearButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand("jb clearqueue", Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        optionsPanel.add(clearButton);

        masterPanel.add(controlsPanel, BorderLayout.CENTER);
        masterPanel.add(optionsPanel, BorderLayout.LINE_END);

        return masterPanel;
    }

    private void updateDefaultPlaylistLabel(IAudioStateMachine audioStateMachine) {
        audioStateMachine.getJukeboxDefaultList().accessAudioKeyPlaylist(playlist -> {
            switch (audioStateMachine.getJukeboxDefaultListLoadState()) {
                case UNLOADED:
                    playlistLabel.setIcon(null);
                    playlistLabel.setText("");
                    playlistLabel.setToolTipText("");
                    break;
                case LOCAL_FILE:
                    playlistLabel.setText(playlist.getUrl());
                    playlistLabel.setIcon(new ImageIcon(ButtonMaker.convertIconPath("icons/save.png")));
                    playlistLabel.setToolTipText("This playlist is a local file; Auto-Save is enabled.");
                    break;
                case REMOTE:
                    playlistLabel.setText(playlist.getUrl());
                    playlistLabel.setIcon(new ImageIcon(ButtonMaker.convertIconPath("icons/internet.png")));
                    playlistLabel.setToolTipText("This playlist was loaded from the Internet; Auto-Save is disabled.");
                    break;
            }
        });
        playlistLabel.repaint();
    }

    private void openQuickLoadMenu(Component invoker, CommandInterpreter commandInterpreter, UIFrame uiFrame){
        JPopupMenu popupMenu = new JPopupMenu("./playlists/");

        // List of recent playlists
        popupMenu.add(new JLabel("- History"));
        for (int i = recentPlaylistsHistory.getAudioKeys().size()-1; i >= 0; i--) {
            AudioKey song = recentPlaylistsHistory.getKey(i);
            JMenuItem item = new JMenuItem(song.getUrl(), new ImageIcon(ButtonMaker.convertIconPath("icons/history.png")));
            item.addActionListener(e -> commandInterpreter
                .evaluateCommand("jb dfl load ".concat(FileIO.sanitizeURIForCommand(song.getUrl())),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI),
                    Command.INTERNAL_MASK));
            popupMenu.add(item);
        }

        // List of playlists in nearby folder
        popupMenu.add(new JLabel("- Playlists Folder"));
        for (File file : FileIO.getFilesInDirectory(FileIO.getRootFilePath().concat("playlists/"))) {
            JMenuItem item = new JMenuItem(file.getName(), new ImageIcon(ButtonMaker.convertIconPath("icons/playlistfile.png")));
            item.addActionListener(e -> commandInterpreter.evaluateCommand("jb dfl load ".concat(FileIO.sanitizeURIForCommand(file.getPath())),
                Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
            popupMenu.add(item);
        }

        // The miscellaneous playlist buttons
        popupMenu.add(new JLabel("- File"));
        String[] actionNames = {"Open Playlist...", "New Playlist...", "Empty Playlist",  "Remote (URL) Playlist"};
        String[] actionIcons = {"icons/open.png",   "icons/new.png",   "icons/empty.png", "icons/internet.png"};
        ActionListener[] actions = {
            e -> openJukeboxPlaylist(commandInterpreter),
            e -> createNewJukeboxPlaylist(commandInterpreter),
            e -> commandInterpreter.evaluateCommand("jb dfl disable", Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK),
            e -> new MakeRequestFrame("jb dfl load ", "Remote Playlist", commandInterpreter, uiFrame, false)
        };
        for (int i = 0; i < actionIcons.length; i++) {
            JMenuItem item = new JMenuItem(actionNames[i], new ImageIcon(ButtonMaker.convertIconPath(actionIcons[i])));
            item.addActionListener(actions[i]);
            popupMenu.add(item);
        }

        // Show menu
        popupMenu.show(invoker, 0, 0);
    }

    private void recordRecentPlaylist(AudioKeyPlaylist playlist){
        AudioKey recordKey = new AudioKey(playlist.getName(), playlist.getUrl());

        // Remove duplicate to promote to front
        recentPlaylistsHistory.removeAudioKey(recordKey);

        // Add to playlist history
        recentPlaylistsHistory.addAudioKey(recordKey);

        // Remove end of list if it is too long (want only 5 elements)
        if (recentPlaylistsHistory.getAudioKeys().size() > 5){
            recentPlaylistsHistory.removeAudioKey(0);
        }

        // Save to disk
        recentPlaylistsHistory.saveToFile(new File(FileIO.expandURIMacros(HISTORY_LOC)));
    }

    @Override
    public void onAudioStateMachineUpdateStatus(IAudioStateMachine.AudioStateMachineStatus status) {

    }

    @Override
    public void onJukeboxDefaultListLoadStateUpdate(IAudioStateMachine.JukeboxDefaultListLoadState loadState, IAudioStateMachine origin) {
        origin.getJukeboxDefaultList().accessAudioKeyPlaylist(playlist -> {
            defaultListTable.pullAudioKeyList(playlist);
            addPlaylistButton.setEnabled(loadState == IAudioStateMachine.JukeboxDefaultListLoadState.LOCAL_FILE);
            if (loadState != IAudioStateMachine.JukeboxDefaultListLoadState.UNLOADED)
                recordRecentPlaylist(playlist);
        });
        updateDefaultPlaylistLabel(origin);
    }

    @Override
    public void onJukeboxLoopingStatusUpdate(boolean loopingStatus) {
        loopingBox.setSelected(loopingStatus);
    }

    private class TrackListingTable extends JPanel implements AudioKeyPlaylistListener {

        private final WalnutbotEnvironment environment;
        UIFrame uiFrame;
        boolean isQueueList;

        JLabel endOfQueueLabel = new JLabel("");

        private TrackListingTable(WalnutbotEnvironment environment, UIFrame uiFrame, boolean isQueueList){
            this.environment = environment;
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            this.isQueueList = isQueueList;
            this.uiFrame = uiFrame;
        }

        private void pullAudioKeyList(AudioKeyPlaylist playlist){
            removeAll();
            if (playlist != null) {
                for (int i = 0; i < playlist.getAudioKeys().size(); i++) {
                    add(new TrackListing(playlist.getKey(i), environment, isQueueList));
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

        private void refresh(){
            if (isQueueList)
                environment.getAudioStateMachine().getJukeboxQueue().accessAudioKeyPlaylist(this::pullAudioKeyList);
            else
                environment.getAudioStateMachine().getJukeboxDefaultList().accessAudioKeyPlaylist(this::pullAudioKeyList);
        }

        @Override
        public void onEvent(AudioKeyPlaylist playlist, AudioKeyPlaylistEvent event) {
            switch (event.getEventType()) {
                case ADD:
                    remove(endOfQueueLabel);
                    add(new TrackListing(event.getKey(), environment, isQueueList));
                    break;
                case REMOVE:
                    remove(event.getPos());
                    if (getComponents().length == 0)
                        add(endOfQueueLabel);
                    break;
                case MODIFY:
                    if (getComponent(event.getPos()) instanceof TrackListing && event.getKey() != null) {
                        TrackListing component = (TrackListing) getComponent(event.getPos());
                        component.setData(event.getKey());
                    }
                    break;
                case SHUFFLE:
                case SORT:
                case ON_SUBSCRIBE:
                    pullAudioKeyList(playlist);
                    break;
                case CLEAR:
                    removeAll();
                    add(endOfQueueLabel);
                    break;
                case EVENT_QUEUE_END:
                    revalidate();
                    repaint();
                    break;
            }
        }
    }

    private class TrackListing extends JPanel implements AudioKeyUIWrapper {

        private AudioKey audioKey;
        private JTextComponent nameText;
        private JTextComponent urlText;

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

        private TrackListing(AudioKey audioKey, WalnutbotEnvironment environment,
            boolean isInQueueList){
            UUID = java.util.UUID.randomUUID();
            this.audioKey = audioKey;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            JButton buttonToMeasure;
            if (isInQueueList){
                JButton postponeButton = ButtonMaker.createIconButton("icons/queue.png", "Postpone", 3);
                postponeButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand(String.format("jb postpone %1$d", findMyPosition()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
                add(postponeButton);
                JButton removeButton = ButtonMaker.createIconButton("icons/cancel.png", "Remove", 3);
                removeButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand(String.format("jb deque %1$d", findMyPosition()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
                add(removeButton);
                buttonToMeasure = removeButton;
            } else {
                //Queue Button
                JButton queueButton = ButtonMaker.createIconButton("icons/queue.png", "Queue", 4);
                queueButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand("jb ".concat(FileIO.sanitizeURIForCommand(audioKey.getUrl())),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
                add(queueButton);
                //Edit Button
                JButton editButton = ButtonMaker.createIconButton("icons/menu.png", "Edit", 4);
                editButton.addActionListener(
                    e -> new ModifyAudioKeyFrame(environment, audioKey, findMyPosition(), ModifyAudioKeyFrame.ModificationType.MODIFY, ModifyAudioKeyFrame.TargetList.JUKEBOX_DEFAULT));
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

    private class PlaylistHistoryCircularQueue<E> {

        private E[] elements;
        private int queueOldestPos;

        @SuppressWarnings("unchecked")
        private PlaylistHistoryCircularQueue(int size){
            elements = (E[]) new Object[size];
            queueOldestPos = 0;
        }

        private void record(E event){
            for (E element : elements)
                if (event.equals(element))
                    return;
            elements[queueOldestPos] = event;
            queueOldestPos = (queueOldestPos + 1) % elements.length;
        }

        private Iterator<E> iterator(){
            return new Iterator<E>() {
                private int currentPos = (queueOldestPos == 0) ? elements.length - 1 : queueOldestPos - 1;
                private int consumeCount;

                @Override public boolean hasNext() {
                    if (consumeCount == elements.length)
                        return false;
                    return elements[currentPos] != null;
                }

                @Override public E next() {
                    if (hasNext()) {
                        E element = elements[currentPos];
                        currentPos = (currentPos == 0) ? elements.length - 1 : currentPos - 1;
                        consumeCount++;
                        return element;
                    } else
                        return null;
                }
            };
        }
    }
}
