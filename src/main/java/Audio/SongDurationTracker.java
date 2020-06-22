package Audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

public class SongDurationTracker {

    private long additionalRuntimeMs; //in ms
    private long startTimestampMs; //in ms

    private long songLength; //in seconds
    private boolean isSongLoaded = false;
    private boolean isSongPlaying = false;

    private JLabel label;

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

    public SongDurationTracker(JLabel jLabel){
        label = jLabel;
        updateLabel();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                updateLabel();
            }
        }, 0, 500);
    }

    private void updateLabel(){
        if (isSongLoaded) {
            long time = additionalRuntimeMs / 1000; //in seconds
            if (isSongPlaying)
                time += (System.currentTimeMillis() - startTimestampMs) / 1000;
            label.setText(String.format("%1$02d:%2$02d / %3$02d:%4$02d", time / 60, time % 60, songLength / 60, songLength % 60));
        } else
            label.setText("00:00 / 00:00");
        label.repaint();
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
        isSongLoaded = true;
        isSongPlaying = true;
    }

    /**
     * Run this whenever a song resumes playing (after being paused, but not when it first starts playing).
     */
    public void onSongResume(){
        if (isSongLoaded)
            startTimestampMs = System.currentTimeMillis();
        isSongLoaded = true;
        isSongPlaying = true;
    }

    /**
     * Run this whenever a song pauses.
     */
    public void onSongPause(){
        additionalRuntimeMs += System.currentTimeMillis() - startTimestampMs;
        isSongPlaying = false;
    }

    /**
     * RUn this whenever a song reaches its end.
     */
    public void onSongEnd(){
        isSongLoaded = false;
        isSongPlaying = false;
    }
}
