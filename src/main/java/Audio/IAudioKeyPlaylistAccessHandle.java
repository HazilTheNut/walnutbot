package Audio;

public interface IAudioKeyPlaylistAccessHandle {

    /**
     * Action to be taken once access has been gained to read or write to the AudioKeyPlaylist
     *
     * @param playlist AudioKeyPlaylist to read or write to
     * @return true if changes have been made and listeners to the AudioKeyPlaylist should be notified.
     */
    boolean onGainAccess(AudioKeyPlaylist playlist);

}
