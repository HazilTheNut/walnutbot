package Utils;

import org.apache.commons.io.filefilter.SuffixFileFilter;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

public class FileIO {

    private static final String[] playlistFileExtensions = { ".playlist", ".wbp" };

    public static String getRootFilePath() {
        String path = FileIO.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decoded = path;
        try {
            decoded = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        decoded = decoded.replace('/', File.separatorChar);
        decoded = decoded.substring(0, decoded.lastIndexOf(File.separatorChar) + 1);
        return decoded;
    }

    /**
     * Find the file extension of an input uri. If no file extension is found (if the string contains no period characters), then the method just returns the input string unaltered.
     *
     * @param uri The uri to find the extension of.
     * @return The file extension, or the full input string if none was found.
     */
    public static String getFileExtension(String uri) {
        int lastIndex = uri.lastIndexOf('.');
        if (lastIndex > 0 && lastIndex < uri.length() - 1)
            return uri.substring(lastIndex);
        else
            return uri;
    }

    /**
     * Finds the file name, given that it is wrapped by the OS separator character at the front and a period character at the end.
     *
     * @param uri The uri to find the file name of
     * @return The file name, or other parts of the input string if poorly formatted.
     */
    public static String getFileName(String uri) {
        int sepIndex = uri.lastIndexOf(File.separatorChar);
        int dotIndex = uri.lastIndexOf('.');
        return uri.substring(Math.max(0, sepIndex), Math.min(uri.length(), dotIndex));
    }

    public static File[] getFilesInDirectory(String uri) {
        File folder = new File(uri);
        if (folder.isDirectory()){
            FileFilter filter = new SuffixFileFilter(playlistFileExtensions);
            return folder.listFiles(filter);
        }
        return new File[0];
    }

    public static String sanitizeURIForCommand(String uri) {
        return String.format("\"%s\"", uri.replace("\\","\\\\").replace("\"", "\\\""));
    }

    public static String expandURIMacros(String uri) {
        return uri.replace("~~/", getRootFilePath()).replace("~~\\", getRootFilePath());
    }
    public static boolean isWebsiteURL(String uri) {
        // Regex retrieved from https://urlregex.com/
        return uri.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    }

    public static boolean isPlaylistFile(String uri) {
        if (isWebsiteURL(uri))
            return false;
        for (String ext : playlistFileExtensions)
            if (getFileExtension(uri).equals(ext))
                return true;
        return false;
    }

    public static String appendPlaylistFileExtension(String uri) {
        if (getFileExtension(uri).equals(".wbp"))
            return uri;
        return uri.concat(".wbp");
    }

    public static File getSoundboardFile(){
        String filepath = String.format("~~/data%ssoundboard.wbp", File.separatorChar);
        return new File(expandURIMacros(filepath));
    }

    public static String[] getValidPlaylistFileExtensions(){
        return playlistFileExtensions;
    }
}
