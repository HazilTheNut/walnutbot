package UI;

import Audio.AudioMaster;
import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;

public class SoundboardPanel extends JPanel {

    private JDA jda;

    public SoundboardPanel(JDA jda, AudioMaster master){
        this.jda = jda;

        setLayout(new BorderLayout());

        add(new ConnectionPanel(jda, master), BorderLayout.PAGE_START);
        add(createSoundsPanel(master), BorderLayout.CENTER);

        validate();
    }

    private JPanel createSoundsPanel(AudioMaster audioMaster){

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        JTextField urlField = new JTextField("https://www.youtube.com/watch?v=xuCn8ux2gbs");
        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> {
            System.out.printf("Playing track of url: %1$s\n", urlField.getText());
            audioMaster.playAudio(urlField.getText(), audioMaster.getSoundboardPlayer());
        });

        JButton searchButton = new JButton("Search...");
        searchButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
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
}
