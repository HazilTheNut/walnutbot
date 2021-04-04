package UI;

import Utils.Transcriber;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

public class InputCollector implements Receiver {
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
        for (byte b : message.getMessage())
            messageDataStr.append(String.format("%x",b));
        Transcriber.printTimestamped("MIDI message received!\ntoString(): %1$s\nStatus: %2$x\nData: %3$x", message.toString(), message.getStatus(), messageDataStr.toString());
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
}
