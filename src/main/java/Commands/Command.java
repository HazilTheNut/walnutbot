package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
import Utils.SettingsLoader;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class Command {

    public static final byte USER_MASK  = 0x01;
    public static final byte ADMIN_MASK = 0x02;

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
        boolean uriAllowed = ((authorPermission & Command.ADMIN_MASK) == Command.ADMIN_MASK) ||
            Boolean.valueOf(SettingsLoader.getSettingsValue("discordAllowLocalAccess", "false")) ||
            !uri.contains("http");
        if (!uriAllowed)
            feedbackHandler.sendMessage("**WARNING:** This bot's admin has blocked access to local files.");
        return uriAllowed;
    }
}
