package io.github.gaming32.superpack.tabs;

import com.google.gson.JsonSyntaxException;
import io.github.gaming32.superpack.*;
import io.github.gaming32.superpack.labrinth.LabrinthGson;
import io.github.gaming32.superpack.labrinth.ModrinthId;
import io.github.gaming32.superpack.labrinth.Project;
import io.github.gaming32.superpack.labrinth.Version;
import io.github.gaming32.superpack.modpack.*;
import io.github.gaming32.superpack.modpack.curseforge.CurseForgeModpackFile;
import io.github.gaming32.superpack.util.*;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class InstallPackTab extends JPanel implements HasLogger, AutoCloseable {
    private static final Logger LOGGER = GeneralUtilKt.getLogger();

    private final SuperpackMainFrame parent;
    private final Modpack pack;
    private final String friendlyName;

    private JTextField selectedPack;
    private JTextField outputDir;
    private JButton browseOutputDir;
    private JComboBox<Side> side;
    private JPanel optionalCheckboxPanel;
    private Map<String, JCheckBox> optionalCheckboxes;
    private JButton viewOnModrinthButton;
    private JButton installButton;
    private JTextPane installOutput;

    private JPanel downloadBars;
    private JProgressBar overallDownloadBar;
    private final JProgressBar[] subDownloadBars = new JProgressBar[15];

    private ModrinthId modrinthProjectId;

    public InstallPackTab(SuperpackMainFrame parent, Modpack pack, String selectedFilename, String friendlyName) throws IOException {
        super();
        this.parent = parent;
        this.pack = pack;
        this.friendlyName = friendlyName;

        createComponents();

        selectedPack.setText(selectedFilename);
    }

    public InstallPackTab(SuperpackMainFrame parent, Modpack pack) throws IOException {
        this(parent, pack, pack.getPath().getAbsolutePath(), pack.getName());
    }

    public void setDefaultSide(Side side) {
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
            } catch (Exception e) {
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
        side = new JComboBox<>(Side.values());
        side.addActionListener(ev -> {
            populateOptionalCheckboxes();
            revalidate();
            repaint();
        });

        optionalCheckboxPanel = new JPanel();
        optionalCheckboxPanel.setLayout(new BoxLayout(optionalCheckboxPanel, BoxLayout.Y_AXIS));
        optionalCheckboxes = new HashMap<>();
        populateOptionalCheckboxes();

        if (pack.getType() == ModpackType.MODRINTH) {
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
        }

        installButton = new JButton("Install!");
        installButton.addActionListener(ev -> {
            setConfigEnabled(false);
            installOutput.setText("");
            final Thread installThread = new Thread(this::doInstall, "InstallThread");
            installThread.setDaemon(true);
            installThread.start();
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

        for (int i = 0; i < subDownloadBars.length; i++) {
            subDownloadBars[i] = new JProgressBar();
            subDownloadBars[i].setStringPainted(true);
            subDownloadBars[i].setVisible(false);
        }

        downloadBars = new JPanel();
        downloadBars.setLayout(new BoxLayout(downloadBars, BoxLayout.Y_AXIS));
        downloadBars.add(overallDownloadBar);
        for (final JProgressBar bar : subDownloadBars) {
            downloadBars.add(bar);
        }
        downloadBars.setVisible(false);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(GeneralUtilKt.build(layout.createParallelGroup(Alignment.CENTER),
            g -> g.addGroup(layout.createSequentialGroup()
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
                ),
            g -> {
                if (!pack.getType().getSupportsSides()) {
                    return g;
                }
                return g.addGroup(layout.createSequentialGroup()
                    .addComponent(
                        sideLabel,
                        GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                    )
                    .addComponent(side)
                );
            },
            g -> g.addComponent(
                    optionalCheckboxPanel, Alignment.TRAILING,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addGroup(GeneralUtilKt.build(layout.createSequentialGroup(),
                    g1 -> {
                        if (viewOnModrinthButton == null) {
                            return g1;
                        }
                        return g1.addComponent(
                            viewOnModrinthButton,
                            GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                        );
                    },
                    g1 -> g1.addComponent(
                        installButton,
                        GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                    )
                ))
                .addComponent(outputScrollPane)
                .addComponent(downloadBars)
        ));
        layout.setVerticalGroup(GeneralUtilKt.build(layout.createSequentialGroup(),
            g -> g.addGroup(layout.createParallelGroup(Alignment.CENTER)
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
                ),
            g -> {
                if (!pack.getType().getSupportsSides()) {
                    return g;
                }
                return g.addGroup(layout.createParallelGroup(Alignment.CENTER)
                    .addComponent(
                        sideLabel,
                        GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                    )
                    .addComponent(
                        side,
                        GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                    )
                );
            },
            g -> g.addComponent(
                    optionalCheckboxPanel,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                )
                .addGroup(GeneralUtilKt.build(layout.createParallelGroup(Alignment.CENTER),
                    g1 -> {
                        if (viewOnModrinthButton == null) {
                            return g1;
                        }
                        return g1.addComponent(
                            viewOnModrinthButton,
                            GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                        );
                    },
                    g1 -> g1.addComponent(
                            installButton,
                            GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE
                        )
                ))
                .addComponent(outputScrollPane)
                .addComponent(downloadBars)
        ));
    }

    private void populateOptionalCheckboxes() {
        optionalCheckboxPanel.removeAll();
        Map<String, JCheckBox> oldOptionalCheckboxes = new HashMap<>(optionalCheckboxes);
        optionalCheckboxes.clear();
        try {
            //noinspection DataFlowIssue
            for (ModpackFile optionalFile : pack.getAllFiles((Side)side.getSelectedItem(), Compatibility.OPTIONAL)) {
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
                hash = GeneralUtilKt.sha1(new FileInputStream(pack.getPath()));
            } catch (Exception e) {
                LOGGER.error("Hashing of " + pack.getPath() + " failed", e);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                MyPacks.Modpack savedPack = MyPacks.INSTANCE.getPack(hash);
                if (savedPack == null) {
                    savedPack = new MyPacks.Modpack();
                    savedPack.setHash(hash);
                }
                savedPack.setName(pack.getName() + ' ' + pack.getVersion());
                savedPack.setDescription(pack.getDescription());
                savedPack.setFilename(friendlyName);
                savedPack.setPath(pack.getPath());
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
                final MyPacks.Modpack savedPack = MyPacks.INSTANCE.getPack(hash);
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

    private boolean doInstall0() throws Exception {
        final int parallelDownloadCount = SuperpackSettings.INSTANCE.getParallelDownloadCount();
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

        final Side env = (Side)side.getSelectedItem();
        println("Creating destination directory...");
        outputDirFile.mkdirs();

        println("\nDownloading files...");
        final String secondaryHash = pack.getType().getSecondaryHash().getAlgorithm();
        final AtomicLong totalDownloadSize = new AtomicLong();
        final AtomicInteger installedCount = new AtomicInteger();
        final AtomicInteger downloadedCount = new AtomicInteger();
        assert env != null;
        List<ModpackFile> filesToDownload = pack.getAllFiles(env);
        resetDownloadBars(filesToDownload.size(), parallelDownloadCount);

        final List<ModpackFile> failedToDownload = new CopyOnWriteArrayList<>();
        final BlockingQueue<ModpackFile> downloadQueue = new LinkedBlockingQueue<>(filesToDownload);

        final DownloadThreadRunner downloadBody = tid -> {
            final JProgressBar progressBar = tid < subDownloadBars.length ? subDownloadBars[tid] : null;
            final MultiMessageDigest digest = new MultiMessageDigest(
                GeneralUtilKt.getSha1(),
                MessageDigest.getInstance(secondaryHash)
            );
            while (true) {
                final ModpackFile file = downloadQueue.poll();
                if (file == null) break; // We're done here
                final String progressName = parallelDownloadCount == 1 ? "file" : file.getPath();
                SwingUtilities.invokeLater(() -> {
                    final int count = installedCount.get();
                    overallDownloadBar.setValue(count);
                    overallDownloadBar.setString("Downloading files... " + count + '/' + filesToDownload.size());
                    if (progressBar != null) {
                        progressBar.setValue(0);
                        progressBar.setString("Downloading " + progressName + "...");
                    }
                });
                if (file.getCompatibility(env) == Compatibility.OPTIONAL) {
                    final JCheckBox optionalCheckBox = optionalCheckboxes.get(file.getPath());
                    if (!optionalCheckBox.isSelected()) {
                        println("Skipped optional file " + file.getPath());
                    }
                }
                final File destPath = new File(outputDirFile, file.getPath());
                if (!destPath.toPath().startsWith(outputDirFile.toPath())) {
                    throw new DisplayErrorMessageMarker(
                        "Unsafe file detected: " + file.getPath() + "\n" +
                            "The developer of this modpack may be attempting to install malware on your computer." +
                            "For safety, further installation of this modpack has been aborted."
                    );
                }
                destPath.getParentFile().mkdirs();
                println("Installing " + file.getPath() + " (" + installedCount.incrementAndGet() + '/' + filesToDownload.size() + ')');
                if (Files.exists(destPath.toPath()) && Files.size(destPath.toPath()) == file.getSize()) {
                    digest.getDigests()[0].reset();
                    try (InputStream is = new DigestInputStream(new FileInputStream(destPath), digest.getDigests()[0])) {
                        GeneralUtilKt.readAndDiscard(is);
                    }
                    if (Arrays.equals(
                        digest.getDigests()[0].digest(),
                        file.getHashes().get("sha1")
                    )) {
                        if (parallelDownloadCount == 1) {
                            println("   Skipping already complete file " + file.getPath());
                        } else {
                            println("Skipping already complete file " + file.getPath());
                        }
                        continue;
                    }
                }
                final File cacheFile = SuperpackKt.getCacheFilePath(file.getHashes().get("sha1"));
                if (cacheFile.isFile() && cacheFile.length() == file.getSize()) {
                    if (parallelDownloadCount == 1) {
                        println("   File found in cache at " + cacheFile);
                    } else {
                        println("File " + file.getPath() + " found in cache at " + cacheFile);
                    }
                    Files.copy(cacheFile.toPath(), destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    continue;
                }
                boolean success = false;
                final String downloadFileSize = GeneralUtilKt.getHumanFileSize(file.getSize());
                for (URL downloadUrl : file.getDownloads()) {
                    if (parallelDownloadCount == 1) {
                        println("   Downloading " + downloadUrl);
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (progressBar == null) return;
                        progressBar.setMaximum(GeneralUtilKt.toIntClamped(file.getSize()));
                        progressBar.setValue(0);
                        progressBar.setString("Downloading " + progressName + "... 0 B / " + downloadFileSize);
                    });
                    digest.reset();
                    long downloadSize;
                    try (InputStream is = new TrackingInputStream(
                        new DigestInputStream(SimpleHttp.stream(downloadUrl), digest),
                        read -> SwingUtilities.invokeLater(() -> {
                            if (progressBar == null) return;
                            progressBar.setValue(GeneralUtilKt.toIntClamped(read));
                            progressBar.setString(
                                "Downloading " + progressName + "... " + GeneralUtilKt.getHumanFileSize(read) + " / " + downloadFileSize
                            );
                        })
                    )) {
                        downloadSize = Files.copy(is, destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        if (parallelDownloadCount == 1) {
                            println("      Failed to download " + downloadUrl + ": " + e);
                        } else {
                            println("Failed to download " + downloadUrl + ": " + e);
                        }
                        continue;
                    }
                    if (parallelDownloadCount == 1) {
                        println("      Downloaded " + GeneralUtilKt.getHumanFileSizeExtended(downloadSize));
                    }
                    if (downloadSize != file.getSize()) {
                        if (parallelDownloadCount == 1) {
                            println(
                                "         ERROR: File size doesn't match! Expected " +
                                    GeneralUtilKt.getHumanFileSizeExtended(file.getSize())
                            );
                        } else {
                            println(
                                "ERROR: File size for " + file.getPath() + " doesn't match! Expected " +
                                    GeneralUtilKt.getHumanFileSizeExtended(file.getSize())
                            );
                        }
                        continue;
                    }
                    byte[] hash1 = digest.getDigests()[0].digest();
                    byte[] hash2 = file.getHashes().get("sha1");
                    if (parallelDownloadCount == 1) {
                        println("      SHA-1: " + GeneralUtilKt.toHexString(hash1));
                    }
                    if (!Arrays.equals(hash1, hash2)) {
                        if (parallelDownloadCount == 1) {
                            println("         ERROR: SHA-1 doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
                        } else {
                            println("ERROR: SHA-1 for " + file.getPath() + " doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
                        }
                        continue;
                    }
                    hash1 = digest.getDigests()[1].digest();
                    hash2 = file.getHashes().get(pack.getType().getSecondaryHash().getApiId());
                    if (parallelDownloadCount == 1) {
                        println("      " + secondaryHash + ": " + GeneralUtilKt.toHexString(hash1));
                    }
                    if (!Arrays.equals(hash1, hash2)) {
                        if (parallelDownloadCount == 1) {
                            println("         ERROR: " + secondaryHash + " doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
                        } else {
                            println(
                                "ERROR: " + secondaryHash + " for " + file.getPath() + " doesn't match! Expected " +
                                    GeneralUtilKt.toHexString(hash2)
                            );
                        }
                        continue;
                    }
                    totalDownloadSize.addAndGet(downloadSize);
                    downloadedCount.incrementAndGet();
                    success = true;
                    break;
                }
                if (success) {
                    cacheFile.getParentFile().mkdirs();
                    Files.copy(destPath.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    failedToDownload.add(file);
                    if (parallelDownloadCount == 1) {
                        println("   Failed to download file.");
                    } else {
                        println("Failed to download " + file.getPath());
                    }
                    try {
                        Files.delete(destPath.toPath());
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        };

        final Thread installThread = Thread.currentThread();
        final Thread[] downloadThreads = new Thread[parallelDownloadCount];
        final Object[] results = new Object[parallelDownloadCount];
        for (int i = 0; i < parallelDownloadCount; i++) {
            final int tid = i;
            downloadThreads[i] = new Thread(() -> {
                try {
                    downloadBody.run(tid);
                    results[tid] = Unit.INSTANCE;
                } catch (InterruptedException e) {
                    // Ignore
                } catch (Exception e) {
                    results[tid] = e;
                }
                subDownloadBars[tid].setVisible(false);
                LockSupport.unpark(installThread);
            }, "InstallThread-" + i);
            downloadThreads[i].setDaemon(true);
            downloadThreads[i].start();
        }

        while (true) {
            LockSupport.park();
            int doneCount = 0;
            for (final Object result : results) {
                if (result instanceof Exception e) {
                    throw e;
                }
                if (result == Unit.INSTANCE) {
                    doneCount++;
                    continue;
                }
                if (result == null) continue;
                throw new AssertionError("results array has unknown value " + result);
            }
            if (doneCount == results.length) break;
        }
        for (final Thread thread : downloadThreads) {
            thread.interrupt();
        }
        for (final Thread thread : downloadThreads) {
            thread.join();
        }

//        for (ModpackFile file : filesToDownload) {
//            SwingUtilities.invokeLater(() -> {
//                overallDownloadBar.setValue(installedCountOld[0]);
//                overallDownloadBar.setString("Downloading files... " + installedCountOld[0] + "/" + filesToDownload.size());
//                subDownloadBars[0].setValue(0);
//                subDownloadBars[0].setString("Downloading file...");
//            });
//            if (file.getCompatibility(env) == Compatibility.OPTIONAL) {
//                JCheckBox optionalCheckBox = optionalCheckboxes.get(file.getPath());
//                if (!optionalCheckBox.isSelected()) {
//                    println("Skipped optional file " + file.getPath());
//                    continue;
//                }
//            }
//            File destPath = new File(outputDirFile, file.getPath());
//            if (!destPath.toPath().startsWith(outputDirFile.toPath())) {
//                throw new DisplayErrorMessageMarker(
//                    "Unsafe file detected: " + file.getPath() + "\n" +
//                    "The developer of this modpack may be attempting to install malware on your computer." +
//                    "For safety, further installation of this modpack has been aborted."
//                );
//            }
//            destPath.getParentFile().mkdirs();
//            println("Installing " + file.getPath() + " (" + ++installedCountOld[0] + "/" + filesToDownload.size() + ")");
//            if (Files.exists(destPath.toPath()) && Files.size(destPath.toPath()) == file.getSize()) {
//                digestOld.getDigests()[0].reset();
//                try (InputStream is = new DigestInputStream(new FileInputStream(destPath), digestOld.getDigests()[0])) {
//                    GeneralUtilKt.readAndDiscard(is);
//                }
//                if (Arrays.equals(
//                    digestOld.getDigests()[0].digest(),
//                    file.getHashes().get("sha1")
//                )) {
//                    println("   Skipping already complete file " + file.getPath());
//                    continue;
//                }
//            }
//            File cacheFile = SuperpackKt.getCacheFilePath(file.getHashes().get("sha1"));
//            if (cacheFile.isFile() && cacheFile.length() == file.getSize()) {
//                println("   File found in cache at " + cacheFile);
//                Files.copy(cacheFile.toPath(), destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
//                continue;
//            }
//            boolean success = false;
//            final String downloadFileSize = GeneralUtilKt.getHumanFileSize(file.getSize());
//            for (URL downloadUrl : file.getDownloads()) {
//                println("   Downloading " + downloadUrl);
//                SwingUtilities.invokeLater(() -> {
//                    subDownloadBars[0].setMaximum(GeneralUtilKt.toIntClamped(file.getSize()));
//                    subDownloadBars[0].setValue(0);
//                    subDownloadBars[0].setString("Downloading file... 0 B / " + downloadFileSize);
//                });
//                digestOld.reset();
//                long downloadSize;
//                try (InputStream is = new TrackingInputStream(
//                    new DigestInputStream(SimpleHttp.stream(downloadUrl), digestOld),
//                    read -> SwingUtilities.invokeLater(() -> {
//                        subDownloadBars[0].setValue(GeneralUtilKt.toIntClamped(read));
//                        subDownloadBars[0].setString("Downloading file... " + GeneralUtilKt.getHumanFileSize(read) + " / " + downloadFileSize);
//                    })
//                )) {
//                    downloadSize = Files.copy(is, destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
//                } catch (IOException e) {
//                    println("      Failed to download " + downloadUrl + ": " + e);
//                    continue;
//                }
//                println("      Downloaded " + GeneralUtilKt.getHumanFileSizeExtended(downloadSize));
//                if (downloadSize != file.getSize()) {
//                    println("         ERROR: File size doesn't match! Expected " + GeneralUtilKt.getHumanFileSizeExtended(file.getSize()));
//                    continue;
//                }
//                byte[] hash1 = digestOld.getDigests()[0].digest();
//                byte[] hash2 = file.getHashes().get("sha1");
//                println("      SHA-1: " + GeneralUtilKt.toHexString(hash1));
//                if (!Arrays.equals(hash1, hash2)) {
//                    println("         ERROR: SHA-1 doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
//                    continue;
//                }
//                hash1 = digestOld.getDigests()[1].digest();
//                hash2 = file.getHashes().get(pack.getType().getSecondaryHash().getApiId());
//                println("      " + secondaryHash + ": " + GeneralUtilKt.toHexString(hash1));
//                if (!Arrays.equals(hash1, hash2)) {
//                    println("         ERROR: " + secondaryHash + " doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
//                    continue;
//                }
//                totalDownloadSizeOld += downloadSize;
//                downloadedCountOld++;
//                success = true;
//                break;
//            }
//            if (success) {
//                cacheFile.getParentFile().mkdirs();
//                Files.copy(destPath.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            } else {
//                failedToDownload.add(file);
//                println("   Failed to download file.");
//                try {
//                    Files.delete(destPath.toPath());
//                } catch (IOException e) {
//                    // Ignore
//                }
//            }
//        }

        println(
            "Downloaded a total of " +
            GeneralUtilKt.getHumanFileSizeExtended(totalDownloadSize.get()) +
            " across " + downloadedCount.get() + " files"
        );
        SwingUtilities.invokeLater(() -> {
            overallDownloadBar.setValue(overallDownloadBar.getMaximum());
            overallDownloadBar.setString("Downloading files... Done!");
        });

        extractOverrides(outputDirFile, null);
        if (pack.getType().getSupportsSides()) {
            extractOverrides(outputDirFile, env);
        }

        println("\nInstall finished!");
        if (!failedToDownload.isEmpty()) {
            final StringBuilder curseForgeMessage = new StringBuilder("<html>Please download the following files manually:<ul>");
            boolean anyCf = false;
            println(failedToDownload.size() + " file(s) failed to download:");
            for (final ModpackFile file : failedToDownload) {
                println("  + " + file.getPath());
                if (pack.getType() == ModpackType.CURSEFORGE && file.getDownloads().isEmpty()) {
                    anyCf = true;
                    curseForgeMessage.append("<li><a href=\"")
                        .append(((CurseForgeModpackFile)file).getBrowserDownloadUrl())
                        .append("\">")
                        .append(file.getPath())
                        .append("</a></li>");
                }
            }
            if (anyCf) {
                SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(
                    this,
                    new JEditorPane("text/html", curseForgeMessage.append("</ul></html>").toString()) {{
                        addHyperlinkListener(e -> {
                            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                try {
                                    Desktop.getDesktop().browse(e.getURL().toURI());
                                } catch (Exception e1) {
                                    LOGGER.error("Failed to open link", e1);
                                }
                            }
                        });
                        setEditable(false);
                        setBorder(null);
                    }},
                    GeneralUtilKt.getTitle(this),
                    JOptionPane.WARNING_MESSAGE
                ));
            }
        }
        return true;
    }

    private void resetDownloadBars(int count, int subBarCount) {
        SwingUtilities.invokeLater(() -> {
            overallDownloadBar.setMaximum(count);
            overallDownloadBar.setValue(0);
            overallDownloadBar.setString("");
            int i = 0;
            for (final JProgressBar bar : subDownloadBars) {
                bar.setValue(0);
                bar.setString("");
                bar.setVisible(i < subBarCount);
                i++;
            }
            downloadBars.setVisible(true);
        });
    }

    private void extractOverrides(File outputDirFile, Side side) throws InterruptedException, IOException {
        String sideName = side == null ? "global" : side.toString().toLowerCase();
        println("\nExtracting " + sideName + " overrides...");
        final var overrides = pack.getOverrides(side);
        resetDownloadBars(overrides.size(), 1);
        int[] i = {0};
        for (final FileOverride override : overrides) {
            SwingUtilities.invokeLater(() -> {
                overallDownloadBar.setValue(i[0]);
                overallDownloadBar.setString("Extracting " + sideName + " overrides... " + i[0] + "/" + overrides.size());
                subDownloadBars[0].setValue(0);
                subDownloadBars[0].setString("Extracting override...");
            });
            String baseName = override.getPath();
            baseName = baseName.substring(baseName.indexOf('/') + 1);
            File destFile = new File(outputDirFile, baseName);
            if (override.isDirectory()) {
                destFile.mkdirs();
                i[0]++;
                continue;
            }
            println("Extracting " + override.getPath() + " (" + (i[0] + 1) + "/" + overrides.size() + ")");
            destFile.getParentFile().mkdirs();
            final String extractFileSize = GeneralUtilKt.getHumanFileSize(override.getSize());
            SwingUtilities.invokeLater(() -> {
                subDownloadBars[0].setMaximum(GeneralUtilKt.toIntClamped(override.getSize()));
                subDownloadBars[0].setValue(0);
                subDownloadBars[0].setString("Extracting override... 0 B / " + extractFileSize);
            });
            try (InputStream is = new TrackingInputStream(
                override.openInputStream(),
                read -> SwingUtilities.invokeLater(() -> {
                    subDownloadBars[0].setValue(GeneralUtilKt.toIntClamped(read));
                    subDownloadBars[0].setString("Extracting override... " + GeneralUtilKt.getHumanFileSize(read) + " / " + extractFileSize);
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

    @FunctionalInterface
    private interface DownloadThreadRunner {
        void run(int tid) throws Exception;
    }
}
