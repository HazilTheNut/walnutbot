package Utils;

import Commands.CommandFeedbackHandler;

public class ConsoleCommandFeedbackHandler implements CommandFeedbackHandler {

    /**
     * Sends a public message in the same channel as where the command is found.
     *
     * @param message           The message to send
     * @param isCopiedToConsole Whether or not the message is copied to this bot's System.out
     */
    @Override public void sendMessage(String message, boolean isCopiedToConsole) {
        if (!isCopiedToConsole)
            Transcriber.printRaw(message);
    }

    /**
     * @return True if the channel where the command is found is a public space, rather than a form of private message
     */
    @Override public boolean isChannelPublic() {
        return false;
    }

    /**
     * Sends a private message to the command author
     *
     * @param message           The message to send
     * @param isCopiedToConsole Whether or not the message is copied to this bot's System.out
     */
    @Override public void sendAuthorPM(String message, boolean isCopiedToConsole) {
        if (!isCopiedToConsole)
            Transcriber.printRaw(message);
    }

    /**
     * Gets a String describing the author of the command.
     *
     * @return A String describing the author of the command.
     */
    @Override public String getAuthor() {
        return Transcriber.AUTH_CONSOLE;
    }

    /**
     * Returns the size of pages to display for listing commands such as help, jb list, etc.
     *
     * @param commandType The command to distinguish page sizes for
     * @return The number of elements to list on a given page.
     */
    @Override public int getListPageSize(CommandType commandType) {
        return Integer.MAX_VALUE;
    }
}
