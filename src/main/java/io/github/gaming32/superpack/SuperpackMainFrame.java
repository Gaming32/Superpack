package io.github.gaming32.superpack;

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
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.OverlayLayout;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility;
import io.github.gaming32.pipeline.Pipelines;
import io.github.gaming32.superpack.jxtabbedpane.AbstractTabRenderer;
import io.github.gaming32.superpack.jxtabbedpane.JXTabbedPane;
import io.github.gaming32.superpack.labrinth.BaseProject;
import io.github.gaming32.superpack.labrinth.LabrinthGson;
import io.github.gaming32.superpack.labrinth.ModrinthId;
import io.github.gaming32.superpack.labrinth.Project;
import io.github.gaming32.superpack.labrinth.SearchResults;
import io.github.gaming32.superpack.labrinth.Version;
import io.github.gaming32.superpack.util.GeneralUtil;
import io.github.gaming32.superpack.util.HasLogger;
import io.github.gaming32.superpack.util.PlaceholderTextField;
import io.github.gaming32.superpack.util.SimpleHttp;
import io.github.gaming32.superpack.util.SoftCacheMap;

public final class SuperpackMainFrame extends JFrame implements HasLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperpackMainFrame.class);
    private static final String MODRINTH_API_ROOT = "https://api.modrinth.com/v2/";

    private final List<Consumer<String>> iconThemeListeners = new ArrayList<>();
    private final Consumer<Boolean> themeListener = isDark -> SwingUtilities.invokeLater(() -> {
        if (isDark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(this);
        for (final var iconListener : iconThemeListeners) {
            iconListener.accept(isDark ? "/dark" : "/light");
        }
    });
    private final OsThemeDetector themeDetector;

    public SuperpackMainFrame(OsThemeDetector themeDetector) {
        super(SuperpackMain.APP_NAME);
        this.themeDetector = themeDetector;
        themeDetector.registerListener(themeListener);

        final JXTabbedPane tabbedPane = new JXTabbedPane(JTabbedPane.LEFT);
        final AbstractTabRenderer tabRenderer = (AbstractTabRenderer)tabbedPane.getTabRenderer();
        tabRenderer.setPrototypeText("Import from file");
        tabRenderer.setHorizontalTextAlignment(SwingConstants.LEADING);

        {
            final JScrollPane modrinthTab = new JScrollPane(
                new ModrinthPanel(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );
            modrinthTab.setBorder(BorderFactory.createEmptyBorder());
            modrinthTab.getVerticalScrollBar().setUnitIncrement(16);
            tabbedPane.addTab("Modrinth", modrinthTab);
            final int modrinthTabIndex = tabbedPane.getTabCount() - 1;
            iconThemeListeners.add(root -> {
                final String iconPath = root + "/modrinth.png";
                final ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
                tabbedPane.setTabComponentAt(
                    modrinthTabIndex,
                    tabRenderer.getTabRendererComponent(tabbedPane, "Modrinth", icon, modrinthTabIndex)
                );
            });
        }
        tabbedPane.addTab("Import from file", new ImportPanel());
        tabbedPane.addTab("Settings", new SettingsPanel());

        tabbedPane.addChangeListener(ev -> {
            final Component component = tabbedPane.getSelectedComponent();
            if (component instanceof SettingsPanel) {
                ((SettingsPanel)component).calculateCacheSize();
            }
        });

        {
            final boolean isDark = themeDetector.isDark();
            for (final var iconListener : iconThemeListeners) {
                iconListener.accept(isDark ? "/dark" : "/light");
            }
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(tabbedPane);
        pack();
        setSize(960, 540);
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
    }

    private interface HasCachedScrollValue {
        int getCachedScrollValue();
        void setCachedScrollValue(int value);
    }

    private final class ModrinthPanel extends JPanel implements HasLogger, Scrollable {
        static final int THUMBNAIL_SIZE = 64;
        static final int PER_PAGE = 50;

        final ImageIcon placeholderIcon;
        final SoftCacheMap<String, Image> imageCache = new SoftCacheMap<>();

        final MainList mainList;
        final ModpackInformationPanel modpackInformationPanel;
        final VersionsList versionsList;

        ModrinthPanel() {
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

        void loadProjectIcon(BaseProject project, Consumer<Image> completionHandler) {
            // Swing has its own method of downloading and caching images like this, however that lacks
            // paralellism and blocks while it loads the images.
            ForkJoinPool.commonPool().submit(() -> {
                final Image image = imageCache.get(project.getId().getId(), key -> {
                    try {
                        return ImageIO.read(project.getIconUrl()).getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_SMOOTH);
                    } catch (IOException e) {
                        LOGGER.error("Error loading icon for {}", project.getSlug(), e);
                        return null;
                    }
                });
                if (image == null) return;
                SwingUtilities.invokeLater(() -> completionHandler.accept(image));
            });
        }

        JLabel createLoadingLabel() {
            final JLabel loading = new JLabel("Loading...");
            loading.setForeground(new Color(0x2a2c2e));
            loading.setFont(loading.getFont().deriveFont(48f));
            return loading;
        }

        <T extends Component & HasCachedScrollValue> JButton createBackButton(String label, Component from, T to) {
            final JButton backButton = new JButton(label);
            backButton.addActionListener(ev -> {
                ModrinthPanel.this.remove(from);
                ModrinthPanel.this.add(to);
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
                        .addComponent(pageSelector, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(resultsCount)
                    );
                    layout.setVerticalGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(searchField)
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
                        InputStream is = SimpleHttp.createUrl(MODRINTH_API_ROOT, "/search", search).openStream();
                        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                    ) {
                        results = LabrinthGson.GSON.fromJson(reader, SearchResults.class);
                    } catch (Exception e) {
                        getLogger().error("Error requesting modpacks", e);
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
                                modpackInformationPanel.loadProject(project.getId());
                                final JScrollPane scrollPane = (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
                                if (scrollPane != null) {
                                    cachedScrollValue = scrollPane.getVerticalScrollBar().getValue();
                                }
                                ModrinthPanel.this.remove(this);
                                ModrinthPanel.this.add(modpackInformationPanel);
                            });

                            final JLabel icon = new JLabel(placeholderIcon);
                            if (project.getIconUrl() != null) {
                                loadProjectIcon(project, image -> icon.setIcon(new ImageIcon(image)));
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
        }

        private final class ModpackInformationPanel extends JPanel implements HasLogger, HasCachedScrollValue {
            final JPanel mainPanel;
            final JLabel loading;

            Thread loadingThread;
            int cachedScrollValue = 0;

            ModpackInformationPanel() {
                final JButton backButton = createBackButton("< Back to Search", this, mainList);

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

            void loadProject(ModrinthId projectId) {
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
                        InputStream is = SimpleHttp.createUrl(MODRINTH_API_ROOT, "/project/" + projectId, Map.of()).openStream();
                        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                    ) {
                        project = LabrinthGson.GSON.fromJson(reader, Project.class);
                    } catch (Exception e) {
                        getLogger().error("Error requesting project", e);
                        return;
                    }
                    LOGGER.info("Received project information for {}", project.getTitle());
                    SwingUtilities.invokeLater(() -> {
                        mainPanel.removeAll();

                        final JPanel nameAndIcon = new JPanel();
                        nameAndIcon.setLayout(new FlowLayout());

                        final JLabel icon = new JLabel(placeholderIcon);
                        if (project.getIconUrl() != null) {
                            loadProjectIcon(project, image -> icon.setIcon(new ImageIcon(image)));
                        }
                        nameAndIcon.add(icon);

                        final JLabel title = new JLabel(project.getTitle());
                        title.setFont(title.getFont().deriveFont(24f));
                        nameAndIcon.add(title);

                        mainPanel.add(nameAndIcon);

                        JLabel sideNote = null;
                        if (project.getClientSide() == EnvCompatibility.UNSUPPORTED) {
                            sideNote = new JLabel("Server pack");
                        } else if (project.getServerSide() == EnvCompatibility.UNSUPPORTED) {
                            sideNote = new JLabel("Client pack");
                        }
                        if (sideNote != null) {
                            sideNote.setHorizontalAlignment(JLabel.CENTER);
                            sideNote.setAlignmentX(JLabel.CENTER_ALIGNMENT);
                            sideNote.setFont(sideNote.getFont().deriveFont(Font.BOLD));
                            mainPanel.add(sideNote);
                        }

                        final JLabel description = new JLabel("<html>" + project.getDescription() + "</html>", JLabel.CENTER);
                        description.setAlignmentX(JLabel.CENTER_ALIGNMENT);
                        description.setBorder(new EmptyBorder(10, 10, 10, 10));
                        mainPanel.add(description);

                        final JPanel actionPanel = new JPanel();

                        final JButton download = new JButton("Download");
                        download.addActionListener(ev -> {
                            LOGGER.info("Download {}", project.getTitle());
                            versionsList.loadProject(projectId);
                            ModrinthPanel.this.remove(this);
                            ModrinthPanel.this.add(versionsList);
                        });
                        actionPanel.add(download);

                        final JButton viewOnModrinth = new JButton("View on Modrinth");
                        viewOnModrinth.addActionListener(ev -> {
                            try {
                                Desktop.getDesktop().browse(new URI("https://modrinth.com/modpack/" + project.getSlug()));
                            } catch (Exception e) {
                                final String message = "Failed to open " + project.getSlug() + " on Modrinth";
                                LOGGER.error(message, e);
                                JOptionPane.showMessageDialog(this, message, getTitle(), JOptionPane.ERROR_MESSAGE);
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
            final JLabel resultsCount;
            final JCheckBox featuredOnly;

            ModrinthId projectId;
            Thread loadingThread;

            VersionsList() {
                loading = createLoadingLabel();

                topPanel = new JPanel();
                topPanel.setLayout(new BorderLayout());

                {
                    final JButton backButtonHome = createBackButton("< Back to Search", this, mainList);

                    final JButton backButtonPack = createBackButton("Back to Pack", this, modpackInformationPanel);

                    final JPanel topPanelLeft = new JPanel();
                    topPanelLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
                    topPanelLeft.add(backButtonHome);
                    topPanelLeft.add(backButtonPack);
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

            void loadProject(ModrinthId projectId) {
                removeAll();
                add(loading, new GridBagConstraints()); // Overrides default constraints
                this.projectId = projectId;
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
                        InputStream is = SimpleHttp.createUrl(MODRINTH_API_ROOT, "/project/" + projectId + "/version", query).openStream();
                        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                    ) {
                        results = LabrinthGson.GSON.fromJson(reader, Version[].class);
                    } catch (Exception e) {
                        getLogger().error("Error requesting versions", e);
                        return;
                    }
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
                                    case "release":
                                        info.append(" style=\"color: #00aa00;\"");
                                        break;
                                    case "beta":
                                        info.append(" style=\"color: #aaaa00;\"");
                                        break;
                                    case "alpha":
                                        info.append(" style=\"color: #aa0000;\"");
                                        break;
                                }
                                info.append(">&#9679; ")
                                    .append(GeneralUtil.capitalize(version.getVersionType()))
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
                            button.add(new JLabel("<html><b>" + version.getDownloads() + "</b> downloads</html>"));
                            add(button);
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

    private final class ImportPanel extends JPanel implements HasLogger {
        ImportPanel() {
            final JButton[] installPackButtonFR = new JButton[1];
            final JTextField filePathField = new JTextField();
            filePathField.setPreferredSize(new Dimension(500, filePathField.getPreferredSize().height));
            GeneralUtil.addDocumentListener(filePathField, ev ->
                installPackButtonFR[0].setEnabled(ev.getDocument().getLength() > 0)
            );

            final JButton browseButton = new JButton("Browse...");
            browseButton.addActionListener(e -> {
                final File file = FileDialogs.mrpack(this);
                if (file != null) {
                    filePathField.setText(file.getAbsolutePath());
                }
            });

            final JButton installPackButton = new JButton("Install Pack...");
            installPackButtonFR[0] = installPackButton;
            installPackButton.setEnabled(false);
            installPackButton.addActionListener(ev -> {
                final File packFile = new File(filePathField.getText());
                if (!packFile.exists()) {
                    GeneralUtil.showErrorMessage(this, "Pack file does not exist:\n" + filePathField.getText(), getTitle());
                    return;
                }
                try {
                    new InstallPackDialog(SuperpackMainFrame.this, packFile, themeDetector);
                } catch (IOException e) {
                    GeneralUtil.showErrorMessage(this, e);
                }
            });

            final JPanel body = new JPanel();
            GroupLayout layout = new GroupLayout(body);
            body.setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(filePathField)
                    .addComponent(browseButton)
                )
                .addComponent(installPackButton)
            );
            layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                    .addComponent(filePathField)
                    .addComponent(browseButton)
                )
                .addComponent(installPackButton)
            );

            setLayout(new GridBagLayout());
            add(body);
        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }
    }

    private final class SettingsPanel extends JPanel implements HasLogger {
        final JLabel cacheSize;
        Thread cacheManageThread;

        SettingsPanel() {
            setLayout(new GridBagLayout());

            {
                final JPanel cacheSettings = new JPanel();

                cacheSize = new JLabel("Cache size: Calculating...");
                calculateCacheSize();

                final JButton openCache = new JButton("Open cache folder...");
                openCache.addActionListener(e -> {
                    try {
                        Desktop.getDesktop().open(SuperpackMain.cacheDir);
                    } catch (Exception ioe) {
                        GeneralUtil.showErrorMessage(this, ioe);
                    }
                });

                final JButton clearCache = new JButton("Clear cache");
                clearCache.addActionListener(e -> {
                    cacheManageThread = new Thread(() -> {
                        try {
                            GeneralUtil.rmdir(SuperpackMain.cacheDir.toPath());
                            SuperpackMain.cacheDir.mkdirs();
                            SuperpackMain.downloadCacheDir.mkdirs();
                            calculateCacheSize();
                        } catch (Exception ioe) {
                            GeneralUtil.showErrorMessage(this, ioe);
                        }
                    });
                    cacheManageThread.setDaemon(true);
                    cacheManageThread.start();
                });

                final GroupLayout layout = new GroupLayout(cacheSettings);
                cacheSettings.setLayout(layout);
                layout.setAutoCreateGaps(true);
                layout.setAutoCreateContainerGaps(true);
                layout.setHorizontalGroup(layout.createParallelGroup()
                    .addComponent(cacheSize)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(openCache)
                        .addComponent(clearCache)
                    )
                );
                layout.setVerticalGroup(layout.createSequentialGroup()
                    .addComponent(cacheSize)
                    .addGroup(layout.createParallelGroup(Alignment.CENTER)
                        .addComponent(openCache)
                        .addComponent(clearCache)
                    )
                );
                cacheSettings.setBorder(new TitledBorder("Cache settings"));
                add(cacheSettings);
            }
        }

        void calculateCacheSize() {
            cacheManageThread = new Thread(() -> {
                long size;
                try {
                    size = GeneralUtil.getDirectorySize(SuperpackMain.cacheDir.toPath());
                } catch (Exception e) {
                    LOGGER.error("Failed to calculate directory size", e);
                    if (cacheManageThread == Thread.currentThread()) {
                        SwingUtilities.invokeLater(() -> cacheSize.setText("Cache size: Failure"));
                    }
                    return;
                }
                if (cacheManageThread != Thread.currentThread()) {
                    // We were superseded by another calculation thread.
                    return;
                }
                SwingUtilities.invokeLater(() -> cacheSize.setText("Cache size: " + GeneralUtil.getHumanFileSize(size)));
            }, "CalculateCacheSize");
            cacheManageThread.setDaemon(true);
            cacheManageThread.start();
        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }
    }
}
