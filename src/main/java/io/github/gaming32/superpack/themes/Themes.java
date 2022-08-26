package io.github.gaming32.superpack.themes;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Themes {
    private static final Logger LOGGER = LoggerFactory.getLogger(Themes.class);
    private static final Map<String, Theme> REGISTERED_THEMES = new HashMap<>();

    public static final Theme FLATLAF_SYSTEM = register(new FlatLafTheme(null));
    public static final Theme FLATLAF_DARK = register(new FlatLafTheme(true));
    public static final Theme FLATLAF_LIGHT = register(new FlatLafTheme(false));
    public static final Theme INTELLIJ_SYSTEM = register(new IntelliJTheme(null));
    public static final Theme DARCULA = register(new IntelliJTheme(true));
    public static final Theme INTELLIJ_LIGHT = register(new IntelliJTheme(false));

    private Themes() {
    }

    @ApiStatus.Internal
    public static void loadDefaults() {
    }

    public static Theme register(Theme theme) {
        final Theme duplicateTheme;
        if ((duplicateTheme = REGISTERED_THEMES.put(theme.getId(), theme)) != null) {
            LOGGER.warn("Duplicate themes: {} and {}", duplicateTheme.getId(), theme.getId());
        }
        return theme;
    }

    public static Theme getTheme(String id) {
        return REGISTERED_THEMES.get(id);
    }
}
