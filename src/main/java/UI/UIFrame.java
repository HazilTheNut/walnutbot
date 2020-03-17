package UI;

import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;

public class UIFrame extends JFrame {

    public UIFrame(JDA jda){

        setTitle("Walnutbot Music & Soundboard Discord Bot");
        setSize(new Dimension(400, 400));

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel testPanel = new JPanel();
        tabbedPane.addTab("Test", testPanel);
        if (jda != null) {
            tabbedPane.addTab("Soundboard", new SoundboardPanel(jda));
        }

        JButton btn = new JButton("Close");
        btn.addActionListener(e -> System.exit(NORMAL));

        JButton test = new JButton("Print");
        test.addActionListener(e -> System.out.println("asdf"));

        testPanel.add(btn);
        testPanel.add(test, BorderLayout.PAGE_END);

        getContentPane().add(tabbedPane);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

}
