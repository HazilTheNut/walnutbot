package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;
import Audio.AudioMaster;
import Utils.ButtonMaker;
import Utils.FileIO;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ModifyAudioKeyFrame extends JFrame {

    ModifyAudioKeyFrame(AudioMaster audioMaster, PlaylistUIWrapper playlistUIWrapper, AudioKeyUIWrapper audioKeyUIWrapper, AudioKeyPlaylist playlist, SaveAfterChangesAction afterChangesAction){

        Container c = getContentPane();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        int FIELD_MARGIN = 10;
        int keyIDtoEdit = playlistUIWrapper.getAudioKeyID(audioKeyUIWrapper);

        //Row for the AudioKey's name
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.LINE_AXIS));
        namePanel.add(Box.createHorizontalStrut(FIELD_MARGIN));
        namePanel.add(new JLabel("Name: "), BorderLayout.LINE_START);
        JTextField nameField = new JTextField();
        if (audioKeyUIWrapper != null) nameField.setText(audioKeyUIWrapper.getData().getName());
        namePanel.add(nameField, BorderLayout.CENTER);

        //Row for the AudioKey's URL
        JPanel urlPanel = new JPanel();
        urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.LINE_AXIS));
        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));
        urlPanel.add(new JLabel("URL: "), BorderLayout.LINE_START);
        JTextField urlField = new JTextField();
        if (audioKeyUIWrapper != null) urlField.setText(audioKeyUIWrapper.getData().getUrl());
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
        if (audioKeyUIWrapper == null) { //If the audioKeyUIWrapper is null, we are creating a new one
            JButton addSoundButton = new JButton("Add to Playlist");
            addSoundButton.addActionListener(e -> {
                AudioKey key = new AudioKey(nameField.getText(), urlField.getText());
                if (key.isValid()) {
                    playlist.addAudioKey(key);
                    //playlist.printPlaylist();
                    playlistUIWrapper.addAudioKey(key);
                    afterChangesAction.save();
                }
                dispose();
            });
            buttonsPanel.add(addSoundButton);
        }

        //Remove Button
        if (audioKeyUIWrapper != null){ //If audioKeyUIWrapper is nonnull, we are modifying one
            JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(e -> {
                playlist.removeAudioKey(audioKeyUIWrapper.getData());
                //playlist.printPlaylist();
                playlistUIWrapper.removeAudioKey(keyIDtoEdit);
                afterChangesAction.save();
                dispose();
            });
            buttonsPanel.add(removeButton);
        }

        //Apply (Changes) Button
        if (audioKeyUIWrapper != null){
            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(e -> {
                if (nameField.getText().length() > 0 && urlField.getText().length() > 0) {
                    playlistUIWrapper.modifyAudioKey(keyIDtoEdit, new AudioKey(nameField.getText(), urlField.getText()));
                    //playlist.printPlaylist();
                    afterChangesAction.save();
                }
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

        setTitle("Sound Editor");
        setSize(new Dimension(430, 110));
        setVisible(true);
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
