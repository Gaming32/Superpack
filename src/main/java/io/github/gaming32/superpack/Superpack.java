package io.github.gaming32.superpack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.swing.SwingUtilities;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jthemedetecor.OsThemeDetector;
import com.sun.jna.Platform;

import io.github.gaming32.superpack.themes.Theme;
import io.github.gaming32.superpack.themes.Themes;
import io.github.gaming32.superpack.util.GeneralUtil;

public class Superpack {
    private static final Logger LOGGER = LoggerFactory.getLogger(Superpack.class);

    public static final String APP_NAME = "Superpack";
    public static final String MODRINTH_API_ROOT = "https://api.modrinth.com/v2/";

    public static final File DATA_DIR = getDataDir();
    public static final File SETTINGS_FILE = new File(DATA_DIR, "settings.json");
    public static final File MYPACKS_FILE = new File(DATA_DIR, "mypacks.json");
    public static final File CACHE_DIR = new File(DATA_DIR, "cache");
    public static final File DOWNLOAD_CACHE_DIR = new File(CACHE_DIR, "downloadCache");
    public static final File ICON_CACHE_DIR = new File(CACHE_DIR, "iconCache");

    public static void main(String[] args) {
        if (Boolean.getBoolean("superpack.debug")) {
            final LoggerContext context = LoggerContext.getContext(false);
            context.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(Level.DEBUG);
            context.updateLoggers();
        }
        LOGGER.debug("If you see this, you're in debug mode :)");

        try (Reader reader = new FileReader(SETTINGS_FILE, StandardCharsets.UTF_8)) {
            SuperpackSettings.INSTANCE.copyFromRead(reader);
        } catch (Exception e) {
            LOGGER.warn("Failed to load settings, using defaults", e);
            SuperpackSettings.INSTANCE.copyFrom(new SuperpackSettings());
        }
        if (SuperpackSettings.INSTANCE.getTheme() == null) {
            SuperpackSettings.INSTANCE.setTheme(Themes.FLATLAF_SYSTEM);
        }
        saveSettings();

        try (Reader reader = new FileReader(MYPACKS_FILE, StandardCharsets.UTF_8)) {
            MyPacks.INSTANCE.copyFromRead(reader);
        } catch (Exception e) {
            LOGGER.warn("Failed to load My Packs, using defaults", e);
            MyPacks.INSTANCE.copyFrom(new MyPacks());
        }
        MyPacks.INSTANCE.removeMissing();
        saveMyPacks();

        final OsThemeDetector themeDetector = OsThemeDetector.getDetector();
        final Theme theme = SuperpackSettings.INSTANCE.getTheme();
        theme.apply(theme.isAffectedBySystem() ? themeDetector.isDark() : false);
        SwingUtilities.invokeLater(() -> {
            final SuperpackMainFrame mainFrame = new SuperpackMainFrame(themeDetector);
            mainFrame.setVisible(true);
            if (args.length > 0) {
                try {
                    mainFrame.openInstallPack(new File(args[0]));
                } catch (Exception e) {
                    GeneralUtil.showErrorMessage(mainFrame, "Failed to open file automatically", e);
                }
            }
        });
    }

    private static File getDataDir() {
        if (Platform.isWindows()) {
            return new File(System.getenv("APPDATA"), ".superpack");
        }
        final String home = System.getProperty("user.dir");
        if (Platform.isMac()) {
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

    public static void saveMyPacks() {
        try (Writer writer = new FileWriter(MYPACKS_FILE, StandardCharsets.UTF_8)) {
            MyPacks.INSTANCE.write(writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save My Packs", e);
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

    public static boolean isThemeDark() {
        final Theme theme = SuperpackSettings.INSTANCE.getTheme();
        return theme.isAffectedBySystem() ? OsThemeDetector.getDetector().isDark() : theme.isDark();
    }
}
