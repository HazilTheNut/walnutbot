import Utils.FileIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Transcriber {


    public void startTranscription(){
        String filename = FileIO.getRootFilePath() + "output.txt";
        File outputFile = new File(filename);
        PrintStream fileOut = null;
        try {
            fileOut = new PrintStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (fileOut != null) {
            System.setOut(fileOut);
            System.setErr(fileOut);
        }
        System.out.printf("BEGIN of Walnutbot (time: %1$s)\n---\n", (new SimpleDateFormat("MM/dd/yyyy kk:mm:ss")).format(new Date()));
    }

}
