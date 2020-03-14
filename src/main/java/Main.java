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
        String token = "NjI5NDEwODA0MDA1MDc3MDI5.XmwpKg.nzF2AMPyNSGzlPS3zDp0dRJPAtE";
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
        System.out.printf("Event Message: \'%1$s\'\n", event.getMessage().getContentRaw());
        if (event.getMessage().getContentRaw().contains("$rand")) {
            Random random = new Random();
            event.getChannel().sendMessage(String.format("**RANDOM NUMBER:** %1$d", random.nextInt(32))).queue();
        }
    }
}
