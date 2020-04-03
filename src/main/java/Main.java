import Audio.AudioMaster;
import Commands.CommandInterpreter;
import UI.UIFrame;
import Utils.FileIO;
import Utils.SettingsLoader;
import Utils.Transcriber;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.entities.ActivityImpl;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import javax.swing.*;
import java.util.Random;

public class Main {

    public static void main(String[] args){
        Transcriber.startTranscription();
        System.out.println(FileIO.getRootFilePath());
        SettingsLoader.readSettings();
        String token = SettingsLoader.getValue("token");
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
        if (jda != null) {
            jda.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT, "sounds // type " + SettingsLoader.getValue("command_char") + "help"));
        }
    }
}
