package Commands;

import Main.WalnutbotEnvironment;
import Utils.SettingsLoader;
import Utils.Transcriber;

public class PermissionsCommand extends Command {

    @Override String getCommandKeyword() {
        return "perms";
    }

    @Override String getHelpArgs() {
        return "<admin|blocked> <add|remove> <user>";
    }

    @Override public String getHelpDescription() {
        return "Manages user permissions";
    }

    @Override String getSpecificHelpDescription() {
        return getHelpDescription().concat("\n\n"
            + "admin|blocked - Switch for modifying either the admins or the blocked users list\n"
            + "add|remove - Whether to add or remove the user from the\n"
            + "user - The name of the user to modify their permissions\n");
    }

    @Override void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 3, feedbackHandler))
            return;
        String list = args[0];
        String opp  = args[1];
        String user = args[2];
        switch (list) {
            case "admin":
                switch (opp) {
                    case "add":
                        SettingsLoader.addAdminUser(user);
                        Transcriber.printAndPost(feedbackHandler, "User `%1$s` promoted to admin user.", user);
                        break;
                    case "remove":
                        SettingsLoader.removeAdminUser(user);
                        Transcriber.printAndPost(feedbackHandler, "User `%1$s` has been demoted.", user);
                        break;
                    default:
                        Transcriber.printAndPost(feedbackHandler, "**ERROR:** `%1$s` is not a valid operation.", opp);
                        break;
                }
                break;
            case "blocked":
                switch (opp) {
                    case "add":
                        SettingsLoader.addBlockedUser(user);
                        Transcriber.printAndPost(feedbackHandler, "User `%1$s` has now been blocked access to this bot.", user);
                        break;
                    case "remove":
                        SettingsLoader.removeBlockedUser(user);
                        Transcriber.printAndPost(feedbackHandler, "User `%1$s` is no longer blocked from using this bot.", user);
                        break;
                    default:
                        Transcriber.printAndPost(feedbackHandler, "**ERROR:** `%1$s` is not a valid operation.", opp);
                        break;
                }
                break;
            default:
                Transcriber.printAndPost(feedbackHandler, "**ERROR:** `%1$s` is not an operable list.", list);
                break;
        }
    }
}
