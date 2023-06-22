package Commands;

import Audio.INotifiableObject;
import Main.WalnutbotEnvironment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class ScriptParser implements INotifiableObject {

    public void parseScriptFile(String path, WalnutbotEnvironment environment, CommandFeedbackHandler commandFeedbackHandler, byte permissionsByte) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            // Parse line-by-line
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    parseLine(line.trim(), environment, commandFeedbackHandler, permissionsByte);
                }
                scanner.close();
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
            try {
                delay(line, environment);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
        // Otherwise, run it like it is a command
        environment.getCommandInterpreter().evaluateCommand(line, commandFeedbackHandler, permissionsByte);
    }

    private void delay(String line, WalnutbotEnvironment environment) throws InterruptedException {
        if (line.equals("@sb")) {
            waitForSoundboard(environment);
        } else if (line.equals("@jb") || line.equals("@load")) {
            waitForLoad(environment);
        } else {
            // Calculate number of milliseconds to wait
            ArrayList<String> parts = splitTimingString(line);
            String[] units = {"h", "m", "s", "ms"};
            long[] unitLengthsMs = {60 * 60 * 1000, 60 * 1000, 1000, 1};
            long waitMs = 0;
            for (String part : parts) {
                // part is formatted as "000...0unit"; find unit portion
                for (int i = 0; i < units.length; i++) {
                    if (part.matches("[0-9]*".concat(units[i]))) {
                        waitMs += Long.parseLong(part.substring(0, part.indexOf(units[i]))) * unitLengthsMs[i];
                        break;
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

    private synchronized void waitForLoad(WalnutbotEnvironment environment) throws InterruptedException {
        if (environment.getAudioStateMachine().notifyWhenAudioLoadingCompletes(this))
            wait();
    }

    private synchronized void waitForSoundboard(WalnutbotEnvironment environment) throws InterruptedException {
        if (environment.getAudioStateMachine().notifyWhenSoundboardCompletes(this))
            wait();
    }

    ArrayList<String> splitTimingString(String line){
        ArrayList<String> parts = new ArrayList<>();
        int partStartPos = 1;
        int partEndPos;
        String[] units = {"h", "m", "s", "ms"};
        String nearestUnit;
        do {
            // Reset search
            nearestUnit = null;
            partEndPos = line.length()+1;
            // Find the position of the next unit
            for (String unit : units) {
                int index = line.indexOf(unit, partStartPos);
                if (index > 0 && index <= partEndPos) {
                    partEndPos = index;
                    nearestUnit = unit;
                }
            }
            // If we found another part of the string (formatted as "000...0unit") add it to the list
            if (nearestUnit != null) {
                parts.add(line.substring(partStartPos, partEndPos + nearestUnit.length()));
                partStartPos = partEndPos + nearestUnit.length();
            }
        } while (nearestUnit != null);
        return parts;
    }

    @Override
    public synchronized void awaken() {
        notifyAll();
    }
}
