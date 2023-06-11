package Audio;

import Utils.FileIO;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioTrackLoadJob {
    private final Semaphore loadResultMutex;

    public AudioTrackLoadJob(){
        loadResultMutex = new Semaphore(1,true);
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
            if (!loadJobSettings.storeLoadedTrackObjects() && !FileIO.isWebsiteURL(loadRequest.getUri())) {
                /* If we don't need to store any loaded track objects and the URI is a local file,
                 *  then we don't need to go through the IPlaybackWrapper
                 */
                loadRequestWithoutPlaybackWrapper(output, trackLoadResultHandler, urisToProcess, loadRequest);
            } else {
                playbackWrapper.loadItem(loadRequest.getSource(), loadRequest.getLoadedTracks(), loadJobSettings,
                        successful -> onPlaybackWrapperLoadComplete(output, trackLoadResultHandler, urisToProcess, loadRequest, successful));
            }
        }
    }

    private void loadRequestWithoutPlaybackWrapper(AudioKeyPlaylistTSWrapper output, ITrackLoadResultHandler trackLoadResultHandler, LinkedList<UriLoadRequest> urisToProcess, UriLoadRequest loadRequest) {
        try {
            loadResultMutex.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        loadRequest.setLoadRequestState(UriLoadRequestState.LOADED_SUCCESSFULLY);
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

    private enum UriLoadRequestState {
        UNLOADED,
        LOADED_SUCCESSFULLY,
        LOADED_UNSUCCESSFULLY
    }

    private static class UriLoadRequest {
        private final String uri;
        private final AudioKey source;
        private UriLoadRequestState loadRequestState;
        private LinkedList<AudioKey> loadedTracks;

        public UriLoadRequest(AudioKey source) {
            this.source = source;
            this.uri = source.getUrl();
            loadRequestState = UriLoadRequestState.UNLOADED;
            loadedTracks = new LinkedList<>();
        }

        public String getUri() {
            return uri;
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
