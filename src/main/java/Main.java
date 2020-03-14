import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import javax.swing.*;
import java.util.Random;

public class Main extends ListenerAdapter {

    public static void main(String[] args){
        Transcriber transcriber = new Transcriber();
        transcriber.startTranscription();
        System.out.println(FileIO.getRootFilePath());
        SettingsLoader.readSettings();
        String token = SettingsLoader.getValue("token");
        if (token == null) {
            System.out.println("WARNING! The token is missing from the config file! (Put \'token=...\' on a line in the file)");
        }
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.addEventListeners(new Main());
        try {
            builder.build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        new UIFrame();
    }

    @Override public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;
        System.out.printf("Event Message: \'%1$s\'\n", event.getMessage().getContentRaw());
        if (event.getMessage().getContentRaw().contains("$rand")) {
            Random random = new Random();
            event.getChannel().sendMessage(String.format("**RANDOM NUMBER:** %1$d", random.nextInt(32))).queue();
            event.getChannel().sendMessage(String.format("Attempted recursion: $rand", random.nextInt(32))).queue();
        }
    }
}
