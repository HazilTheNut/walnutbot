package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.ButtonMaker;
import Utils.FileIO;
import Utils.Transcriber;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

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
        JTextField urlField = new JTextField("");
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
        confirmButton.addActionListener(e -> {
            makeRequest(audioMaster, urlField.getText());
            urlField.setText("");
        });
        buttonPanel.add(confirmButton);

        JButton cancelButton = new JButton("Exit");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(buttonPanel);

        setVisible(true);
    }

    private void makeRequest(AudioMaster audioMaster, String url){
        ArrayList<AudioKey> loadedFromFile = AudioKeyPlaylistLoader.grabKeysFromPlaylist(url);
        if (loadedFromFile.size() > 0){
            for (AudioKey audioKey : loadedFromFile)
                audioMaster.queueJukeboxSong(audioKey, () -> {}, () -> Transcriber.print("WARNING! Invalid Audio Key: %1$s", audioKey));
        }
        else audioMaster.queueJukeboxSong(new AudioKey("Requested", url), () -> {}, () -> Transcriber.print("WARNING! Invalid URL: %1$s", url));
    }
}
