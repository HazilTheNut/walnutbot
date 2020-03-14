import javax.swing.*;
import java.awt.*;

public class UIFrame extends JFrame {

    public UIFrame(){

        setTitle("Walnutbot Music & Soundboard Discord Bot");
        setSize(new Dimension(400, 100));

        JButton btn = new JButton("Close");
        btn.addActionListener(e -> System.exit(NORMAL));

        getContentPane().add(btn);

        setVisible(true);
    }

}
