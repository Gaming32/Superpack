package io.github.gaming32.superpack;

import java.io.File;
import java.util.Locale;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

public class SuperpackMain {
    static File dataDir;
    static File cacheDir;
    static File downloadCacheDir;

    public static void main(String[] args) {
        dataDir = getDataDir();
        cacheDir = new File(dataDir, "cache");
        downloadCacheDir = new File(cacheDir, "downloadCache");

        final OsThemeDetector themeDetector = OsThemeDetector.getDetector();
        if (themeDetector.isDark()) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.invokeLater(() -> new LauncherFrame(themeDetector));
    }

    private static File getDataDir() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA"), ".superlauncher");
        }
        String home = System.getProperty("user.dir");
        if (os.contains("mac")) {
            return new File(home, "Library/Application Support/superlauncher");
        }
        return new File(home, ".superlauncher");
    }
}
