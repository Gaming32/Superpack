package io.github.gaming32.superpack.themes;

public abstract class Theme {
    private final String id;

    protected Theme(String id) {
        this.id = id;
    }

    /**
     * Initializes a theme without an ID. Callers are expected to override {@link #getId}
     */
    protected Theme() {
        id = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return getId();
    }

    public abstract void apply(boolean isDark);

    /**
     * Whether this theme is affected by system light mode/dark mode. Themes that return false will always be passed
     * the value of {@link #isDark} to the {@code isDark} parameter of {@link #apply}. Those themes also won't have
     * {@link #systemThemeChanged} called on them.
     */
    public boolean isAffectedBySystem() {
        return false;
    }

    /**
     * Whether this theme is inherently dark. This will not be called for themes that return {@code true} for
     * {@link #isAffectedBySystem}.
     */
    public boolean isDark() {
        return false;
    }

    public void systemThemeChanged(boolean isDark) {
        apply(isDark);
    }

    @Override
    public String toString() {
        return getName();
    }
}
