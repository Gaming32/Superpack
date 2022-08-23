package io.github.gaming32.superpack.tabs;

import static io.github.gaming32.superpack.util.GeneralUtil.THUMBNAIL_SIZE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility;
import io.github.gaming32.mrpacklib.Mrpack.EnvSide;
import io.github.gaming32.pipeline.Pipelines;
import io.github.gaming32.superpack.ProgressDialog;
import io.github.gaming32.superpack.Superpack;
import io.github.gaming32.superpack.SuperpackMainFrame;
import io.github.gaming32.superpack.labrinth.LabrinthGson;
import io.github.gaming32.superpack.labrinth.ModrinthId;
import io.github.gaming32.superpack.labrinth.Project;
import io.github.gaming32.superpack.labrinth.SearchResults;
import io.github.gaming32.superpack.labrinth.Version;
import io.github.gaming32.superpack.util.GeneralUtil;
import io.github.gaming32.superpack.util.HasLogger;
import io.github.gaming32.superpack.util.MultiMessageDigest;
import io.github.gaming32.superpack.util.PlaceholderTextField;
import io.github.gaming32.superpack.util.SimpleHttp;
import io.github.gaming32.superpack.util.TrackingInputStream;

public final class ModrinthTab extends JPanel implements HasLogger, Scrollable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModrinthTab.class);
    private static final int PER_PAGE = 50;

    private final SuperpackMainFrame parent;

    private final ImageIcon placeholderIcon;

    private final MainList mainList;
    private final ModpackInformationPanel modpackInformationPanel;
    private final VersionsList versionsList;

    public ModrinthTab(SuperpackMainFrame parent) {
        this.parent = parent;

        placeholderIcon = new ImageIcon(getClass().getResource("/placeholder.png"));

        mainList = new MainList();
        modpackInformationPanel = new ModpackInformationPanel();
        versionsList = new VersionsList();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(mainList);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 40;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 400;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void openOnModrinth(ModrinthId projectId) {
        removeAll();
        add(modpackInformationPanel);
        modpackInformationPanel.loadProject(projectId.getId());
    }

    private JLabel createLoadingLabel() {
        final JLabel loading = new JLabel("Loading...");
        loading.setForeground(new Color(0x2a2c2e));
        loading.setFont(loading.getFont().deriveFont(48f));
        return loading;
    }

    private <T extends Component & HasCachedScrollValue> JButton createBackButton(String label, Component from, T to) {
        final JButton backButton = new JButton(label);
        backButton.addActionListener(ev -> {
            ModrinthTab.this.remove(from);
            ModrinthTab.this.add(to);
            SwingUtilities.invokeLater(() -> { // Wait until after the GUI refreshes
                final JScrollPane scrollPane = (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, to);
                if (scrollPane != null) {
                    scrollPane.getVerticalScrollBar().setValue(to.getCachedScrollValue());
                }
                to.setCachedScrollValue(0);
            });
        });
        return backButton;
    }

    private final class MainList extends JPanel implements HasLogger, HasCachedScrollValue {
        final JLabel resultsCount;
        final JComboBox<String> pageSelector;

        Thread loadingThread;
        boolean disablePageSelector;
        int cachedScrollValue;

        MainList() {
            final JLabel loading = createLoadingLabel();

            final JPanel topPanel;
            {
                topPanel = new JPanel();

                final PlaceholderTextField searchField = new PlaceholderTextField();
                searchField.setPlaceholder("Search...");

                final JButton gotoButton = new JButton("Jump to Project...");
                gotoButton.addActionListener(ev -> {
                    final String id = JOptionPane.showInputDialog(
                        parent,
                        "Enter the ID or slug of the modpack you want to view:",
                        "Jump to ID",
                        JOptionPane.QUESTION_MESSAGE
                    );
                    LOGGER.info("Jump to ID {}", id);
                    if (id == null) return;
                    loadProject(id);
                });

                resultsCount = new JLabel();

                pageSelector = new JComboBox<>();

                final Consumer<SearchResults> refreshResults = results -> {
                    for (final Component component : getComponents()) {
                        if (component != topPanel) {
                            remove(component);
                        }
                    }
                };
                GeneralUtil.addDocumentListener(searchField, ev ->
                    loadElements(
                        0,
                        ev.getDocument().getLength() == 0 ? null : searchField.getText(),
                        refreshResults
                    )
                );
                pageSelector.addActionListener(ev -> {
                    if (disablePageSelector) return;
                    loadElements(
                        pageSelector.getSelectedIndex() * PER_PAGE,
                        searchField.getDocument().getLength() == 0 ? null : searchField.getText(),
                        refreshResults
                    );
                });

                final GroupLayout layout = new GroupLayout(topPanel);
                topPanel.setLayout(layout);
                layout.setAutoCreateGaps(true);
                layout.setAutoCreateContainerGaps(true);
                layout.setHorizontalGroup(layout.createSequentialGroup()
                    .addComponent(searchField)
                    .addComponent(gotoButton)
                    .addComponent(pageSelector, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(resultsCount)
                );
                layout.setVerticalGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(searchField)
                    .addComponent(gotoButton)
                    .addComponent(pageSelector)
                    .addComponent(resultsCount)
                );
            }

            loadElements(
                0, null,
                results -> {
                    remove(loading);
                    add(topPanel);
                }
            );

            setLayout(new GridBagLayout() {{
                defaultConstraints.fill = GridBagConstraints.HORIZONTAL;
                defaultConstraints.weightx = 1;
                defaultConstraints.gridx = 0;
            }});
            add(loading, new GridBagConstraints()); // Overrides default constraints
        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }

        @Override
        public int getCachedScrollValue() {
            return cachedScrollValue;
        }

        @Override
        public void setCachedScrollValue(int value) {
            this.cachedScrollValue = value;
        }

        void loadElements(
            int offset,
            String query,
            Consumer<SearchResults> onPreComplete
        ) {
            if (loadingThread != null) {
                while (loadingThread.isAlive()) {
                    Thread.onSpinWait(); // Definitely don't Thread.sleep, because this is probably the AWT event thread
                }
            }
            loadingThread = new Thread(() -> {
                final SearchResults results;
                final Map<String, Object> search = new HashMap<>(Map.of(
                    "facets", "[[\"project_type:modpack\"]]",
                    "offset", offset,
                    "limit", PER_PAGE
                ));
                if (query != null) {
                    search.put("query", query);
                }
                try (
                    InputStream is = SimpleHttp.stream(SimpleHttp.createUrl(Superpack.MODRINTH_API_ROOT, "/search", search));
                    Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                ) {
                    results = LabrinthGson.GSON.fromJson(reader, SearchResults.class);
                } catch (Exception e) {
                    LOGGER.error("Error requesting modpacks", e);
                    final Timer timer = new Timer(1000, ev -> loadElements(offset, query, onPreComplete));
                    timer.setRepeats(false);
                    timer.start();
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    onPreComplete.accept(results);
                    for (final SearchResults.Result project : results.getHits()) {
                        final JButton button = new JButton();
                        final GroupLayout layout = new GroupLayout(button);
                        button.setLayout(layout);
                        button.addActionListener(ev -> {
                            LOGGER.info("Clicked {}", project.getTitle());
                            loadProject(project.getId().toString());
                        });

                        final JLabel icon = new JLabel(placeholderIcon);
                        if (project.getIconUrl() != null) {
                            GeneralUtil.loadProjectIcon(project.getIconUrl(), image -> {
                                if (image != null) {
                                    icon.setIcon(new ImageIcon(image));
                                }
                            });
                        }

                        final JLabel title = new JLabel(project.getTitle());
                        title.setFont(title.getFont().deriveFont(24f));

                        final JLabel description = new JLabel("<html>" + project.getDescription() + "</html>");

                        final JPanel details = new JPanel();
                        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
                        details.setOpaque(false);
                        details.add(title);
                        details.add(description);

                        layout.setAutoCreateGaps(true);
                        layout.setAutoCreateContainerGaps(true);
                        layout.setHorizontalGroup(layout.createSequentialGroup()
                            .addComponent(icon, THUMBNAIL_SIZE, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                            .addComponent(details)
                        );
                        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(icon, THUMBNAIL_SIZE, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                            .addComponent(details)
                        );
                        add(button);
                    }
                    resultsCount.setText(results.getTotalHits() + " hit" + (results.getTotalHits() == 1 ? "" : "s"));
                    {
                        disablePageSelector = true;
                        int pageCount = results.getTotalHits() / PER_PAGE;
                        if (pageCount == 0 || results.getTotalHits() % PER_PAGE != 0) {
                            pageCount++;
                        }
                        pageSelector.removeAllItems();
                        for (int i = 0; i < pageCount; i++) {
                            pageSelector.addItem("Page " + (i + 1));
                        }
                        pageSelector.setSelectedIndex(results.getOffset() / PER_PAGE);
                        disablePageSelector = false;
                    }
                    revalidate();
                    repaint();
                });
            }, "ModrinthSearchThread");
            loadingThread.setDaemon(true);
            loadingThread.start();
        }

        void loadProject(String projectIdOrSlug) {
            modpackInformationPanel.loadProject(projectIdOrSlug);
            final JScrollPane scrollPane = (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            if (scrollPane != null) {
                cachedScrollValue = scrollPane.getVerticalScrollBar().getValue();
            }
            ModrinthTab.this.remove(this);
            ModrinthTab.this.add(modpackInformationPanel);
        }
    }

    private final class ModpackInformationPanel extends JPanel implements HasLogger, HasCachedScrollValue {
        final JButton backButton;
        final JPanel mainPanel;
        final JLabel loading;

        Thread loadingThread;
        int cachedScrollValue = 0;

        ModpackInformationPanel() {
            backButton = createBackButton("< Back to Search", this, mainList);

            final JPanel backPanel = new JPanel();
            backPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            backPanel.setOpaque(false);
            backPanel.add(backButton);

            loading = createLoadingLabel();
            loading.setHorizontalAlignment(JLabel.CENTER);
            loading.setAlignmentX(JLabel.CENTER_ALIGNMENT);

            mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            final OverlayLayout layout = new OverlayLayout(this);
            setLayout(layout);
            add(backPanel);
            add(mainPanel);
        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }

        @Override
        public int getCachedScrollValue() {
            return cachedScrollValue;
        }

        @Override
        public void setCachedScrollValue(int value) {
            this.cachedScrollValue = value;
        }

        @Override
        public void doLayout() {
            for (final Component component : getComponents()) {
                component.setSize(component.getPreferredSize());
            }
            super.doLayout();
        }

        public void loadProject(String projectIdOrSlug) {
            mainPanel.removeAll();
            mainPanel.add(loading);
            if (loadingThread != null) {
                while (loadingThread.isAlive()) {
                    Thread.onSpinWait(); // Definitely don't Thread.sleep, because this is probably the AWT event thread
                }
            }
            loadingThread = new Thread(() -> {
                final Project project;
                try (
                    InputStream is = SimpleHttp.stream(SimpleHttp.createUrl(Superpack.MODRINTH_API_ROOT, "/project/" + projectIdOrSlug, Map.of()));
                    Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                ) {
                    project = LabrinthGson.GSON.fromJson(reader, Project.class);
                } catch (Exception e) {
                    getLogger().error("Error requesting project", e);
                    if (e instanceof FileNotFoundException) {
                        GeneralUtil.onlyShowErrorMessage(this, "Could not find project " + projectIdOrSlug);
                    } else {
                        GeneralUtil.onlyShowErrorMessage(this, "Could not load project");
                    }
                    SwingUtilities.invokeLater(() -> GeneralUtil.callAction(backButton));
                    return;
                }
                LOGGER.info("Received project information for {}", project.getTitle());
                SwingUtilities.invokeLater(() -> {
                    mainPanel.removeAll();

                    final JPanel nameAndIcon = new JPanel();
                    nameAndIcon.setLayout(new FlowLayout());

                    final JLabel icon = new JLabel(placeholderIcon);
                    if (project.getIconUrl() != null) {
                        GeneralUtil.loadProjectIcon(project.getIconUrl(), image -> icon.setIcon(new ImageIcon(image)));
                    }
                    nameAndIcon.add(icon);

                    final JLabel title = new JLabel(project.getTitle());
                    title.setFont(title.getFont().deriveFont(24f));
                    nameAndIcon.add(title);

                    mainPanel.add(nameAndIcon);

                    JLabel sideNote = null;
                    final EnvSide defaultSide;
                    if (project.getClientSide() == EnvCompatibility.UNSUPPORTED) {
                        defaultSide = EnvSide.SERVER;
                        sideNote = new JLabel("Server pack");
                    } else if (project.getServerSide() == EnvCompatibility.UNSUPPORTED) {
                        defaultSide = EnvSide.CLIENT;
                        sideNote = new JLabel("Client pack");
                    } else {
                        defaultSide = null;
                    }
                    if (sideNote != null) {
                        sideNote.setHorizontalAlignment(JLabel.CENTER);
                        sideNote.setAlignmentX(JLabel.CENTER_ALIGNMENT);
                        sideNote.setFont(sideNote.getFont().deriveFont(Font.BOLD));
                        mainPanel.add(sideNote);
                    }

                    final JLabel description = new JLabel("<html>" + project.getDescription() + "</html>", JLabel.CENTER);
                    description.setAlignmentX(JLabel.CENTER_ALIGNMENT);
                    description.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                    mainPanel.add(description);

                    final JPanel actionPanel = new JPanel();

                    final JButton download = new JButton("Download");
                    download.addActionListener(ev -> {
                        LOGGER.info("Download {}", project.getTitle());
                        versionsList.loadProject(projectIdOrSlug, defaultSide);
                        ModrinthTab.this.remove(this);
                        ModrinthTab.this.add(versionsList);
                    });
                    actionPanel.add(download);

                    final JButton viewOnModrinth = new JButton("View on Modrinth");
                    viewOnModrinth.addActionListener(ev -> {
                        try {
                            Desktop.getDesktop().browse(new URI("https://modrinth.com/modpack/" + project.getSlug()));
                        } catch (Exception e) {
                            final String message = "Failed to open " + project.getSlug() + " on Modrinth";
                            LOGGER.error(message, e);
                            GeneralUtil.onlyShowErrorMessage(this, message);
                        }
                    });
                    actionPanel.add(viewOnModrinth);

                    mainPanel.add(actionPanel);

                    final JPanel bodyPanel = new JPanel();
                    bodyPanel.setBorder(
                        // This border looks nice, but we don't have a title, so we can't just use TitledBorder
                        UIManager.getBorder("TitledBorder.border")
                    );
                    bodyPanel.setLayout(new BorderLayout());

                    final JEditorPane body = new JEditorPane();
                    body.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
                    body.addHyperlinkListener(ev -> {
                        if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            if (ev.getURL() == null) {
                                LOGGER.warn("Swing passed us a null URL!");
                                return;
                            }
                            try {
                                Desktop.getDesktop().browse(ev.getURL().toURI());
                            } catch (Exception e) {
                                GeneralUtil.showErrorMessage(this, "Failed to open link", e);
                            }
                        } else if (ev.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        } else if (ev.getEventType() == HyperlinkEvent.EventType.EXITED) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    });
                    body.setText("<html>Loading body...</html>");
                    body.setEditable(false);
                    body.setOpaque(false);
                    bodyPanel.add(body);

                    while (loadingThread.isAlive()) {
                        Thread.onSpinWait(); // Definitely don't Thread.sleep, because this is the AWT event thread
                    }
                    loadingThread = new Thread(() -> {
                        final String markdown;
                        try {
                            markdown = GeneralUtil.renderMarkdown(project.getBody());
                        } catch (Exception e) {
                            LOGGER.error("Error rendering body", e);
                            return;
                        }
                        LOGGER.info("Received rendered markdown");
                        SwingUtilities.invokeLater(() -> {
                            body.setText("<html>" + markdown + "</html>");
                            body.setCaretPosition(0);
                        });
                    }, "MarkdownRenderThread");
                    loadingThread.setDaemon(true);
                    loadingThread.start();

                    mainPanel.add(bodyPanel);

                    mainPanel.revalidate();
                    mainPanel.repaint();
                });
            }, "ModrinthProjectThread");
            loadingThread.setDaemon(true);
            loadingThread.start();
        }
    }

    private final class VersionsList extends JPanel implements HasLogger {
        final JLabel loading;
        final JPanel topPanel;
        final JButton backButtonPack;
        final JLabel resultsCount;
        final JCheckBox featuredOnly;

        String projectIdOrSlug;
        EnvSide defaultSide;
        Thread loadingThread;

        VersionsList() {
            loading = createLoadingLabel();

            topPanel = new JPanel();
            topPanel.setLayout(new BorderLayout());

            {
                backButtonPack = createBackButton("< Back to Pack", this, modpackInformationPanel);

                final JButton backButtonHome = createBackButton("Back to Search", this, mainList);

                final JPanel topPanelLeft = new JPanel();
                topPanelLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
                topPanelLeft.add(backButtonPack);
                topPanelLeft.add(backButtonHome);
                topPanel.add(topPanelLeft, BorderLayout.WEST);
            }

            {
                resultsCount = new JLabel();

                featuredOnly = new JCheckBox("Featured only");
                featuredOnly.addActionListener(ev -> loadElements(results -> {
                    for (final Component component : getComponents()) {
                        if (component != topPanel) {
                            remove(component);
                        }
                    }
                }));

                final JPanel topPanelRight = new JPanel();
                topPanelRight.setLayout(new FlowLayout(FlowLayout.RIGHT));
                topPanelRight.add(resultsCount);
                topPanelRight.add(featuredOnly);
                topPanel.add(topPanelRight, BorderLayout.EAST);
            }

            setLayout(new GridBagLayout() {{
                defaultConstraints.fill = GridBagConstraints.HORIZONTAL;
                defaultConstraints.weightx = 1;
                defaultConstraints.gridx = 0;
            }});
        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }

        void loadProject(String projectIdOrSlug, EnvSide defaultSide) {
            removeAll();
            add(loading, new GridBagConstraints()); // Overrides default constraints
            this.projectIdOrSlug = projectIdOrSlug;
            this.defaultSide = defaultSide;
            loadElements(results -> {
                remove(loading);
                add(topPanel);
            });
        }

        void loadElements(Consumer<Version[]> onPreComplete) {
            if (loadingThread != null) {
                while (loadingThread.isAlive()) {
                    Thread.onSpinWait(); // Definitely don't Thread.sleep, because this is probably the AWT event thread
                }
            }
            loadingThread = new Thread(() -> {
                final Version[] results;
                final Map<String, Object> query = new HashMap<>();
                if (featuredOnly.isSelected()) {
                    query.put("featured", true);
                }
                try (
                    InputStream is = SimpleHttp.stream(SimpleHttp.createUrl(Superpack.MODRINTH_API_ROOT, "/project/" + projectIdOrSlug + "/version", query));
                    Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                ) {
                    results = LabrinthGson.GSON.fromJson(reader, Version[].class);
                } catch (Exception e) {
                    getLogger().error("Error requesting versions", e);
                    GeneralUtil.onlyShowErrorMessage(this, "Could not load project versions");
                    SwingUtilities.invokeLater(() -> GeneralUtil.callAction(backButtonPack));
                    return;
                }
                LOGGER.info("Received versions information for {}", projectIdOrSlug);
                SwingUtilities.invokeLater(() -> {
                    onPreComplete.accept(results);
                    for (final Version version : results) {
                        final JButton button = new JButton();
                        button.setLayout(new GridLayout(1, 3));
                        {
                            final JPanel leftPanel = new JPanel();
                            leftPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
                            leftPanel.setOpaque(false);
                            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

                            final JLabel name = new JLabel(version.getName());
                            name.setFont(name.getFont().deriveFont(Font.BOLD));
                            leftPanel.add(name);

                            final StringBuilder info = new StringBuilder("<html><span");
                            switch (version.getVersionType()) {
                                case RELEASE:
                                    info.append(" style=\"color: #00aa00;\"");
                                    break;
                                case BETA:
                                    info.append(" style=\"color: #aaaa00;\"");
                                    break;
                                case ALPHA:
                                    info.append(" style=\"color: #aa0000;\"");
                                    break;
                            }
                            info.append(">&#9679; ")
                                .append(version.getVersionType())
                                .append("</span> &bull; ")
                                .append(version.getVersionNumber())
                                .append("</html>");
                            final JLabel statusAndVersion = new JLabel(info.toString());
                            leftPanel.add(statusAndVersion);

                            button.add(leftPanel);
                        }
                        {
                            final JPanel centerPanel = new JPanel();
                            centerPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
                            centerPanel.setOpaque(false);
                            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

                            final JLabel loaders = new JLabel(
                                Pipelines.iterator(version.getLoaders())
                                    .map(GeneralUtil::capitalize)
                                    .collect(Collectors.joining(", "))
                            );
                            centerPanel.add(loaders);

                            final JLabel gameVersions = new JLabel(String.join(", ", version.getGameVersions()));
                            centerPanel.add(gameVersions);

                            button.add(centerPanel);
                        }
                        final Version.File file = Pipelines.iterator(version.getFiles())
                            .filter(Version.File::isPrimary)
                            .findFirst()
                            .orElse(version.getFiles()[0]);
                        {
                            final JPanel rightPanel = new JPanel();
                            rightPanel.setAlignmentX(JPanel.RIGHT_ALIGNMENT);
                            rightPanel.setOpaque(false);
                            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

                            rightPanel.add(new JLabel("<html><b>" + version.getDownloads() + "</b> downloads</html>"));
                            rightPanel.add(new JLabel(GeneralUtil.getHumanFileSize(file.getSize())));

                            button.add(rightPanel);
                        }
                        add(button);
                        button.addActionListener(ev -> {
                            final ProgressDialog progress = new ProgressDialog(
                                parent,
                                parent.themeDetector,
                                "Download modpack",
                                "Downloading " + file.getFilename()
                            );
                            final File cacheFile = Superpack.getCacheFilePath(file.getHashes().getSha1());
                            final Runnable completed = () -> {
                                progress.setVisible(false);
                                try {
                                    final InstallPackTab dialog = new InstallPackTab(
                                        parent,
                                        cacheFile,
                                        file.getUrl().toExternalForm(),
                                        file.getFilename()
                                    );
                                    if (defaultSide != null) {
                                        dialog.setDefaultSide(defaultSide);
                                    }
                                    parent.openInstallPack(dialog);
                                } catch (IOException e) {
                                    GeneralUtil.showErrorMessage(this, e);
                                }
                            };
                            if (!cacheFile.exists() || cacheFile.length() != file.getSize()) {
                                final Thread downloadThread = new Thread(() -> {
                                    try {
                                        cacheFile.getParentFile().mkdirs();
                                        final String downloadFileSize = GeneralUtil.getHumanFileSize(file.getSize());
                                        final URL downloadUrl = file.getUrl();
                                        progress.getLogger().info("Downloading {}", downloadUrl);
                                        SwingUtilities.invokeLater(() -> {
                                            progress.setMaximum(GeneralUtil.clampToInt(file.getSize()));
                                            progress.setProgress(0);
                                            progress.setString("Downloading file... 0 B / " + downloadFileSize);
                                        });
                                        final MultiMessageDigest digest = new MultiMessageDigest(
                                            GeneralUtil.getSha1(),
                                            MessageDigest.getInstance("SHA-512")
                                        );
                                        long downloadSize;
                                        try (InputStream is = new TrackingInputStream(
                                            new DigestInputStream(SimpleHttp.stream(downloadUrl), digest),
                                            read -> {
                                                if (progress.cancelled()) {
                                                    final InterruptedIOException e = new InterruptedIOException();
                                                    e.bytesTransferred = GeneralUtil.clampToInt(read);
                                                    throw new UncheckedIOException(e);
                                                }
                                                SwingUtilities.invokeLater(() -> {
                                                    progress.setProgress(GeneralUtil.clampToInt(read));
                                                    progress.setString(
                                                        "Downloading file... " +
                                                        GeneralUtil.getHumanFileSize(read) +
                                                        " / " + downloadFileSize
                                                    );
                                                });
                                            }
                                        )) {
                                            downloadSize = Files.copy(is, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                        } catch (IOException e) {
                                            if (e instanceof InterruptedIOException) {
                                                progress.getLogger().info("Download cancelled");
                                                JOptionPane.showMessageDialog(
                                                    this, "Download cancelled", "Download cancelled", JOptionPane.INFORMATION_MESSAGE
                                                );
                                            } else {
                                                progress.getLogger().error("Failed to download " + downloadUrl, e);
                                                GeneralUtil.onlyShowErrorMessage(progress, "Failed to download " + downloadUrl);
                                                progress.setVisible(false);
                                            }
                                            return;
                                        }
                                        progress.getLogger().info("Downloaded " + downloadSize + " bytes");
                                        if (downloadSize != file.getSize()) {
                                            GeneralUtil.showErrorMessage(
                                                progress,
                                                "File size doesn't match! Expected " + file.getSize() + " bytes"
                                            );
                                            progress.setVisible(false);
                                            return;
                                        }
                                        byte[] hash1 = digest.getDigests()[0].digest();
                                        byte[] hash2 = file.getHashes().getSha1();
                                        if (!Arrays.equals(hash1, hash2)) {
                                            progress.getLogger().error(
                                                "SHA-1 doesn't match! Expected {}, got {}",
                                                GeneralUtil.toHexString(hash2),
                                                GeneralUtil.toHexString(hash1)
                                            );
                                            GeneralUtil.onlyShowErrorMessage(progress, "SHA-1 hash doesn't match!");
                                            progress.setVisible(false);
                                            return;
                                        }
                                        hash1 = digest.getDigests()[1].digest();
                                        hash2 = file.getHashes().getSha512();
                                        if (!Arrays.equals(hash1, hash2)) {
                                            progress.getLogger().error(
                                                "SHA-512 doesn't match! Expected {}, got {}",
                                                GeneralUtil.toHexString(hash2),
                                                GeneralUtil.toHexString(hash1)
                                            );
                                            GeneralUtil.onlyShowErrorMessage(progress, "SHA-512 hash doesn't match!");
                                            progress.setVisible(false);
                                            return;
                                        }
                                        SwingUtilities.invokeLater(completed);
                                    } catch (Exception e) {
                                        GeneralUtil.showErrorMessage(progress, e);
                                        progress.setVisible(false);
                                    }
                                }, "PackDownloader");
                                downloadThread.setDaemon(true);
                                downloadThread.start();
                                progress.setVisible(true);
                            } else {
                                LOGGER.info("Using cached file {}", cacheFile);
                                completed.run();
                            }
                        });
                    }
                    resultsCount.setText(results.length + " version" + (results.length == 1 ? "" : "s"));
                    revalidate();
                    repaint();
                });
            }, "ModrinthVersionsThread");
            loadingThread.setDaemon(true);
            loadingThread.start();
        }
    }
}
