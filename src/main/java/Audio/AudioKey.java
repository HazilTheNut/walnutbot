package Audio;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AudioKey implements Comparable<AudioKey>{

    String name;
    String url;

    public AudioKey(String name, String url) {
        this.name = name;
        this.url = url;
    }

    /**
     * Constructs an AudioKey from a single string, formatted as follows:
     * "SONG NAME@SONG URL"
     *
     * @param formattedKey The formatted String representing this AudioKey
     */
    public AudioKey(String formattedKey){
        int separatorIndex = formattedKey.indexOf('@');
        if (separatorIndex > 0){
            name = formattedKey.substring(0, separatorIndex);
            url = formattedKey.substring(Math.min(separatorIndex + 1, formattedKey.length()));
        }
    }

    public boolean isValid(){
        return name != null && url != null && name.length() > 0 && url.length() > 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override public String toString() {
        return String.format("%1$s@%2$s", name, url);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AudioKey audioKey = (AudioKey) o;
        return Objects.equals(getName(), audioKey.getName()) && Objects
            .equals(getUrl(), audioKey.getUrl());
    }

    @Override public int hashCode() {
        return Objects.hash(getName(), getUrl());
    }

    @Override public int compareTo(@NotNull AudioKey o) {
        return name.compareTo(o.getName());
    }
}
