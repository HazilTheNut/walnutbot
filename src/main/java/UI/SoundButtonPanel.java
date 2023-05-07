package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Commands.Command;
import Commands.CommandInterpreter;
import Main.WalnutbotEnvironment;
import Utils.Transcriber;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class SoundButtonPanel extends JPanel implements AudioKeyUIWrapper {
    private AudioKey audioKey;
    private JButton playButton;
    private UUID uuid;

    SoundButtonPanel(AudioKey audioKey, WalnutbotEnvironment environment){
        uuid = UUID.randomUUID();
        this.audioKey = audioKey;
        setLayout(new BorderLayout());

        playButton = new JButton(this.audioKey.getName());
        playButton.setMargin(new Insets(4, 8, 4, 8));
        playButton.addActionListener(e -> environment.getCommandInterpreter().evaluateCommand(
            String.format("sb \"%1$s\"", audioKey.getName()),
            Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI),
            Command.INTERNAL_MASK
        ));
        add(playButton, BorderLayout.CENTER);

        JButton menuButton = ButtonMaker.createIconButton("icons/menu.png", "Config", 2);
        menuButton.addActionListener(e -> {
            int listPos = findMyPosition();
            environment.getAudioStateMachine().getSoundboardList().accessAudioKeyPlaylist(playlist -> new ModifyAudioKeyFrame(environment, playlist.getKey(listPos), listPos,
                    ModifyAudioKeyFrame.ModificationType.MODIFY, ModifyAudioKeyFrame.TargetList.SOUNDBOARD));
        });
        add(menuButton, BorderLayout.LINE_END);
    }

    public AudioKey getAudioKey() {
        return audioKey;
    }

    private int findMyPosition(){
        for (int i = 0; i < getParent().getComponents().length; i++) {
            if (getParent().getComponents()[i] instanceof SoundButtonPanel) {
                SoundButtonPanel trackListing = (SoundButtonPanel) getParent().getComponents()[i];
                if (trackListing.getUUID().equals(uuid))
                    return i;
            }
        }
        return -1;
    }

    public UUID getUUID() {
        return uuid;
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
