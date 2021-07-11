package UI;

import Utils.FileIO;
import Utils.SettingsLoader;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ButtonMaker {

    public static JButton createIconButton(String iconPath, String backupName, int horizontalSpacing){
        JButton jButton;
        String path = convertIconPath(iconPath);
        File iconFile = new File(path);
        if (iconFile.exists()){
            jButton = new JButton(new ImageIcon(path));
            jButton.setMargin(new Insets(4, horizontalSpacing, 4, horizontalSpacing));
        } else
            jButton = new JButton(backupName);
        jButton.setToolTipText(backupName);
        return jButton;
    }

    public static String convertIconPath(String path){
        String basepath = FileIO.getRootFilePath().concat(path);
        if (Boolean.valueOf(SettingsLoader.getBotConfigValue("use_multitheme_icons")))
            return basepath.replace("icons/", "icons/darktheme/");
        return basepath;
    }

}
