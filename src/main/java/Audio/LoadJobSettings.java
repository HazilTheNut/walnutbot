package Audio;

public class LoadJobSettings {
    private final boolean storeLoadedTrackObjects;
    private final boolean recursivelyExpandPlaylistFiles;

    public LoadJobSettings(boolean storeLoadedTrackObjects, boolean recursivelyExpandPlaylistFiles) {
        this.storeLoadedTrackObjects = storeLoadedTrackObjects;
        this.recursivelyExpandPlaylistFiles = recursivelyExpandPlaylistFiles;
    }

    public boolean storeLoadedTrackObjects() {
        return storeLoadedTrackObjects;
    }

    public boolean recursivelyExpandPlaylistFiles() {
        return recursivelyExpandPlaylistFiles;
    }

    @Override
    public String toString() {
        return String.format("{storeLoadedTrackObjects: %b, recursivelyExpandPlaylistFiles: %b}",
                storeLoadedTrackObjects, recursivelyExpandPlaylistFiles);
    }
}
