package Utils;

import Commands.CommandFeedbackHandler;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class Transcriber {

    private static final ArrayList<TranscriptReceiver> transcriptReceivers;
    private static final ConcurrentLinkedQueue<OutputLogLine> logQueue;

    public static final String AUTH_UI = "UI";
    public static final String AUTH_CONSOLE = "";

    static {
        transcriptReceivers = new ArrayList<>();
        logQueue = new ConcurrentLinkedQueue<>();
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
        logQueue.add(new OutputLogLine(message));
        flushLogQueue();
    }

    public static void printAndPost(CommandFeedbackHandler commandFeedbackHandler, String formattedString, Object... args) {
        String message = String.format(formattedString, args);
        logQueue.add(new OutputLogLine(message, commandFeedbackHandler));
        flushLogQueue();
    }

    public static void printRaw(String formattedString, Object... args){
        String message = String.format(formattedString, args);
        logQueue.add(new OutputLogLine(message));
        flushLogQueue();
    }

    private static synchronized void flushLogQueue(){
        while (!logQueue.isEmpty()) {
            OutputLogLine logLine = logQueue.poll();
            System.out.println(logLine.getMessage());
            if (logLine.getCommandFeedbackHandler() != null)
                logLine.getCommandFeedbackHandler().sendMessage(logLine.getMessage(), true);
        }
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

        private final String author;

        private GenericCommandFeedbackHandler(String author){
            this.author = author;
        }

        /**
         * Sends a public message in the same channel as where the command is found.
         *
         * @param message           The message to send
         * @param isCopiedToConsole Whether message is copied to this bot's System.out
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
         * @param isCopiedToConsole Whether the message is copied to this bot's System.out
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

    private static class OutputLogLine {
        private final String message;
        @Nullable
        private final CommandFeedbackHandler commandFeedbackHandler;

        public OutputLogLine(String message) {
            this.message = message;
            commandFeedbackHandler = null;
        }

        public OutputLogLine(String message, @Nullable CommandFeedbackHandler commandFeedbackHandler) {
            this.message = message;
            this.commandFeedbackHandler = commandFeedbackHandler;
        }

        public String getMessage() {
            return message;
        }

        @Nullable
        public CommandFeedbackHandler getCommandFeedbackHandler() {
            return commandFeedbackHandler;
        }
    }
}
