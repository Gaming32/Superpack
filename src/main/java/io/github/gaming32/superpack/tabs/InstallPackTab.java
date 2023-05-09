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
import java.util.List;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.zip.ZipFile;

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
    private JCheckBox skipOverrides;
    private JTextPane installOutput;

    private JPanel downloadBars;
    private JProgressBar overallDownloadBar;
    private final JProgressBar[] subDownloadBars = new JProgressBar[15];

    private boolean hashedProject;
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

        hashProjectAndLookup(() -> {
            if (viewOnModrinthButton != null && modrinthProjectId != null) {
                viewOnModrinthButton.setEnabled(true);
            }
        });
        if (pack.getType() == ModpackType.MODRINTH) {
            viewOnModrinthButton = new JButton("View on Modrinth");
            viewOnModrinthButton.setEnabled(false);
            viewOnModrinthButton.addActionListener(ev -> hashProjectAndLookup(this::openOnModrinth));
        }

        installButton = new JButton("Install!");
        installButton.addActionListener(ev -> {
            setConfigEnabled(false);
            installOutput.setText("");
            final Thread installThread = new Thread(this::doInstall, "InstallThread");
            installThread.setDaemon(true);
            installThread.start();
        });

        skipOverrides = new JCheckBox("Skip Overrides");

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
                        .addComponent(
                            skipOverrides,
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
                        .addComponent(
                            skipOverrides,
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

    private void hashProjectAndLookup(Runnable completionAction) {
        if (modrinthProjectId != null || (hashedProject && viewOnModrinthButton == null)) {
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
                savedPack.setType(pack.getType());
                savedPack.setFilename(friendlyName);
                savedPack.setPath(pack.getPath());
                MyPacks.INSTANCE.addPack(savedPack);
                MyPacks.INSTANCE.setDirty();
                SuperpackKt.saveMyPacks();
            });
            hashedProject = true;
            if (pack.getType() != ModpackType.MODRINTH) {
                SwingUtilities.invokeLater(completionAction);
                return;
            }
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
        if (enabled) {
            skipOverrides.setEnabled(true);
        }
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

        final long startTime = System.currentTimeMillis();

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
        final int parallelDownloadCount = Math.min(filesToDownload.size(), SuperpackSettings.INSTANCE.getParallelDownloadCount());
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
                    if (progressBar == null) return;
                    progressBar.setValue(0);
                    progressBar.setString("Downloading " + progressName + "...");
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
                final File cacheFile;
                if (file.getHashes().containsKey("sha1")) {
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
                    cacheFile = SuperpackKt.getCacheFilePath(file.getHashes().get("sha1"));
                    if (cacheFile.isFile() && cacheFile.length() == file.getSize()) {
                        if (parallelDownloadCount == 1) {
                            println("   File found in cache at " + cacheFile);
                        } else {
                            println("File " + file.getPath() + " found in cache at " + cacheFile);
                        }
                        Files.copy(cacheFile.toPath(), destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        continue;
                    }
                } else {
                    cacheFile = null;
                }
                boolean success = false;
                for (URL downloadUrl : file.getDownloads()) {
                    final long downloadSize = download(
                        parallelDownloadCount,
                        "   ",
                        downloadUrl,
                        progressBar,
                        digest,
                        file,
                        destPath,
                        secondaryHash
                    );
                    if (downloadSize < 0) continue; // Error
                    totalDownloadSize.addAndGet(downloadSize);
                    downloadedCount.incrementAndGet();
                    success = true;
                    break;
                }
                if (success) {
                    if (cacheFile != null) {
                        cacheFile.getParentFile().mkdirs();
                        Files.copy(destPath.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
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

        runParallel(downloadBody, parallelDownloadCount);

        println(
            "Downloaded a total of " +
            GeneralUtilKt.getHumanFileSizeExtended(totalDownloadSize.get()) +
            " across " + downloadedCount.get() + " files"
        );
        SwingUtilities.invokeLater(() -> {
            overallDownloadBar.setValue(overallDownloadBar.getMaximum());
            overallDownloadBar.setString("Downloading files... Done!");
        });

        SwingUtilities.invokeAndWait(() -> skipOverrides.setEnabled(false));
        if (!skipOverrides.isSelected()) {
            extractOverrides(outputDirFile, null);
            if (pack.getType().getSupportsSides()) {
                extractOverrides(outputDirFile, env);
            }
        }

        long durationIgnore = 0;

        if (!failedToDownload.isEmpty()) {
            final List<CurseForgeModpackFile> becauseOfCf = new ArrayList<>();
            println("\n" + failedToDownload.size() + " file(s) failed to download:");
            for (final ModpackFile file : failedToDownload) {
                final boolean wasCf = pack.getType() == ModpackType.CURSEFORGE && file.getDownloads().isEmpty();
                if (wasCf) {
                    becauseOfCf.add((CurseForgeModpackFile)file);
                }
                println("  + " + file.getPath() + (wasCf ? " (blacklisted by author)" : ""));
            }
            if (!becauseOfCf.isEmpty()) {
                final Map<Integer, String> listElements = new LinkedHashMap<>();
                for (final CurseForgeModpackFile file : becauseOfCf) {
                    listElements.put(
                        file.getFileId(),
                        "<li><a href=\"" + file.getBrowserDownloadUrl() + "\">" + file.getPath() + "</a></li>"
                    );
                }
                final File downloadsDir = GeneralUtilKt.getDownloadsFolder();
                final String messageHeader = "<html>Please download the following files manually to " +
                    downloadsDir +
                    ".<ul>";
                final String messageFooter = "</ul></html>";
                final JEditorPane pane = new JEditorPane(
                    "text/html",
                    messageHeader + String.join("", listElements.values()) + messageFooter
                );
                pane.addHyperlinkListener(e -> {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception e1) {
                            LOGGER.error("Failed to open link", e1);
                        }
                    }
                });
                pane.setEditable(false);
                pane.setBorder(null);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this,
                    pane,
                    GeneralUtilKt.getTitle(this),
                    JOptionPane.WARNING_MESSAGE
                ));
                final MultiMessageDigest digest = new MultiMessageDigest(
                    GeneralUtilKt.getSha1(),
                    MessageDigest.getInstance(secondaryHash)
                );
                while (!listElements.isEmpty()) {
                    for (final CurseForgeModpackFile file : becauseOfCf) {
                        if (!listElements.containsKey(file.getFileId())) continue;
                        final File downloadDest = new File(downloadsDir, file.getFile().fileName().replace(' ', '+'));
                        if (!downloadDest.exists()) continue;
                        if (download(
                            1,
                            "",
                            downloadDest.toURI().toURL(),
                            subDownloadBars[0],
                            digest,
                            file,
                            new File(outputDirFile, file.getPath()), // Already validated
                            secondaryHash
                        ) >= 0) {
                            if (!downloadDest.delete()) {
                                println("Failed to delete " + downloadDest);
                            }
                            listElements.remove(file.getFileId());
                            pane.setText(messageHeader + String.join("", listElements.values()) + messageFooter);
                        }
                    }
                    //noinspection BusyWait
                    Thread.sleep(1000);
                    durationIgnore += 1000;
                }
                pane.setText(messageHeader + "<li>All done!</li>" + messageFooter);
            }
        }
        println(
            "\nInstall finished in " +
                GeneralUtilKt.prettyDuration(System.currentTimeMillis() - startTime - durationIgnore) + '!'
        );
        return true;
    }

    private void runParallel(DownloadThreadRunner runner, int parallelDownloadCount) throws InterruptedException {
        if (parallelDownloadCount == 0) {
            LOGGER.info("parallelDownloadCount == 0, skipping {}", runner);
            return;
        }
        final Thread installThread = Thread.currentThread();
        final Thread[] threads = new Thread[parallelDownloadCount];
        final Object[] results = new Object[parallelDownloadCount];
        for (int i = 0; i < parallelDownloadCount; i++) {
            final int tid = i;
            threads[i] = new Thread(() -> {
                try {
                    runner.run(tid);
                    results[tid] = Unit.INSTANCE;
                } catch (InterruptedException e) {
                    // Ignore
                } catch (Exception e) {
                    results[tid] = e;
                }
                subDownloadBars[tid].setVisible(false);
                LockSupport.unpark(installThread);
            }, "InstallThread-" + i);
            threads[i].setDaemon(true);
            threads[i].start();
        }

        final Set<Thread> failedThreads = new HashSet<>();
        while (true) {
            LockSupport.park();
            int doneCount = 0;
            int i = -1;
            for (final Object result : results) {
                final Thread thread = threads[++i];
                if (result instanceof Exception e) {
                    failedThreads.add(thread);
                    results[i] = Unit.INSTANCE;
                    LOGGER.error("Error in thread {}", thread.getName(), e);
                    continue;
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
        for (final Thread thread : threads) {
            thread.interrupt();
        }
        for (final Thread thread : threads) {
            thread.join();
        }
        if (failedThreads.size() == threads.length) {
            throw new IllegalStateException("All threads errored!");
        }
    }

    private long download(
        int parallelDownloadCount,
        String indent,
        URL downloadUrl,
        JProgressBar progressBar,
        MultiMessageDigest digest,
        ModpackFile file,
        File destPath,
        String secondaryHash
    ) throws Exception {
        final String progressName = parallelDownloadCount == 1 ? "file" : file.getPath();
        final String downloadFileSize = GeneralUtilKt.getHumanFileSize(file.getSize());

        if (parallelDownloadCount == 1) {
            println(indent + "Downloading " + downloadUrl);
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
                println(indent + "   Failed to download " + downloadUrl + ": " + e);
            } else {
                println("Failed to download " + downloadUrl + ": " + e);
            }
            return -1L;
        }
        if (parallelDownloadCount == 1) {
            println(indent + "   Downloaded " + GeneralUtilKt.getHumanFileSizeExtended(downloadSize));
        }
        if (downloadSize != file.getSize()) {
            if (parallelDownloadCount == 1) {
                println(
                    indent + "      ERROR: File size doesn't match! Expected " +
                        GeneralUtilKt.getHumanFileSizeExtended(file.getSize())
                );
            } else {
                println(
                    "ERROR: File size for " + file.getPath() + " doesn't match! Expected " +
                        GeneralUtilKt.getHumanFileSizeExtended(file.getSize())
                );
            }
            return -1L;
        }

        byte[] hash1, hash2;

        if (file.getHashes().containsKey("sha1")) {
            hash1 = digest.getDigests()[0].digest();
            hash2 = file.getHashes().get("sha1");
            if (parallelDownloadCount == 1) {
                println(indent + "   SHA-1: " + GeneralUtilKt.toHexString(hash1));
            }
            if (!Arrays.equals(hash1, hash2)) {
                if (parallelDownloadCount == 1) {
                    println(indent + "      ERROR: SHA-1 doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
                } else {
                    println("ERROR: SHA-1 for " + file.getPath() + " doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
                }
                return -1L;
            }
        }

        final String secondaryHashApi = pack.getType().getSecondaryHash().getApiId();
        if (file.getHashes().containsKey(secondaryHashApi)) {
            hash1 = digest.getDigests()[1].digest();
            hash2 = file.getHashes().get(pack.getType().getSecondaryHash().getApiId());
            if (parallelDownloadCount == 1) {
                println(indent + "   " + secondaryHash + ": " + GeneralUtilKt.toHexString(hash1));
            }
            if (!Arrays.equals(hash1, hash2)) {
                if (parallelDownloadCount == 1) {
                    println(indent + "      ERROR: " + secondaryHash + " doesn't match! Expected " + GeneralUtilKt.toHexString(hash2));
                } else {
                    println(
                        "ERROR: " + secondaryHash + " for " + file.getPath() + " doesn't match! Expected " +
                            GeneralUtilKt.toHexString(hash2)
                    );
                }
                return -1L;
            }
        }

        return downloadSize;
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

    private void extractOverrides(File outputDirFile, Side side) throws InterruptedException {
        String sideName = side == null ? "global" : side.toString().toLowerCase();
        println("\nExtracting " + sideName + " overrides...");
        final List<FileOverride> overrides = pack.getOverrides(side);
        final int parallelExtractCount = Math.min(overrides.size(), SuperpackSettings.INSTANCE.getParallelDownloadCount());

        resetDownloadBars(overrides.size(), parallelExtractCount);

        final BlockingQueue<FileOverride> extractQueue = new LinkedBlockingQueue<>(overrides);
        final AtomicInteger extractedCount = new AtomicInteger();
        final DownloadThreadRunner extractBody = tid -> {
            final JProgressBar progressBar = tid < subDownloadBars.length ? subDownloadBars[tid] : null;
            try (ZipFile zf = new ZipFile(pack.getPath())) {
                while (true) {
                    final FileOverride override = extractQueue.poll();
                    if (override == null) break;
                    final String progressName = parallelExtractCount == 1 ? "override" : override.getPath();
                    SwingUtilities.invokeLater(() -> {
                        final int count = extractedCount.get();
                        overallDownloadBar.setValue(count);
                        overallDownloadBar.setString("Extracting " + sideName + " overrides... " + count + '/' + overrides.size());
                        if (progressBar == null) return;
                        progressBar.setValue(0);
                        progressBar.setString("Extracting " + progressName + "...");
                    });
                    String baseName = override.getPath();
                    baseName = baseName.substring(baseName.indexOf('/') + 1);
                    final File destFile = new File(outputDirFile, baseName);
                    if (override.isDirectory()) {
                        destFile.mkdirs();
                        extractedCount.incrementAndGet();
                        continue;
                    }
                    println("Extracting " + override.getPath() + " (" + (extractedCount.incrementAndGet() + 1) + '/' + overrides.size() + ")");
                    destFile.getParentFile().mkdirs();
                    final String extractFileSize = GeneralUtilKt.getHumanFileSize(override.getSize());
                    SwingUtilities.invokeLater(() -> {
                        if (progressBar == null) return;
                        progressBar.setMaximum(GeneralUtilKt.toIntClamped(override.getSize()));
                        progressBar.setValue(0);
                        progressBar.setString("Extracting " + progressName + "... 0 B / " + extractFileSize);
                    });
                    try (InputStream is = new TrackingInputStream(
                        override.openInputStream(zf),
                        read -> SwingUtilities.invokeLater(() -> {
                            if (progressBar == null) return;
                            progressBar.setValue(GeneralUtilKt.toIntClamped(read));
                            progressBar.setString(
                                "Extracting " + progressName +  "... " +
                                    GeneralUtilKt.getHumanFileSize(read) + " / " + extractFileSize
                            );
                        })
                    )) {
                        Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        };

        runParallel(extractBody, parallelExtractCount);

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
