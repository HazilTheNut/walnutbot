package UI;

import Audio.AudioMaster;
import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;

public class UIFrame extends JFrame {

    public UIFrame(JDA jda, AudioMaster master){

        setTitle("Walnutbot Music & Soundboard Discord Bot");
        setSize(new Dimension(400, 400));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Settings", new SettingsPanel(jda, master));
        if (jda != null) {
            tabbedPane.addTab("Soundboard", new SoundboardPanel(master));
        }

        getContentPane().add(tabbedPane);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

}
