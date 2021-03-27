package Commands;

/**
 * An interface that abstracts the process of sending text messages as a response to command input.
 */
public interface CommandFeedbackHandler {

    /**
     * Sends a public message in the same channel as where the command is found.
     *
     * @param message The message to send
     */
    void sendMessage(String message);

    /**
     * @return True if the channel where the command is found is a public space, rather than a form of private message
     */
    boolean isChannelPublic();

    /**
     * Sends a private message to the command author
     *
     * @param message The message to send
     */
    void sendAuthorPM(String message);

    /**
     * Gets a String describing the author of the command.
     *
     * @return A String describing the author of the command.
     */
    String getAuthor();
}
