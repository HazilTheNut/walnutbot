package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.SettingsLoader;
import Utils.Transcriber;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class Command {

    public static final byte USER_MASK  = 0x01;     //For non-admin users
    public static final byte ADMIN_MASK = 0x02;     //For admin users
    public static final byte INTERNAL_MASK = 0x04;  //For the bot sending commands to itself
    public static final byte BLOCKED_MASK = 0x00;   //For blocked users

    private String commandTreeStr; // A String describing the command as a subcommand of a another one, such as "super sub"
    private List<Command> subcommands;

    String getCommandKeyword(){
        return "-";
    }

    String getHelpArgs(){
        return "";
    }

    String getHelpCommandUsage(){
        return String.format("%1$s %2$s", commandTreeStr, getHelpArgs()).trim();
    }

    public String getPermissionName() {
        return String.format("perm_%s", commandTreeStr.trim().replace(' ', '_'));
    }

    public String getHelpDescription(){
        return "-";
    }

    String getSpecificHelpDescription(){
        return getHelpDescription();
    }

    void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args){
        // Override this for command's behavior
    }

    public String getCommandTreeStr() {
        return commandTreeStr;
    }

    public void setCommandTreeStr(String commandTreeStr) {
        this.commandTreeStr = commandTreeStr;
    }

    void addSubCommand(Command subcommand){
        if (subcommands == null)
            subcommands = new LinkedList<>();
        subcommands.add(subcommand);
    }

    void updateSubCommandTreeStr(){
        if (subcommands == null)
            subcommands = new LinkedList<>();
        for (Command subcommand : subcommands){
            subcommand.setCommandTreeStr(String.format("%1$s %2$s", commandTreeStr, subcommand.getCommandKeyword()));
            subcommand.updateSubCommandTreeStr();
        }
    }

    @Nonnull
    List<Command> getSubCommands(){
        if (subcommands == null)
            subcommands = new LinkedList<>();
        return subcommands;
    }

    /**
     * Returns true if the input permission is enough to run this command.
     *
     * @param permission The permission in question. Use of USER_MASK and ADMIN_MASK is encouraged for this parameter.
     * @return If the AuthorPermission is sufficient to run this command
     */
    boolean isPermissionSufficient(byte permission){
        if ((permission & INTERNAL_MASK) != 0) return true;
        //if ((permission & BLOCKED_MASK) != 0) return false;
        byte settingsPerms = Byte.valueOf(SettingsLoader.getSettingsValue(getPermissionName(), "3"));
        return (permission & settingsPerms) != 0;
    }

    /**
     * Helper method for easier use; it checks the input URI against the permissions of the author and returns true if the input is allowed.
     *
     * @param uri The URI to check for instances of attempts at accessing the local disk without permission
     * @param feedbackHandler The CommandFeedbackHandler to state that a certain action is disallowed if it is.
     * @param authorPermission The permission vector of the command author.
     * @return True if the author is allowed to use the URI.
     */
    boolean sanitizeLocalAccess(String uri, CommandFeedbackHandler feedbackHandler, byte authorPermission){
        if ((authorPermission & INTERNAL_MASK) != 0) return true;
        boolean uriAllowed = ((authorPermission & Command.ADMIN_MASK) == Command.ADMIN_MASK);
        uriAllowed |= Boolean.valueOf(SettingsLoader.getSettingsValue("discordAllowLocalAccess", "false"));
        uriAllowed |= uri.indexOf("http") == 0;
        if (!uriAllowed)
            Transcriber.printAndPost(feedbackHandler, "**WARNING:** This bot's admin has blocked access to local files.");
        return uriAllowed;
    }

    /**
     * Checks to see if there are not enough arguments in the arguments array.
     * If there isn't, the method will automatically print and post a formatted message about it.
     *
     * The intended usage of this method is:
     * @code if (argsInsufficient(args, n)) return;
     *
     * @param args The array of String arguments
     * @param minLength The minimum number of arguments
     * @return True if there are not enough arguments and false otherwise.
     */
    boolean argsInsufficient(String[] args, int minLength, CommandFeedbackHandler feedbackHandler){
        if (args.length < minLength){
            Transcriber.printAndPost(feedbackHandler, "**ERROR:** Not enough arguments. Usage: `%1$s`", getHelpCommandUsage());
            return true;
        }
        return false;
    }
}
