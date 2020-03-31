package UI;

import Audio.AudioMaster;
import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    public SettingsPanel(JDA jda, AudioMaster audioMaster){

        setLayout(new BorderLayout());
        add(new ConnectionPanel(jda, audioMaster), BorderLayout.PAGE_START);

        validate();

    }

}
