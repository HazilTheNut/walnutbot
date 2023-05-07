package Commands;

import Main.WalnutbotEnvironment;
import Utils.SettingsLoader;

import java.util.List;
import java.util.ListIterator;

public class ConnectListCommand extends Command {

    @Override String getCommandKeyword() {
        return "list";
    }

    @Override public String getHelpDescription() {
        return "Lists all voice channels available to the bot";
    }

    @Override
    void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        String message = "```List of available voice channels to connect to.\n"
                + "Format: \"<Server Name> <Channel Name>\"\n"
                + "Server and channel names with spaces are wrapped in quotation marks for ease of copy-and-pasting.\n\n"
                + listItems(environment.getCommunicationPlatformManager().getListOfVoiceChannels(), args[0], getHelpCommandUsage(),
                feedbackHandler.getListPageSize(CommandFeedbackHandler.CommandType.CONNECT))
                + "```";
        feedbackHandler.sendAuthorPM(message, false);
    }

    private String listItems(List<String> voiceChannels, String pageNumber, String helpCommandUsage, int pageSize){
        // Get page number
        int page = 1;
        if (pageNumber != null) {
            try {
                page = Math.max(1, Integer.valueOf(pageNumber));
            } catch (NumberFormatException ignored) { }
        }
        StringBuilder list = new StringBuilder();

        // Get number of pages
        int pagecount = (int) Math.ceil((float) voiceChannels.size() / pageSize);
        if (pagecount > 1)
            list.append(String.format("Page %1$d of %2$d:\n", page, pagecount));

        // List elements
        int baseAddr = (page - 1) * pageSize;
        ListIterator<String> iterator = voiceChannels.listIterator(baseAddr);
        for (int i = 0; i < pageSize; i++) {
            int addr = baseAddr + i;
            if (addr >= voiceChannels.size())
                break;
            for (String p : iterator.next().split(":")) {
                String part = p.trim();
                if (part.contains(" "))
                    list.append('\"').append(part).append('\"');
                else
                    list.append(part);
                list.append(' ');
            }
            list.append('\n');
        }

        // Remind usage of command to get more pages
        if (pagecount > 1)
            list.append("\nDo ").append(SettingsLoader.getBotConfigValue("command_char"))
                .append(helpCommandUsage).append(" to see further into the list.");
        // Return
        return list.toString();
    }
}
