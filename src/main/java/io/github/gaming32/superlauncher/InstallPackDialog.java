package io.github.gaming32.superlauncher;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.mrpacklib.Mrpack;
import io.github.gaming32.mrpacklib.Mrpack.EnvSide;
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
    private final ZipFile packZip;
    private final Mrpack pack;

    private JTextField selectedPack;
    private JTextField outputDir;
    private JComboBox<EnvSide> side;

    public InstallPackDialog(LauncherFrame parent, File packFile, OsThemeDetector themeDetector) throws IOException {
        super(parent, "Install Pack", ModalityType.APPLICATION_MODAL);
        this.themeDetector = themeDetector;
        pack = new Mrpack(packZip = new ZipFile(packFile));
        themeDetector.registerListener(themeListener);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        createComponents();

        pack();
        selectedPack.setText(packZip.getName());

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
        JLabel selectedPackLabel = new JLabel("Selected pack:");
        selectedPack = new JTextField(10);
        selectedPack.setEditable(false);

        JLabel outputDirLabel = new JLabel("Output folder:");
        outputDir = new JTextField(10);
        JButton browseOutputDir = new JButton("Browse...");
        browseOutputDir.addActionListener(ev -> {
            File outputDirFile = FileDialogs.outputDir(this);
            if (outputDirFile == null) return;
            outputDir.setText(outputDirFile.getAbsolutePath());
        });

        JLabel sideLabel = new JLabel("Installation side:");
        side = new JComboBox<>(EnvSide.values());

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
            .addGroup(layout.createSequentialGroup()
                .addComponent(selectedPackLabel)
                .addComponent(selectedPack)
            )
            .addGroup(layout.createSequentialGroup()
                .addComponent(outputDirLabel)
                .addComponent(outputDir)
                .addComponent(browseOutputDir)
            )
            .addGroup(layout.createSequentialGroup()
                .addComponent(sideLabel)
                .addComponent(side)
            )
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.CENTER)
                .addComponent(selectedPackLabel)
                .addComponent(selectedPack)
            )
            .addGroup(layout.createParallelGroup(Alignment.CENTER)
                .addComponent(outputDirLabel)
                .addComponent(outputDir)
                .addComponent(browseOutputDir)
            )
            .addGroup(layout.createParallelGroup(Alignment.CENTER)
                .addComponent(sideLabel)
                .addComponent(side)
            )
        );
    }
}
