package UI;

import Utils.SettingsLoader;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import javax.swing.*;
import java.awt.*;

public class KeybindsPanel extends JPanel {

    public KeybindsPanel(){

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        InputCollector inputCollector = new InputCollector();

        if (Boolean.valueOf(SettingsLoader.getBotConfigValue("enable_midi_input"))){
            JPanel midiPanel = new JPanel();
            midiPanel.setLayout(new BoxLayout(midiPanel, BoxLayout.LINE_AXIS));

            midiPanel.add(new JLabel("Midi Input Device: "));

            JComboBox<MidiDevice.Info> deviceComboBox = new JComboBox<>();

            for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
                deviceComboBox.addItem(info);
            }

            midiPanel.add(deviceComboBox);

            JButton bindButton = new JButton("Bind");
            JLabel boundMidiDeviceLabel = new JLabel("Receiving MIDI input from: null");

            bindButton.addActionListener(e -> {
                int index = deviceComboBox.getSelectedIndex();
                if (index > -1) {
                    try {
                        MidiDevice device = MidiSystem.getMidiDevice(deviceComboBox.getItemAt(index));
                        device.open();
                        for (Transmitter transmitter : device.getTransmitters())
                            transmitter.setReceiver(inputCollector);
                        boundMidiDeviceLabel.setText(String.format("Receiving MIDI input from: %s", device.toString()));
                    } catch (MidiUnavailableException e1) {
                        e1.printStackTrace();
                    }
                }
            });

            midiPanel.add(bindButton);
            midiPanel.setAlignmentX(LEFT_ALIGNMENT);

            add(midiPanel);

            boundMidiDeviceLabel.setAlignmentX(LEFT_ALIGNMENT);
            add(boundMidiDeviceLabel);
        }

    }

}
