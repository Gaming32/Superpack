package io.github.gaming32.superpack.themes;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SimpleClassTheme extends Theme {
    private final String className;
    private String name;
    private boolean affectedBySystem;
    private boolean dark;

    public SimpleClassTheme(String id, String className) {
        super(id);
        this.className = className;
        name = null;
        affectedBySystem = false;
        dark = false;
    }

    public SimpleClassTheme(String id, Class<? extends LookAndFeel> clazz) {
        this(id, clazz.getName());
    }

    @Override
    public void apply(boolean isDark) {
        try {
            UIManager.setLookAndFeel(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull String getName() {
        return name != null ? name : getId(); // getId() may be overriden
    }

    public SimpleClassTheme name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean isAffectedBySystem() {
        return affectedBySystem;
    }

    public SimpleClassTheme affectedBySystem() {
        affectedBySystem = true;
        return this;
    }

    @Override
    public boolean isDark() {
        return dark;
    }

    public SimpleClassTheme dark() {
        dark = true;
        return this;
    }
}
