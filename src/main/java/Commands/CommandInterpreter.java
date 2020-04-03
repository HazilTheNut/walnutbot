package Commands;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import Utils.Transcriber;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class CommandInterpreter extends ListenerAdapter {

    private HashMap<String, Command> commandMap;
    private JDA jda;
    private AudioMaster audioMaster;

    public CommandInterpreter(JDA jda, AudioMaster audioMaster){
        commandMap = new HashMap<>();
        this.jda = jda;
        this.audioMaster = audioMaster;
        //Add commands to map
        addCommand(new HelpCommand(this));
        addCommand(new SoundboardCommand());
        addCommand(new SoundboardListCommand());
        addCommand(new InstantPlayCommand());
    }

    private void addCommand(Command command){
        commandMap.put(command.getCommandName(), command);
    }

    public int getLongestCommandHelpName(){
        int max = 0;
        for (String command : commandMap.keySet())
            max = Math.max(max, commandMap.get(command).getHelpName().length());
        return max;
    }

    public HashMap<String, Command> getCommandMap() {
        return commandMap;
    }

    @Override public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        //Transcriber.print("Raw message: \"%1$s\"", event.getMessage().getContentRaw());
        //Input sanitation
        if (event.getAuthor().isBot())
            return;
        //Fetch command character
        String commandCharStr = SettingsLoader.getValue("command_char");
        char command_char;
        if (commandCharStr == null || commandCharStr.length() <= 0) {
            Transcriber.print("WARNING! config.txt malformed - \'command_char\' is missing!");
            return;
        } else {
            command_char = commandCharStr.charAt(0);
            //Transcriber.print("command_char=\'%1$c\'", command_char);
        }
        //Run command if incoming message starts with the command character
        String messageContent = event.getMessage().getContentRaw();
        if (messageContent.length() > 0 && messageContent.charAt(0) == command_char){
            String commandStr = messageContent.substring(1); //Clip off the command character from the rest of the command
            String[] parts = commandStr.split(" ");
            if (commandMap.containsKey(parts[0])){ //If command is valid
                String[] args = new String[parts.length-1]; //Command arguments are the same as the parts array aside from the first element
                System.arraycopy(parts, 1, args, 0, parts.length - 1);
                commandMap.get(parts[0]).onRunCommand(jda, audioMaster, event, args);
            } else {
                Transcriber.printAndPost(event.getChannel(), "**ERROR:** Command `%1$s` was not recognized!", parts[0]);
            }
        }
    }
}
