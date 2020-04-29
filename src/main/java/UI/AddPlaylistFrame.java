package UI;

import Audio.AudioKey;
import Audio.AudioMaster;
import Utils.Transcriber;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.swing.*;
import java.awt.*;

public class AddPlaylistFrame extends JFrame {

    public AddPlaylistFrame(AudioMaster audioMaster, PlaylistUIWrapper playlistUIWrapper){

        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        setTitle("Import Playlist");
        setSize(new Dimension(550, 100));

        int FIELD_MARGIN = 10;

        JPanel urlPanel = new JPanel();
        urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.LINE_AXIS));

        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));
        urlPanel.add(new JLabel("URL: "), BorderLayout.LINE_START);
        JTextField urlField = new JTextField("Enter URL Here...");
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(Box.createHorizontalStrut(FIELD_MARGIN));

        add(urlPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        JButton confirmButton = new JButton("Import");
        confirmButton.addActionListener(e -> importPlaylist(urlField.getText(), audioMaster, playlistUIWrapper));
        buttonPanel.add(confirmButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(buttonPanel);

        setVisible(true);
    }

    private void importPlaylist(String url, AudioMaster audioMaster, PlaylistUIWrapper playlistUIWrapper){
        Transcriber.print("Loading playlist from %1$s", url);
        audioMaster.getPlayerManager().loadItem(url, new PlaylistScraper(audioMaster, playlistUIWrapper));
        dispose();
    }

    private class PlaylistScraper implements AudioLoadResultHandler {

        private AudioMaster audioMaster;
        private PlaylistUIWrapper playlistUIWrapper;

        public PlaylistScraper(AudioMaster audioMaster, PlaylistUIWrapper playlistUIWrapper) {
            this.audioMaster = audioMaster;
            this.playlistUIWrapper = playlistUIWrapper;
        }

        /**
         * Called when the requested item is a track and it was successfully loaded.
         *
         * @param track The loaded track
         */
        @Override public void trackLoaded(AudioTrack track) {
            AudioKey audioKey = new AudioKey(track);
            audioMaster.getJukeboxDefaultList().addAudioKey(audioKey);
            audioMaster.saveJukeboxDefault();
            playlistUIWrapper.addAudioKey(audioKey);
        }

        /**
         * Called when the requested item is a playlist and it was successfully loaded.
         *
         * @param playlist The loaded playlist
         */
        @Override public void playlistLoaded(AudioPlaylist playlist) {
            for (AudioTrack track : playlist.getTracks()){
                AudioKey audioKey = new AudioKey(track);
                audioMaster.getJukeboxDefaultList().addAudioKey(audioKey);
                playlistUIWrapper.addAudioKey(audioKey);
            }
            audioMaster.saveJukeboxDefault();
        }

        /**
         * Called when there were no items found by the specified identifier.
         */
        @Override public void noMatches() {

        }

        /**
         * Called when loading an item failed with an exception.
         *
         * @param exception The exception that was thrown
         */
        @Override public void loadFailed(FriendlyException exception) {

        }
    }
}