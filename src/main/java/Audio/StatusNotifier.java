package Audio;

public interface StatusNotifier {

    void trackStarted();

    void trackEnded();

    void trackLoadFailure();

}
