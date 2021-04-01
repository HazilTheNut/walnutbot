package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.Transcriber;

public class ConnectListCommand extends Command {

    @Override String getCommandKeyword() {
        return "list";
    }

    @Override public String getHelpDescription() {
        return "Lists all voice channels available to the bot.";
    }

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        StringBuilder builder = new StringBuilder("```List of available voice channels to connect to.\n"
            + "Format: \"<Server Name> <Channel Name>\"\n"
            + "Server and channel names with spaces are wrapped in quotation marks for ease of copy-and-pasting.\n\n");
        for (String s : botManager.getListOfVoiceChannels()) {
            String[] parts = s.split(":");
            for (String p : parts) {
                String part = p.trim();
                if (part.contains(" "))
                    builder.append('\"').append(part).append('\"');
                else
                    builder.append(part);
                builder.append(' ');
            }
            builder.append('\n');
        }
        builder.append("```");

        feedbackHandler.sendAuthorPM(builder.toString());
    }
}
