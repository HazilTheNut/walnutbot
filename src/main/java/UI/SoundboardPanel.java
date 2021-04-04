package UI;

import Audio.*;
import Commands.Command;
import Commands.CommandInterpreter;
import Utils.FileIO;
import Utils.SettingsLoader;
import Utils.Transcriber;

import javax.swing.*;
import java.awt.*;

public class SoundboardPanel extends JPanel implements PlayerTrackListener{

    private JLabel playerStatusLabel;
    private SoundsMainPanel soundsPanel;

    private CommandInterpreter commandInterpreter;

    private SoundsMainPanel soundsMainPanel;
    private LayoutManager listLayout;
    private LayoutManager gridLayout;

    public SoundboardPanel(AudioMaster master, CommandInterpreter commandInterpreter){

        this.commandInterpreter = commandInterpreter;

        setLayout(new BorderLayout());

        soundsPanel = createSoundsPanel(master);
        JPanel miscPanel = createMiscPanel(master, commandInterpreter);

        JScrollPane scrollPane = new JScrollPane(soundsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.validate();

        add(miscPanel, BorderLayout.PAGE_START);
        add(scrollPane, BorderLayout.CENTER);
        add(createTempPlayPanel(commandInterpreter), BorderLayout.PAGE_END);

        validate();

        master.getGenericTrackScheduler().addPlayerTrackListener(this);
    }

    private JPanel createMiscPanel(AudioMaster audioMaster, CommandInterpreter commandInterpreter){
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        //panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        JButton addButton = new JButton("Add Sound");
        addButton.addActionListener(e -> new ModifyAudioKeyFrame(audioMaster, new AudioKey("",""), -1, commandInterpreter, ModifyAudioKeyFrame.ModificationType.ADD, ModifyAudioKeyFrame.TargetList.SOUNDBOARD));
        panel.add(addButton);

        JButton sortButton = new JButton("Sort A-Z");
        sortButton.addActionListener(e -> commandInterpreter.evaluateCommand("sb sort",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        panel.add(sortButton);

        playerStatusLabel = new JLabel("Status: ");

        panel.add(playerStatusLabel);

        JButton stopButton = ButtonMaker.createIconButton("icons/stop.png", "Stop", 8);
        stopButton.addActionListener(e -> commandInterpreter.evaluateCommand("sb stop",
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
        panel.add(stopButton);

        JButton listLayoutButton = ButtonMaker.createIconButton("icons/menu.png", "List Layout", 8);
        JButton gridLayoutButton = ButtonMaker.createIconButton("icons/grid.png", "Grid Layout", 8);

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

        return panel;
    }

    private SoundsMainPanel createSoundsPanel(AudioMaster audioMaster){
        soundsMainPanel = new SoundsMainPanel(audioMaster, commandInterpreter);
        listLayout = new WrapLayout(WrapLayout.LEFT, 6, 2);
        gridLayout = new GridLayout(0, 4);

        if (SettingsLoader.getSettingsValue("soundboard_preferred_layout", "list").equals("grid"))
            soundsMainPanel.setLayout(gridLayout);
        else
            soundsMainPanel.setLayout(listLayout);

        soundsMainPanel.loadSoundboard();

        soundsMainPanel.validate();

        return soundsMainPanel;
    }

    private JPanel createTempPlayPanel(CommandInterpreter commandInterpreter){

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        JTextField urlField = new JTextField("Enter URL Here for Instant Play");
        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", 12);
        playButton.addActionListener(e -> {
            commandInterpreter.evaluateCommand(
                String.format("sb url %1$s", urlField.getText().replace("\\", "\\\\").replace("\"", "\\\"")),
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

    @Override public void onTrackStart() {
        playerStatusLabel.setText("Status: Playing");
        playerStatusLabel.repaint();
    }

    @Override public void onTrackStop() {
        playerStatusLabel.setText("Status: Stopped");
        playerStatusLabel.repaint();
    }

    @Override public void onTrackError() {
        playerStatusLabel.setText("Status: ERROR!");
        playerStatusLabel.repaint();
    }

    private class SoundsMainPanel extends JPanel implements AudioKeyPlaylistListener {

        AudioMaster audioMaster;
        CommandInterpreter commandInterpreter;

        public SoundsMainPanel(AudioMaster audioMaster, CommandInterpreter commandInterpreter){
            this.audioMaster = audioMaster;
            this.commandInterpreter = commandInterpreter;
            audioMaster.getSoundboardList().addAudioKeyPlaylistListener(this);
        }

        private void loadSoundboard(){
            removeAll();
            AudioKeyPlaylist soundboard = audioMaster.getSoundboardList();
            for (int i = 0; i < soundboard.getAudioKeys().size(); i++) {
                AudioKey key = soundboard.getKey(i);
                add(new SoundButtonPanel(key, audioMaster, commandInterpreter));
            }
        }

        @Override public void onAddition(AudioKeyPlaylistEvent event) {
            add(new SoundButtonPanel(event.getKey(), audioMaster, commandInterpreter));
            revalidate();
            repaint();
        }

        @Override public void onRemoval(AudioKeyPlaylistEvent event) {
            loadSoundboard();
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
            if (getComponent(event.getPos()) instanceof AudioKeyUIWrapper) {
                ((AudioKeyUIWrapper)getComponent(event.getPos())).setData(event.getKey());
                revalidate();
                repaint();
            }
        }

        /**
         * Called when the AudioKeyPlaylist has been cleared.
         */
        @Override public void onClear() {
            removeAll();
            revalidate();
            repaint();
        }

        /**
         * Called when the AudioKeyPlaylist has been shuffled.
         */
        @Override public void onShuffle() {
            loadSoundboard();
            revalidate();
            repaint();
        }

        @Override public void onSort() {
            loadSoundboard();
            revalidate();
            repaint();
        }

        @Override public void onNewPlaylist() {
            loadSoundboard();
            revalidate();
            repaint();
        }
    }
}
