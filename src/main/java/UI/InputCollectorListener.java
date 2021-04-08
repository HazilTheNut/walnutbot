package UI;

public interface InputCollectorListener {

    void onSavingStateChange(boolean isMappingSaved);

    void onInputMessageSend(String inputMessage);

    void onMidiDeviceBind(String deviceString);

}
