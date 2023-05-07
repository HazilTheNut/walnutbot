package UI;

import Audio.AudioMaster;
import Commands.CommandInterpreter;
import CommuncationPlatform.ICommunicationPlatformManager;
import Main.WalnutbotEnvironment;
import Utils.SettingsLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public class UIFrame extends JFrame implements ComponentListener {

    public UIFrame(WalnutbotEnvironment environment, boolean botInitSuccessful){

        setTitle("Walnutbot Music & Soundboard Discord Bot");

        int width = Integer.parseInt(SettingsLoader.getSettingsValue("windowWidth", "645"));
        int height = Integer.parseInt(SettingsLoader.getSettingsValue("windowHeight", "545"));

        setSize(new Dimension(width, height));

        addComponentListener(this);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Settings", new SettingsPanel(environment, botInitSuccessful));
        boolean keybindsPanelEnabled = Boolean.parseBoolean(SettingsLoader.getBotConfigValue("enable_global_keybinds")) ||
                                     Boolean.parseBoolean(SettingsLoader.getBotConfigValue("enable_midi_input"));
        if (botInitSuccessful) {
            tabbedPane.addTab("Soundboard", new SoundboardPanel(environment));
            tabbedPane.addTab("Jukebox", new JukeboxPanel(environment, this));
            if (keybindsPanelEnabled)
                tabbedPane.addTab("Keybinds", new KeybindsPanel(environment.getCommandInterpreter()));
        }
        tabbedPane.addTab("Log", new ConsolePanel(environment.getCommandInterpreter()));

        getContentPane().add(tabbedPane);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void writeWindowSize(){
        SettingsLoader.modifySettingsValue("windowWidth", String.valueOf(getWidth()));
        SettingsLoader.modifySettingsValue("windowHeight", String.valueOf(getHeight()));
        SettingsLoader.writeSettingsFile();
    }

    /**
     * Invoked when the component's size changes.
     *
     * @param e the event to be processed
     */
    @Override public void componentResized(ComponentEvent e) {
        writeWindowSize();
    }

    /**
     * Invoked when the component's position changes.
     *
     * @param e the event to be processed
     */
    @Override public void componentMoved(ComponentEvent e) {

    }

    /**
     * Invoked when the component has been made visible.
     *
     * @param e the event to be processed
     */
    @Override public void componentShown(ComponentEvent e) {

    }

    /**
     * Invoked when the component has been made invisible.
     *
     * @param e the event to be processed
     */
    @Override public void componentHidden(ComponentEvent e) {

    }


}
