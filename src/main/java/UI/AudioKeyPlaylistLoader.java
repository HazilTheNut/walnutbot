package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;

import java.io.File;
import java.util.ArrayList;

public class AudioKeyPlaylistLoader {

    public static ArrayList<AudioKey> grabKeysFromPlaylist(String uri){
        if (uri.contains(".playlist")) { //Search for file extension
            File file = new File(uri);
            AudioKeyPlaylist playlist = new AudioKeyPlaylist(file);
            return playlist.getAudioKeys();
        } else { //Otherwise, don't try to read as a .playlist file.
            ArrayList<AudioKey> keys = new ArrayList<>();
            keys.add(new AudioKey("requested", uri));
            return keys;
        }
    }

}
