package UI;

import Audio.*;
import Commands.Command;
import Commands.CommandInterpreter;
import Main.WalnutbotEnvironment;
import Utils.FileIO;
import Utils.SettingsLoader;
import Utils.Transcriber;

import javax.swing.*;
import java.awt.*;

public class SoundboardPanel extends JPanel implements IAudioStateMachineListener{

    private JLabel playerStatusLabel;
    private SoundsMainPanel soundsPanel;

    private SoundsMainPanel soundsMainPanel;
    private LayoutManager listLayout;
    private LayoutManager gridLayout;

    public SoundboardPanel(WalnutbotEnvironment environment){

        setLayout(new BorderLayout());

        soundsPanel = createSoundsPanel(environment);
        JPanel miscPanel = createMiscPanel(environment);

        JScrollPane scrollPane = new JScrollPane(soundsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.validate();

        add(miscPanel, BorderLayout.PAGE_START);
        add(scrollPane, BorderLayout.CENTER);
        add(createTempPlayPanel(environment), BorderLayout.PAGE_END);

        validate();

        environment.getAudioStateMachine().addAudioStateMachineListener(this);
    }

    private JPanel createMiscPanel(WalnutbotEnvironment environment){
        JPanel panel = new JPanel();
        //panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        final int STRUT_SIZE = 2;

        JButton addButton = new JButton("Add Sound");
        addButton.addActionListener(e -> new ModifyAudioKeyFrame(environment, new AudioKey("",""), -1, ModifyAudioKeyFrame.ModificationType.ADD, ModifyAudioKeyFrame.TargetList.SOUNDBOARD));
        panel.add(addButton);
        panel.add(Box.createHorizontalStrut(STRUT_SIZE));

        JButton sortButton = new JButton("Sort A-Z");
        sortButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand("sb sort",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        panel.add(sortButton);
        panel.add(Box.createHorizontalStrut(STRUT_SIZE));

        JButton stopButton = ButtonMaker.createIconButton("icons/stop.png", "Stop", 8);
        stopButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand("sb stop",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        panel.add(stopButton);
        panel.add(Box.createHorizontalStrut(STRUT_SIZE));

        playerStatusLabel = new JLabel("Status: ");
        panel.add(playerStatusLabel);

        JButton listLayoutButton = ButtonMaker.createIconButton("icons/menu.png", "List Layout", 8);
        JButton gridLayoutButton = ButtonMaker.createIconButton("icons/grid.png", "Grid Layout", 8);

        panel.add(Box.createHorizontalGlue());

        listLayoutButton.addActionListener(e -> {
            soundsMainPanel.setLayout(listLayout);
            soundsMainPanel.revalidate();
            soundsMainPanel.repaint();
            SettingsLoader.modifySettingsValue("soundboard_preferred_layout", "list");
            SettingsLoader.writeSettingsFile();
            gridLayoutButton.setEnabled(true);
            listLayoutButton.setEnabled(false);
        });
        listLayoutButton.setEnabled(!SettingsLoader.getSettingsValue("soundboard_preferred_layout", "list").equals("list"));
        panel.add(listLayoutButton);
        panel.add(Box.createHorizontalStrut(STRUT_SIZE));

        gridLayoutButton.addActionListener(e -> {
            soundsMainPanel.setLayout(gridLayout);
            soundsMainPanel.revalidate();
            soundsMainPanel.repaint();
            SettingsLoader.modifySettingsValue("soundboard_preferred_layout", "grid");
            SettingsLoader.writeSettingsFile();
            gridLayoutButton.setEnabled(false);
            listLayoutButton.setEnabled(true);

        });
        gridLayoutButton.setEnabled(!SettingsLoader.getSettingsValue("soundboard_preferred_layout", "list").equals("grid"));
        panel.add(gridLayoutButton);

        //audioMaster.setSoundboardUIWrapper(this);

        panel.setBorder(BorderFactory.createEmptyBorder(STRUT_SIZE, STRUT_SIZE, STRUT_SIZE, STRUT_SIZE));

        return panel;
    }

    private SoundsMainPanel createSoundsPanel(WalnutbotEnvironment environment){
        soundsMainPanel = new SoundsMainPanel(environment);
        listLayout = new WrapLayout(WrapLayout.LEFT, 6, 2);
        gridLayout = new GridLayout(0, 4);

        if (SettingsLoader.getSettingsValue("soundboard_preferred_layout", "list").equals("grid"))
            soundsMainPanel.setLayout(gridLayout);
        else
            soundsMainPanel.setLayout(listLayout);

        soundsMainPanel.validate();

        return soundsMainPanel;
    }

    private JPanel createTempPlayPanel(WalnutbotEnvironment environment){

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        JTextField urlField = new JTextField("Enter URL Here for Instant Play");
        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", 12);
        playButton.addActionListener(e -> {
            environment.getCommandInterpreter().evaluateCommand(
                String.format("sb url %1$s", FileIO.sanitizeURIForCommand(urlField.getText())),
                Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI),
                Command.INTERNAL_MASK
            );
            urlField.setText("");
        });

        JButton searchButton = ButtonMaker.createIconButton("icons/open.png", "Search...", 5);
        searchButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
            fileChooser.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION)
                urlField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        });

