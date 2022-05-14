package io.github.gaming32.superlauncher;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

public final class LauncherFrame extends JFrame {
    @SuppressWarnings("unused")
    private final OsThemeDetector themeDetector;

    public LauncherFrame(OsThemeDetector themeDetector) {
        this.themeDetector = themeDetector;
        themeDetector.registerListener(isDark -> SwingUtilities.invokeLater(() -> {
            if (isDark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            SwingUtilities.updateComponentTreeUI(this);
        }));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        createComponents();

        pack();
        setTitle("Super Launcher");
        setVisible(true);
    }

    private void createComponents() {
    }
}
