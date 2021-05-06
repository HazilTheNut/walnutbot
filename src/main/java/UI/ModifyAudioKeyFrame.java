package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Commands.Command;
import Commands.CommandInterpreter;
import Utils.BotInfo;
import Utils.FileIO;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class ModifyAudioKeyFrame extends JFrame {

    public enum ModificationType {
        ADD, MODIFY, REMOVE
    }

    public enum TargetList {
        SOUNDBOARD, JUKEBOX_DEFAULT, JUKEBOX_QUEUE
    }

    ModifyAudioKeyFrame(AudioMaster audioMaster, @Nonnull AudioKey base, int pos, CommandInterpreter commandInterpreter,
        ModificationType modificationType, TargetList targetList){

        Container c = getContentPane();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        int FIELD_MARGIN = 10;

        //Row for the AudioKey's name
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.LINE_AXIS));
        namePanel.add(Box.createHorizontalStrut(FIELD_MARGIN));
        namePanel.add(new JLabel("Name: "), BorderLayout.LINE_START);
        JTextField nameField = new JTextField();
        if (base != null) nameField.setText(base.getName());
        namePanel.add(nameField, BorderLayout.CENTER);

        //Row for the AudioKey's URL
        JPanel urlPanel = new JPanel();
        urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.LINE_AXIS));
        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));
        urlPanel.add(new JLabel("URL: "), BorderLayout.LINE_START);
        JTextField urlField = new JTextField();
        if (base != null) urlField.setText(base.getUrl());
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));

        //Add Fetch Info Button
        JButton fetchInfoButton = ButtonMaker.createIconButton("icons/extract.png", "Fetch Song Name from URL", 5);
        fetchInfoButton.addActionListener(e -> audioMaster.getPlayerManager().loadItem(urlField.getText(), new InfoFetchLoadResultHandler(nameField)));
        namePanel.add(fetchInfoButton);
        namePanel.add(Box.createHorizontalStrut(FIELD_MARGIN));

        //Row for buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 3));

        //Open Sound Button
        JButton openButton = ButtonMaker.createIconButton("icons/open.png", "Search", 5);
        openButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(FileIO.getRootFilePath());
            fileChooser.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
            fileChooser.setFileFilter(new FileNameExtensionFilter(BotInfo.getFileChooserTitle(), BotInfo.ACCEPTED_AUDIO_FORMATS));
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filepath = fileChooser.getSelectedFile().getAbsolutePath();
                urlField.setText(filepath);
                String name = filepath.substring(Math.min(filepath.length(), filepath.lastIndexOf(
                    File.separatorChar) + 1));
                name = name.substring(0, name.lastIndexOf('.'));
                nameField.setText(name);
            }
        });
        buttonsPanel.add(openButton);

        //Add Sound Button
        if (modificationType == ModificationType.ADD) {
            JButton addSoundButton = new JButton("Add to Playlist");
            addSoundButton.addActionListener(e -> {
                AudioKey key = new AudioKey(nameField.getText(), urlField.getText());
                if (key.isValid()) {
                    commandInterpreter.evaluateCommand(buildCommand(nameField, urlField, ModificationType.ADD, targetList, -1, base.getName()),
                        Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
                }
                dispose();
            });
            buttonsPanel.add(addSoundButton);
        }

        //Remove Button
        if (modificationType == ModificationType.MODIFY) {
                JButton removeButton = new JButton("Remove");
                removeButton.addActionListener(e -> {
                    commandInterpreter.evaluateCommand(buildCommand(nameField, urlField, ModificationType.REMOVE, targetList, pos, base.getName()),
                        Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
                    dispose();
                });
                buttonsPanel.add(removeButton);

            //Apply (Changes) Button
            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(e -> {
                commandInterpreter.evaluateCommand(buildCommand(nameField, urlField, ModificationType.MODIFY, targetList, pos, base.getName()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
                dispose();
            });
            buttonsPanel.add(applyButton);
        }

        //Cancel Button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonsPanel.add(cancelButton);

        panel.add(namePanel);
        panel.add(urlPanel);
        panel.add(buttonsPanel);

        c.add(panel);
        c.validate();

        setTitle(String.format("Sound Editor (pos: %1$d)", pos));
        setSize(new Dimension(430, 110));
        setVisible(true);
    }

    private String buildCommand(JTextField nameField, JTextField urlField, ModificationType modificationType, TargetList targetList, int pos, String originalName){
        String name = FileIO.sanitizeURIForCommand(nameField.getText());
        String url  = FileIO.sanitizeURIForCommand(urlField.getText());
        switch (targetList){
            case SOUNDBOARD: {
                switch (modificationType){
                    case ADD: return String.format("sb add %1$s %2$s", name, url);
                    case MODIFY: return String.format("sb modify %1$s -name %2$s -url %3$s", originalName, name, url);
                    case REMOVE: return String.format("sb remove %1$d", pos);
                }
                return "ERROR";
            }
            case JUKEBOX_QUEUE: {
                switch (modificationType){
                    case ADD: return String.format("jb %1$s", url);
                    case MODIFY: return "ERROR";
                    case REMOVE: return "ERROR";
                }
                return "ERROR";
            }
            case JUKEBOX_DEFAULT:
                switch (modificationType){
                    case ADD: return String.format("jb dfl add %1$s", url);
                    case MODIFY: return String.format("jb dfl modify %1$d -name %2$s -url %3$s", pos, name, url);
                    case REMOVE: return String.format("jb dfl remove %1$d", pos);
                }
                return "ERROR";
            default:
                return "ERROR ";
        }
    }

    private class InfoFetchLoadResultHandler implements AudioLoadResultHandler {

        JTextField nameRecipient;

        private InfoFetchLoadResultHandler(JTextField nameRecipient) {
            this.nameRecipient = nameRecipient;
        }

        /**
         * Called when the requested item is a track and it was successfully loaded.
         *
         * @param track The loaded track
         */
        @Override public void trackLoaded(AudioTrack track) {
            nameRecipient.setText(track.getInfo().title);
        }

        /**
         * Called when the requested item is a playlist and it was successfully loaded.
         *
         * @param playlist The loaded playlist
         */
        @Override public void playlistLoaded(AudioPlaylist playlist) {

        }

        /**
         * Called when there were no items found by the specified identifier.
         */
        @Override public void noMatches() {

        }

        /**
         * Called when loading an item failed with an exception.
         *
         * @param exception The exception that was thrown
         */
        @Override public void loadFailed(FriendlyException exception) {

        }
    }

    public interface SaveAfterChangesAction {
        void save();
    }
}
