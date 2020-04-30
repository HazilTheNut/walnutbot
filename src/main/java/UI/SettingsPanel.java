package UI;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

public class SettingsPanel extends JPanel {

    private static final int VOLUME_SLIDER_SCALE_MAX = 100;
    private JSlider masterVolumeSlider;
    private JSlider soundboardVolumeSlider;
    private JSlider musicVolumeSlider;

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

        masterVolumeSlider = generateVolumeSlider(SettingsLoader.getSettingsValue("masterVolume", "null"));
        masterVolumeSlider.addChangeListener(e -> {
            assignAudioMasterVolumes(audioMaster);
            updateVolumeInfoLabel(volumeInfoLabel, audioMaster);
        });
        panel.add(new JLabel("Master Volume (%)"));
        panel.add(masterVolumeSlider);

        soundboardVolumeSlider = generateVolumeSlider(SettingsLoader.getSettingsValue("soundboardVolume", "null"));
        soundboardVolumeSlider.addChangeListener(e -> {
            assignAudioMasterVolumes(audioMaster);
            updateVolumeInfoLabel(volumeInfoLabel, audioMaster);
        });
        panel.add(new JLabel("Soundboard Volume (%)"));
        panel.add(soundboardVolumeSlider);

        musicVolumeSlider = generateVolumeSlider(SettingsLoader.getSettingsValue("jukeboxVolume", "null"));
        musicVolumeSlider.addChangeListener(e -> {
            assignAudioMasterVolumes(audioMaster);
            updateVolumeInfoLabel(volumeInfoLabel, audioMaster);
        });
        panel.add(new JLabel("Jukebox Volume (%)"));
        panel.add(musicVolumeSlider);

        panel.add(volumeInfoLabel);

        assignAudioMasterVolumes(audioMaster);
        updateVolumeInfoLabel(volumeInfoLabel, audioMaster);

        return panel;
    }

    private JSlider generateVolumeSlider(String initValue) {
        JSlider slider;
        try {
            slider = new JSlider(SwingConstants.HORIZONTAL, 0, VOLUME_SLIDER_SCALE_MAX, Integer.valueOf(initValue));
        } catch (NumberFormatException e) {
            slider = new JSlider(SwingConstants.HORIZONTAL, 0, VOLUME_SLIDER_SCALE_MAX, (int) (VOLUME_SLIDER_SCALE_MAX * AudioMaster.VOLUME_DEFAULT));
        }
        slider.setMajorTickSpacing(VOLUME_SLIDER_SCALE_MAX / 5);
        slider.setMinorTickSpacing(VOLUME_SLIDER_SCALE_MAX / 20);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        return slider;
    }

    private void updateVolumeInfoLabel(JLabel infoLabel, AudioMaster audioMaster){
        infoLabel.setText(String.format("RAW VOLUMES - Soundboard: %1$d Jukebox: %2$d", audioMaster.getSoundboardPlayer().getVolume(), audioMaster.getJukeboxPlayer().getVolume()));
        infoLabel.repaint();
        SettingsLoader.modifySettingsValue("masterVolume", String.valueOf(masterVolumeSlider.getValue()));
        SettingsLoader.modifySettingsValue("soundboardVolume", String.valueOf(soundboardVolumeSlider.getValue()));
        SettingsLoader.modifySettingsValue("jukeboxVolume", String.valueOf(musicVolumeSlider.getValue()));
        SettingsLoader.writeSettingsFile();
    }

    private void assignAudioMasterVolumes(AudioMaster audioMaster){
        audioMaster.setMasterVolume(masterVolumeSlider.getValue() / (double)VOLUME_SLIDER_SCALE_MAX);
        audioMaster.setSoundboardVolume(soundboardVolumeSlider.getValue() / (double)VOLUME_SLIDER_SCALE_MAX);
        audioMaster.setJukeboxVolume(musicVolumeSlider.getValue() / (double)VOLUME_SLIDER_SCALE_MAX);
    }
}
