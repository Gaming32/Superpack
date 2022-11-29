package io.github.gaming32.superpack.themes;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class IntelliJTheme extends Theme {
    @Nullable
    private final Boolean forcedDark;

    IntelliJTheme(@Nullable Boolean forcedDark) {
        this.forcedDark = forcedDark;
    }

    @Override
    @NotNull
    public String getId() {
        if (forcedDark == null) {
            return "intellij-system";
        } else if (forcedDark) {
            return "darcula";
        } else {
            return "intellij-light";
        }
    }

    @Override
    @NotNull
    public String getName() {
        if (forcedDark == null) {
            return "IntelliJ System";
        } else if (forcedDark) {
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
        return Boolean.TRUE.equals(forcedDark);
    }
}
