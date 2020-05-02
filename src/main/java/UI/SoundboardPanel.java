package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Audio.AudioMaster;
import Utils.ButtonMaker;
import Utils.FileIO;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public class SoundboardPanel extends JPanel implements PlayerTrackListener{

    private JLabel playerStatusLabel;

    public SoundboardPanel(AudioMaster master){

        setLayout(new BorderLayout());

        SoundsMainPanel soundsPanel = createSoundsPanel(master);
        JPanel miscPanel = createMiscPanel(master, soundsPanel);

        JScrollPane scrollPane = new JScrollPane(soundsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.validate();

        add(miscPanel, BorderLayout.PAGE_START);
        add(scrollPane, BorderLayout.CENTER);
        add(createTempPlayPanel(master), BorderLayout.PAGE_END);

        validate();

        master.getGenericTrackScheduler().addPlayerTrackListener(this);
    }

    private JPanel createMiscPanel(AudioMaster audioMaster, SoundsMainPanel soundPanel){
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new JButton("Add Sound");
        addButton.addActionListener(e -> new ModifyAudioKeyFrame(audioMaster, soundPanel, null, audioMaster.getSoundboardList(), audioMaster::saveSoundboard));
        panel.add(addButton);

        JButton sortButton = new JButton("Sort A-Z");
        sortButton.addActionListener(e -> {
            audioMaster.getSoundboardList().getAudioKeys().sort(Comparator.comparing(AudioKey::getName));
            audioMaster.saveSoundboard();
            loadSoundboard(audioMaster, soundPanel);
            soundPanel.revalidate();
            soundPanel.repaint();
        });
        panel.add(sortButton);

        playerStatusLabel = new JLabel("Status: ");

        panel.add(playerStatusLabel);

        JButton stopButton = ButtonMaker.createIconButton("icons/stop.png", "Stop", 8);
        stopButton.addActionListener(e -> audioMaster.resumeJukebox());
        panel.add(stopButton);

        return panel;
    }

    private SoundsMainPanel createSoundsPanel(AudioMaster audioMaster){
        SoundsMainPanel panel = new SoundsMainPanel(audioMaster);
        panel.setLayout(new WrapLayout(WrapLayout.LEFT, 6, 2));

        loadSoundboard(audioMaster, panel);

        panel.validate();

        return panel;
    }

    private void loadSoundboard(AudioMaster audioMaster, SoundsMainPanel panel){
        panel.removeAll();
        AudioKeyPlaylist soundboard = audioMaster.getSoundboardList();
        for (AudioKey audioKey : soundboard.getAudioKeys()){
            panel.add(new SoundButtonPanel(audioKey, audioMaster, panel));
        }
    }

    private JPanel createTempPlayPanel(AudioMaster audioMaster){

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        JTextField urlField = new JTextField("Enter URL Here for Instant Play");
        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", 12);
        playButton.addActionListener(e -> {
            System.out.printf("Soundboard Instant Play - Playing track of url: %1$s\n", urlField.getText());
            audioMaster.playSoundboardSound(urlField.getText());
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

    private class SoundsMainPanel extends JPanel implements PlaylistUIWrapper{

        AudioMaster audioMaster;

        private SoundsMainPanel(AudioMaster audioMaster){
            this.audioMaster = audioMaster;
        }

        @Override public void addAudioKey(AudioKey key) {
            add(new SoundButtonPanel(key, audioMaster, this));
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
