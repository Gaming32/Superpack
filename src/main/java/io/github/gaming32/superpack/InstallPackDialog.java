package io.github.gaming32.superpack;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.mrpacklib.Mrpack;
import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility;
import io.github.gaming32.mrpacklib.Mrpack.EnvSide;
import io.github.gaming32.mrpacklib.packindex.PackFile;
import io.github.gaming32.superpack.util.DisplayErrorMessageMarker;
import io.github.gaming32.superpack.util.GeneralUtil;
import io.github.gaming32.superpack.util.HasLogger;
import io.github.gaming32.superpack.util.MultiMessageDigest;
import io.github.gaming32.superpack.util.NonWrappingTextPane;

public final class InstallPackDialog extends JDialog implements HasLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstallPackDialog.class);

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
    private JButton browseOutputDir;
    private JComboBox<EnvSide> side;
    private JPanel optionalCheckboxPanel;
    private Map<String, JCheckBox> optionalCheckboxes;
    private JButton installButton;
    private JTextPane installOutput;

    public InstallPackDialog(SuperpackMainFrame parent, File packFile, OsThemeDetector themeDetector) throws IOException {
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
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void dispose() {
        super.dispose();
        themeDetector.removeListener(themeListener);
        if (pack != null) { // It's null if initialization failed
            try {
                pack.close();
            } catch (IOException e) {
                GeneralUtil.showErrorMessage(this, e);
            }
        }
    }

    @Override
    public void pack() {
        super.pack();
        setMinimumSize(new Dimension(getWidth(), getHeight() - installOutput.getHeight() + 20));
    }

    private void createComponents() {
        JLabel selectedPackLabel = new JLabel("Selected pack:");
        selectedPack = new JTextField(10);
        selectedPack.setEditable(false);

        JLabel outputDirLabel = new JLabel("Output folder:");
        outputDir = new JTextField(10);
        browseOutputDir = new JButton("Browse...");
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
            setConfigEnabled(false);
            installOutput.setText("");
            new Thread(this::doInstall, "InstallThread").start();
        });

        installOutput = new NonWrappingTextPane();
        installOutput.setEditable(false);
        installOutput.setAutoscrolls(true);
        installOutput.setFont(new Font(
            "Lucida Console",
            installOutput.getFont().getStyle(),
            installOutput.getFont().getSize()
        ));
        JScrollPane outputScrollPane = new JScrollPane(
            installOutput,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        outputScrollPane.getMinimumSize().height = 20;
        outputScrollPane.setPreferredSize(new Dimension(200, 150));

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
            .addComponent(outputScrollPane)
        );
    }

    private void populateOptionalCheckboxes() {
        optionalCheckboxPanel.removeAll();
        Map<String, JCheckBox> oldOptionalCheckboxes = new HashMap<>(optionalCheckboxes);
        optionalCheckboxes.clear();
        try {
            for (PackFile optionalFile : pack.getAllFiles((EnvSide)side.getSelectedItem(), EnvCompatibility.OPTIONAL)) {
                JCheckBox checkBox = new JCheckBox(optionalFile.getPath());
                checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
                checkBox.setAlignmentX(Component.RIGHT_ALIGNMENT);
                optionalCheckboxPanel.add(checkBox);
                optionalCheckboxes.put(optionalFile.getPath(), checkBox);
                JCheckBox oldCheckBox = oldOptionalCheckboxes.get(optionalFile.getPath());
                if (oldCheckBox != null) {
                    checkBox.setSelected(oldCheckBox.isSelected());
                } else {
                    checkBox.setSelected(true);
                }
            }
        } catch (IOException e) {
            GeneralUtil.showErrorMessage(this, e);
        }
    }

    private void setConfigEnabled(boolean enabled) {
        outputDir.setEditable(enabled);
        browseOutputDir.setEnabled(enabled);
        side.setEnabled(enabled);
        for (JCheckBox optionalCheckBox : optionalCheckboxes.values()) {
            optionalCheckBox.setEnabled(enabled);
        }
        installButton.setEnabled(enabled);
    }

    private void println(String s) throws InterruptedException {
        LOGGER.info(s);
        if (!isVisible()) {
            throw new InterruptedException();
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                installOutput.setEditable(true);
                installOutput.setCaretPosition(installOutput.getDocument().getLength());
                installOutput.replaceSelection(s + System.lineSeparator());
                installOutput.setEditable(false);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            GeneralUtil.showErrorMessage(this, e);
        }
    }

    private void doInstall() {
        boolean showSuccessMessage;
        try {
            showSuccessMessage = doInstall0();
        } catch (InterruptedException e) {
            showSuccessMessage = false;
        } catch (DisplayErrorMessageMarker e) {
            showSuccessMessage = false;
            GeneralUtil.showErrorMessage(this, e);
        } catch (Exception e) {
            showSuccessMessage = false;
            GeneralUtil.showErrorMessage(this, e);
        }
        if (isVisible()) {
            final boolean showSuccessMessage0 = showSuccessMessage;
            SwingUtilities.invokeLater(() -> {
                setConfigEnabled(true);
                if (showSuccessMessage0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Finished installing pack " + packFile.getName() + "!",
                        getTitle(),
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            });
        }
    }

    private boolean doInstall0() throws InterruptedException, IOException, NoSuchAlgorithmException {
        if (outputDir.getText().isEmpty()) {
            println("Please specify a destination directory");
            return false;
        }
        final File outputDirFile = new File(outputDir.getText());

        final EnvSide env = (EnvSide)side.getSelectedItem();
        println("Creating destination directory...");
        outputDirFile.mkdirs();

        println("\nDownloading files...");
        MultiMessageDigest digest = new MultiMessageDigest(
            MessageDigest.getInstance("SHA-1"),
            MessageDigest.getInstance("SHA-512")
        );
        long totalDownloadSize = 0;
        int installedCount = 0;
        int downloadedCount = 0;
        List<PackFile> filesToDownload = pack.getAllFiles(env);
        for (PackFile file : filesToDownload) {
            if (file.getEnv().get(env) == EnvCompatibility.OPTIONAL) {
                JCheckBox optionalCheckBox = optionalCheckboxes.get(file.getPath());
                if (!optionalCheckBox.isSelected()) {
                    println("Skipped optional file " + file.getPath());
                    continue;
                }
            }
            File destPath = new File(outputDirFile, file.getPath());
            if (!destPath.toPath().startsWith(outputDirFile.toPath())) {
                throw new DisplayErrorMessageMarker(
                    "Unsafe file detected: " + file.getPath() + "\n" +
                    "The developer of this modpack may be attempting to install malware on your computer." +
                    "For safety, further installation of this modpack has been aborted."
                );
            }
            destPath.getParentFile().mkdirs();
            println("Installing " + file.getPath() + " (" + ++installedCount + "/" + filesToDownload.size() + ")");
            if (Files.exists(destPath.toPath()) && Files.size(destPath.toPath()) == file.getFileSize()) {
                digest.getDigests()[0].reset();
                try (InputStream is = new DigestInputStream(new FileInputStream(destPath), digest.getDigests()[0])) {
                    byte[] buf = new byte[8192];
                    while (is.read(buf) != -1);
                }
                if (Arrays.equals(
                    digest.getDigests()[0].digest(),
                    file.getHashes().get("sha1")
                )) {
                    println("   Skipping already complete file " + file.getPath());
                    continue;
                }
            }
            File cacheFile = getCacheFile(file);
            if (cacheFile.isFile()) {
                println("   File found in cache at " + cacheFile);
                Files.copy(cacheFile.toPath(), destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                continue;
            }
            boolean success = false;
            for (URL downloadUrl : file.getDownloads()) {
                println("   Downloading " + downloadUrl);
                digest.reset();
                long downloadSize;
                try (InputStream is = new DigestInputStream(downloadUrl.openStream(), digest)) {
                    downloadSize = Files.copy(is, destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                println("      Downloaded " + downloadSize + " bytes");
                if (downloadSize != file.getFileSize()) {
                    println("         ERROR: File size doesn't match! Expected " + file.getFileSize() + " bytes");
                    continue;
                }
                byte[] hash1 = digest.getDigests()[0].digest();
                byte[] hash2 = file.getHashes().get("sha1");
                println("      SHA-1: " + toHexString(hash1));
                if (!Arrays.equals(hash1, hash2)) {
                    println("         ERROR: SHA-1 doesn't match! Expected " + toHexString(hash2));
                    continue;
                }
                hash1 = digest.getDigests()[1].digest();
                hash2 = file.getHashes().get("sha512");
                println("      SHA-512: " + toHexString(hash1));
                if (!Arrays.equals(hash1, hash2)) {
                    println("         ERROR: SHA-512 doesn't match! Expected " + toHexString(hash2));
                    continue;
                }
                totalDownloadSize += downloadSize;
                downloadedCount++;
                success = true;
                break;
            }
            if (success) {
                cacheFile.getParentFile().mkdirs();
                Files.copy(destPath.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                println("   Failed to download file.");
                Files.delete(destPath.toPath());
            }
        }
        println("Downloaded a total of " + totalDownloadSize + " bytes across " + downloadedCount + " files");

        extractOverrides(outputDirFile, null);
        extractOverrides(outputDirFile, env);

        println("\nInstall success!");
        return true;
    }

    private void extractOverrides(File outputDirFile, EnvSide side) throws InterruptedException, IOException {
        String sideName = side == null ? "global" : side.toString().toLowerCase();
        println("\nExtracting " + sideName + " overrides...");
        List<ZipEntry> overrides = pack.getOverrides(side);
        int i = 0;
        for (ZipEntry override : overrides) {
            String baseName = override.getName();
            baseName = baseName.substring(baseName.indexOf('/') + 1);
            File destFile = new File(outputDirFile, baseName);
            if (override.isDirectory()) {
                destFile.mkdirs();
                i++;
                continue;
            }
            println("Extracting " + override.getName() + " (" + (i + 1) + "/" + overrides.size() + ")");
            destFile.getParentFile().mkdirs();
            try (InputStream is = packZip.getInputStream(override)) {
                Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            i++;
        }
        println("Extracted " + overrides.size() + " " + sideName + " overrides");
    }

    private static File getCacheFile(PackFile file) {
        String hash = toHexString(file.getHashes().get("sha1"));
        String path =
            hash.substring(0, 2) + '/' +
            hash.substring(2, 4) + '/' +
            hash.substring(4) + '/' +
            file.getPath();
        return new File(SuperpackMain.downloadCacheDir, path);
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static String toHexString(byte[] arr) {
        StringBuilder result = new StringBuilder(arr.length << 1);
        for (byte v : arr) {
            result.append(HEX_CHARS[(v & 0xff) >> 4]);
            result.append(HEX_CHARS[v & 0xf]);
        }
        return result.toString();
    }
}
