package UI;

import javax.swing.*;
import java.awt.*;

public class JukeboxPanel extends JPanel {

    public JukeboxPanel(){

        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, new JTextArea(), new JTextArea());

        add(splitPane, BorderLayout.CENTER);
        validate();

    }

}
