package UI;

import Commands.Command;
import Commands.CommandFeedbackHandler;
import Commands.CommandInterpreter;
import Utils.Transcriber;
import Utils.TranscriptReceiver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ConsolePanel extends JPanel implements TranscriptReceiver {

    private JTextArea outputArea;
    private JScrollPane scrollPane;

    public ConsolePanel(CommandInterpreter commandInterpreter){
        Transcriber.addTranscriptReceiver(this);
        setLayout(new BorderLayout());

        outputArea = new JTextArea();
        outputArea.setEditable(false);

        scrollPane = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        JPanel consoleInputPanel = new JPanel();
        consoleInputPanel.setLayout(new BorderLayout());

        consoleInputPanel.add(new JLabel(">: "), BorderLayout.LINE_START);

        JTextField consoleInput = new JTextField();
        consoleInput.setName(">: ");
        consoleInput.addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent e) {

            }

            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    commandInterpreter.evaluateCommand(consoleInput.getText(),
                        new ConsoleCommandFeedbackHandler(),
                        Command.INTERNAL_MASK);
                    consoleInput.setText("");
                }
            }

            @Override public void keyReleased(KeyEvent e) {

            }
        });

        consoleInputPanel.add(consoleInput, BorderLayout.CENTER);
        if (commandInterpreter != null) add(consoleInputPanel, BorderLayout.PAGE_END);

        Transcriber.printTimestamped("Console successfully connected to system output!");
    }

    @Override public void receiveMessage(String message) {
        JScrollBar vertBar = scrollPane.getVerticalScrollBar();
        boolean scrollBarAtBottom = Math.abs(vertBar.getValue() + vertBar.getVisibleAmount() - vertBar.getMaximum()) < (int)(0.15 * vertBar.getMaximum());
        outputArea.append(message.concat("\n"));
        //Keep scroll bar at the bottom if it previously was.
        if (scrollBarAtBottom) vertBar.setValue(vertBar.getMaximum());
    }

    private class ConsoleCommandFeedbackHandler implements CommandFeedbackHandler {

        /**
         * Sends a public message in the same channel as where the command is found.
         *
         * @param message           The message to send
         * @param isCopiedToConsole Whether or not the message is copied to this bot's System.out
         */
        @Override public void sendMessage(String message, boolean isCopiedToConsole) {
            if (!isCopiedToConsole)
                Transcriber.printRaw(message);
        }

        /**
         * @return True if the channel where the command is found is a public space, rather than a form of private message
         */
        @Override public boolean isChannelPublic() {
            return false;
        }

        /**
         * Sends a private message to the command author
         *
         * @param message           The message to send
         * @param isCopiedToConsole Whether or not the message is copied to this bot's System.out
         */
        @Override public void sendAuthorPM(String message, boolean isCopiedToConsole) {
            if (!isCopiedToConsole)
                Transcriber.printRaw(message);
        }

        /**
         * Gets a String describing the author of the command.
         *
         * @return A String describing the author of the command.
         */
        @Override public String getAuthor() {
            return Transcriber.AUTTH_CONSOLE;
        }

        /**
         * Returns the size of pages to display for listing commands such as help, jb list, etc.
         *
         * @param commandType The command to distinguish page sizes for
         * @return The number of elements to list on a given page.
         */
        @Override public int getListPageSize(CommandType commandType) {
            return Integer.MAX_VALUE;
        }
    }
}
