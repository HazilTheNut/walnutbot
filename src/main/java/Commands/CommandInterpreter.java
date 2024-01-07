package Commands;

import Main.WalnutbotEnvironment;
import Main.WalnutbotInfo;
import Utils.*;
import java.util.*;

public class CommandInterpreter {

    private final HashMap<String, Command> commandMap;
    private final WalnutbotEnvironment environment;

    public CommandInterpreter(WalnutbotEnvironment environment){
        commandMap = new HashMap<>();
        this.environment = environment;
        //Add commands to map
        addCommand(new HelpCommand());
        addCommand(new RequestCommand());
        addCommand(new SoundboardCommand());
        addCommand(new ConnectCommand());
        addCommand(new DisconnectCommand());
        addCommand(new VolumeCommand());
        addCommand(new PermissionsCommand());
        addCommand(new ScriptCommand(this));
        addCommand(new GenericCommand("status", "Prints out the current status of the bot", ((environment1, feedbackHandler) -> {
            String channelStr = environment1.getCommunicationPlatformManager().connectedVoiceChannelToString();
            StringBuilder jukeboxDefaultListName = new StringBuilder();
            environment1.getAudioStateMachine().getJukeboxDefaultList().accessAudioKeyPlaylist(playlist -> jukeboxDefaultListName.append(playlist.getUrl()));
            StringBuilder jukeboxQueueLength = new StringBuilder();
            environment1.getAudioStateMachine().getJukeboxQueue().accessAudioKeyPlaylist(playlist -> jukeboxQueueLength.append(playlist.getAudioKeys().size()));
            Transcriber.printAndPost(feedbackHandler,
                "**Bot Status:**\n```\n"
                    + "Version: %6$s\n"
                    + "Volumes: sb=%1$d jb=%2$d\n"
                    + "Connected Channel: %3$s\n"
                    + "Jukebox Default List: %4$s\n"
                    + "Jukebox Queue Length: %5$s\n```",
                    environment1.getAudioStateMachine().getSoundboardVolume(), environment1.getAudioStateMachine().getJukeboxVolume(), channelStr,
                jukeboxDefaultListName, jukeboxQueueLength,
                WalnutbotInfo.VERSION_NUMBER);
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

    public void receiveMessageFromCommunicationPlatform(String message, CommandFeedbackHandler commandFeedbackHandler, byte userPermission) {
        if (isAPrefixedCommand(message))
            evaluateCommand(removeCommandChar(message), commandFeedbackHandler, userPermission);
    }

    public void readHeadlessInput(){
        //Thread inputThread = new Thread(() -> {
           Scanner sc = new Scanner(System.in);
           String input;
           String exitWord = "exit";
           do {
               input = sc.nextLine();
               if (!input.equals(exitWord))
                   evaluateCommand(input, new ConsoleCommandFeedbackHandler(), Command.INTERNAL_MASK);
           } while (!input.equals(exitWord));
           System.exit(0);
        //});
        //inputThread.start();
    }

    private boolean isAPrefixedCommand(String commandRawText){
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
            if (!Boolean.parseBoolean(SettingsLoader.getSettingsValue(getCommandAllowanceSettingName(parts[0]), "true"))) {
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
                baseCommand.onRunCommand(environment, feedbackHandler, authorPermission, args);
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
}
