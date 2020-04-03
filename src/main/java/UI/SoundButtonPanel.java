package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.ButtonMaker;

import javax.swing.*;
import java.awt.*;

public class SoundButtonPanel extends JPanel implements AudioKeyUIWrapper {
    private AudioKey audioKey;
    private JButton playButton;

    SoundButtonPanel(AudioKey key, AudioMaster audioMaster, PlaylistUIWrapper owningPanel){
        audioKey = key;
        setLayout(new BorderLayout());

        playButton = new JButton(key.getName());
        playButton.addActionListener(e -> audioMaster.playSoundboardSound(key.getUrl()));
        add(playButton, BorderLayout.CENTER);

        JButton menuButton = ButtonMaker.createIconButton("icons/menu.png", "Config", 2);
        menuButton.addActionListener(e -> new ModifyTrackFrame(audioMaster, owningPanel, this));
        add(menuButton, BorderLayout.LINE_END);
    }

    public AudioKey getAudioKey() {
        return audioKey;
    }

    public void setAudioKey(AudioKey audioKey) {
        this.audioKey = audioKey;
    }

    public JButton getPlayButton() {
        return playButton;
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof SoundButtonPanel) {
            SoundButtonPanel soundButtonPanel = (SoundButtonPanel) obj;
            return audioKey.equals(soundButtonPanel.getAudioKey());
        }
        return false;
    }

    @Override public void setData(AudioKey data) {
        audioKey.setUrl(data.getUrl());
        audioKey.setName(data.getName());
        playButton.setText(data.getName());
        revalidate();
        repaint();
    }

    @Override public AudioKey getData() {
        return audioKey;
    }
}
