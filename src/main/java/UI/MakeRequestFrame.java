package UI;

import Commands.Command;
import Commands.CommandInterpreter;
import Utils.FileIO;
import Utils.Transcriber;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;

public class MakeRequestFrame extends JFrame implements WindowStateListener {

    private String previousFilePath;
    private JCheckBox keepWindowOpenCheckBox;

    public MakeRequestFrame(String baseCommand, String windowTitle, CommandInterpreter commandInterpreter, JFrame uiFrame){
        this(baseCommand, windowTitle, commandInterpreter, uiFrame, true);
    }

    public MakeRequestFrame(String baseCommand, String windowTitle, CommandInterpreter commandInterpreter, JFrame uiFrame, boolean enableFileDialog){
        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        setTitle(windowTitle);
        setSize(new Dimension(550, 100));

        int FIELD_MARGIN = 10;

        JPanel urlPanel = new JPanel();
        urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.LINE_AXIS));

        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));
        urlPanel.add(new JLabel("URL: "), BorderLayout.LINE_START);
        JTextField urlField = new JTextField("");
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));

        add(urlPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        if (enableFileDialog) {
            JButton openFileButton =
                ButtonMaker.createIconButton("icons/open.png", "Open File...", 4);
            openFileButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
                fileChooser.setFileFilter(new FileNameExtensionFilter("Walnutbot Playlist", "playlist"));
                fileChooser.setMultiSelectionEnabled(true);
                if (previousFilePath != null)
                    fileChooser.setSelectedFile(new File(previousFilePath));
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    if (fileChooser.getSelectedFiles().length == 1) {
                        previousFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                        urlField.setText(previousFilePath);
                    } else if (fileChooser.getSelectedFile().length() > 1){
                        StringBuilder builder = new StringBuilder();
                        for (File file : fileChooser.getSelectedFiles()) {
                            builder.append('\"').append(file.getAbsolutePath()).append("\" ");
                            previousFilePath = file.getAbsolutePath();
                        }
                        urlField.setText(builder.toString());
                    }
                }
            });
            buttonPanel.add(openFileButton);
        }

        JButton confirmButton = new JButton("Request");
        confirmButton.addActionListener(e -> {
            processUrlFieldForRequests(baseCommand, commandInterpreter, urlField.getText());
            urlField.setText("");
        });
        buttonPanel.add(confirmButton);

        JButton cancelButton = new JButton("Exit");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        keepWindowOpenCheckBox = new JCheckBox("Keep this window open");
        buttonPanel.add(keepWindowOpenCheckBox);

        add(buttonPanel);

        setVisible(true);
        uiFrame.addWindowStateListener(this);
    }

    private void processUrlFieldForRequests(String baseCommand, CommandInterpreter commandInterpreter, String url){
        if (url.indexOf('\"') < 0) { // Only one element
            makeRequest(baseCommand, commandInterpreter, FileIO.sanitizeURIForCommand(url));
        } else {
            int posQuotOne = 0;
            int posQuotTwo = 0;
            while (posQuotOne >= 0){
                posQuotOne = url.indexOf('\"', posQuotOne);
                if (posQuotOne >= 0) { // If successfully found a new '"' character
                    posQuotTwo = url.indexOf('\"', posQuotOne + 1);
                    if (posQuotTwo > 0) { // If successfully found a second '"' character
                        makeRequest(baseCommand, commandInterpreter, FileIO.sanitizeURIForCommand(url.substring(posQuotOne+1, posQuotTwo)));
                    }
                }
                if (posQuotTwo >= 0 && posQuotOne >= 0) posQuotOne = posQuotTwo + 1;
            }
        }
    }

    private void makeRequest(String baseCommand, CommandInterpreter commandInterpreter, String url){
        commandInterpreter.evaluateCommand(baseCommand.concat(url),
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        if (!keepWindowOpenCheckBox.isSelected())
            dispose();
    }

    /**
     * Invoked when window state is changed.
     *
     * @param e the event to be processed
     */
    @Override public void windowStateChanged(WindowEvent e) {
        Transcriber.printTimestamped("Window State Change: %s)", e.paramString());
        if ((e.getNewState() & 1) == 0) {
            setState(Frame.NORMAL);
            setVisible(true);
            Transcriber.printTimestamped("Manual song request window setting itself to visible.");
        }
    }
}
