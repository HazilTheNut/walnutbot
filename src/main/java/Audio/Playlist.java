package Audio;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Playlist {

    private String name;
    private ArrayList<AudioKey> audioKeys;

    public Playlist(String name){
        audioKeys = new ArrayList<>();
        this.name = name;
    }

    public Playlist(File file){
        this(file.getName());
        audioKeys = new ArrayList<>();
        if (file.exists() && file.isFile()){
            try {
                FileInputStream outputStream = new FileInputStream(file);
                Scanner sc = new Scanner(outputStream);
                while(sc.hasNext()){
                    AudioKey key = new AudioKey(sc.nextLine());
                    if (key.isValid())
                        audioKeys.add(key);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else
            System.out.printf("File \'%1$s\' was not found!\n", file.getAbsolutePath());
    }

    public ArrayList<AudioKey> getAudioKeys() {
        return audioKeys;
    }

    public String getName() {
        return name;
    }

    public void printPlaylist(){
        System.out.printf("Playlist \'%1$s\' contains:\n", name);
        for (AudioKey key : audioKeys)
            System.out.println(key.toString());
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
}
