package Utils;

import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Transcriber {

    private static ArrayList<TranscriptReceiver> transcriptReceivers;

    static {
        transcriptReceivers = new ArrayList<>();
    }

    public static void addTranscriptReceiver(TranscriptReceiver receiver){
        transcriptReceivers.add(receiver);
    }

    public static void print(String formattedString, Object... args){
        String time = String.format("[%1$s] ", (new SimpleDateFormat("MM/dd/yyyy kk:mm:ss")).format(new Date()));
        String message = time.concat(String.format(formattedString, args));
        System.out.println(message);
        for (TranscriptReceiver transcriptReceiver : transcriptReceivers)
            transcriptReceiver.receiveMessage(message);
    }

    public static void printAndPost(MessageChannel channel, String formattedString, Object... args){
        String message = String.format(formattedString, args);
        (channel.sendMessage(message)).queue();
        System.out.println(message);
        for (TranscriptReceiver transcriptReceiver : transcriptReceivers)
            transcriptReceiver.receiveMessage(message);
    }

    public static void startTranscription(){
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
