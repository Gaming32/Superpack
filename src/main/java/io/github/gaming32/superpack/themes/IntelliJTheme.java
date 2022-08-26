package io.github.gaming32.superpack.themes;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;

final class IntelliJTheme extends Theme {
    private final Boolean forcedDark;

    IntelliJTheme(Boolean forcedDark) {
        this.forcedDark = forcedDark;
    }

    @Override
    public String getId() {
        if (forcedDark == null) {
            return "intellij-system";
        } else if (forcedDark.booleanValue()) {
            return "darcula";
        } else {
            return "intellij-light";
        }
    }

    @Override
    public String getName() {
        if (forcedDark == null) {
            return "IntelliJ System";
        } else if (forcedDark.booleanValue()) {
            return "Darcula";
        } else {
            return "IntelliJ Light";
        }
    }

    @Override
    public void apply(boolean isDark) {
        if (isDark) {
            FlatDarculaLaf.setup();
        } else {
            FlatIntelliJLaf.setup();
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
