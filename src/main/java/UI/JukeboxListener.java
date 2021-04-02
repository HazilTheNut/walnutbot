package UI;

import Audio.AudioMaster;

public interface JukeboxListener {

    void onDefaultListChange(AudioMaster audioMaster);

    void onJukeboxChangeLoopState(boolean isLoopingSong);

    void updateDefaultPlaylistLabel(String playlistName);

}
