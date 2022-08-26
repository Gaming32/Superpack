package io.github.gaming32.superpack.themes;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Themes {
    private static final Logger LOGGER = LoggerFactory.getLogger(Themes.class);
    private static final Map<String, Theme> REGISTERED_THEMES = new LinkedHashMap<>();

    public static final Theme METAL = register(
        new SimpleClassTheme("metal", MetalLookAndFeel.class)
            .name("Metal")
    );
    public static final Theme NATIVE = register(
        new SimpleClassTheme("native", UIManager.getSystemLookAndFeelClassName())
            .name("Native")
            .affectedBySystem()
    );
    public static final Theme FLATLAF_SYSTEM = register(new FlatLafTheme(null));
    public static final Theme FLATLAF_DARK = register(new FlatLafTheme(true));
    public static final Theme FLATLAF_LIGHT = register(new FlatLafTheme(false));
    public static final Theme INTELLIJ_SYSTEM = register(new IntelliJTheme(null));
    public static final Theme DARCULA = register(new IntelliJTheme(true));
    public static final Theme INTELLIJ_LIGHT = register(new IntelliJTheme(false));
    public static final Theme NIMBUS = register(
        new SimpleClassTheme("nimbus", NimbusLookAndFeel.class)
            .name("Nimbus")
    );
    public static final Theme WINDOWS_CLASSIC = register(
        new SimpleClassTheme("windows-classic", "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel")
            .name("Windows Classic")
    );

    public static final Theme DEFAULT = FLATLAF_SYSTEM;

    private Themes() {
    }

    @ApiStatus.Internal
    public static void loadDefaults() {
    }

    public static Theme register(Theme theme) {
        final Theme duplicateTheme;
        if ((duplicateTheme = REGISTERED_THEMES.put(theme.getId(), theme)) != null) {
            LOGGER.warn("Duplicate themes for id {}: {} and {}", theme.getId(), duplicateTheme, theme);
        }
        return theme;
    }

    public static Theme getTheme(String id) {
        return REGISTERED_THEMES.get(id);
    }
}
