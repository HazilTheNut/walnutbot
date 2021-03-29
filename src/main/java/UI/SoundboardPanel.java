package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Audio.AudioMaster;
import Commands.Command;
import Commands.CommandFeedbackHandler;
import Commands.CommandInterpreter;
import Utils.ButtonMaker;
import Utils.FileIO;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public class SoundboardPanel extends JPanel implements PlayerTrackListener, SoundboardUIWrapper{

    private JLabel playerStatusLabel;
    private SoundsMainPanel soundsPanel;

    private CommandInterpreter commandInterpreter;
    private UIFrame uiFrame;

    public SoundboardPanel(AudioMaster master, CommandInterpreter commandInterpreter, UIFrame uiFrame){

        this.commandInterpreter = commandInterpreter;
        this.uiFrame = uiFrame;

        setLayout(new BorderLayout());

        soundsPanel = createSoundsPanel(master);
        JPanel miscPanel = createMiscPanel(master, soundsPanel, uiFrame);

        JScrollPane scrollPane = new JScrollPane(soundsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.validate();

        add(miscPanel, BorderLayout.PAGE_START);
        add(scrollPane, BorderLayout.CENTER);
        add(createTempPlayPanel(commandInterpreter, uiFrame), BorderLayout.PAGE_END);

        validate();

        master.getGenericTrackScheduler().addPlayerTrackListener(this);
    }

    private JPanel createMiscPanel(AudioMaster audioMaster, SoundsMainPanel soundPanel, UIFrame uiFrame){
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new JButton("Add Sound");
        addButton.addActionListener(e -> new ModifyAudioKeyFrame(audioMaster, soundPanel, null, audioMaster.getSoundboardList(), audioMaster::saveSoundboard));
        panel.add(addButton);

        JButton sortButton = new JButton("Sort A-Z");
        sortButton.addActionListener(e -> commandInterpreter.evaluateCommand("sb sort", uiFrame.getCommandFeedbackHandler(), Command.ADMIN_MASK));
        panel.add(sortButton);

        playerStatusLabel = new JLabel("Status: ");

        panel.add(playerStatusLabel);

        JButton stopButton = ButtonMaker.createIconButton("icons/stop.png", "Stop", 8);
        stopButton.addActionListener(e -> audioMaster.resumeJukebox());
        panel.add(stopButton);

        audioMaster.setSoundboardUIWrapper(this);

        return panel;
    }

    private SoundsMainPanel createSoundsPanel(AudioMaster audioMaster){
        SoundsMainPanel panel = new SoundsMainPanel(audioMaster, commandInterpreter, uiFrame);
        panel.setLayout(new WrapLayout(WrapLayout.LEFT, 6, 2));

        loadSoundboard(audioMaster, panel, commandInterpreter, uiFrame);

        panel.validate();

        return panel;
    }

    private void loadSoundboard(AudioMaster audioMaster, SoundsMainPanel panel, CommandInterpreter commandInterpreter, UIFrame uiFrame){
        panel.removeAll();
        AudioKeyPlaylist soundboard = audioMaster.getSoundboardList();
        for (AudioKey audioKey : soundboard.getAudioKeys()){
            panel.add(new SoundButtonPanel(audioKey, audioMaster,  commandInterpreter, uiFrame, panel));
        }
    }

    private JPanel createTempPlayPanel(CommandInterpreter commandInterpreter, UIFrame uiFrame){

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        JTextField urlField = new JTextField("Enter URL Here for Instant Play");
        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", 12);
        playButton.addActionListener(e -> {
            commandInterpreter.evaluateCommand(
                String.format("sb url %1$s", urlField.getText()),
                uiFrame.getCommandFeedbackHandler(),
                Command.ADMIN_MASK
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

    @Override public void updateSoundboardSoundsList(AudioMaster audioMaster) {
        loadSoundboard(audioMaster, soundsPanel, commandInterpreter, uiFrame);
        soundsPanel.revalidate();
        soundsPanel.repaint();
    }

    private class SoundsMainPanel extends JPanel implements PlaylistUIWrapper{

        AudioMaster audioMaster;
        CommandInterpreter commandInterpreter;
        UIFrame uiFrame;

        public SoundsMainPanel(AudioMaster audioMaster, CommandInterpreter commandInterpreter, UIFrame uiFrame){
            this.audioMaster = audioMaster;
            this.commandInterpreter = commandInterpreter;
            this.uiFrame = uiFrame;
        }

        @Override public void addAudioKey(AudioKey key) {
            add(new SoundButtonPanel(key, audioMaster, commandInterpreter, uiFrame, this));
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
}
