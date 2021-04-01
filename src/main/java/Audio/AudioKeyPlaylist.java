package Audio;

import Utils.FileIO;
import Utils.Transcriber;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.*;
import java.util.*;

public class AudioKeyPlaylist {

    private String name;
    private String url;
    private ArrayList<AudioKey> audioKeys;
    private CircularFifoQueue<AudioKey> previousRandomDrawings; //For pesudo-random selection of songs
    private ArrayList<AudioKeyPlaylistListener> eventListeners;
    private boolean isURLValid;

    public AudioKeyPlaylist(String name){
        this(name, "NULL");
    }

    public AudioKeyPlaylist(String name, String uri){
        audioKeys = new ArrayList<>();
        eventListeners = new ArrayList<>();
        this.name = name;
        isURLValid = true;
        url = uri;
    }

    public AudioKeyPlaylist(File file) { this(file, true); }

    /**
     * Instantiates an AudioKeyPlaylist from a File with a .playlist format.
     *
     * @param file The File to open and create an AudioKeyPlaylist from
     * @param loopback If the file doesn't exist or is not formatted correctly, setting this to true will instantiate
     *                 this AudioKeyPlaylist with a single element in its list, which is an AudioKey whose url is the location of the file provided.
     */
    public AudioKeyPlaylist(File file, boolean loopback){
        this(file.getName());
        audioKeys = new ArrayList<>();
        url = file.getPath();
        isURLValid = false;
        if (file.exists() && file.isFile()){
            if (FileIO.getFileExtension(url).equals("playlist")) {
                try {
                    name = file.getName();
                    FileInputStream outputStream = new FileInputStream(file);
                    Scanner sc = new Scanner(outputStream);
                    while (sc.hasNext()) {
                        AudioKey key = new AudioKey(sc.nextLine());
                        if (key.isValid())
                            audioKeys.add(key);
                    }
                    isURLValid = true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (loopback)
                audioKeys.add(new AudioKey(FileIO.getFileName(url), url));
        } else if (loopback){
            //Transcriber.printTimestamped("\'%1$s\' is not a file.", url);
            audioKeys.add(new AudioKey("Requested", url));
        }
    }

    public void addAudioKeyPlaylistListener(AudioKeyPlaylistListener listener){
        eventListeners.add(listener);
        listener.onNewPlaylist();
    }

    public ArrayList<AudioKey> getAudioKeys() {
        return audioKeys;
    }

    public AudioKey getKey(int pos){
        return audioKeys.get(pos);
    }

    public void clearPlaylist(){
        audioKeys.clear();
        if (previousRandomDrawings != null) instantiatePreviousDrawingsQueue();
        for (AudioKeyPlaylistListener listener : eventListeners) listener.onClear();
    }

    public boolean isEmpty(){
        return audioKeys.isEmpty();
    }

    private void instantiatePreviousDrawingsQueue(){
        previousRandomDrawings = new CircularFifoQueue<>(audioKeys.size() >> 1);
    }

    public AudioKey getRandomAudioKey(){
        if (isEmpty())
            return null;
        Random random = new Random();
        //Create a new queue if the
        if (previousRandomDrawings == null) instantiatePreviousDrawingsQueue();
        //Pick a song that has not been played recently
        AudioKey selected;
        short timeout = 32; //Give the random number generator 32 tries to find a new song - should be enough
        do {
            selected = audioKeys.get(random.nextInt(audioKeys.size()));
            timeout--;
        } while (timeout > 0 && previousRandomDrawings.contains(selected)); //Since the queue's size is never larger than half the Default List, there will always be a new song to pick and a decent likelihood of picking it.
        previousRandomDrawings.add(selected);
        return selected;
    }

    void shuffle(){
        AudioKey[] shuffledArray = new AudioKey[audioKeys.size()];
        Random random = new Random();
        for (int i = 0; i < shuffledArray.length; i++)
            shuffledArray[i] = audioKeys.remove(random.nextInt(audioKeys.size()));
        audioKeys.addAll(Arrays.asList(shuffledArray));
        for (AudioKeyPlaylistListener listener : eventListeners) listener.onShuffle();
    }

    void sort(){
        audioKeys.sort(Comparator.comparing(AudioKey::getName));
        for (AudioKeyPlaylistListener listener : eventListeners) listener.onSort();
    }

    public String getName() {
        return name;
    }

    public void printPlaylist(){
        Transcriber.printTimestamped("Playlist \'%1$s\' contains:", name);
        for (AudioKey key : audioKeys)
            Transcriber.printRaw(key.toString());
    }

    public void addAudioKey(AudioKey audioKey){
        audioKeys.add(audioKey);
        if (previousRandomDrawings != null) instantiatePreviousDrawingsQueue();
        AudioKeyPlaylistEvent event = new AudioKeyPlaylistEvent(audioKey, audioKeys.size()-1);
        for (AudioKeyPlaylistListener listener : eventListeners) listener.onAddition(event);
    }

    public AudioKey removeAudioKey(AudioKey key){
        int pos = audioKeys.indexOf(key);
        if (audioKeys.remove(key)){
            if (previousRandomDrawings != null) instantiatePreviousDrawingsQueue();
            AudioKeyPlaylistEvent event = new AudioKeyPlaylistEvent(key, pos);
            for (AudioKeyPlaylistListener listener : eventListeners) listener.onRemoval(event);
            return key;
        }
        return null;
    }

    public AudioKey removeAudioKey(int pos){
        if (previousRandomDrawings != null) instantiatePreviousDrawingsQueue();
        if (pos >= 0 && pos < audioKeys.size()) {
            AudioKey audioKey = audioKeys.remove(pos);
            AudioKeyPlaylistEvent event = new AudioKeyPlaylistEvent(audioKey, pos);
            for (AudioKeyPlaylistListener listener : eventListeners) listener.onRemoval(event);
            return audioKey;
        }
        return null;
    }

    public AudioKey removeAudioKey(String name){
        for (int i = 0; i < audioKeys.size(); i++)
            if (audioKeys.get(i).getName().equals(name)) {
                if (previousRandomDrawings != null) instantiatePreviousDrawingsQueue();
                AudioKeyPlaylistEvent event = new AudioKeyPlaylistEvent(audioKeys.get(i), i);
                for (AudioKeyPlaylistListener listener : eventListeners) listener.onRemoval(event);
                return audioKeys.remove(i);
            }
        return null;
    }

    /**
     * Modifies a name-matching AudioKey in this AudioKeyPlaylist by substituting the name and url of the matching AudioKey with the input AudioKey.
     * This method does not modify the loaded AudioTrack of the name-matching AudioKey.
     *
     * @param name The String name to match an AudioKey in this playlist.
     * @param newData The AudioKey data to substitute into the name-matching AudioKey. Leaving either the name or the url null leaves the name-matching AudioKey's respective data unaltered.
     * @return True if a name-matching AudioKey exists in this AudioKeyPlaylist, and false otherwise.
     */
    public boolean modifyAudioKey(String name, AudioKey newData){
        for (int i = 0; i < audioKeys.size(); i++)
            if (audioKeys.get(i).getName().equals(name)) {
                modifyAudioKey(i, newData);
                return true;
            }
        return false;
    }

    public void modifyAudioKey(int pos, AudioKey newData){
        if (newData.getName() != null)
            audioKeys.get(pos).setName(newData.getName());
        if (newData.getUrl() != null)
            audioKeys.get(pos).setUrl(newData.getUrl());
        AudioKeyPlaylistEvent event = new AudioKeyPlaylistEvent(audioKeys.get(pos), pos);
        for (AudioKeyPlaylistListener listener : eventListeners) listener.onModification(event);
    }

    public void saveToFile(File file){
        try {
            if (file.exists() && file.isFile()) file.delete();
            FileOutputStream outputStream = new FileOutputStream(file);
            PrintWriter writer = new PrintWriter(outputStream);
            for (AudioKey key : audioKeys)
                writer.println(key.toString());
            writer.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isURLValid() {
        return isURLValid;
    }

    public String getUrl() {
        return url;
    }

    @Override public String toString() {
        return String.format("\"%1$s\" @ %2$s", name, url);
    }
}
