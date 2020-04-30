package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.ButtonMaker;
import Utils.FileIO;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MakeRequestFrame extends JFrame {

    private String previousFilePath;

    public MakeRequestFrame(AudioMaster audioMaster){
        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        setTitle("Request Songs");
        setSize(new Dimension(550, 100));

        int FIELD_MARGIN = 10;

        JPanel urlPanel = new JPanel();
        urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.LINE_AXIS));

        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));
        urlPanel.add(new JLabel("URL: "), BorderLayout.LINE_START);
        JTextField urlField = new JTextField("Enter URL Here...");
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));

        add(urlPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        JButton openFileButton = ButtonMaker.createIconButton("icons/open.png", "Open File...", 4);
        openFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
            if (previousFilePath != null)
                fileChooser.setSelectedFile(new File(previousFilePath));
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                previousFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                urlField.setText(previousFilePath);
            }
        });
        buttonPanel.add(openFileButton);

        JButton confirmButton = new JButton("Request");
        confirmButton.addActionListener(e -> audioMaster.queueJukeboxSong(new AudioKey("Requested", urlField.getText()), () -> {}, () -> {}));
        buttonPanel.add(confirmButton);

        JButton cancelButton = new JButton("Exit");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(buttonPanel);

        setVisible(true);
    }
}
