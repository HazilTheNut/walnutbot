package UI;

import Audio.AudioMaster;
import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    private static final int VOLUME_SLIDER_SCALE_MAX = 100;

    public SettingsPanel(JDA jda, AudioMaster audioMaster) {

        setLayout(new BorderLayout());
        if (jda != null) {
            add(new ConnectionPanel(jda, audioMaster), BorderLayout.PAGE_START);
            add(createMainPanel(audioMaster), BorderLayout.CENTER);
        } else
            add(new JLabel(
                    "WARNING! Looks like the bot failed to load! (Check output.txt for more info)"),
                BorderLayout.CENTER);

        validate();

    }

    private JPanel createMainPanel(AudioMaster audioMaster) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        JLabel volumeInfoLabel = new JLabel("");

        JSlider masterVolumeSlider = generateVolumeSlider();
        masterVolumeSlider.addChangeListener(e -> {
            audioMaster.setMasterVolume(masterVolumeSlider.getValue() / (double)VOLUME_SLIDER_SCALE_MAX);
            updateVolumeInfoLabel(volumeInfoLabel, audioMaster);
        });
        panel.add(new JLabel("Master Volume (%)"));
        panel.add(masterVolumeSlider);

        JSlider soundboardVolumeSlider = generateVolumeSlider();
        soundboardVolumeSlider.addChangeListener(e -> {
            audioMaster.setSoundboardVolume(soundboardVolumeSlider.getValue() / (double)VOLUME_SLIDER_SCALE_MAX);
            updateVolumeInfoLabel(volumeInfoLabel, audioMaster);
        });
        panel.add(new JLabel("Soundboard Volume (%)"));
        panel.add(soundboardVolumeSlider);

        JSlider musicVolumeSlider = generateVolumeSlider();
        musicVolumeSlider.addChangeListener(e -> {
            audioMaster.setMasterVolume(musicVolumeSlider.getValue() / (double)VOLUME_SLIDER_SCALE_MAX);
            updateVolumeInfoLabel(volumeInfoLabel, audioMaster);
        });
        panel.add(new JLabel("Music Volume (%)"));
        panel.add(musicVolumeSlider);

        panel.add(volumeInfoLabel);

        return panel;
    }

    private JSlider generateVolumeSlider() {
        JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0, VOLUME_SLIDER_SCALE_MAX, (int) (VOLUME_SLIDER_SCALE_MAX * AudioMaster.VOLUME_DEFAULT));
        slider.setMajorTickSpacing(VOLUME_SLIDER_SCALE_MAX / 5);
        slider.setMinorTickSpacing(VOLUME_SLIDER_SCALE_MAX / 20);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        return slider;
    }

    private void updateVolumeInfoLabel(JLabel infoLabel, AudioMaster audioMaster){
        infoLabel.setText(String.format("RAW VOLUMES - Soundboard: %1$d", audioMaster.getSoundboardPlayer().getVolume()));
        infoLabel.repaint();
    }
}
