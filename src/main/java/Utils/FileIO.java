package Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class FileIO {

    public static String getRootFilePath(){
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
}
