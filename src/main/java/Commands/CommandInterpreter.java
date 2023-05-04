package Commands;

import Audio.AudioMaster;
import Utils.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.*;

public class CommandInterpreter extends ListenerAdapter {

    private HashMap<String, Command> commandMap;
    private IBotManager botManager;
    private AudioMaster audioMaster;

    public CommandInterpreter(IBotManager botManager, AudioMaster audioMaster){
        commandMap = new HashMap<>();
        this.botManager = botManager;
        this.audioMaster = audioMaster;
        audioMaster.setCommandInterpreter(this);
        //Add commands to map
        addCommand(new HelpCommand(this));
        addCommand(new RequestCommand());
        addCommand(new SoundboardCommand());
        addCommand(new ConnectCommand());
        addCommand(new DisconnectCommand());
        addCommand(new VolumeCommand());
        addCommand(new PermissionsCommand());
        addCommand(new ScriptCommand(this));
        addCommand(new GenericCommand("status", "Prints out the current status of the bot", ((audioMaster1, feedbackHandler) -> {
            String channelStr = (audioMaster.getConnectedChannel() == null) ? "null" : String.format("%1$s : %2$s",
                audioMaster.getConnectedChannel().getGuild(), audioMaster.getConnectedChannel().getName());
            Transcriber.printAndPost(feedbackHandler,
                "**Bot Status:**\n```\n"
                    + "Version: %6$s\n"
                    + "Raw Volumes: sb=%1$d jb=%2$d\n"
                    + "Connected Channel: %3$s\n"
                    + "Jukebox Default List: %4$s\n"
                    + "Jukebox Queue Length: %5$s\n```",
                audioMaster.getSoundboardPlayer().getVolume(), audioMaster.getJukeboxPlayer().getVolume(), channelStr,
                audioMaster.getJukeboxDefaultListName(), audioMaster.getJukeboxQueueList().getAudioKeys().size(),
                BotInfo.VERSION_NUMBER);
        })));
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
        commandMap.put(command.getCommandKeyword(), command);
        command.setCommandTreeStr(command.getCommandKeyword());
        command.updateSubCommandTreeStr();
    }

    public String getCommandAllowanceSettingName(String command){
        return String.format("allowCommand_%1$s", command);
    }

    @Override public void onMessageReceived(MessageReceivedEvent event) {
        //Transcriber.printTimestamped("Raw message: \"%1$s\"", event.getMessage().getContentRaw());

        //Input sanitation
        //Transcriber.printTimestamped("My name: \'%1$s\"", botManager.getBotName());
        if (!Boolean.parseBoolean(SettingsLoader.getBotConfigValue("accept_bot_messages")) && event.getAuthor().isBot())
            return;
        //Ensure the bot doesn't get stuck in response loops
        if (event.getAuthor().getAsTag().equals(botManager.getBotName()))
            return;

        //Run command if incoming message starts with the command character
        String messageContent = event.getMessage().getContentRaw();
        if (isADiscordCommand(messageContent))
            evaluateCommand(removeCommandChar(messageContent), new DiscordCommandFeedbackHandler(event), getUserPermissions(event.getAuthor().getAsTag()));
    }

    public void readHeadlessInput(){
        Thread inputThread = new Thread(() -> {
           Scanner sc = new Scanner(System.in);
           String input;
           String exitWord = "exit";
           do {
               input = sc.nextLine();
               if (!input.equals(exitWord))
                   evaluateCommand(input, new ConsoleCommandFeedbackHandler(), Command.INTERNAL_MASK);
           } while (!input.equals(exitWord));
           System.exit(0);
        });
        inputThread.start();
    }

    private byte getUserPermissions(String username){
        if (SettingsLoader.isAdminUser(username))
            return Command.ADMIN_MASK;
        if (SettingsLoader.isBlockedUser(username))
            return Command.BLOCKED_MASK;
        return Command.USER_MASK;
    }

    private boolean isADiscordCommand(String commandRawText){
        String commandCharStr = SettingsLoader.getBotConfigValue("command_char");
        if (commandCharStr == null) return false;
        return commandRawText.length() > commandCharStr.length() && commandRawText.substring(0, commandCharStr.length()).equals(commandCharStr);
    }

    private String removeCommandChar(String commandRawText){
        String commandCharStr = SettingsLoader.getBotConfigValue("command_char");
        if (commandCharStr == null || commandCharStr.length() <= 0) {
            Transcriber.printRaw("WARNING! config.ini malformed - \'command_char\' is missing!");
            return commandRawText;
        }
        if (commandRawText.length() > commandCharStr.length() && commandRawText.substring(0, commandCharStr.length()).equals(commandCharStr)) {
            return commandRawText.substring(commandCharStr.length());
        }
        return commandRawText;
    }

