package Audio;

import org.jetbrains.annotations.Nullable;

public class AudioKeyPlaylistEvent {

    public enum AudioKeyPlaylistEventType {
        ADD,                // An AudioKey has been added to the playlist
        REMOVE,             // An AudioKey key has been removed from the playlist
        MODIFY,             // An AudioKey in the playlist has been modified
        CLEAR,              // The playlist was cleared
        SORT,               // The playlist was sorted
        SHUFFLE,            // The playlist was shuffled
        ON_SUBSCRIBE,       // Called when the AudioKeyPlaylistListener was added to a playlist
        EVENT_QUEUE_END,    // When the AudioKeyPlaylist flushes its event queue, this is the last event sent
    }

    private AudioKey key;

    private int pos;

    private AudioKeyPlaylistEventType eventType;

    public AudioKeyPlaylistEvent(AudioKey key, int pos, AudioKeyPlaylistEventType eventType) {
        this.key = key;
        this.pos = pos;
        this.eventType = eventType;
    }

    @Nullable
    public AudioKey getKey() {
        return key;
    }

    /**
     * Gets the position in the list where the change is made.
     *
     * @return The position in the list. The value returned is -1 if the element was appended to the end of the list.
     */
    public int getPos() {
        return pos;
    }

    public AudioKeyPlaylistEventType getEventType() {
        return eventType;
    }
}
