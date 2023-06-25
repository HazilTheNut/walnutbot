package Audio;

import Main.WalnutbotInfo;
import Utils.FileIO;
import Utils.Transcriber;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioTrackLoadJob {

    private final UUID id;
    private final Semaphore loadResultMutex;

    public AudioTrackLoadJob(){
        loadResultMutex = new Semaphore(1,true);
        id = UUID.randomUUID();
    }

    public boolean loadItem(String uri, AudioKeyPlaylistTSWrapper output, IPlaybackWrapper playbackWrapper, ITrackLoadResultHandler trackLoadResultHandler, LoadJobSettings loadJobSettings) {
        try {
            Thread loadThread = new Thread(() -> loadItemThread(uri, output, playbackWrapper, trackLoadResultHandler, loadJobSettings));
            loadThread.start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void loadItemThread(String uri, AudioKeyPlaylistTSWrapper output, IPlaybackWrapper playbackWrapper, ITrackLoadResultHandler trackLoadResultHandler, LoadJobSettings loadJobSettings) {
        // Populate urisToProcess to construct list of URIs to load
        LinkedList<UriLoadRequest> urisToProcess = new LinkedList<>();
        populateURIsToProcessList(uri, urisToProcess, loadJobSettings);

        // Load each URI
        for (UriLoadRequest loadRequest : urisToProcess) {
            UriSourceType uriSourceType = resolveUriType(loadRequest.getUri());
            Transcriber.printRaw("URI \"%s\" is %s", loadRequest.getUri(), uriSourceType.name());
            switch (uriSourceType) {
                case LOCAL_PLAYLIST:
                    /*
                     * This should have been expanded by above unless loadJobSettings.recursivelyExpandPlaylistFiles() is false.
                     * In which case they can appear here, though a playlist file is not music and this isn't playable
                     */
                    if (loadJobSettings.storeLoadedTrackObjects())
                        loadRequestWithoutPlaybackWrapper(output, trackLoadResultHandler, urisToProcess, loadRequest, UriLoadRequestState.LOADED_UNSUCCESSFULLY);
                    else
                        loadRequestWithoutPlaybackWrapper(output, trackLoadResultHandler, urisToProcess, loadRequest, UriLoadRequestState.LOADED_SUCCESSFULLY);
                    break;
                case LOCAL_MUSIC:
                case WEBSITE_LINK:
                    if (loadJobSettings.storeLoadedTrackObjects())
                        playbackWrapper.loadItem(loadRequest.getSource(), loadRequest.getLoadedTracks(), loadJobSettings,
                                successful -> onPlaybackWrapperLoadComplete(output, trackLoadResultHandler, urisToProcess, loadRequest, successful));
                    else
                        loadRequestWithoutPlaybackWrapper(output, trackLoadResultHandler, urisToProcess, loadRequest, UriLoadRequestState.LOADED_SUCCESSFULLY);
                    break;
                case INVALID:
                default:
                    loadRequestWithoutPlaybackWrapper(output, trackLoadResultHandler, urisToProcess, loadRequest, UriLoadRequestState.LOADED_UNSUCCESSFULLY);
                    break;
            }
        }
    }

    private void loadRequestWithoutPlaybackWrapper(AudioKeyPlaylistTSWrapper output, ITrackLoadResultHandler trackLoadResultHandler, LinkedList<UriLoadRequest> urisToProcess, UriLoadRequest loadRequest, UriLoadRequestState requestState) {
        try {
            loadResultMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (loadRequest.getSource().getName() == null)
            loadRequest.getSource().setName(loadRequest.getUri());
        loadRequest.setLoadRequestState(requestState);
        if (requestState == UriLoadRequestState.LOADED_SUCCESSFULLY)
            loadRequest.getLoadedTracks().add(loadRequest.getSource());
        if (isURIsToProcessListDoneLoading(urisToProcess)) {
            boolean result = populateOutputAudioKeyPlaylist(output, urisToProcess);
            trackLoadResultHandler.onTracksLoaded(output, result);
        }
        loadResultMutex.release();
    }

    private void onPlaybackWrapperLoadComplete(AudioKeyPlaylistTSWrapper output, ITrackLoadResultHandler trackLoadResultHandler, LinkedList<UriLoadRequest> urisToProcess, UriLoadRequest loadRequest, boolean successful) {
        try {
            loadResultMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (successful)
            loadRequest.setLoadRequestState(UriLoadRequestState.LOADED_SUCCESSFULLY);
        else
            loadRequest.setLoadRequestState(UriLoadRequestState.LOADED_UNSUCCESSFULLY);
        // Populate the AudioKeyPlaylistTSWrapper output after all tracks have been loaded
        if (isURIsToProcessListDoneLoading(urisToProcess)) {
            boolean result = populateOutputAudioKeyPlaylist(output, urisToProcess);
            trackLoadResultHandler.onTracksLoaded(output, result);
        }
        loadResultMutex.release();
    }

    private void populateURIsToProcessList(String uri, List<UriLoadRequest> urisToProcess, LoadJobSettings loadJobSettings) {
        LinkedList<String> alreadyLoadedPlaylistURIs = new LinkedList<>();
        populateURIsToProcessList(uri, urisToProcess, loadJobSettings, alreadyLoadedPlaylistURIs);
    }

    private void populateURIsToProcessList(String uri, List<UriLoadRequest> urisToProcess, LoadJobSettings loadJobSettings, List<String> alreadyLoadedPlaylistURIs) {
        if (!FileIO.isPlaylistFile(uri)) {
            // If not a playlist file, only one URI needs to be loaded
            // Name set to null to indicate using loaded track info to populate name field
            urisToProcess.add(new UriLoadRequest(new AudioKey(null, uri)));
            return;
        }
        AudioKeyPlaylist fromFile = new AudioKeyPlaylist(new File(uri));
        for (AudioKey key : fromFile.getAudioKeys()) {
            if (loadJobSettings.recursivelyExpandPlaylistFiles() && FileIO.isPlaylistFile(key.getUrl())) {
                // Recurse through playlist files
                if (!alreadyLoadedPlaylistURIs.contains(key.getUrl())) {
                    LinkedList<String> recurseList = new LinkedList<>(alreadyLoadedPlaylistURIs);
                    recurseList.add(key.getUrl());
                    populateURIsToProcessList(key.getUrl(), urisToProcess, loadJobSettings, recurseList);
                }
            } else
                urisToProcess.add(new UriLoadRequest(key));
        }
    }

    private boolean isURIsToProcessListDoneLoading(List<UriLoadRequest> urisToProcess){
        for (UriLoadRequest loadRequest : urisToProcess)
            if (loadRequest.getLoadRequestState() == UriLoadRequestState.UNLOADED)
                return false;
        return true;
    }

    private boolean populateOutputAudioKeyPlaylist(AudioKeyPlaylistTSWrapper output, List<UriLoadRequest> urisToProcess) {
        AtomicBoolean successful = new AtomicBoolean(false);
        output.accessAudioKeyPlaylist(playlist -> {
            for (UriLoadRequest loadRequest : urisToProcess) {
                if (loadRequest.getLoadRequestState() == UriLoadRequestState.LOADED_SUCCESSFULLY) {
                    for (AudioKey key : loadRequest.getLoadedTracks()) {
                        playlist.addAudioKey(key);
                    }
                    successful.set(true);
                }
            }
        });
        return successful.get();
    }

    public UUID getId() {
        return id;
    }

    private enum UriLoadRequestState {
        UNLOADED,
        LOADED_SUCCESSFULLY,
        LOADED_UNSUCCESSFULLY
    }

    private enum UriSourceType {
        INVALID,
        LOCAL_MUSIC,
        LOCAL_PLAYLIST,
        WEBSITE_LINK
    }

    private UriSourceType resolveUriType(String uri) {
        // First check for website link
        if (FileIO.isWebsiteURL(uri))
            return UriSourceType.WEBSITE_LINK;
        // Otherwise, this should be some sort of local file
        if (FileIO.isPlaylistFile(uri))
            return UriSourceType.LOCAL_PLAYLIST;
        // Check to see if it is a playable music file
        String ext = FileIO.getFileExtension(uri);
        for (String acceptedExt : WalnutbotInfo.ACCEPTED_AUDIO_FORMATS)
            if (ext.equals(acceptedExt))
                return UriSourceType.LOCAL_MUSIC;
        // If all else fails, it's invalid
        return UriSourceType.INVALID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioTrackLoadJob that = (AudioTrackLoadJob) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static class UriLoadRequest {
        private final AudioKey source;
        private UriLoadRequestState loadRequestState;
        private final LinkedList<AudioKey> loadedTracks;

        public UriLoadRequest(AudioKey source) {
            this.source = source;
            loadRequestState = UriLoadRequestState.UNLOADED;
            loadedTracks = new LinkedList<>();
        }

        public String getUri() {
            return source.getUrl();
        }

        public UriLoadRequestState getLoadRequestState() {
            return loadRequestState;
        }

        public void setLoadRequestState(UriLoadRequestState loadRequestState) {
            this.loadRequestState = loadRequestState;
        }

        public LinkedList<AudioKey> getLoadedTracks() {
            return loadedTracks;
        }

        public AudioKey getSource() {
            return source;
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", source, loadRequestState.name());
        }
    }
}
