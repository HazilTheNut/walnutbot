package UI;

import Audio.AudioMaster;
import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    public SettingsPanel(JDA jda, AudioMaster audioMaster){

        setLayout(new BorderLayout());
        if (jda != null)
            add(new ConnectionPanel(jda, audioMaster), BorderLayout.PAGE_START);
        else
            add(new JLabel("WARNING! Looks like the bot failed to load! (Check output.txt for more info)"), BorderLayout.CENTER);

        validate();

    }

}
