package Commands;

import Audio.IAudioStateMachine;
import Main.WalnutbotEnvironment;
import Utils.FileIO;
import Utils.Transcriber;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ScriptCommand extends Command {

    private CommandInterpreter commandInterpreter;

    public ScriptCommand (CommandInterpreter commandInterpreter){
        this.commandInterpreter = commandInterpreter;
    }

    @Override String getCommandKeyword() {
        return "script";
    }

    @Override String getHelpArgs() {
        return "<file>";
    }

    @Override public String getHelpDescription() {
        return "Reads a text file as a series of commands";
    }

    @Override String getSpecificHelpDescription() {
        return getHelpDescription().concat(""
            + "\nNote: all commands in the script are ran with the same permissions as the user who ran the script command."
            + "\n\nfile - The path to the text file");
    }

    @Override void onRunCommand(WalnutbotEnvironment environment, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        String expandedURI = FileIO.expandURIMacros(args[0]);
        if (sanitizeLocalAccess(expandedURI, feedbackHandler, permissions)){
            Thread parseThread = new Thread(() -> {
                ScriptParser parser = new ScriptParser();
                parser.parseScriptFile(expandedURI, environment, feedbackHandler, permissions);
            });
            parseThread.start();
        }
    }
}
