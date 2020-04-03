package UI;

import Utils.Transcriber;
import Utils.TranscriptReceiver;

import javax.swing.*;
import java.awt.*;

public class ConsolePanel extends JPanel implements TranscriptReceiver {

    private JTextArea outputArea;
    private JScrollPane scrollPane;

    public ConsolePanel(){
        Transcriber.addTranscriptReceiver(this);
        setLayout(new BorderLayout());

        outputArea = new JTextArea();
        outputArea.setEditable(false);

        scrollPane = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        Transcriber.print("Console successfully connected to system output!");
    }

    @Override public void receiveMessage(String message) {
        JScrollBar vertBar = scrollPane.getVerticalScrollBar();
        boolean scrollBarAtBottom = Math.abs(vertBar.getValue() + vertBar.getVisibleAmount() - vertBar.getMaximum()) < (int)(0.15 * vertBar.getMaximum());
        outputArea.append(message.concat("\n"));
        //Keep scroll bar at the bottom if it previously was.
        if (scrollBarAtBottom) vertBar.setValue(vertBar.getMaximum());
    }
}
