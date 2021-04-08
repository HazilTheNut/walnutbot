import Audio.AudioMaster;
import Commands.CommandInterpreter;
import UI.UIFrame;
import Utils.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import javax.swing.*;

public class Main {

    public static void main(String[] args){

        // Read CLA's
        boolean headlessMode = false;
        for (String arg : args)
            if(arg.equals("-headless"))
                headlessMode = true;

        Transcriber.startTranscription(headlessMode);
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
        AudioMaster audioMaster = new AudioMaster();
        if (jda == null)
            Transcriber.printTimestamped("WARNING: JDA is null!");
        BotManager botManager = null;
        if (jda != null){
            botManager = new DiscordBotManager(jda, audioMaster);
            botManager.updateStatus();
            audioMaster.setDiscordBotManager(botManager);
        }
        CommandInterpreter commandInterpreter = null;
        if (botManager != null) {
            commandInterpreter = new CommandInterpreter(botManager, audioMaster);
            jda.addEventListener(commandInterpreter);
        }
        if (headlessMode) {
            if (commandInterpreter != null)
                commandInterpreter.readHeadlessInput();
            else
                System.out.println("ERROR: Discord bot did not properly initialize!");
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            new UIFrame(botManager, audioMaster, commandInterpreter, (jda != null));
        }
    }
}
