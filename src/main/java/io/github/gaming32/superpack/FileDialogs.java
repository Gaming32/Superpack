package io.github.gaming32.superpack;

import io.github.gaming32.superpack.util.GeneralUtilKt;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

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

    public static File saveMrpack(Component parent) {
        FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FILE_CHOOSER.setMultiSelectionEnabled(false);
        FILE_CHOOSER.setDialogTitle(null);
        FILE_CHOOSER.setAcceptAllFileFilterUsed(false);
        FILE_CHOOSER.resetChoosableFileFilters();
        FILE_CHOOSER.addChoosableFileFilter(new FileNameExtensionFilter("Modrinth Modpacks (*.mrpack)", "mrpack"));
        if (FILE_CHOOSER.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return GeneralUtilKt.getSelectedSaveFile(FILE_CHOOSER);
    }

    public static File outputDir(Component parent) {
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
