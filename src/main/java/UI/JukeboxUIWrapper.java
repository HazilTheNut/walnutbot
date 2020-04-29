package UI;

import Audio.AudioMaster;

public interface JukeboxUIWrapper {

    void refreshDefaultList(AudioMaster audioMaster);

    void refreshQueueList(AudioMaster audioMaster);

    void updateDefaultPlaylistLabel(String playlistName);

}
