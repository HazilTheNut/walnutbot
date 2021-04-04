package Utils;

import Commands.CommandFeedbackHandler;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Transcriber {

    private static ArrayList<TranscriptReceiver> transcriptReceivers;

    public static final String AUTH_UI = "UI";
    public static final String AUTH_CONSOLE = "";

    static {
        transcriptReceivers = new ArrayList<>();
    }

    public static CommandFeedbackHandler getGenericCommandFeedBackHandler(String author) {
        return new GenericCommandFeedbackHandler(author);
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
        commandFeedbackHandler.sendMessage(message, true);
    }

    public static void printRaw(String formattedString, Object... args){
        String message = String.format(formattedString, args);
        System.out.println(message);
    }

    public static void startTranscription(boolean headlessMode){
        if (headlessMode){
            startTranscriptionHeadlessMode();
            return;
        }
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

    private static void startTranscriptionHeadlessMode(){
        try {
            OutputSplitter splitter = new OutputSplitter(System.out.toString(), System.out, transcriptReceivers);
            transcriptReceivers.add(new HeadlessFileOutWriter(FileIO.getRootFilePath().concat("output.txt")));
            System.setOut(splitter);
            System.setOut(splitter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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

        private String author;

        private GenericCommandFeedbackHandler(String author){
            this.author = author;
        }

        /**
         * Sends a public message in the same channel as where the command is found.
         *
         * @param message           The message to send
         * @param isCopiedToConsole Whether or not the message is copied to this bot's System.out
         */
        @Override public void sendMessage(String message, boolean isCopiedToConsole) {

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
         * @param message           The message to send
         * @param isCopiedToConsole Whether or not the message is copied to this bot's System.out
         */
        @Override public void sendAuthorPM(String message, boolean isCopiedToConsole) {

        }

        /**
         * Gets a String describing the author of the command.
         *
         * @return A String describing the author of the command.
         */
        @Override public String getAuthor() {
            return author;
        }

        /**
         * Returns the size of pages to display for listing commands such as help, jb list, etc.
         *
         * @param commandType The command to distinguish page sizes for
         * @return The number of elements to list on a given page.
         */
        @Override public int getListPageSize(CommandType commandType) {
            return Integer.MAX_VALUE;
        }
    }

    private static class HeadlessFileOutWriter implements TranscriptReceiver {

        private PrintStream fileOut;

        private HeadlessFileOutWriter(String filepath){
            File outputFile = new File(filepath);
            try {
                fileOut = new PrintStream(outputFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override public void receiveMessage(String message) {
            fileOut.println(message);
        }
    }
}
