package Audio;

public class AudioKeyPlaylistEvent {

    private AudioKey key;
    private int pos;

    public AudioKeyPlaylistEvent(AudioKey key, int pos) {
        this.key = key;
        this.pos = pos;
    }

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
}
