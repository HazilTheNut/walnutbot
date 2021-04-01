package Audio;

public interface AudioKeyPlaylistListener {

    void onAddition(AudioKeyPlaylistEvent event);

    void onRemoval(AudioKeyPlaylistEvent event);

    /**
     * Called when an element of the AudioKeyPlaylist has been modified.
     * This method is called after the element has been modified.
     *
     * @param event The AudioKeyPlaylistEvent describing the details of the event.
     */
    void onModification(AudioKeyPlaylistEvent event);

    /**
     * Called when the AudioKeyPlaylist has been cleared.
     */
    void onClear();

    /**
     * Called when the AudioKeyPlaylist has been shuffled.
     */
    void onShuffle();

    /**
     * Called when the AudioKeyPlaylist has been sorted.
     */
    void onSort();

    /**
     * Called when the AudioKeyPlaylist changes for a new one.
     */
    void onNewPlaylist();
}
