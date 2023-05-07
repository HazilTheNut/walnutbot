package UI;

import Audio.AudioMaster;
import Audio.VolumeChangeListener;
import Commands.Command;
import Commands.CommandInterpreter;
import CommuncationPlatform.ICommunicationPlatformManager;
import Utils.SettingsLoader;
import Utils.Transcriber;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Comparator;

public class SettingsPanel extends JPanel implements VolumeChangeListener {

    private static final int VOLUME_SLIDER_SCALE_MAX = 100;
    private JSlider mainVolumeSlider;
    private JSlider soundboardVolumeSlider;
    private JSlider jukeboxVolumeSlider;

    private JLabel volumeInfoLabel;

    public SettingsPanel(ICommunicationPlatformManager botManager, AudioMaster audioMaster, CommandInterpreter commandInterpreter, boolean botInitSuccessful) {

        BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        setLayout(layout);
        if (botInitSuccessful) {
            audioMaster.setVolumeChangeListener(this);
            add(new ConnectionPanel(botManager, commandInterpreter), BorderLayout.PAGE_START);
            add(createMainPanel(audioMaster, commandInterpreter), BorderLayout.CENTER);
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

    private JPanel createMainPanel(AudioMaster audioMaster, CommandInterpreter commandInterpreter) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(new TitledBorder("Volume"));

        volumeInfoLabel = new JLabel("");

        mainVolumeSlider = generateVolumeSlider(audioMaster.getMainVolume());
        mainVolumeSlider.addChangeListener(e -> {
            if (mainVolumeSlider.getValue() != audioMaster.getMainVolume())
                commandInterpreter.evaluateCommand(String.format("vol main %1$d", mainVolumeSlider.getValue()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        });
        panel.add(new JLabel("Main Volume (%)"));
        panel.add(mainVolumeSlider);

        soundboardVolumeSlider = generateVolumeSlider(audioMaster.getSoundboardVolume());
        soundboardVolumeSlider.addChangeListener(e -> {
            if (soundboardVolumeSlider.getValue() != audioMaster.getSoundboardVolume())
                commandInterpreter.evaluateCommand(String.format("vol sb %1$d", soundboardVolumeSlider.getValue()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        });
        panel.add(new JLabel("Soundboard Volume (%)"));
        panel.add(soundboardVolumeSlider);

        jukeboxVolumeSlider = generateVolumeSlider(audioMaster.getJukeboxVolume());
        jukeboxVolumeSlider.addChangeListener(e -> {
            if (jukeboxVolumeSlider.getValue() != audioMaster.getJukeboxVolume())
                commandInterpreter.evaluateCommand(String.format("vol jb %1$d", jukeboxVolumeSlider.getValue()),
                    Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_UI), Command.INTERNAL_MASK);
        });
        panel.add(new JLabel("Jukebox Volume (%)"));
        panel.add(jukeboxVolumeSlider);

        panel.add(volumeInfoLabel);

        assignAudioMasterVolumes(audioMaster);
        updateVolumeInfoLabel(audioMaster);

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

        java.util.List<Command> commandList = audioMaster.getCommandInterpreter().getExpandedCommandList();
        commandList.sort(Comparator.comparing(Command::getCommandTreeStr));
        for (Command command : commandList){
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

    private JSlider generateVolumeSlider(int initValue) {
        JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0, VOLUME_SLIDER_SCALE_MAX, initValue);
        slider.setMajorTickSpacing(VOLUME_SLIDER_SCALE_MAX / 5);
        slider.setMinorTickSpacing(VOLUME_SLIDER_SCALE_MAX / 20);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        return slider;
    }

    private void updateVolumeInfoLabel(AudioMaster audioMaster){
        volumeInfoLabel.setText(String.format("RAW VOLUMES - Soundboard: %1$d Jukebox: %2$d", audioMaster.getSoundboardPlayer().getVolume(), audioMaster.getJukeboxPlayer().getVolume()));
        volumeInfoLabel.repaint();
    }

    private void assignAudioMasterVolumes(AudioMaster audioMaster){
        audioMaster.setMainVolume(mainVolumeSlider.getValue());
        audioMaster.setSoundboardVolume(soundboardVolumeSlider.getValue());
        audioMaster.setJukeboxVolume(jukeboxVolumeSlider.getValue());
    }

    /**
     * Called when the Main Volume changes.
     *
     * @param vol A value ranging from 0 to 100 describing the Main Volume
     * @param audioMaster The AudioMaster which called this method
     */
    @Override public void onMainVolumeChange(int vol, AudioMaster audioMaster) {
        mainVolumeSlider.setValue(vol);
        updateVolumeInfoLabel(audioMaster);
    }

    /**
     * Called when the Soundboard Volume changes.
     *
     * @param vol A value ranging from 0 to 100 describing the Soundboard Volume
     * @param audioMaster The AudioMaster which called this method
     */
    @Override public void onSoundboardVolumeChange(int vol, AudioMaster audioMaster) {
        soundboardVolumeSlider.setValue(vol);
        updateVolumeInfoLabel(audioMaster);
    }

    /**
     * Called when the Jukebox Volume changes.
     *
     * @param vol A value ranging from 0 to 100 describing the Jukebox Volume
     * @param audioMaster The AudioMaster which called this method
     */
    @Override public void onJukeboxVolumeChange(int vol, AudioMaster audioMaster) {
        jukeboxVolumeSlider.setValue(vol);
        updateVolumeInfoLabel(audioMaster);
    }
}
