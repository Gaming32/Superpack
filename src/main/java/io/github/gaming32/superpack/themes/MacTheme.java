package io.github.gaming32.superpack.themes;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class MacTheme extends Theme {
    @Nullable
    private final Boolean forcedDark;

    MacTheme(@Nullable Boolean forcedDark) {
        this.forcedDark = forcedDark;
    }

    @Override
    @NotNull
    public String getId() {
        if (forcedDark == null) {
            return "mac-system";
        } else if (forcedDark) {
            return "mac-dark";
        } else {
            return "mac-light";
        }
    }

    @Override
    @NotNull
    public String getName() {
        if (forcedDark == null) {
            return "Mac System";
        } else if (forcedDark) {
            return "Mac Dark";
        } else {
            return "Mac Light";
        }
    }

    @Override
    public void apply(boolean isDark) {
        if (isDark) {
            FlatMacDarkLaf.setup();
        } else {
            FlatMacLightLaf.setup();
        }
    }

    @Override
    public boolean isAffectedBySystem() {
        return forcedDark == null;
    }

    @Override
    public boolean isDark() {
        return Boolean.TRUE.equals(forcedDark);
    }
}
