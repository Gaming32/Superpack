package io.github.gaming32.superpack.tabs;

import com.google.gson.JsonSyntaxException;
import io.github.gaming32.mrpacklib.Mrpack;
import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility;
import io.github.gaming32.mrpacklib.Mrpack.EnvSide;
import io.github.gaming32.mrpacklib.packindex.PackFile;
import io.github.gaming32.mrpacklib.packindex.PackIndex;
import io.github.gaming32.superpack.FileDialogs;
import io.github.gaming32.superpack.MyPacks;
import io.github.gaming32.superpack.MyPacks.Modpack;
import io.github.gaming32.superpack.SuperpackKt;
import io.github.gaming32.superpack.SuperpackMainFrame;
import io.github.gaming32.superpack.labrinth.LabrinthGson;
import io.github.gaming32.superpack.labrinth.ModrinthId;
import io.github.gaming32.superpack.labrinth.Project;
import io.github.gaming32.superpack.labrinth.Version;
import io.github.gaming32.superpack.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.io.*;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class InstallPackTab extends JPanel implements HasLogger, AutoCloseable {
    private static final Logger LOGGER = GeneralUtilKt.getLogger();

    private final SuperpackMainFrame parent;
    private final File packFile;
    private final ZipFile packZip;
    private final Mrpack pack;
    private final String friendlyName;

    private JTextField selectedPack;
    private JTextField outputDir;
    private JButton browseOutputDir;
    private JComboBox<EnvSide> side;
    private JPanel optionalCheckboxPanel;
    private Map<String, JCheckBox> optionalCheckboxes;
    private JButton viewOnModrinthButton;
    private JButton installButton;
    private JTextPane installOutput;

    private JPanel downloadBars;
    private JProgressBar overallDownloadBar;
    private JProgressBar singleDownloadBar;

    private ModrinthId modrinthProjectId;

    public InstallPackTab(SuperpackMainFrame parent, File packFile, String selectedFilename, String friendlyName) throws IOException {
        super();
        this.parent = parent;
        this.packFile = packFile;
        pack = new Mrpack(packZip = new ZipFile(packFile));
        this.friendlyName = friendlyName;

        createComponents();

        selectedPack.setText(selectedFilename);
    }

    public InstallPackTab(SuperpackMainFrame parent, File packFile) throws IOException {
        this(parent, packFile, packFile.getAbsolutePath(), packFile.getName());
    }

    public void setDefaultSide(EnvSide side) {
        this.side.setSelectedItem(side);
    }

    @Override
    @NotNull
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void close() {
        if (pack != null) { // It's null if initialization failed
            try {
                pack.close();
            } catch (IOException e) {
                GeneralUtilKt.showErrorMessage(this, e);
            }
        }
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
            revalidate();
            repaint();
        });

        optionalCheckboxPanel = new JPanel();
        optionalCheckboxPanel.setLayout(new BoxLayout(optionalCheckboxPanel, BoxLayout.Y_AXIS));
        optionalCheckboxes = new HashMap<>();
        populateOptionalCheckboxes();

        viewOnModrinthButton = new JButton("View on Modrinth");
        viewOnModrinthButton.setEnabled(false);
        getModrinthProjectId(() -> {
            if (modrinthProjectId != null) {
                viewOnModrinthButton.setEnabled(true);
            }
        });
        viewOnModrinthButton.addActionListener(ev -> {
            if (modrinthProjectId == null) {
                getModrinthProjectId(this::openOnModrinth);
                return;
            }
            openOnModrinth();
        });

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

        overallDownloadBar = new JProgressBar();
        overallDownloadBar.setStringPainted(true);

        singleDownloadBar = new JProgressBar();
        singleDownloadBar.setStringPainted(true);

        downloadBars = new JPanel();
        downloadBars.setLayout(new BoxLayout(downloadBars, BoxLayout.Y_AXIS));
        downloadBars.add(overallDownloadBar);
        downloadBars.add(singleDownloadBar);
        downloadBars.setVisible(false);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
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
            .addGroup(layout.createSequentialGroup()
                .addComponent(
                    viewOnModrinthButton,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    installButton,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
            )
            .addComponent(outputScrollPane)
            .addComponent(downloadBars)
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
            .addGroup(layout.createParallelGroup(Alignment.CENTER)
                .addComponent(
                    viewOnModrinthButton,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    installButton,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
            )
            .addComponent(outputScrollPane)
            .addComponent(downloadBars)
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
            GeneralUtilKt.showErrorMessage(this, e);
        }
    }

    private void getModrinthProjectId(Runnable completionAction) {
        if (modrinthProjectId != null) {
            completionAction.run();
            return;
        }
        final Thread lookupThread = new Thread(() -> {
            final byte[] hash;
            try {
                hash = GeneralUtilKt.sha1(new FileInputStream(packFile));
            } catch (Exception e) {
                LOGGER.error("Hashing of " + packFile + " failed", e);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                PackIndex index;
                try {
                    index = pack.getPackIndex();
                } catch (IOException e) {
                    // Should *never* happen
                    LOGGER.error("Failed to read pack index while writing My Packs", e);
                    return;
                }
                Modpack savedPack = MyPacks.INSTANCE.getPack(hash);
                if (savedPack == null) {
                    savedPack = new Modpack();
                    savedPack.setHash(hash);
                }
                savedPack.setName(index.getName());
                savedPack.setDescription(index.getSummary());
                savedPack.setFilename(friendlyName);
                savedPack.setPath(packFile);
                MyPacks.INSTANCE.addPack(savedPack);
                MyPacks.INSTANCE.setDirty();
                SuperpackKt.saveMyPacks();
            });
            final Version versionData;
            try (Reader reader = new InputStreamReader(SimpleHttp.stream(SimpleHttp.createUrl(
                SuperpackKt.MODRINTH_API_ROOT,
                "/version_file/" + GeneralUtilKt.toHexString(hash),
                Map.of()
            )))) {
                versionData = LabrinthGson.GSON.fromJson(reader, Version.class);
            } catch (IOException | JsonSyntaxException e) { // Gson rethrows IOExceptions as JsonSyntaxExceptions
                SwingUtilities.invokeLater(completionAction);
                return;
            }
            modrinthProjectId = versionData.getProjectId();
            SwingUtilities.invokeLater(completionAction);
            final Project projectData;
            try (Reader reader = new InputStreamReader(SimpleHttp.stream(SimpleHttp.createUrl(
                SuperpackKt.MODRINTH_API_ROOT,
                "/project/" + modrinthProjectId,
                Map.of()
            )))) {
                projectData = LabrinthGson.GSON.fromJson(reader, Project.class);
            } catch (IOException | JsonSyntaxException e) { // Gson rethrows IOExceptions as JsonSyntaxExceptions
                LOGGER.error("Failed to request project information", e);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                final Modpack savedPack = MyPacks.INSTANCE.getPack(hash);
                if (savedPack == null) return; // Shouldn't happen, but better safe than sorry
                savedPack.setIconUrl(projectData.getIconUrl());
                MyPacks.INSTANCE.setDirty();
                SuperpackKt.saveMyPacks();
            });
        }, "LookupModrinthVersion");
        lookupThread.setDaemon(true);
        lookupThread.start();
    }

    private void openOnModrinth() {
        if (modrinthProjectId == null) {
            GeneralUtilKt.onlyShowErrorMessage(this, "File not found on Modrinth");
            return;
        }
        parent.openOnModrinth(modrinthProjectId);
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
            GeneralUtilKt.showErrorMessage(this, e);
        }
    }

    private void doInstall() {
        boolean showSuccessMessage;
        try {
            showSuccessMessage = doInstall0();
        } catch (InterruptedException e) {
            showSuccessMessage = false;
        } catch (Exception e) {
            showSuccessMessage = false;
            GeneralUtilKt.showErrorMessage(this, e);
        }
        if (isVisible()) {
            final boolean showSuccessMessage0 = showSuccessMessage;
            SwingUtilities.invokeLater(() -> {
                setConfigEnabled(true);
                if (showSuccessMessage0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Finished installing pack " + friendlyName + "!",
                        GeneralUtilKt.getTitle(this),
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            });
        }
    }

    private boolean doInstall0() throws InterruptedException, IOException, NoSuchAlgorithmException, InvocationTargetException {
        if (outputDir.getText().isEmpty()) {
            println("Please specify a destination directory");
            return false;
        }
        final File outputDirFile = new File(outputDir.getText());
        if (outputDirFile.getName().equals("mods")) {
            final boolean[] proceed = {false};
            SwingUtilities.invokeAndWait(() ->
                proceed[0] = JOptionPane.showConfirmDialog(
                    this,
                    "Superpack is not designed to install directly into mods folders. " +
                        "You may have meant to select the containing folder, " + outputDirFile.getParentFile().getName() + ". " +
                        "Would you like to proceed with the installation anyway?",
                    GeneralUtilKt.getTitle(this),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                ) == JOptionPane.YES_OPTION
            );
            if (!proceed[0]) return false;
        }

        final EnvSide env = (EnvSide)side.getSelectedItem();
        println("Creating destination directory...");
        outputDirFile.mkdirs();

        println("\nDownloading files...");
        MultiMessageDigest digest = new MultiMessageDigest(
            GeneralUtilKt.getSha1(),
            MessageDigest.getInstance("SHA-512")
        );
        long totalDownloadSize = 0;
        int[] installedCount = {0};
        int downloadedCount = 0;
        List<PackFile> filesToDownload = pack.getAllFiles(env);
        resetDownloadBars(filesToDownload.size());
        for (PackFile file : filesToDownload) {
            SwingUtilities.invokeLater(() -> {
                overallDownloadBar.setValue(installedCount[0]);
                overallDownloadBar.setString("Downloading files... " + installedCount[0] + "/" + filesToDownload.size());
                singleDownloadBar.setValue(0);
                singleDownloadBar.setString("Downloading file...");
            });
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
            println("Installing " + file.getPath() + " (" + ++installedCount[0] + "/" + filesToDownload.size() + ")");
            if (Files.exists(destPath.toPath()) && Files.size(destPath.toPath()) == file.getFileSize()) {
                digest.getDigests()[0].reset();
                try (InputStream is = new DigestInputStream(new FileInputStream(destPath), digest.getDigests()[0])) {
                    GeneralUtilKt.readAndDiscard(is);
                }
                if (Arrays.equals(
                    digest.getDigests()[0].digest(),
                    file.getHashes().get("sha1")
                )) {
                    println("   Skipping already complete file " + file.getPath());
                    continue;
                }
            }
            File cacheFile = SuperpackKt.getCacheFilePath(file.getHashes().get("sha1"));
            if (cacheFile.isFile() && cacheFile.length() == file.getFileSize()) {
                println("   File found in cache at " + cacheFile);
                Files.copy(cacheFile.toPath(), destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                continue;
            }
            boolean success = false;
            final String downloadFileSize = GeneralUtilKt.getHumanFileSize(file.getFileSize());
            for (URL downloadUrl : file.getDownloads()) {
                println("   Downloading " + downloadUrl);
                SwingUtilities.invokeLater(() -> {
                    singleDownloadBar.setMaximum(GeneralUtilKt.toIntClamped(file.getFileSize()));
                    singleDownloadBar.setValue(0);
                    singleDownloadBar.setString("Downloading file... 0 B / " + downloadFileSize);
                });
                digest.reset();
                long downloadSize;
                try (InputStream is = new TrackingInputStream(
                    new DigestInputStream(SimpleHttp.stream(downloadUrl), digest),
                    read -> SwingUtilities.invokeLater(() -> {
                        singleDownloadBar.setValue(GeneralUtilKt.toIntClamped(read));
                        singleDownloadBar.setString("Downloading file... " + GeneralUtilKt.getHumanFileSize(read) + " / " + downloadFileSize);
                    })
                )) {
                    downloadSize = Files.copy(is, destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    println("      Failed to download " + downloadUrl + ": " + e);
                    continue;
                }
                println("      Downloaded " + GeneralUtilKt.getHumanFileSizeExtended(downloadSize));
                if (downloadSize != file.getFileSize()) {
                    println("         ERROR: File size doesn't match! Expected " + GeneralUtilKt.getHumanFileSizeExtended(file.getFileSize()));
                    continue;
                }
                byte[] hash1 = digest.getDigests()[0].digest();
                byte[] hash2 = file.getHashes().get("sha1");
                println("      SHA-1: " + GeneralUtilKt.toHexString(hash1));
                if (!Arrays.equals(hash1, hash2)) {
                    println("         ERROR: SHA-1 doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
                    continue;
                }
                hash1 = digest.getDigests()[1].digest();
                hash2 = file.getHashes().get("sha512");
                println("      SHA-512: " + GeneralUtilKt.toHexString(hash1));
                if (!Arrays.equals(hash1, hash2)) {
                    println("         ERROR: SHA-512 doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
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
                try {
                    Files.delete(destPath.toPath());
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        println(
            "Downloaded a total of " +
            GeneralUtilKt.getHumanFileSizeExtended(totalDownloadSize) +
            " across " + downloadedCount + " files"
        );
        SwingUtilities.invokeLater(() -> {
            overallDownloadBar.setValue(overallDownloadBar.getMaximum());
            overallDownloadBar.setString("Downloading files... Done!");
        });

        extractOverrides(outputDirFile, null);
        extractOverrides(outputDirFile, env);

        println("\nInstall success!");
        return true;
    }

    private void resetDownloadBars(int count) {
        SwingUtilities.invokeLater(() -> {
            overallDownloadBar.setMaximum(count);
            overallDownloadBar.setValue(0);
            overallDownloadBar.setString("");
            singleDownloadBar.setValue(0);
            singleDownloadBar.setString("");
            downloadBars.setVisible(true);
        });
    }

    private void extractOverrides(File outputDirFile, EnvSide side) throws InterruptedException, IOException {
        String sideName = side == null ? "global" : side.toString().toLowerCase();
        println("\nExtracting " + sideName + " overrides...");
        List<ZipEntry> overrides = pack.getOverrides(side);
        resetDownloadBars(overrides.size());
        int[] i = {0};
        for (ZipEntry override : overrides) {
            SwingUtilities.invokeLater(() -> {
                overallDownloadBar.setValue(i[0]);
                overallDownloadBar.setString("Extracting " + sideName + " overrides... " + i[0] + "/" + overrides.size());
                singleDownloadBar.setValue(0);
                singleDownloadBar.setString("Extracting override...");
            });
            String baseName = override.getName();
            baseName = baseName.substring(baseName.indexOf('/') + 1);
            File destFile = new File(outputDirFile, baseName);
            if (override.isDirectory()) {
                destFile.mkdirs();
                i[0]++;
                continue;
            }
            println("Extracting " + override.getName() + " (" + (i[0] + 1) + "/" + overrides.size() + ")");
            destFile.getParentFile().mkdirs();
            final String extractFileSize = GeneralUtilKt.getHumanFileSize(override.getSize());
            SwingUtilities.invokeLater(() -> {
                singleDownloadBar.setMaximum(GeneralUtilKt.toIntClamped(override.getSize()));
                singleDownloadBar.setValue(0);
                singleDownloadBar.setString("Extracting override... 0 B / " + extractFileSize);
            });
            try (InputStream is = new TrackingInputStream(
                packZip.getInputStream(override),
                read -> SwingUtilities.invokeLater(() -> {
                    singleDownloadBar.setValue(GeneralUtilKt.toIntClamped(read));
                    singleDownloadBar.setString("Extracting override... " + GeneralUtilKt.getHumanFileSize(read) + " / " + extractFileSize);
                })
            )) {
                Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            i[0]++;
        }
        println("Extracted " + overrides.size() + " " + sideName + " overrides");
        SwingUtilities.invokeLater(() -> {
            overallDownloadBar.setValue(overallDownloadBar.getMaximum());
            overallDownloadBar.setString("Extracting " + sideName + " overrides... Done!");
        });
    }
}
