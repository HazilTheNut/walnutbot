import Audio.AudioMaster;
import Commands.CommandInterpreter;
import UI.UIFrame;
import Utils.FileIO;
import Utils.SettingsLoader;
import Utils.Transcriber;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;
import javax.swing.*;

public class Main {

    public static void main(String[] args){
        Transcriber.startTranscription();
        System.out.println(FileIO.getRootFilePath());
        SettingsLoader.initialize();
        String token = SettingsLoader.getBotConfigValue("token");
        if (token == null) {
            System.out.println("WARNING! The token is missing from the config file! (Put \'token=...\' on a line in the file)");
        }
        JDABuilder builder = JDABuilder.createDefault(token);
        JDA jda = null;
        try {
            jda = builder.build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        AudioMaster audioMaster = new AudioMaster();
        if (jda == null)
            Transcriber.print("WARNING: JDA is null!");
        else
            jda.addEventListener(new CommandInterpreter(jda, audioMaster));
        new UIFrame(jda, audioMaster);
        if (jda != null)
            setupStatus(jda, Boolean.valueOf(SettingsLoader.getBotConfigValue("status_use_default")));
    }

    private static void setupStatus(JDA jda, boolean useDefault){
        if (useDefault){
            jda.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT, "sounds / type " + SettingsLoader.getBotConfigValue("command_char") + "help"));
        } else {
            try {
                Activity.ActivityType type;
                switch (SettingsLoader.getBotConfigValue("status_type").toUpperCase()){
                    case "WATCHING":
                        type = Activity.ActivityType.WATCHING;
                        break;
                    case "LISTENING":
                        type = Activity.ActivityType.LISTENING;
                        break;
                    case "EMPTY":
                        return;
                    default:
                    case "PLAYING":
                        type = Activity.ActivityType.DEFAULT;
                        break;
                }
                String message = SettingsLoader.getBotConfigValue("status_message");
                message = message.replaceAll("%help%", SettingsLoader.getBotConfigValue("command_char").concat("help"));
                jda.getPresence().setActivity(Activity.of(type, message));
            } catch (NullPointerException | IllegalArgumentException e){
                e.printStackTrace();
                setupStatus(jda, true);
            }
        }
    }
}
