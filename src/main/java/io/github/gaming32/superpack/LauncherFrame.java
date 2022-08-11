package io.github.gaming32.superpack;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.superpack.util.GeneralUtil;

public final class LauncherFrame extends JFrame {
    private final Consumer<Boolean> themeListener = isDark -> SwingUtilities.invokeLater(() -> {
        if (isDark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(this);
    });
    private final OsThemeDetector themeDetector;

    public LauncherFrame(OsThemeDetector themeDetector) {
        super("Superpack");
        this.themeDetector = themeDetector;
        themeDetector.registerListener(themeListener);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        createComponents();

        pack();
        setVisible(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        themeDetector.removeListener(themeListener);
    }

    private void createComponents() {
        JButton installPackButton = new JButton("Install Pack...");
        installPackButton.addActionListener(ev -> {
            File packFile = FileDialogs.mrpack(this);
            if (packFile == null) return;
            try {
                new InstallPackDialog(this, packFile, themeDetector);
            } catch (IOException e) {
                GeneralUtil.showErrorMessage(this, e);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addComponent(installPackButton)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(installPackButton)
        );
    }
}
