package Main;

public class WalnutbotInfo {

    public static final String VERSION_NUMBER = "1.0_pre13";

    public static final String[] ACCEPTED_AUDIO_FORMATS = {"mp3", "mp4", "flac", "wav", "ogg", "mkv", "mka", "m4a", "aac"};

    public static String getFileChooserTitle(){
        StringBuilder builder = new StringBuilder("Accepted Audio Formats (");
        for (int i = 0; i < ACCEPTED_AUDIO_FORMATS.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append('.').append(ACCEPTED_AUDIO_FORMATS[i]);
        }
        return builder.append(')').toString();
    }

}
