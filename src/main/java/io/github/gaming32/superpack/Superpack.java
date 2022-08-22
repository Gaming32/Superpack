package io.github.gaming32.superpack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.superpack.util.GeneralUtil;

public class Superpack {
    private static final Logger LOGGER = LoggerFactory.getLogger(Superpack.class);

    public static final String APP_NAME = "Superpack";
    public static final String MODRINTH_API_ROOT = "https://api.modrinth.com/v2/";

    public static final File DATA_DIR = getDataDir();
    public static final File SETTINGS_FILE = new File(DATA_DIR, "settings.json");
    public static final File CACHE_DIR = new File(DATA_DIR, "cache");
    public static final File DOWNLOAD_CACHE_DIR = new File(CACHE_DIR, "downloadCache");
    public static final File ICON_CACHE_DIR = new File(CACHE_DIR, "iconCache");

    public static void main(String[] args) {
        try (Reader reader = new FileReader(SETTINGS_FILE, StandardCharsets.UTF_8)) {
            SuperpackSettings.INSTANCE.copyFromRead(reader);
        } catch (Exception e) {
            LOGGER.warn("Failed to load settings, using defaults", e);
            SuperpackSettings.INSTANCE.copyFrom(new SuperpackSettings());
        }
        saveSettings();
        final OsThemeDetector themeDetector = OsThemeDetector.getDetector();
        if (themeDetector.isDark()) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.invokeLater(() -> new SuperpackMainFrame(themeDetector).setVisible(true));
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

    public static void saveSettings() {
        try (Writer writer = new FileWriter(SETTINGS_FILE, StandardCharsets.UTF_8)) {
            SuperpackSettings.INSTANCE.write(writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save settings", e);
        }
    }

    public static File getCacheFilePath(byte[] sha1) {
        String hash = GeneralUtil.toHexString(sha1);
        String path =
            hash.substring(0, 2) + '/' +
            hash.substring(2, 4) + '/' +
            hash.substring(4);
        return new File(DOWNLOAD_CACHE_DIR, path);
    }

    public static File getCacheFile(byte[] sha1, long fileSize) {
        final File file = getCacheFilePath(sha1);
        if (file.exists() && file.length() == fileSize) {
            return file;
        }
        return null;
    }
}
