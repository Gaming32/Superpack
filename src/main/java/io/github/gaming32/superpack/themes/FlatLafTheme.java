package io.github.gaming32.superpack.themes;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

final class FlatLafTheme extends Theme {
    private final Boolean forcedDark;

    FlatLafTheme(Boolean forcedDark) {
        this.forcedDark = forcedDark;
    }

    @Override
    public String getId() {
        if (forcedDark == null) {
            return "flatlaf-system";
        } else if (forcedDark.booleanValue()) {
            return "flatlaf-dark";
        } else {
            return "flatlaf-light";
        }
    }

    @Override
    public String getName() {
        if (forcedDark == null) {
            return "FlatLaf System";
        } else if (forcedDark.booleanValue()) {
            return "FlatLaf Dark";
        } else {
            return "FlatLaf Light";
        }
    }

    @Override
    public void apply(boolean isDark) {
        if (forcedDark != null) {
            isDark = forcedDark.booleanValue();
        }
        if (isDark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    @Override
    public boolean isAffectedBySystem() {
        return forcedDark == null;
    }

    @Override
    public boolean isDark() {
        // This should never be called when forcedDark == null
        return forcedDark.booleanValue();
    }
}
