package Commands;

import Audio.IAudioStateMachine;
import Main.WalnutbotEnvironment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class ScriptParser {

    public void parseScriptFile(String path, WalnutbotEnvironment environment, CommandFeedbackHandler commandFeedbackHandler, byte permissionsByte) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            // Parse line-by-line
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    parseLine(line, environment, commandFeedbackHandler, permissionsByte);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void parseLine(String line, WalnutbotEnvironment environment, CommandFeedbackHandler commandFeedbackHandler, byte permissionsByte) {
        // Check for if the line has any content
        if (line.length() == 0)
            return;
        // Check for comment line
        if (line.charAt(0) == '#')
            return;
        // Check for delay commands
        if (line.charAt(0) == '@') {
            delay(line, environment);
            return;
        }
        // Otherwise, run it like it is a command
        environment.getCommandInterpreter().evaluateCommand(line, commandFeedbackHandler, permissionsByte);
    }

    private void delay(String line, WalnutbotEnvironment environment) {
        if (line.equals("@sb")) {
            environment.getAudioStateMachine().notifyWhenSoundboardCompletes(this);
        } else if (line.equals("@jb") || line.equals("@load")) {
            environment.getAudioStateMachine().notifyWhenAudioLoadingCompletes(this);
        } else {
            // Calculate number of milliseconds to wait
            long waitMs = 0;
            int pos = 1;
            String[] units = {"h", "m", "s", "ms"};
            long[] unitLengthsMs = {60 * 60 * 1000, 60 * 1000, 1000, 1};
            for (int i = 0; i < units.length; i++) {
                String unit = units[i];
                long unitLength = unitLengthsMs[i];
                if (pos < line.length()) {
                    int unitPos = line.indexOf(unit, pos);
                    if (unitPos > 0) {
                        waitMs += Integer.parseInt(line.substring(pos, unitPos)) * unitLength;
                        pos = unitPos + unit.length();
                    }
                }
            }
            // Wait that many milliseconds
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
