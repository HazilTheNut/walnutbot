package Utils;

import Utils.FileIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class SettingsLoader {

    private static HashMap<String, String> settings;

    public static void readSettings(){
        File settingsFile = new File(FileIO.getRootFilePath() + "config.txt");
        settings = new HashMap<>();
        try {
            FileInputStream inputStream = new FileInputStream(settingsFile);
            Scanner sc = new Scanner(inputStream);

            while(sc.hasNext()){
                String in = sc.nextLine();
                if (in.length() > 0 && in.charAt(0) != '#'){
                    int split = in.indexOf('=');
                    String var = in.substring(0, split);
                    String value = in.substring(Math.min(split+1,in.length()));
                    settings.put(var, value);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the settings value from the config.txt file.
     *
     * @param var The name of the variable you want to fetch the value for.
     * @return The value of the variable being searched for. Returns null if missing.
     */
    public static String getValue(String var){
        if (settings.containsKey(var))
            return settings.get(var);
        return null;
    }
}
