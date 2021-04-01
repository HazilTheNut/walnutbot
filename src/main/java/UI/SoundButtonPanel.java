package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Commands.Command;
import Commands.CommandInterpreter;
import Utils.ButtonMaker;
import Utils.Transcriber;

import javax.swing.*;
import java.awt.*;

public class SoundButtonPanel extends JPanel implements AudioKeyUIWrapper {
    private AudioKey audioKey;
    private JButton playButton;

    SoundButtonPanel(AudioKey audioKey, int listPos, AudioMaster audioMaster, CommandInterpreter commandInterpreter, UIFrame uiFrame){
        this.audioKey = audioKey;
        setLayout(new BorderLayout());

        playButton = new JButton(this.audioKey.getName());
        playButton.addActionListener(e -> commandInterpreter.evaluateCommand(
            String.format("sb \"%1$s\"", audioKey.getName()),
            Transcriber.getGenericCommandFeedBackHandler(),
            Command.INTERNAL_MASK
        ));
        add(playButton, BorderLayout.CENTER);

        JButton menuButton = ButtonMaker.createIconButton("icons/menu.png", "Config", 2);
        menuButton.addActionListener(e -> new ModifyAudioKeyFrame(audioMaster, audioMaster.getSoundboardList().getKey(listPos), listPos,
                commandInterpreter, ModifyAudioKeyFrame.ModificationType.MODIFY, ModifyAudioKeyFrame.TargetList.SOUNDBOARD, uiFrame));
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
