import Audio.AudioMaster;
import Commands.Command;
import Commands.CommandInterpreter;
import UI.UIFrame;
import Utils.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;

public class Main {

    public static void main(String[] args){

        // Read CLA's
        boolean headlessMode = false;
        String startupScriptLoc = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-headless"))
                headlessMode = true;
            else if (arg.equals("-run") && i < args.length - 1)
                startupScriptLoc = args[i+1];
        }

        // Load settings and start logging
        Transcriber.startTranscription(headlessMode);
        Date now = new Date();
        System.out.printf("BEGIN of Walnutbot (time: %1$s)\n\tResiding in %2$s\n---\n",
                (new SimpleDateFormat("MM/dd/yyyy kk:mm:ss")).format(now),
                FileIO.getRootFilePath());
        SettingsLoader.initialize();

        // Validate bot token
        String token = SettingsLoader.getBotConfigValue("token");
        if (token == null) {
            System.out.println("WARNING! The token is missing from the config file! (Put \'token=...\' on a line in the file)");
        }

        // Connect to discord bot
        EnumSet<GatewayIntent> intents = EnumSet.of(
                // To accept commands from users
                GatewayIntent.GUILD_MESSAGES,
                // To send direct messages if DMing the bot
                GatewayIntent.DIRECT_MESSAGES,
                // We need voice states to connect to the voice channel
                GatewayIntent.GUILD_VOICE_STATES
        );

        JDA jda = JDABuilder.createDefault(token, intents)
                .enableCache(CacheFlag.VOICE_STATE)
                .build();

        // Start up bot core
        AudioMaster audioMaster = new AudioMaster();
        if (jda == null)
            Transcriber.printTimestamped("WARNING: JDA is null!");
        IBotManager botManager = null;
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

        // Startup script
        if (commandInterpreter != null && startupScriptLoc != null)
            commandInterpreter.evaluateCommand("script ".concat(startupScriptLoc), Transcriber.getGenericCommandFeedBackHandler("Startup"), Command.INTERNAL_MASK);
    }
}
