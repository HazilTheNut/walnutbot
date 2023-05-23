package Commands;

/**
 * An interface that abstracts the process of sending text messages as a response to command input.
 */
public interface CommandFeedbackHandler {

    /**
     * Sends a public message in the same channel as where the command is found.
     *
     * @param message The message to send
     * @param isCopiedToConsole Whether the message is copied to this bot's System.out
     */
    void sendMessage(String message, boolean isCopiedToConsole);

    /**
     * @return True if the channel where the command is found is a public space, rather than a form of private message
     */
    boolean isChannelPublic();

    /**
     * Sends a private message to the command author
     *
     * @param message The message to send
     * @param isCopiedToConsole Whether the message is copied to this bot's System.out
     */
    void sendAuthorPM(String message, boolean isCopiedToConsole);

    /**
     * Gets a String describing the author of the command.
     *
     * @return A String describing the author of the command.
     */
    String getAuthor();


    enum CommandType {
        HELP, QUEUE, DEFAULT, CONNECT
    }

    /**
     * Returns the size of pages to display for listing commands such as help, jb list, etc.
     *
     * @param commandType The command to distinguish page sizes for
     * @return The number of elements to list on a given page.
     */
    int getListPageSize(CommandType commandType);
}
