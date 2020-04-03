import Audio.AudioMaster;
import UI.UIFrame;
import Utils.FileIO;
import Utils.Transcriber;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import javax.swing.*;

public class Main extends ListenerAdapter {

    public static void main(String[] args){
        Transcriber.startTranscription();
        System.out.println(FileIO.getRootFilePath());
        SettingsLoader.readSettings();
        String token = SettingsLoader.getValue("token");
        if (token == null) {
            System.out.println("WARNING! The token is missing from the config file! (Put \'token=...\' on a line in the file)");
        }
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.addEventListeners(new Main());
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
        if (jda == null)
            System.out.println("WARNING: JDA was null!");
        AudioMaster audioMaster = new AudioMaster();
        new UIFrame(jda, audioMaster);
    }

    @Override public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;
        /*
        System.out.printf("Event Message: \'%1$s\'\n", event.receiveMessage().getContentRaw());
        if (event.receiveMessage().getContentRaw().contains("$rand")) {
            Random random = new Random();
            event.getChannel().sendMessage(String.format("**RANDOM NUMBER:** %1$d", random.nextInt(32))).queue();
            event.getChannel().sendMessage(String.format("Attempted recursion: $rand", random.nextInt(32))).queue();
        }
        */
    }
}
