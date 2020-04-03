package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.ButtonMaker;
import Utils.FileIO;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ModifyTrackFrame extends JFrame {

    ModifyTrackFrame(AudioMaster audioMaster, PlaylistUIWrapper playlistUIWrapper, AudioKeyUIWrapper audioKeyUIWrapper){

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
        namePanel.add(Box.createHorizontalStrut(FIELD_MARGIN));

        //Row for the AudioKey's URL
        JPanel urlPanel = new JPanel();
        urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.LINE_AXIS));
        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));
        urlPanel.add(new JLabel("URL: "), BorderLayout.LINE_START);
        JTextField urlField = new JTextField();
        if (audioKeyUIWrapper != null) urlField.setText(audioKeyUIWrapper.getData().getUrl());
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));

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
                    audioMaster.getSoundboardList().addAudioKey(key);
                    //audioMaster.getSoundboardList().printPlaylist();
                    playlistUIWrapper.addAudioKey(key);
                    audioMaster.saveSoundboard();
                }
                dispose();
            });
            buttonsPanel.add(addSoundButton);
        }

        //Remove Button
        if (audioKeyUIWrapper != null){ //If audioKeyUIWrapper is nonnull, we are modifying one
            JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(e -> {
                audioMaster.getSoundboardList().removeAudioKey(audioKeyUIWrapper.getData());
                //audioMaster.getSoundboardList().printPlaylist();
                playlistUIWrapper.removeAudioKey(keyIDtoEdit);
                audioMaster.saveSoundboard();
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
                    //audioMaster.getSoundboardList().printPlaylist();
                    audioMaster.saveSoundboard();
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

        setTitle("Add New Sound");
        setSize(new Dimension(430, 110));
        setVisible(true);
    }

}
