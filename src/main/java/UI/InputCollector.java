package UI;

import Commands.Command;
import Commands.CommandInterpreter;
import Utils.Transcriber;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.sound.midi.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class InputCollector implements Receiver, NativeKeyListener {

    private TreeMap<String, String> inputMap;
    private InputReceiveAction tempInputReceiveOverride;
    private CommandInterpreter commandInterpreter;

    private Timer saveTimer;
    private AtomicBoolean saveScheduled;

    private File inputsFile;
    private InputCollectorListener inputCollectorListener;

    private MidiDevice boundMidiDevice;

    public InputCollector(File inputsFile, CommandInterpreter commandInterpreter){
        this.commandInterpreter = commandInterpreter;
        inputMap = new TreeMap<>();
        this.inputsFile = inputsFile;
        if (inputsFile.exists() && inputsFile.isFile()){
            try {
                FileInputStream fileInputStream = new FileInputStream(inputsFile);
                Scanner sc = new Scanner(fileInputStream);
                while (sc.hasNext()){
                    String line = sc.nextLine();
                    int splitPos = line.indexOf('>');
                    if (splitPos >= 0){
                        inputMap.put(line.substring(0, splitPos), line.substring(splitPos+1));
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        saveTimer = new Timer("Global Input File Save Timer");
        saveScheduled = new AtomicBoolean(false);
    }

    public List<InputMapping> listInputMappings(){
        LinkedList<InputMapping> list = new LinkedList<>();
        for (String inputMessage : inputMap.keySet())
            list.add(new InputMapping(inputMessage, inputMap.get(inputMessage)));
        return list;
    }

    private void scheduleSave(){
        if (!saveScheduled.get()){
            if (inputCollectorListener != null)
                inputCollectorListener.onSavingStateChange(false);
            saveTimer.schedule(new TimerTask() {
                @Override public void run() {
                    writeKeybindsFile();
                }
            }, 3000);
        }
    }

    private void writeKeybindsFile(){
        try {
            FileOutputStream fileOut = new FileOutputStream(inputsFile);
            PrintWriter writer = new PrintWriter(fileOut);
            for (String inputMessage : inputMap.keySet()){
                writer.printf("%1$s>%2$s\n", inputMessage, inputMap.get(inputMessage));
            }
            writer.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inputCollectorListener != null)
            inputCollectorListener.onSavingStateChange(true);
    }

    /**
     * Adds an "input message"-command pair to the input mapping.
     * If an input message is already in the mapping, this method will do nothing.
     *
     * @param inputMessage The input message to register
     * @param command The command to associate with the input message
     * @return false if an input message already exists in the mapping, and true otherwise
     */
    public boolean addInputMapping(String inputMessage, String command){
        if (inputMap.containsKey(inputMessage))
            return false;
        inputMap.put(inputMessage, command);
        scheduleSave();
        return true;
    }

    /**
     * Modifies the command of an "input message"-command pair already in the mapping.
     *
     * @param inputMessage The "input message" to set a new command to
     * @param command The new command to associate with the input message
     * @return True if the input message was successfully re-associated, and false otherwise
     */
    public boolean modifyMappingCommand(String inputMessage, String command){
        if (!inputMap.containsKey(inputMessage))
            return false;
        inputMap.put(inputMessage, command);
        scheduleSave();
        return true;
    }

    /**
     * Removes an "input message"-command pair from the input mapping
     *
     * @param inputMessage The input message for which you attempt to remove
     * @return The command associated with the "input message"-command pair that was removed.
     */
    public String removeInputMapping(String inputMessage){
        String command = inputMap.remove(inputMessage);
        if (command != null)
            scheduleSave();
        return command;
    }

    public boolean containsInputKey(String inputMessage){
        return inputMap.containsKey(inputMessage);
    }

    public String getMappedCommand(String inputMessage){
        return inputMap.get(inputMessage);
    }

    public void bindToTransmitter(MidiDevice midiDevice){
        if (boundMidiDevice != null) {
            boundMidiDevice.close();
        }
        try {
            midiDevice.open();
            for (Transmitter transmitter : midiDevice.getTransmitters())
                transmitter.setReceiver(this);
            boundMidiDevice = midiDevice;
            if (inputCollectorListener != null)
                inputCollectorListener.onMidiDeviceBind(boundMidiDevice.getDeviceInfo().toString());
            Transcriber.printRaw("Input Collection bound to MIDI device \"%s\"", midiDevice.getDeviceInfo().toString());
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void bindToTransmitter(String transmitterString){
        for (MidiDevice.Info deviceInfo : MidiSystem.getMidiDeviceInfo()){
            if (deviceInfo.toString().equals(transmitterString)) {
                try {
                    bindToTransmitter(MidiSystem.getMidiDevice(deviceInfo));
                } catch (MidiUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sends a MIDI message and time-stamp to this receiver. If time-stamping is
     * not supported by this receiver, the time-stamp value should be -1.
     *
     * @param message   the MIDI message to send
     * @param timeStamp the time-stamp for the message, in microseconds
     * @throws IllegalStateException if the receiver is closed
     */
    @Override public void send(MidiMessage message, long timeStamp) {
        StringBuilder messageDataStr = new StringBuilder();
        byte[] bytes = message.getMessage();
        if (bytes.length < 1)
            return;
        String status = decodeStatusByte(bytes[0]);
        if (status != null) {
            messageDataStr.append(status).append(" : ");
            for (int i = 1; i < bytes.length; i++) {
                messageDataStr.append(String.format("%x", bytes[i]));
            }
            onInputMessage(messageDataStr.toString());
        }
        Transcriber.printTimestamped("MIDI message received!\ntoString(): %1$s\nStatus: %2$x\nData: %3$x", message.toString(), message.getStatus(), messageDataStr.toString());
    }

    private String decodeStatusByte(byte status){
        // Miscellaneous messages
        switch ((int)status) {
            case 0xF3: return "Song Select";
            case 0xFA: return "Start";
            case 0xFB: return "Continue";
            case 0xFC: return "Stop";
        }
        // General messages
        byte channel = (byte)(status & (byte)0x0F);
        String channelString = String.format("Chn%d ", channel);
        byte event = (byte)(status >> 8);
        switch ((int)event) {
            //case 0x8: return channelString.concat("Note Off");
            case 0x9: return channelString.concat("Note On");
            case 0xA: return channelString.concat("Polyphonic Aftertouch");
            case 0xB: return channelString.concat("Control/Mode Change");
            case 0xC: return channelString.concat("Program Change");
            case 0xD: return channelString.concat("Channel Aftertouch");
            case 0xE: return channelString.concat("Pitch Bend Change");
            default: return null;
        }
    }

    /*
    /**
     * Indicates that the application has finished using the receiver, and that
     * limited resources it requires may be released or made available.
     * <p>
     * If the creation of this {@code Receiver} resulted in implicitly opening
     * the underlying device, the device is implicitly closed by this method.
     * This is true unless the device is kept open by other {@code Receiver} or
     * {@code Transmitter} instances that opened the device implicitly, and
     * unless the device has been opened explicitly. If the device this
     * {@code Receiver} is retrieved from is closed explicitly by calling
     * {@link MidiDevice#close MidiDevice.close}, the {@code Receiver} is
     * closed, too. For a detailed description of open/close behaviour see the
     * class description of {@link MidiDevice MidiDevice}.
     *
     * @see MidiSystem#getReceiver
     */
    @Override public void close() {

    }

    private static final int[] EXTENDED_KEYCODES = {
        NativeKeyEvent.VC_F1,
        NativeKeyEvent.VC_F2,
        NativeKeyEvent.VC_F3,
        NativeKeyEvent.VC_F4,
        NativeKeyEvent.VC_F5,
        NativeKeyEvent.VC_F6,
        NativeKeyEvent.VC_F7,
        NativeKeyEvent.VC_F8,
        NativeKeyEvent.VC_F9,
        NativeKeyEvent.VC_F10,
        NativeKeyEvent.VC_F11,
        NativeKeyEvent.VC_F12
    };

    @Override public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {

    }

    @Override public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        /*
        Transcriber.printRaw("Key Event:\nparamString(): %1$s\nkeyChar: %2$s\nkeyCode: %3$s\nkeyStr: %4$s",
            nativeKeyEvent.paramString(), nativeKeyEvent.getKeyChar(), nativeKeyEvent.getKeyCode(),
            NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode()));
            */
        String input = buildKeyPressInputString(nativeKeyEvent);
        if (input != null) {
            //Transcriber.printRaw("Key Pressed: (%2$d) %1$s", input, input.length());
            onInputMessage(input);
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {

    }

    private String buildKeyPressInputString(NativeKeyEvent nativeKeyEvent){
        if (!isKeyEventValid(nativeKeyEvent))
            return null;
        String message;
        if (nativeKeyEvent.getModifiers() == 0)
            message = NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode());
        else
            message = String.format("%1$s+%2$s", NativeKeyEvent.getModifiersText(nativeKeyEvent.getModifiers()), NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode()));
        return message;
    }

    private boolean isKeyEventValid(NativeKeyEvent nativeKeyEvent){
        for (int code : EXTENDED_KEYCODES)
            if (nativeKeyEvent.getKeyCode() == code)
                return true;
        return !nativeKeyEvent.isActionKey();
    }

    public void setTempInputReceiveOverride(InputReceiveAction tempInputReceiveOverride) {
        this.tempInputReceiveOverride = tempInputReceiveOverride;
    }

    private void onInputMessage(String inputMessage){
        if (inputCollectorListener != null)
            inputCollectorListener.onInputMessageSend(inputMessage);
        if (tempInputReceiveOverride != null) {
            tempInputReceiveOverride.doAction(inputMessage);
            tempInputReceiveOverride = null;
        }
        else if (inputMap.containsKey(inputMessage))
            commandInterpreter.evaluateCommand(inputMap.get(inputMessage),
                Transcriber.getGenericCommandFeedBackHandler(inputMessage),
                Command.INTERNAL_MASK);
    }

    public InputCollectorListener getInputCollectorListener() {
        return inputCollectorListener;
    }

    public void setInputCollectorListener(InputCollectorListener inputCollectorListener) {
        this.inputCollectorListener = inputCollectorListener;
    }

    public interface InputReceiveAction {
        void doAction(String inputMessage);
    }

    public class InputMapping {
        String inputMessage;
        String command;

        public InputMapping(String inputMessage, String command) {
            this.inputMessage = inputMessage;
            this.command = command;
        }

        public String getInputMessage() {
            return inputMessage;
        }

        public String getCommand() {
            return command;
        }
    }
}
