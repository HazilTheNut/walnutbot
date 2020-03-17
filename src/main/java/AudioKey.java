import java.util.Objects;

public class AudioKey {

    String name;
    String url;

    public AudioKey(String name, String url) {
        this.name = name;
        this.url = url;
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
        return name;
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
}