    /**
     * Evaluates a String as a command to the discord bot.
     * The input String is a space (' ') separated list of command and subcommand keywords followed by the command arguments.
     * Additionally, the input String does not need to be preceded with the command_char described in config.txt
     *
     * @param commandText The text of the command.
     * @param commandFeedbackHandler The CommandFeedbackHandler, which takes various forms of output from the Command and displays it however it wishes to.
     * @param authorPermission The byte describing the command author's level of permission.
     */
    public void evaluateCommand(String commandText, CommandFeedbackHandler commandFeedbackHandler, byte authorPermission){
        String author = commandFeedbackHandler.getAuthor();
        if (author.length() < 1)
            Transcriber.printTimestamped("> %1$s", commandText);
        else
            Transcriber.printTimestamped("%1$s > %2$s", author, commandText);
        String[] parts = splitCommandStr(commandText);
        if (commandMap.containsKey(parts[0])){ //If command is valid
            if (!Boolean.valueOf(SettingsLoader.getSettingsValue(getCommandAllowanceSettingName(parts[0]), "true"))) {
                Transcriber.printAndPost(commandFeedbackHandler, "**WARNING:** This bot's admin has blocked usage of this command.");
                return;
            }
            String[] subarray = removeFirstElement(parts); //Either the command arguments or a command-args group of a subcommand
            searchAndRunCommand(subarray, 0, commandMap.get(parts[0]), commandFeedbackHandler, authorPermission);
        } else {
            Transcriber.printAndPost(commandFeedbackHandler, "**ERROR:** Command `%1$s` was not recognized!", parts[0]);
        }
    }

    private void searchAndRunCommand(String[] args, int depth, Command baseCommand, CommandFeedbackHandler feedbackHandler, byte authorPermission){
        Command foundSubCommand = null;
        for (Command command : baseCommand.getSubCommands()) {
            if (command.getCommandKeyword().equals(args[0])) {
                foundSubCommand = command;
                break;
            }
        }
        if (foundSubCommand != null) {
            String[] subarray = removeFirstElement(args); //The command-args group of a subcommand
            searchAndRunCommand(subarray, depth + 1, foundSubCommand, feedbackHandler, authorPermission);
        } else {
            if (baseCommand.isPermissionSufficient(authorPermission))
                baseCommand.onRunCommand(botManager, audioMaster, feedbackHandler, authorPermission, args);
            else
                Transcriber.printAndPost(feedbackHandler, "**WARNING:** You do not have permission to run this command!");
        }
    }

    private String[] splitCommandStr(String commandStr){
        String filteredText = removeCommandChar(commandStr);
        ArrayList<String> partsList = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        boolean withinQuotations = false;
        for (int i = 0; i < filteredText.length(); i++) {
            char c = filteredText.charAt(i);
            if (c == ' '){
                if (withinQuotations)
                    builder.append(c);
                else if (builder.length() > 0){
                    partsList.add(builder.toString());
                    builder.setLength(0);
                }
            } else if (c == '\\'){
                i++;
                if (i < filteredText.length())
                    builder.append(filteredText.charAt(i));
            } else if (c == '\"'){
                if (withinQuotations){
                    partsList.add(builder.toString());
                    builder.setLength(0);
                    withinQuotations = false;
                } else {
                    withinQuotations = true;
                }
            } else
                builder.append(c);
        }
        partsList.add(builder.toString());
        // Assemble array of strings
        String[] parts = new String[partsList.size()];
        for (int i = 0; i < partsList.size(); i++) parts[i] = partsList.get(i);
        return parts;
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

        /**
         * Sends a public message in the same channel as where the command is found.
         *
         * @param message           The message to send
         * @param isCopiedToConsole Whether or not the message is copied to this bot's System.out
         */
        @Override public void sendMessage(String message, boolean isCopiedToConsole) {
            event.getChannel().sendMessage(message).queue();
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
         * @param message           The message to send
         * @param isCopiedToConsole Whether or not the message is copied to this bot's System.out
         */
        @Override public void sendAuthorPM(String message, boolean isCopiedToConsole) {
            if (isChannelPublic())
                event.getChannel().sendMessage(String.format("<@%1$s> I have sent a PM to you.", event.getAuthor().getId())).queue();
            event.getAuthor().openPrivateChannel().complete().sendMessage(message).queue();
        }

        /**
         * Gets a String describing the author of the command.
         *
         * @return A String describing the author of the command.
         */
        @Override public String getAuthor() {
            return event.getAuthor().getAsTag();
        }

        /**
         * Returns the size of pages to display for listing commands such as help, jb list, etc.
         *
         * @param commandType The command to distinguish page sizes for
         * @return The number of elements to list on a given page.
         */
        @Override public int getListPageSize(CommandType commandType) {
            switch (commandType){
                case HELP: return 15;
                case QUEUE: return 10;
                case DEFAULT: return 10;
                case CONNECT: return 15;
                default: return 10;
            }
        }
    }
}
