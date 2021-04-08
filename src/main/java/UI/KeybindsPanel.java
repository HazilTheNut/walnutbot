package UI;

import Commands.CommandInterpreter;
import Utils.FileIO;
import Utils.SettingsLoader;
import Utils.Transcriber;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.dispatcher.SwingDispatchService;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeybindsPanel extends JPanel implements InputCollectorListener{

    private JPanel keybindsListingPanel;
    private JLabel savingStateLabel;
    private JLabel mostRecentInputLabel;

    private JLabel boundMidiDeviceLabel;
    private String boundMidiDeviceString;

    public KeybindsPanel(CommandInterpreter commandInterpreter){

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        File keybindsFile = new File(FileIO.getRootFilePath().concat("keybinds.txt"));

        InputCollector inputCollector = new InputCollector(keybindsFile, commandInterpreter);
        inputCollector.setInputCollectorListener(this);

        if (Boolean.valueOf(SettingsLoader.getBotConfigValue("enable_midi_input"))){
            add(setupMIDI(inputCollector));
        }

        if (Boolean.valueOf(SettingsLoader.getBotConfigValue("enable_global_keybinds"))){
            try {
                // Set up global input collection
                GlobalScreen.registerNativeHook();
                GlobalScreen.addNativeKeyListener(inputCollector);
                GlobalScreen.setEventDispatcher(new SwingDispatchService());
                Transcriber.printRaw("Successful bind to global input.");

                // Ignore non-warning output from global input collector.
                Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
                logger.setLevel(Level.WARNING);
                logger.setUseParentHandlers(true);

            } catch (NativeHookException e) {
                Transcriber.printRaw("ERROR: Could not properly bind to global input!");
                e.printStackTrace();
            }
        }

        mostRecentInputLabel = new JLabel();
        onInputMessageSend("");
        mostRecentInputLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(mostRecentInputLabel);

        add(createKeybindsEditPanel(inputCollector));
    }

    private JPanel setupMIDI(InputCollector inputCollector){

        // Since things are so interdependent, all components are instantiated first
        JPanel midiPanel = new JPanel();

        JPanel chooseDevicePanel = new JPanel();
        JPanel boundDevicePanel = new JPanel();

        boundMidiDeviceLabel = new JLabel();
        JComboBox<MidiDevice.Info> deviceComboBox = new JComboBox<>();
        JButton bindButton = new JButton("Bind");

        JButton rememberDeviceButton = new JButton("Set as Default");

        // Main panel
        midiPanel.setLayout(new BoxLayout(midiPanel, BoxLayout.PAGE_AXIS));

        // "Choose Device" row of panel
        chooseDevicePanel.setLayout(new BoxLayout(chooseDevicePanel, BoxLayout.LINE_AXIS));

        chooseDevicePanel.add(new JLabel("MIDI Input Device: "));

        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            deviceComboBox.addItem(info);
        }

        chooseDevicePanel.add(deviceComboBox);

        bindButton.addActionListener(e -> {
            int index = deviceComboBox.getSelectedIndex();
            if (index > -1) {
                inputCollector.bindToTransmitter(deviceComboBox.getItemAt(index).toString());
                rememberDeviceButton.setEnabled(true);
            }
        });
        bindButton.setAlignmentX(LEFT_ALIGNMENT);

        chooseDevicePanel.add(bindButton);

        // "Bound Device" row of panel
        boundDevicePanel.setLayout(new BoxLayout(boundDevicePanel, BoxLayout.LINE_AXIS));

        String defaultDevice = SettingsLoader.getSettingsValue("midi_default_device", "");
        if (defaultDevice.length() > 1){
            inputCollector.bindToTransmitter(defaultDevice);
        } else
            onMidiDeviceBind("");
        boundDevicePanel.add(boundMidiDeviceLabel);

        rememberDeviceButton.addActionListener(e -> {
            SettingsLoader.modifySettingsValue("midi_default_device", boundMidiDeviceString);
            SettingsLoader.writeSettingsFile();
            rememberDeviceButton.setEnabled(false);
        });
        boundDevicePanel.add(rememberDeviceButton);

        chooseDevicePanel.setAlignmentX(LEFT_ALIGNMENT);
        boundDevicePanel.setAlignmentX(LEFT_ALIGNMENT);

        midiPanel.add(chooseDevicePanel);
        midiPanel.add(boundDevicePanel);

        midiPanel.setAlignmentX(LEFT_ALIGNMENT);
        midiPanel.setBorder(BorderFactory.createTitledBorder("MIDI Input"));

        return midiPanel;
    }

    private JPanel createKeybindsEditPanel(InputCollector inputCollector){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));


        JButton addButton = ButtonMaker.createIconButton("icons/input.png", "Add Input", 8);
        addButton.addActionListener(e -> {
            addButton.setEnabled(false);
            inputCollector.setTempInputReceiveOverride(inputMessage -> {
                if (!inputCollector.containsInputKey(inputMessage)) {
                    inputCollector.addInputMapping(inputMessage, "<command>");
                    reloadKeybindsPanel(inputCollector);
                }
                addButton.setEnabled(true);
            });
        });
        upperPanel.add(addButton);

        savingStateLabel = new JLabel();
        savingStateLabel.setOpaque(true);
        savingStateLabel.setHorizontalTextPosition(JLabel.RIGHT);
        savingStateLabel.setVerticalTextPosition(JLabel.CENTER);
        onSavingStateChange(true);
        upperPanel.add(savingStateLabel);

        panel.add(upperPanel, BorderLayout.PAGE_START);

        keybindsListingPanel = new JPanel();
        keybindsListingPanel.setLayout(new GridBagLayout());

        reloadKeybindsPanel(inputCollector);

        JScrollPane scrollPane = new JScrollPane(keybindsListingPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);

        panel.setAlignmentX(LEFT_ALIGNMENT);

        return panel;
    }

    private void reloadKeybindsPanel(InputCollector inputCollector){
        keybindsListingPanel.removeAll();
        List<InputCollector.InputMapping> inputMappingList = inputCollector.listInputMappings();
        Iterator<InputCollector.InputMapping> listIterator = inputMappingList.iterator();

        // Place all elements
        int y = 0;
        while (listIterator.hasNext()){
            InputCollector.InputMapping mapping = listIterator.next();
            new KeybindingListingPanel(keybindsListingPanel, y, mapping.getInputMessage(), mapping.getCommand(), inputCollector);
            y++;
        }

        // Place filler component to use up remaining space
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weighty = 1;
        c.gridy = y;
        keybindsListingPanel.add(new JPanel(), c);

        // Rebuild panel
        keybindsListingPanel.revalidate();
        keybindsListingPanel.repaint();
    }

    @Override public void onSavingStateChange(boolean isMappingSaved) {
        if (isMappingSaved) {
            savingStateLabel.setText("Saved");
            savingStateLabel.setIcon(new ImageIcon(FileIO.getRootFilePath().concat(ButtonMaker.convertIconPath("icons/save.png"))));
        } else {
            savingStateLabel.setText("Saving...");
            savingStateLabel.setIcon(new ImageIcon(FileIO.getRootFilePath().concat(ButtonMaker.convertIconPath("icons/refresh.png"))));
        }
        savingStateLabel.repaint();
    }

    @Override public void onInputMessageSend(String inputMessage) {
        mostRecentInputLabel.setText(String.format("Most recent input: %s", inputMessage));
        mostRecentInputLabel.repaint();
    }

    @Override public void onMidiDeviceBind(String deviceString) {
        boundMidiDeviceLabel.setText(String.format("Receiving MIDI input from: %s", deviceString));
        boundMidiDeviceString = deviceString;
        boundMidiDeviceLabel.repaint();
    }

    private class KeybindingListingPanel {

        private String inputMessage;
        private String command;

        private JTextArea inputLabel;
        private JTextArea commandLabel;

        private KeybindingListingPanel(JPanel panel, int gridy, String inputMessage, String command, InputCollector inputCollector){
            this.inputMessage = inputMessage;
            this.command = command;
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 2, 2, 2);
            c.gridx = 0;
            c.gridy = gridy;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.FIRST_LINE_START;

            JButton removeButton = ButtonMaker.createIconButton("icons/cancel.png", "Remove", 5);
            removeButton.addActionListener(e -> {
                inputCollector.removeInputMapping(inputMessage);
                reloadKeybindsPanel(inputCollector);
            });
            panel.add(removeButton, c);

            c.gridx++;
            JButton recordButton = ButtonMaker.createIconButton("icons/input.png", "Record", 5);
            recordButton.addActionListener(e -> {
                String currentInput = inputLabel.getText();
                inputLabel.setText("...");
                inputCollector.setTempInputReceiveOverride(input -> {
                    if (!inputCollector.containsInputKey(input)) {
                        String removedCommand = inputCollector.removeInputMapping(inputMessage);
                        inputCollector.addInputMapping(input, removedCommand);
                        reloadKeybindsPanel(inputCollector);
                    } else
                        inputLabel.setText(currentInput);
                });
            });
            panel.add(recordButton, c);

            c.gridx++;
            c.weightx = 0.15;
            c.fill = GridBagConstraints.HORIZONTAL;
            inputLabel = new JTextArea(inputMessage);
            inputLabel.setEditable(false);
            panel.add(inputLabel, c);

            c.gridx++;
            c.weightx = 0.85;
            commandLabel = new JTextArea(command);
            commandLabel.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) {
                    inputCollector.modifyMappingCommand(inputLabel.getText(), commandLabel.getText());
                }

                @Override public void removeUpdate(DocumentEvent e) {
                    inputCollector.modifyMappingCommand(inputLabel.getText(), commandLabel.getText());
                }

                @Override public void changedUpdate(DocumentEvent e) {
                    inputCollector.modifyMappingCommand(inputLabel.getText(), commandLabel.getText());
                }
            });
            panel.add(commandLabel, c);

            //setMaximumSize(new Dimension(Integer.MAX_VALUE, (int)removeButton.getPreferredSize().getHeight() + (2 * 2)));
        }
    }
}
