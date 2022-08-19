package io.github.gaming32.superpack;

import java.io.File;
import java.util.Locale;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.superpack.util.GeneralUtil;

public class Superpack {
    public static final String APP_NAME = "Superpack";
    public static final String MODRINTH_API_ROOT = "https://api.modrinth.com/v2/";

    public static final File dataDir = getDataDir();
    public static final File cacheDir = new File(dataDir, "cache");
    public static final File downloadCacheDir = new File(cacheDir, "downloadCache");

    public static void main(String[] args) {
        final OsThemeDetector themeDetector = OsThemeDetector.getDetector();
        if (themeDetector.isDark()) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.invokeLater(() -> new SuperpackMainFrame(themeDetector));
    }

    private static File getDataDir() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA"), ".superpack");
        }
        String home = System.getProperty("user.dir");
        if (os.contains("mac")) {
            return new File(home, "Library/Application Support/superpack");
        }
        return new File(home, ".superpack");
    }

    public static File getCacheFilePath(byte[] sha1) {
        String hash = GeneralUtil.toHexString(sha1);
        String path =
            hash.substring(0, 2) + '/' +
            hash.substring(2, 4) + '/' +
            hash.substring(4);
        return new File(downloadCacheDir, path);
    }

    public static File getCacheFile(byte[] sha1, long fileSize) {
        final File file = getCacheFilePath(sha1);
        if (file.exists() && file.length() == fileSize) {
            return file;
        }
        return null;
    }
}