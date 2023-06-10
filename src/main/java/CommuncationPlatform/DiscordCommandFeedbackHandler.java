package CommuncationPlatform;

import Commands.CommandFeedbackHandler;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DiscordCommandFeedbackHandler implements CommandFeedbackHandler {

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
            case HELP:
            case CONNECT:
                return 15;
            case QUEUE:
            case DEFAULT:
            default:
                return 10;
        }
    }
}
