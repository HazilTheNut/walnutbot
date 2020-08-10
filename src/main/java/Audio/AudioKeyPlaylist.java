package Audio;

import Utils.FileIO;
import Utils.Transcriber;

import java.io.*;
import java.util.*;

public class AudioKeyPlaylist {

    private String name;
    private String url;
    private ArrayList<AudioKey> audioKeys;
    private boolean isURLValid;

    public AudioKeyPlaylist(String name){
        audioKeys = new ArrayList<>();
        this.name = name;
        isURLValid = true;
        url = "NULL";
    }

    public AudioKeyPlaylist(File file){
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
            } else
                audioKeys.add(new AudioKey(FileIO.getFileName(url), url));
        } else {
            Transcriber.print("File \'%1$s\' was not found!", url);
            audioKeys.add(new AudioKey("Requested", url));
        }
    }

    public ArrayList<AudioKey> getAudioKeys() {
        return audioKeys;
    }

    public boolean isEmpty(){
        return audioKeys.isEmpty();
    }

    public AudioKey getRandomAudioKey(){
        if (isEmpty())
            return null;
        Random random = new Random();
        return audioKeys.get(random.nextInt(audioKeys.size()));
    }

    void shuffle(){
        AudioKey[] shuffledArray = new AudioKey[audioKeys.size()];
        Random random = new Random();
        for (int i = 0; i < shuffledArray.length; i++)
            shuffledArray[i] = audioKeys.remove(random.nextInt(audioKeys.size()));
        audioKeys.addAll(Arrays.asList(shuffledArray));
    }

    public String getName() {
        return name;
    }

    public void printPlaylist(){
        Transcriber.print("Playlist \'%1$s\' contains:", name);
        for (AudioKey key : audioKeys)
            Transcriber.print(key.toString());
    }

    public void addAudioKey(AudioKey audioKey){
        audioKeys.add(audioKey);
    }

    public AudioKey removeAudioKey(AudioKey key){
        if (audioKeys.remove(key)){
            return key;
        }
        return null;
    }

    public AudioKey removeAudioKey(int pos){
        return audioKeys.remove(pos);
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
