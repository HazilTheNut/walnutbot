package Utils;

import Commands.CommandFeedbackHandler;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Transcriber {

    private static ArrayList<TranscriptReceiver> transcriptReceivers;
    private static CommandFeedbackHandler genericCommandFeedBackHandler;

    static {
        transcriptReceivers = new ArrayList<>();
        genericCommandFeedBackHandler = new GenericCommandFeedbackHandler();
    }

    public static CommandFeedbackHandler getGenericCommandFeedBackHandler() {
        return genericCommandFeedBackHandler;
    }

    public static void addTranscriptReceiver(TranscriptReceiver receiver){
        transcriptReceivers.add(receiver);
    }

    public static void printTimestamped(String formattedString, Object... args){
        String time = String.format("[%1$s] ", (new SimpleDateFormat("MM/dd/yyyy kk:mm:ss")).format(new Date()));
        String message = time.concat(String.format(formattedString, args));
        System.out.println(message);
    }

    public static void printAndPost(CommandFeedbackHandler commandFeedbackHandler, String formattedString, Object... args) {
        String message = String.format(formattedString, args);
        System.out.println(message);
        commandFeedbackHandler.sendMessage(message);
    }

    public static void printRaw(String formattedString, Object... args){
        String message = String.format(formattedString, args);
        System.out.println(message);
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
            try {
                OutputSplitter splitter = new OutputSplitter(filename, fileOut, transcriptReceivers);
                System.setOut(splitter);
                System.setErr(splitter);
            } catch (FileNotFoundException e) {
                System.setOut(fileOut);
                System.setErr(fileOut);
                e.printStackTrace();
            }
        }
        System.out.printf("BEGIN of Walnutbot (time: %1$s)\n---\n", (new SimpleDateFormat("MM/dd/yyyy kk:mm:ss")).format(new Date()));
    }

    private static class OutputSplitter extends PrintStream {

        PrintStream fileOutStream;
        ArrayList<TranscriptReceiver> transcriptReceivers;

        public OutputSplitter(String filename, PrintStream fileOutStream, ArrayList<TranscriptReceiver> transcriptReceivers) throws FileNotFoundException {
            super(filename);
            this.fileOutStream = fileOutStream;
            this.transcriptReceivers = transcriptReceivers;
        }

        private void sendToReceivers(String s){
            for (TranscriptReceiver transcriptReceiver : transcriptReceivers)
                transcriptReceiver.receiveMessage(s);
        }

        @Override public void print(String s) {
            fileOutStream.print(s);
            //super.printTimestamped(s);
            sendToReceivers(s);
        }

        @Override public void println(String x) {
            fileOutStream.println(x);
            //super.println(x);
            sendToReceivers(x);
        }
    }

    private static class GenericCommandFeedbackHandler implements CommandFeedbackHandler {

        /**
         * Sends a public message in the same channel as where the command is found.
         *
         * @param message The message to send
         */
        @Override public void sendMessage(String message) {
            //Transcriber.printRaw(message);
        }

        /**
         * @return True if the channel where the command is found is a public space, rather than a form of private message
         */
        @Override public boolean isChannelPublic() {
            return true;
        }

        /**
         * Sends a private message to the command author
         *
         * @param message The message to send
         */
        @Override public void sendAuthorPM(String message) {
            //Transcriber.printRaw(message);
        }

        /**
         * Gets a String describing the author of the command.
         *
         * @return A String describing the author of the command.
         */
        @Override public String getAuthor() {
            return "Bot";
        }
    }
}
