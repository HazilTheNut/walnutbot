package Commands;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import Utils.Transcriber;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.collections4.set.ListOrderedSet;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CommandInterpreter extends ListenerAdapter {

    private HashMap<String, Command> commandMap;
    private JDA jda;
    private AudioMaster audioMaster;

    public CommandInterpreter(JDA jda, AudioMaster audioMaster){
        commandMap = new HashMap<>();
        this.jda = jda;
        this.audioMaster = audioMaster;
        audioMaster.setCommandInterpreter(this);
        //Add commands to map
        addCommand(new HelpCommand(this));
        addCommand(new RequestCommand());
        addCommand(new SoundboardCommand());
        addCommand(new EchoCommand(this));
    }

    public List<Command> getExpandedCommandList(){
        LinkedList<Command> list = new LinkedList<>();
        for (Command command : commandMap.values())
            getExpandedCommandListHelper(command, list);
        return list;
    }

    private void getExpandedCommandListHelper(Command command, List<Command> runningList){
        runningList.add(command);
        for (Command subcommand : command.getSubCommands())
            getExpandedCommandListHelper(subcommand, runningList);
    }

    private void addCommand(Command command){
        commandMap.put(command.getCommandName(), command);
        command.setCommandTreeStr(command.getCommandName());
        command.updateSubCommandTreeStr();
    }

    public int getLongestCommandHelpName(){
        int max = 0;
        for (Command command : getExpandedCommandList())
            max = Math.max(max, command.getHelpName().length());
        return max;
    }

    public String getCommandAllowanceSettingName(String command){
        return String.format("allowCommand_%1$s", command);
    }

    @Override public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        //Transcriber.print("Raw message: \"%1$s\"", event.getMessage().getContentRaw());
        //Input sanitation
        if (!Boolean.valueOf(SettingsLoader.getBotConfigValue("accept_bot_messages")) && event.getAuthor().isBot())
            return;
        //Fetch command character

        //Run command if incoming message starts with the command character
        String messageContent = event.getMessage().getContentRaw();
        evaluateCommand(messageContent, new DiscordCommandFeedbackHandler(event), Command.USER_MASK);
    }

    private void evaluateCommand(String commandText, CommandFeedbackHandler commandFeedbackHandler, byte authorPermission){
        String commandCharStr = SettingsLoader.getBotConfigValue("command_char");
        if (commandCharStr == null || commandCharStr.length() <= 0) {
            Transcriber.print("WARNING! config.txt malformed - \'command_char\' is missing!");
            return;
        }
        if (commandText.length() > commandCharStr.length() && commandText.substring(0, commandCharStr.length()).equals(commandCharStr)){
            String commandStr = commandText.substring(commandCharStr.length()); //Clip off the command character from the rest of the command
            Transcriber.print("command received: \'%1$s\' (from %2$s)", commandText, commandFeedbackHandler.getAuthor());

            String[] parts = commandStr.split(" ");
            if (commandMap.containsKey(parts[0])){ //If command is valid
                if (!Boolean.valueOf(SettingsLoader.getSettingsValue(getCommandAllowanceSettingName(parts[0]), "true"))) {
                    commandFeedbackHandler.sendMessage("**WARNING:** This bot's admin has blocked usage of this command.");
                    return;
                }
                String[] subarray = removeFirstElement(parts); //Either the command arguments or a command-args group of a subcommand
                searchAndRunCommand(subarray, 0, commandMap.get(parts[0]), commandFeedbackHandler, authorPermission);
            } else {
                Transcriber.printAndPost(commandFeedbackHandler, "**ERROR:** Command `%1$s` was not recognized!", parts[0]);
            }
        }
    }

    private void searchAndRunCommand(String[] args, int depth, Command baseCommand, CommandFeedbackHandler feedbackHandler, byte authorPermission){
        Command foundSubCommand = null;
        if (depth < args.length) {
            for (Command command : baseCommand.getSubCommands()) {
                if (command.getCommandName().equals(args[depth])) {
                    foundSubCommand = command;
                    break;
                }
            }
        }
        if (foundSubCommand != null) {
            String[] subarray = removeFirstElement(args); //The command-args group of a subcommand
            searchAndRunCommand(subarray, depth + 1, foundSubCommand, feedbackHandler, authorPermission);
        } else {
            if (baseCommand.isPermissionSufficient(authorPermission))
                baseCommand.onRunCommand(jda, audioMaster, feedbackHandler, authorPermission, args);
            else
                feedbackHandler.sendMessage("**WARNING:** You do not have permission to run this command!");
        }
    }

    private String[] removeFirstElement(String[] arr){
        if (arr == null || arr.length <= 1)
            return new String[]{""};
        String[] subarray = new String[arr.length-1]; //Either the command arguments or a command-args group of a subcommand
        System.arraycopy(arr, 1, subarray, 0, subarray.length);
        return subarray;
    }

    private class DiscordCommandFeedbackHandler implements CommandFeedbackHandler {

        MessageReceivedEvent event;

        public DiscordCommandFeedbackHandler(MessageReceivedEvent event) {
            this.event = event;
        }

        @Override public void sendMessage(String message) {
            (event.getChannel().sendMessage(message)).queue(); //I can't remember why I did the extra parenthesis wrapping, but I'm too scared to try it without.
        }

        /**
         * @return True if the channel where the command is found is a public space, rather than a form of private message
         */
        @Override public boolean isChannelPublic() {
            return event.getChannel().getType().isGuild();
        }

        /**
         * Sends a private message to the command author
         *
         * @param message The message to send
         */
        @Override public void sendAuthorPM(String message) {
            if (isChannelPublic())
                (event.getChannel().sendMessage(String.format("<@%1$s> I have sent a PM to you.", event.getAuthor().getId()))).queue();
            (event.getAuthor().openPrivateChannel().complete().sendMessage(message)).queue();
        }

        /**
         * Gets a String describing the author of the command.
         *
         * @return A String describing the author of the command.
         */
        @Override public String getAuthor() {
            return event.getAuthor().getAsTag();
        }
    }
}
