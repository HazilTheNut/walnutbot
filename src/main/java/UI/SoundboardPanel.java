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

public class SoundboardPanel extends JPanel {

    private JDA jda;

    public SoundboardPanel(JDA jda, AudioMaster master){
        this.jda = jda;

        setLayout(new BorderLayout());

        add(new ConnectionPanel(jda, master), BorderLayout.PAGE_START);
        add(createSoundsPanel(master), BorderLayout.CENTER);
        add(createTempPlayPanel(master), BorderLayout.PAGE_END);

        validate();
    }

    private JPanel createSoundsPanel(AudioMaster audioMaster){
        JPanel panel = new JPanel();
        panel.setLayout(new WrapLayout(WrapLayout.LEFT, 3, 2));

        Playlist soundboard = audioMaster.getSoundboardList();
        for (AudioKey audioKey : soundboard.getAudioKeys()){
            panel.add(generateSoundPlayPanel(audioKey, audioMaster));
        }

        panel.validate();
        return panel;
    }

    private JScrollPane generateSoundPlayPanel(AudioKey key, AudioMaster audioMaster){
        JPanel internalPanel = new JPanel();
        internalPanel.setLayout(new BorderLayout());

        JButton playButton = new JButton(key.getName());
        playButton.addActionListener(e -> audioMaster.playSoundboardSound(key.getUrl()));
        internalPanel.add(playButton, BorderLayout.CENTER);

        JButton menuButton = ButtonMaker.createIconButton("icons/menu.png", "Config", 5);
        internalPanel.add(menuButton, BorderLayout.LINE_END);

        internalPanel.validate();

        JScrollPane scrollPane = new JScrollPane(internalPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.validate();

        return scrollPane;
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
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
            fileChooser.setSelectedFile(new File(FileIO.getRootFilePath()));
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION)
                urlField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        });

        panel.add(urlField);
        panel.add(playButton);
        panel.add(searchButton);

        return panel;
    }
}
