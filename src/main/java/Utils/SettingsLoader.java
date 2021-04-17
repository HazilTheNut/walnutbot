package Utils;

import java.io.*;
import java.util.*;

public class SettingsLoader {

    private static HashMap<String, String> botconfig; //From the program's POV, is read-only. config.txt will preserve the comments in the file
    private static HashMap<String, String> settings; //Is writable & readable. settings.txt will not preserve comments in the file

    private static HashMap<String, String> adminUsers;
    private static HashMap<String, String> blockedUsers;

    public static void initialize(){
        botconfig = readSettingsFile(getConfigPath());
        settings = readSettingsFile(getSettingsPath());

        adminUsers = readUsersFile(getAdminsPath());
        blockedUsers = readUsersFile(getBlockedPath());
    }

    private static HashMap<String, String> readSettingsFile(String filepath){
        File settingsFile = new File(filepath);
        HashMap<String, String> settings = new HashMap<>();
        try {
            FileInputStream inputStream = new FileInputStream(settingsFile);
            Scanner sc = new Scanner(inputStream);

            while(sc.hasNext()){
                String in = sc.nextLine();
                if (in.length() > 0 && in.charAt(0) != '#'){
                    int split = in.indexOf('=');
                    if (split > 0) {
                        String var = in.substring(0, split);
                        String value = in.substring(Math.min(split + 1, in.length()));
                        settings.put(var, value);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Transcriber.printTimestamped("Could not find config/settings file at %1$s - if config.txt is missing, this bot is broken; if settings.txt is missing, will use default settings.", filepath);
        }
        return settings;
    }

    private static HashMap<String, String> readUsersFile(String filepath){
        File usersFile = new File(filepath);
        HashMap<String, String> users = new HashMap<>();
        try {
            FileInputStream inputStream = new FileInputStream(usersFile);
            Scanner sc = new Scanner(inputStream);

            while(sc.hasNext()){
                String in = sc.nextLine();
                users.put(in, in);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Transcriber.printTimestamped("Could not find user registry file at %1$s; using defaults", filepath);
        }
        return users;
    }

    private static String getConfigPath(){
        return String.format("%sconfig.ini", FileIO.getRootFilePath());
    }

    private static String getSettingsPath(){
        return String.format("%ssettings.ini", FileIO.getRootFilePath());
    }

    private static String getAdminsPath(){
        return String.format("%sadmins.txt", FileIO.getRootFilePath());
    }

    private static String getBlockedPath(){
        return String.format("%sblocked.txt", FileIO.getRootFilePath());
    }

    /**
     * Returns the settings value from the config.txt file.
     *
     * @param var The name of the variable you want to fetch the value for.
     * @return The value of the variable being searched for. Returns null if missing.
     */
    public static String getBotConfigValue(String var){
        if (botconfig.containsKey(var))
            return botconfig.get(var);
        return null;
    }

    /**
     * Returns the settings value from the settings.txt file.
     *
     * @param var The name of the variable you want to fetch the value for.
     * @param defaultSetting What to return if the setting is missing in the map.
     * @return The value of the variable being searched for. Returns the default value if missing.
     */
    public static String getSettingsValue(String var, String defaultSetting){
        if (settings.containsKey(var))
            return settings.get(var);
        return defaultSetting;
    }

    /**
     * Writes to the settings map and then saves it to settings.txt.
     * If the variable
     *
     * @param var The name of the variable you want to modify. If it does not exist, it becomes added to the map.
     * @param value The value to write to the variable.
     */
    public static void modifySettingsValue(String var, String value){
        settings.put(var, value);
    }

    /**
     * Writes the current set of settings to settings.ini
     */
    public static void writeSettingsFile(){
        File settingsFile = new File(getSettingsPath());
        settingsFile.delete();
        try {
            FileOutputStream outputStream = new FileOutputStream(settingsFile);
            PrintWriter writer = new PrintWriter(outputStream);
            for (String key : settings.keySet()){
                writer.println(String.format("%1$s=%2$s", key, settings.get(key)));
            }
            writer.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Writes the value contents of a HashMap of user names (either the "admins" or "blocked" list) to a file at the input path
     *
     * @param path Where to write the file to.
     * @param userMap The HashMap of users to write
     */
    private static void writeUsersFile(String path, HashMap<String, String> userMap){
        File userFile = new File(path);
        if (userFile.exists()) userFile.delete();
        try {
            FileOutputStream outputStream = new FileOutputStream(userFile);
            PrintWriter writer = new PrintWriter(outputStream);
            for (String user : userMap.values())
                writer.println(user);
            writer.close();
            outputStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void writeAdminsFile(){
        writeUsersFile(getAdminsPath(), adminUsers);
    }

    public static void writeBlockedFile(){
        writeUsersFile(getBlockedPath(), blockedUsers);
    }

    public static void addAdminUser(String user){
        adminUsers.put(user, user);
        writeAdminsFile();
    }

    public static void removeAdminUser(String user){
        if (adminUsers.remove(user) != null)
        writeAdminsFile();
    }

    public static void addBlockedUser(String user){
        blockedUsers.put(user, user);
        writeBlockedFile();
    }

    public static void removeBlockedUser(String user){
        if (blockedUsers.remove(user) != null)
            writeBlockedFile();
    }

    public static boolean isAdminUser(String user){
        return adminUsers.containsKey(user);
    }

    public static boolean isBlockedUser(String user){
        return blockedUsers.containsKey(user);
    }

    public Set<String> getAdminList(){
        return adminUsers.keySet();
    }

    public Set<String> getBlockedList(){
        return blockedUsers.keySet();
    }
}