        panel.add(urlField);
        panel.add(playButton);
        panel.add(searchButton);

        panel.setBorder(BorderFactory.createTitledBorder("Instant Play"));

        return panel;
    }

    @Override
    public void onAudioStateMachineUpdateStatus(IAudioStateMachine.AudioStateMachineStatus status) {
        switch (status) {
            case SOUNDBOARD_PLAYING:
            case SOUNDBOARD_PLAYING_JUKEBOX_READY:
            case SOUNDBOARD_PLAYING_JUKEBOX_PAUSED:
            case SOUNDBOARD_PLAYING_JUKEBOX_SUSPENDED:
                playerStatusLabel.setText("Status: Playing");
                break;
            case INACTIVE:
            case JUKEBOX_PAUSED:
            case JUKEBOX_PLAYING:
                playerStatusLabel.setText("Status: Stopped");
                break;
        }
        playerStatusLabel.repaint();
    }

    @Override
    public void onJukeboxDefaultListLoadStateUpdate(IAudioStateMachine.JukeboxDefaultListLoadState loadState, IAudioStateMachine origin) {

    }

    @Override
    public void onJukeboxLoopingStatusUpdate(boolean loopingStatus) {

    }

    private static class SoundsMainPanel extends JPanel implements AudioKeyPlaylistListener {
        private final WalnutbotEnvironment environment;

        public SoundsMainPanel(WalnutbotEnvironment environment){
            this.environment = environment;
            environment.getAudioStateMachine().getSoundboardList().accessAudioKeyPlaylist(playlist -> playlist.addAudioKeyPlaylistListener(this));
        }

        private void loadSoundboard(AudioKeyPlaylist playlist){
            removeAll();
            for (int i = 0; i < playlist.getAudioKeys().size(); i++) {
                AudioKey key = playlist.getKey(i);
                add(new SoundButtonPanel(key, environment));
            }
        }

        @Override
        public void onEvent(AudioKeyPlaylist playlist, AudioKeyPlaylistEvent event) {
            switch (event.getEventType()) {
                case ADD:
                    add(new SoundButtonPanel(event.getKey(), environment));
                    break;
                case REMOVE:
                case SORT:
                case SHUFFLE:
                case ON_SUBSCRIBE:
                    loadSoundboard(playlist);
                    break;
                case MODIFY:
                    if (getComponent(event.getPos()) instanceof AudioKeyUIWrapper) {
                        ((AudioKeyUIWrapper)getComponent(event.getPos())).setData(event.getKey());
                    }
                    break;
                case CLEAR:
                    removeAll();
                    break;
                case EVENT_QUEUE_END:
                    revalidate();
                    repaint();
                    break;
            }
        }
    }
}
