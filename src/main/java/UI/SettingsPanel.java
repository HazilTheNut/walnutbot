package UI;

import Audio.AudioMaster;
import Commands.Command;
import Commands.CommandInterpreter;
import Utils.BotManager;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;
import okhttp3.internal.http2.Settings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class SettingsPanel extends JPanel {

    private static final int VOLUME_SLIDER_SCALE_MAX = 100;
    private JSlider masterVolumeSlider;
    private JSlider soundboardVolumeSlider;
    private JSlider musicVolumeSlider;

    public SettingsPanel(BotManager botManager, AudioMaster audioMaster, boolean botInitSuccessful) {

        BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        setLayout(layout);
        if (botInitSuccessful) {
            add(new ConnectionPanel(botManager, audioMaster), BorderLayout.PAGE_START);
            add(createMainPanel(audioMaster), BorderLayout.CENTER);
            add(createPermissionsPanel(audioMaster), BorderLayout.PAGE_END);
        } else
            add(new JLabel(
                    "WARNING! Looks like the bot failed to load! (Check output.txt for more info)"),
                BorderLayout.CENTER);

        for (Component c : getComponents())
            if (c instanceof JComponent)
                ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);

        validate();
    }

    private JPanel createMainPanel(AudioMaster audioMaster) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(new TitledBorder("Volume"));

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

    private JPanel createPermissionsPanel(AudioMaster audioMaster){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Permissions"));

        JCheckBox allowLocalAccessBox = new JCheckBox("Allow Non-admin users to access local files (for Soundboard or Jukebox requests)");
        allowLocalAccessBox.addChangeListener(e -> {
            SettingsLoader.modifySettingsValue("discordAllowLocalAccess", String.valueOf(allowLocalAccessBox.isSelected()));
            SettingsLoader.writeSettingsFile();
        });
        allowLocalAccessBox.setSelected(Boolean.valueOf(SettingsLoader.getSettingsValue("discordAllowLocalAccess", "false")));
        allowLocalAccessBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(allowLocalAccessBox);

        panel.add(new JLabel("Allowed Commands for Non-Admin Users:"));

        JPanel commandsAllowPanel = new JPanel();
        commandsAllowPanel.setLayout(new BoxLayout(commandsAllowPanel, BoxLayout.PAGE_AXIS));
        commandsAllowPanel.setAlignmentX(0);
        JScrollPane allowCommandScrollPane = new JScrollPane(commandsAllowPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        int unitIncrement = 0;

        for (Command command : audioMaster.getCommandInterpreter().getExpandedCommandList()){
            JCheckBox commandAllowBox = new JCheckBox(String.format("%1$s : %2$s", command.getCommandTreeStr(), command.getHelpDescription()));
            commandAllowBox.addChangeListener(e -> {
                SettingsLoader.modifySettingsValue(command.getPermissionName(), (commandAllowBox.isSelected()) ? "3" : "2");
                SettingsLoader.writeSettingsFile();
            });
            String setting = SettingsLoader.getSettingsValue(command.getPermissionName(), "3");
            commandAllowBox.setSelected(setting.equals("3"));
            commandsAllowPanel.add(commandAllowBox);
            unitIncrement = Math.max(unitIncrement, (int)commandAllowBox.getMinimumSize().getHeight());
        }

        allowCommandScrollPane.getVerticalScrollBar().setUnitIncrement(unitIncrement);
        allowCommandScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(allowCommandScrollPane);

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
