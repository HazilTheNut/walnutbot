package UI;

import Audio.AudioKey;
import Audio.AudioKeyPlaylist;

import java.io.File;
import java.util.ArrayList;

public class AudioKeyPlaylistLoader {

    public static ArrayList<AudioKey> grabKeysFromPlaylist(String uri){
        File file = new File(uri);
        AudioKeyPlaylist playlist = new AudioKeyPlaylist(file);
        if (playlist.isURLValid())
            return playlist.getAudioKeys();
        else
            return new ArrayList<>();
    }

}
