package Audio;

public interface IAudioKeyPlaylistAccessHandle {

    /**
     * Action to be taken once access has been gained to read or write to the AudioKeyPlaylist
     *
     * @param playlist AudioKeyPlaylist to read or write to
     */
    void onGainAccess(AudioKeyPlaylist playlist);

}
