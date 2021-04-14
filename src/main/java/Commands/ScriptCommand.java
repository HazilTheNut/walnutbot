package Commands;

import Audio.AudioMaster;
import Utils.BotManager;
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

    @Override void onRunCommand(BotManager botManager, AudioMaster audioMaster, CommandFeedbackHandler feedbackHandler, byte permissions, String[] args) {
        if (argsInsufficient(args, 1, feedbackHandler))
            return;
        String expandedURI = FileIO.expandURIMacros(args[0]);
        if (sanitizeLocalAccess(expandedURI, feedbackHandler, permissions)){
            File scriptFile = new File(expandedURI);
            if (scriptFile.exists() && scriptFile.isFile()){
                CommandFeedbackHandler scriptFeedbackHandler = Transcriber.getGenericCommandFeedBackHandler(Transcriber.AUTH_CONSOLE);
                Thread scriptThread = new Thread(() -> {
                    try {
                        Scanner sc = new Scanner(scriptFile);
                        while (sc.hasNext()){
                            String command = sc.nextLine();
                            if (command.length() > 0) {
                                if (command.charAt(0) == '@')
                                    delay(command, audioMaster);
                                else if (command.charAt(0) != '#')
                                    commandInterpreter
                                        .evaluateCommand(command, scriptFeedbackHandler, permissions);
                            }
                        }
                        sc.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                });
                scriptThread.start();
            }
        }
    }

    private void delay(String delayString, AudioMaster audioMaster) {
        if (delayString.equals("@sb")){
                try {
                    do {
                        Thread.sleep(100);
                    } while (audioMaster.isSoundboardActive());
                } catch (InterruptedException e) {
                    e.printStackTrace();
            }
        } else if (delayString.equals("@jb")){
            try {
                do {
                    Thread.sleep(100);
                } while (audioMaster.isProcessingJukeboxRequests());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else{
            // Calculate number of milliseconds to wait
            long waitMs = 0;
            int pos = 1;
            String[] units = {"h", "m", "s", "ms"};
            long[] unitLengthsMs = {60 * 60 * 1000, 60 * 1000, 1000, 1};
            for (int i = 0; i < units.length; i++) {
                String unit = units[i];
                long unitLength = unitLengthsMs[i];
                if (pos < delayString.length()){
                    int unitPos = delayString.indexOf(unit, pos);
                    if (unitPos > 0) {
                        waitMs += Integer.parseInt(delayString.substring(pos, unitPos)) * unitLength;
                        pos = unitPos + unit.length();
                    }
                }
            }
            // Wait that many milliseconds
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

}
