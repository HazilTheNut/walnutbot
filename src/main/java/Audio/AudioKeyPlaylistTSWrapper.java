package Audio;

import java.util.concurrent.Semaphore;

public class AudioKeyPlaylistTSWrapper {

    private final AudioKeyPlaylist audioKeyPlaylist;
    private final Semaphore accessSemaphore;

    public AudioKeyPlaylistTSWrapper(AudioKeyPlaylist audioKeyPlaylist) {
        this.audioKeyPlaylist = audioKeyPlaylist;
        accessSemaphore = new Semaphore(1,true);
    }

    /**
     * Blocking access to the AudioKeyPlaylist this object encapsulates.
     *
     * @param accessHandle What to do when access is gained
     */
    public synchronized void accessAudioKeyPlaylist(IAudioKeyPlaylistAccessHandle accessHandle) {
        try {
            accessSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            accessHandle.onGainAccess(audioKeyPlaylist);
            audioKeyPlaylist.flushEventQueue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        accessSemaphore.release();
    }

    public boolean isNonEmpty(){
        return !audioKeyPlaylist.isEmpty();
    }

    public synchronized void addAudioKeyPlaylistListener(AudioKeyPlaylistListener listener){
        // Does not use the accessSemaphore as the ON_SUBSCRIBE event will often involve reading from the audioKeyPlaylist
        audioKeyPlaylist.addAudioKeyPlaylistListener(listener);
    }
}
