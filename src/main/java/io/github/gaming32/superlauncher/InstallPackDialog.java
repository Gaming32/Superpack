package io.github.gaming32.superlauncher;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.mrpacklib.Mrpack;
import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility;
import io.github.gaming32.mrpacklib.Mrpack.EnvSide;
import io.github.gaming32.mrpacklib.packindex.PackFile;
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
    private final File packFile;
    private final ZipFile packZip;
    private final Mrpack pack;

    private JTextField selectedPack;
    private JTextField outputDir;
    private JComboBox<EnvSide> side;
    private JPanel optionalCheckboxPanel;
    private Map<String, JCheckBox> optionalCheckboxes;
    private JButton installButton;
    private JTextPane installOutput;

    public InstallPackDialog(LauncherFrame parent, File packFile, OsThemeDetector themeDetector) throws IOException {
        super(parent, "Install Pack", ModalityType.APPLICATION_MODAL);
        this.themeDetector = themeDetector;
        this.packFile = packFile;
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
        side.addActionListener(ev -> {
            populateOptionalCheckboxes();
            pack();
        });

        optionalCheckboxPanel = new JPanel();
        optionalCheckboxPanel.setLayout(new BoxLayout(optionalCheckboxPanel, BoxLayout.Y_AXIS));
        optionalCheckboxes = new HashMap<>();
        populateOptionalCheckboxes();

        installButton = new JButton("Install!");
        installButton.addActionListener(ev -> {
            installButton.setEnabled(false);
            installOutput.setText("");
            new Thread(this::doInstall, "InstallThread").start();
        });

        installOutput = new JTextPane();
        installOutput.setEditable(false);
        installOutput.setAutoscrolls(true);
        JScrollPane outputScrollPane = new JScrollPane(
            installOutput,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        outputScrollPane.revalidate();

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
            .addGroup(layout.createSequentialGroup()
                .addComponent(
                    selectedPackLabel,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(selectedPack)
            )
            .addGroup(layout.createSequentialGroup()
                .addComponent(
                    outputDirLabel,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(outputDir)
                .addComponent(
                    browseOutputDir,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
            )
            .addGroup(layout.createSequentialGroup()
                .addComponent(
                    sideLabel,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(side)
            )
            .addComponent(
                optionalCheckboxPanel, Alignment.TRAILING,
                GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
            )
            .addComponent(
                installButton,
                GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
            )
            .addComponent(outputScrollPane)
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.CENTER)
                .addComponent(
                    selectedPackLabel,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    selectedPack,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
            )
            .addGroup(layout.createParallelGroup(Alignment.CENTER)
                .addComponent(
                    outputDirLabel,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    outputDir,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    browseOutputDir,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
            )
            .addGroup(layout.createParallelGroup(Alignment.CENTER)
                .addComponent(
                    sideLabel,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    side,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
            )
            .addComponent(
                optionalCheckboxPanel,
                GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
            )
            .addComponent(
                installButton,
                GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
            )
            .addComponent(
                outputScrollPane,
                GroupLayout.PREFERRED_SIZE, 150, Integer.MAX_VALUE
            )
        );
    }

    private void populateOptionalCheckboxes() {
        optionalCheckboxPanel.removeAll();
        optionalCheckboxes.clear();
        try {
            for (PackFile optionalFile : pack.getAllFiles((EnvSide)side.getSelectedItem(), EnvCompatibility.OPTIONAL)) {
                JCheckBox checkBox = new JCheckBox(optionalFile.getPath());
                checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
                checkBox.setAlignmentX(Component.RIGHT_ALIGNMENT);
                optionalCheckboxPanel.add(checkBox);
                optionalCheckboxes.put(optionalFile.getPath(), checkBox);
            }
        } catch (IOException e) {
            GeneralUtil.showErrorMessage(this, e);
        }
    }

    private void println(String s) {
        System.out.println(s);
        try {
            SwingUtilities.invokeAndWait(() -> {
                installOutput.setEditable(true);
                installOutput.setCaretPosition(installOutput.getDocument().getLength());
                installOutput.replaceSelection(s + '\n');
                installOutput.setEditable(false);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            GeneralUtil.showErrorMessage(this, e);
        }
    }

    private void doInstall() {
        try {
            doInstall0();
        } catch (InterruptedException e) {
            GeneralUtil.showErrorMessage(this, e);
        }
        if (isVisible()) {
            SwingUtilities.invokeLater(() -> {
                installButton.setEnabled(true);
                JOptionPane.showMessageDialog(
                    this,
                    "Finished installing pack " + packFile.getName() + "!",
                    getTitle(),
                    JOptionPane.INFORMATION_MESSAGE
                );
            });
        }
    }

    private void doInstall0() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            if (!isVisible()) return;
            println(Integer.toString(i));
            Thread.sleep(1000);
        }
    }
}
