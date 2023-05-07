package Audio;

public class AudioKeyPlaylistTSWrapper {

    private final AudioKeyPlaylist audioKeyPlaylist;

    public AudioKeyPlaylistTSWrapper(AudioKeyPlaylist audioKeyPlaylist) {
        this.audioKeyPlaylist = audioKeyPlaylist;
    }

    /**
     * Blocking access to the AudioKeyPlaylist this object encapsulates.
     *
     * @param accessHandle What to do when access is gained
     */
    public synchronized void accessAudioKeyPlaylist(IAudioKeyPlaylistAccessHandle accessHandle) {
        accessHandle.onGainAccess(audioKeyPlaylist);
        audioKeyPlaylist.flushEventQueue();
    }

    public boolean isNonEmpty(){
        return !audioKeyPlaylist.isEmpty();
    }
}
