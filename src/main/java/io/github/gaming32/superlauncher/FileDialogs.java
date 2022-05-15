package io.github.gaming32.superlauncher;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class FileDialogs {
    private static final JFileChooser FILE_CHOOSER = new JFileChooser();

    private FileDialogs() {
    }

    public static File mrpack(Component parent) {
        FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FILE_CHOOSER.setMultiSelectionEnabled(false);
        FILE_CHOOSER.setDialogTitle("Select modpack...");
        FILE_CHOOSER.setAcceptAllFileFilterUsed(false);
        FILE_CHOOSER.resetChoosableFileFilters();
        FILE_CHOOSER.addChoosableFileFilter(new FileNameExtensionFilter("Modrinth Modpacks (*.mrpack)", "mrpack"));
        if (FILE_CHOOSER.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return FILE_CHOOSER.getSelectedFile();
    }

    public static File destDir(Component parent) {
        FILE_CHOOSER.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        FILE_CHOOSER.setMultiSelectionEnabled(false);
        FILE_CHOOSER.setDialogTitle("Select destination directory...");
        FILE_CHOOSER.setAcceptAllFileFilterUsed(false);
        FILE_CHOOSER.resetChoosableFileFilters();
        if (FILE_CHOOSER.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return FILE_CHOOSER.getSelectedFile();
    }
}
