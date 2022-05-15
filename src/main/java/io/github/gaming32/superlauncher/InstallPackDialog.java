package io.github.gaming32.superlauncher;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.mrpacklib.Mrpack;
import io.github.gaming32.superlauncher.util.GeneralUtil;

public final class InstallPackDialog extends JDialog {
    private final Consumer<Boolean> themeListener = isDark -> SwingUtilities.invokeLater(() -> {
        if (isDark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(this);
    });
    private final OsThemeDetector themeDetector;
    private final Mrpack pack;

    public InstallPackDialog(LauncherFrame parent, File packFile, OsThemeDetector themeDetector) throws IOException {
        super(parent, "Install Pack", ModalityType.APPLICATION_MODAL);
        this.themeDetector = themeDetector;
        pack = new Mrpack(new ZipFile(packFile));
        themeDetector.registerListener(themeListener);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        createComponents();

        pack();
        setTitle("Install Pack");
        setVisible(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        themeDetector.removeListener(themeListener);
        try {
            pack.close();
        } catch (IOException e) {
            GeneralUtil.showErrorMessage(this, e);
        }
    }

    private void createComponents() {
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
        );
    }
}
