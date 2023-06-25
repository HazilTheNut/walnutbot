package UI;

import Commands.Command;
import Commands.CommandInterpreter;
import Utils.ConsoleCommandFeedbackHandler;
import Utils.FileIO;
import Utils.Transcriber;
import Utils.TranscriptReceiver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

public class ConsolePanel extends JPanel implements TranscriptReceiver {

    private final JTextArea outputArea;
    private final JScrollPane scrollPane;

    public ConsolePanel(CommandInterpreter commandInterpreter){
        Transcriber.addTranscriptReceiver(this);
        setLayout(new BorderLayout());

        outputArea = new JTextArea();
        outputArea.setEditable(false);

        scrollPane = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new BorderLayout());

        JButton scriptsButton = new JButton("Scripts");
        scriptsButton.addActionListener(e -> openScriptsMenu(scriptsButton, commandInterpreter));
        lowerPanel.add(scriptsButton, BorderLayout.LINE_START);

        JPanel consoleInputPanel = new JPanel();
        consoleInputPanel.setLayout(new BorderLayout());

        consoleInputPanel.add(new JLabel("Enter Commands: "), BorderLayout.LINE_START);

        JTextField consoleInput = new JTextField();
        consoleInput.setName(">: ");
        consoleInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
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
        lowerPanel.add(consoleInputPanel, BorderLayout.CENTER);
        if (commandInterpreter != null) add(lowerPanel, BorderLayout.PAGE_END);

        Transcriber.printTimestamped("Console successfully connected to system output!");
    }

    private void openScriptsMenu(Component invoker, CommandInterpreter commandInterpreter) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new JLabel(".wbs scripts in ~~/scripts/:"));

        for (File file : FileIO.getScriptFilesInDirectory(FileIO.getRootFilePath().concat("scripts/"))) {
            JMenuItem menuItem = new JMenuItem(file.getName(), new ImageIcon(ButtonMaker.convertIconPath("icons/quick_menu.png")));
            menuItem.addActionListener(e -> commandInterpreter.evaluateCommand("script ".concat(FileIO.sanitizeURIForCommand(file.getAbsolutePath())),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK));
            popupMenu.add(menuItem);
        }

        popupMenu.show(invoker, 0, 0);
    }

    @Override public void receiveMessage(String message) {
        JScrollBar vertBar = scrollPane.getVerticalScrollBar();
        boolean scrollBarAtBottom = Math.abs(vertBar.getValue() + vertBar.getVisibleAmount() - vertBar.getMaximum()) < (int)(0.15 * vertBar.getMaximum());
        outputArea.append(message.concat("\n"));
        //Keep scroll bar at the bottom if it previously was.
        if (scrollBarAtBottom) vertBar.setValue(vertBar.getMaximum());
    }

}
