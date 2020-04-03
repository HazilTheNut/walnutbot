package UI;

import Audio.AudioKey;

public interface PlaylistUIWrapper {

    void addAudioKey(AudioKey key);

    int getAudioKeyID(AudioKeyUIWrapper keyUIWrapper);

    void modifyAudioKey(int keyID, AudioKey newData);

    void removeAudioKey(int keyID);
}
