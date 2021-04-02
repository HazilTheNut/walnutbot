package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Audio.AudioKeyPlaylistScraper;
import Audio.AudioMaster;
import Commands.Command;
import Commands.CommandInterpreter;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.swing.*;
import java.awt.*;

public class ImportMusicFrame extends JFrame {

    public ImportMusicFrame(String baseCommand, CommandInterpreter commandInterpreter){

        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        setTitle("Import Music");
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

        JButton confirmButton = new JButton("Import");
        confirmButton.addActionListener(e -> {
            commandInterpreter.evaluateCommand(String.format("%1$s %2$s", baseCommand, urlField.getText()),
                Transcriber.getGenericCommandFeedBackHandler(),
                Command.INTERNAL_MASK);
            dispose();
        });
        buttonPanel.add(confirmButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(buttonPanel);

        setVisible(true);
    }
}
