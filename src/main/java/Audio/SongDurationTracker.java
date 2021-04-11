package Audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

public class SongDurationTracker {

    private long additionalRuntimeMs; //the amount of time the song has been playing that is not accounted for
                                        // with tracking the starting timestamp, which accrues whenever the song is paused, in ms
    private long startTimestampMs; //the System.currentTimeMillis() timestamp at which the song started or resumed, in ms

    private long songLength; //in seconds
    private int songsLoaded = 0; //Should be either 0 or 1, but cannot be a boolean as it is less async-friendly
    private int songsPlaying = 0; //Same for this too

    private JLabel timeLabel;
    private JLabel trackTitleLabel;
    private String noSongPlayingString;

    /*
    * SongDurationTracker: It creates a timer and tracks how long a song has been playing, and writes its output to a JLabel.
    *
    * There are many possible implementations, but they do require the instantiation of a timer of some sort.
    * One could spin up a thread that increments a time counter at 1 second intervals, but repeated starting and stopping of a song could cause a drift in accuracy.
    *
    * Thus the solution below was used:
    *   The SongDurationTracker keeps track of the system timestamp of when a song starts, and then measures the
    *   system's current timestamp against the one in memory to piece together how long the song has been playing.
    *   However, if the song pauses midway through, the timestamp in memory ceases to be accurate. Thus, it must also
    *   store how long the song has been playing before it has been stopped.
    *
    *   Fortunately, the math is quite simple for that, as the total runtime of the song can be continuously tracked by
    *   adding the unpause-pause time length to the current running total runtime. This results in virtually no loss in accuracy over multiple pausings and unpausings.
    * */

    public SongDurationTracker(JLabel timeLabel, JLabel trackTttleLabel, String noSongPlayingString){
        this.timeLabel = timeLabel;
        this.trackTitleLabel = trackTttleLabel;
        this.noSongPlayingString = noSongPlayingString;
        updateTimeLabel();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                updateTimeLabel();
            }
        }, 0, 500);
    }

    private void updateTimeLabel(){
        if (songsLoaded > 0) {
            long time = additionalRuntimeMs / 1000; //in seconds
            if (songsPlaying > 0)
                time += (System.currentTimeMillis() - startTimestampMs) / 1000;
            timeLabel.setText(String.format("%1$02d:%2$02d / %3$02d:%4$02d", time / 60, time % 60, songLength / 60, songLength % 60));
        } else
            timeLabel.setText("--:-- / --:--");
        timeLabel.repaint();
    }

    private void updateTrackTitleLabel(AudioTrack track){
        if (track == null) {
            trackTitleLabel.setText(noSongPlayingString);
            trackTitleLabel.repaint();
        } else {
            trackTitleLabel.setText(String.format("%1$s - %2$s", track.getInfo().title, track.getInfo().author));
            trackTitleLabel.repaint();
        }
    }

    /**
     * Run this whenever a song starts.
     *
     * @param startingTrack The AudioTrack of the song being played. This method simply grabs the song's length and stores it as a field.
     */
    public void onSongStart(AudioTrack startingTrack){
        songLength = startingTrack.getDuration() / 1000;
        startTimestampMs = System.currentTimeMillis();
        additionalRuntimeMs = 0;
        songsLoaded++;
        songsPlaying++;
        updateTrackTitleLabel(startingTrack);
    }

    /**
     * Run this whenever a song resumes playing (after being paused, but not when it first starts playing).
     */
    public void onSongResume(){
        if (songsLoaded > 0)
            startTimestampMs = System.currentTimeMillis();
        songsPlaying++;
    }

    /**
     * Run this whenever a song pauses.
     */
    public void onSongPause(){
        additionalRuntimeMs += System.currentTimeMillis() - startTimestampMs;
        songsPlaying--;
    }

    /**
     * Run this whenever a song reaches its end.
     */
    public void onSongEnd(){
        songsLoaded--;
        songsPlaying--;
        updateTrackTitleLabel(null);
    }

    public void reset(){
        songsLoaded = 0;
        songsPlaying = 0;
        updateTrackTitleLabel(null);
    }
}
