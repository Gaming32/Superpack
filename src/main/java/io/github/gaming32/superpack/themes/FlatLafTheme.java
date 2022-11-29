package io.github.gaming32.superpack.themes;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class FlatLafTheme extends Theme {
    @Nullable
    private final Boolean forcedDark;

    FlatLafTheme(@Nullable Boolean forcedDark) {
        this.forcedDark = forcedDark;
    }

    @Override
    @NotNull
    public String getId() {
        if (forcedDark == null) {
            return "flatlaf-system";
        } else if (forcedDark) {
            return "flatlaf-dark";
        } else {
            return "flatlaf-light";
        }
    }

    @Override
    @NotNull
    public String getName() {
        if (forcedDark == null) {
            return "FlatLaf System";
        } else if (forcedDark) {
            return "FlatLaf Dark";
        } else {
            return "FlatLaf Light";
        }
    }

    @Override
    public void apply(boolean isDark) {
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
        return Boolean.TRUE.equals(forcedDark);
    }
}
