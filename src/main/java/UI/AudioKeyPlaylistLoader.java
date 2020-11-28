package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;

import java.io.File;
import java.util.ArrayList;

public class AudioKeyPlaylistLoader {

    public static ArrayList<AudioKey> grabKeysFromPlaylist(String uri){
        if (uri.indexOf("http") != 0) { //Check for if it isn't a url
            File file = new File(uri);
            AudioKeyPlaylist playlist = new AudioKeyPlaylist(file, true);
            return playlist.getAudioKeys();
        } else { //Otherwise, don't try to read as a file. Doing so may switch forward slashes to backslashes, which corrupts the url.
            ArrayList<AudioKey> keys = new ArrayList<>();
            keys.add(new AudioKey("requested", uri));
            return keys;
        }
    }

}
