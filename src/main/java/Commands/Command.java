package Commands;

import Audio.AudioMaster;
import Utils.SettingsLoader;
import net.dv8tion.jda.api.JDA;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Command {

    static final byte USER_MASK  = 0x01;
    static final byte ADMIN_MASK = 0x02;

    private String commandTreeStr; // A String describing the command as a subcommand of a another one, such as "super sub"
    private List<Command> subcommands;

    String getCommandName(){
        return "-";
    }

    String getHelpArgs(){
        return "";
    }

    String getHelpName(){
        return String.format("%1$s %2$s", commandTreeStr, getHelpArgs()).trim();
    }

    public String getPermissionName() {
        return String.format("perm_%s", commandTreeStr.trim().replace(' ', '_'));
    }

    public String getHelpDescription(){
        return "-";
    }

    String getSpecificHelpDescription(){
        return "-";
    }

    void onRunCommand(JDA jda, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args){
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
            subcommand.setCommandTreeStr(String.format("%1$s %2$s", commandTreeStr, subcommand.getCommandName()));
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
}
