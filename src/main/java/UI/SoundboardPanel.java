package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Audio.Playlist;
import Utils.ButtonMaker;
import Utils.FileIO;
import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Comparator;

public class SoundboardPanel extends JPanel {

    public SoundboardPanel(AudioMaster master){

        setLayout(new BorderLayout());

        JPanel soundsPanel = createSoundsPanel(master);
        JPanel miscPanel = createMiscPanel(master, soundsPanel);

        JScrollPane scrollPane = new JScrollPane(soundsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.validate();

        add(miscPanel, BorderLayout.PAGE_START);
        add(scrollPane, BorderLayout.CENTER);
        add(createTempPlayPanel(master), BorderLayout.PAGE_END);

        validate();
    }

    private JPanel createMiscPanel(AudioMaster audioMaster, JPanel soundPanel){
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new JButton("Add Sound");
        addButton.addActionListener(e -> new ModifySoundboardFrame(audioMaster, soundPanel, null));
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

        return panel;
    }

    private JPanel createSoundsPanel(AudioMaster audioMaster){
        JPanel panel = new JPanel();
        panel.setLayout(new WrapLayout(WrapLayout.LEFT, 3, 2));

        loadSoundboard(audioMaster, panel);

        panel.validate();

        return panel;
    }

    private void loadSoundboard(AudioMaster audioMaster, JPanel panel){
        panel.removeAll();
        Playlist soundboard = audioMaster.getSoundboardList();
        for (AudioKey audioKey : soundboard.getAudioKeys()){
            panel.add(new SoundPanel(audioKey, audioMaster, panel));
        }
    }

    private JPanel createTempPlayPanel(AudioMaster audioMaster){

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        JTextField urlField = new JTextField("https://www.youtube.com/watch?v=xuCn8ux2gbs");
        JButton playButton = ButtonMaker.createIconButton("icons/start.png", "Play", 12);
        playButton.addActionListener(e -> {
            System.out.printf("Playing track of url: %1$s\n", urlField.getText());
            audioMaster.playSoundboardSound(urlField.getText());
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

        return panel;
    }

    private class ModifySoundboardFrame extends JFrame {

        private ModifySoundboardFrame(AudioMaster audioMaster, JPanel soundsPanel, SoundPanel keyToEdit){

            Container c = getContentPane();

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            
            //Row for the AudioKey's name
            JPanel namePanel = new JPanel();
            namePanel.setLayout(new BorderLayout());
            namePanel.add(new JLabel("Name: "), BorderLayout.LINE_START);
            JTextField nameField = new JTextField();
            if (keyToEdit != null) nameField.setText(keyToEdit.getAudioKey().getName());
            namePanel.add(nameField, BorderLayout.CENTER);
            
            //Row for the AudioKey's URL
            JPanel urlPanel = new JPanel();
            urlPanel.setLayout(new BorderLayout());
            urlPanel.add(new JLabel("URL: "), BorderLayout.LINE_START);
            JTextField urlField = new JTextField();
            if (keyToEdit != null) urlField.setText(keyToEdit.getAudioKey().getUrl());
            urlPanel.add(urlField, BorderLayout.CENTER);

            //Row for buttons
            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 3));

            //Open Sound Button
            JButton openButton = ButtonMaker.createIconButton("icons/open.png", "Search", 5);
            openButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
                fileChooser.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String filepath = fileChooser.getSelectedFile().getAbsolutePath();
                    urlField.setText(filepath);
                    String name = filepath.substring(Math.min(filepath.length(), filepath.lastIndexOf(File.separatorChar) + 1));
                    name = name.substring(0, name.lastIndexOf('.'));
                    nameField.setText(name);
                }
            });
            buttonsPanel.add(openButton);

            //Add Sound Button
            if (keyToEdit == null) { //If the keyToEdit is null, we are creating a new one
                JButton addSoundButton = new JButton("Add to Playlist");
                addSoundButton.addActionListener(e -> {
                    AudioKey key = new AudioKey(nameField.getText(), urlField.getText());
                    if (key.isValid()) {
                        audioMaster.getSoundboardList().addAudioKey(key);
                        audioMaster.getSoundboardList().printPlaylist();
                        audioMaster.saveSoundboard();
                        soundsPanel.add(new SoundPanel(key, audioMaster, soundsPanel));
                        soundsPanel.revalidate();
                        soundsPanel.repaint();
                    }
                    dispose();
                });
                buttonsPanel.add(addSoundButton);
            }

            //Remove Button
            if (keyToEdit != null){ //If keyToEdit is nonnull, we are modifying one
                JButton removeButton = new JButton("Remove");
                removeButton.addActionListener(e -> {
                    audioMaster.getSoundboardList().removeAudioKey(keyToEdit.getAudioKey());
                    audioMaster.getSoundboardList().printPlaylist();
                    audioMaster.saveSoundboard();
                    soundsPanel.remove(keyToEdit);
                    soundsPanel.revalidate();
                    soundsPanel.repaint();
                    dispose();
                });
                buttonsPanel.add(removeButton);
            }

            //Apply (Changes) Button
            if (keyToEdit != null){
                JButton applyButton = new JButton("Apply");
                applyButton.addActionListener(e -> {
                    if (nameField.getText().length() > 0 && urlField.getText().length() > 0) {
                        keyToEdit.getAudioKey().setName(nameField.getText());
                        keyToEdit.getAudioKey().setUrl(urlField.getText());
                        keyToEdit.getPlayButton().setText(nameField.getText());
                        audioMaster.getSoundboardList().printPlaylist();
                        audioMaster.saveSoundboard();
                        soundsPanel.revalidate();
                        soundsPanel.repaint();
                    }
                    dispose();
                });
                buttonsPanel.add(applyButton);
            }

            //Cancel Button
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dispose());
            buttonsPanel.add(cancelButton);

            panel.add(namePanel);
            panel.add(urlPanel);
            panel.add(buttonsPanel);

            c.add(panel);
            c.validate();

            setTitle("Add New Sound");
            setSize(new Dimension(430, 110));
            setVisible(true);
        }

    }

    private class SoundPanel extends JPanel{
        private AudioKey audioKey;
        private JButton playButton;

        private SoundPanel(AudioKey key, AudioMaster audioMaster, JPanel soundsPanel){
            audioKey = key;
            setLayout(new BorderLayout());

            playButton = new JButton(key.getName());
            playButton.addActionListener(e -> audioMaster.playSoundboardSound(key.getUrl()));
            add(playButton, BorderLayout.CENTER);

            JButton menuButton = ButtonMaker.createIconButton("icons/menu.png", "Config", 5);
            menuButton.addActionListener(e -> new ModifySoundboardFrame(audioMaster, soundsPanel, this));
            add(menuButton, BorderLayout.LINE_END);
        }

        public AudioKey getAudioKey() {
            return audioKey;
        }

        public void setAudioKey(AudioKey audioKey) {
            this.audioKey = audioKey;
        }

        public JButton getPlayButton() {
            return playButton;
        }

        @Override public boolean equals(Object obj) {
            if (obj instanceof SoundPanel) {
                SoundPanel soundPanel = (SoundPanel) obj;
                return audioKey.equals(soundPanel.getAudioKey());
            }
            return false;
        }
    }
}
