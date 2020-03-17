package UI;

import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;

public class SoundboardPanel extends JPanel {

    private JDA jda;

    public SoundboardPanel(JDA jda){
        this.jda = jda;

        setLayout(new BorderLayout());

        add(new ConnectionPanel(jda), BorderLayout.PAGE_START);
        add(createSoundsPanel(), BorderLayout.CENTER);

        validate();
    }

    private JPanel createSoundsPanel(){

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        JTextField urlField = new JTextField();
        JButton playButton = new JButton("Play");

        panel.add(urlField);
        panel.add(playButton);

        return panel;
    }
}
