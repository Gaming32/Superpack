package io.github.gaming32.superpack;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.superpack.jxtabbedpane.AbstractTabRenderer;
import io.github.gaming32.superpack.jxtabbedpane.JXTabbedPane;
import io.github.gaming32.superpack.labrinth.LabrinthGson;
import io.github.gaming32.superpack.labrinth.SearchResults;
import io.github.gaming32.superpack.util.GeneralUtil;
import io.github.gaming32.superpack.util.HasLogger;
import io.github.gaming32.superpack.util.SimpleHttp;
import io.github.gaming32.superpack.util.SoftCacheMap;

public final class SuperpackMainFrame extends JFrame implements HasLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperpackMainFrame.class);
    private static final String MODRINTH_API_ROOT = "https://api.modrinth.com/v2/";

    private final Consumer<Boolean> themeListener = isDark -> SwingUtilities.invokeLater(() -> {
        if (isDark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(this);
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
            tabbedPane.addTab("Modrinth", modrinthTab);
        }
        tabbedPane.addTab("Import from file", new ImportPanel());
        tabbedPane.addTab("Settings", new SettingsPanel());

        tabbedPane.addChangeListener(ev -> {
            final Component component = tabbedPane.getSelectedComponent();
            if (component instanceof SettingsPanel) {
                ((SettingsPanel)component).calculateCacheSize();
            }
        });

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

    private final class ModrinthPanel extends JPanel implements HasLogger {
        final static int THUMBNAIL_SIZE = 64;

        final Image placeholderImage;
        final SoftCacheMap<String, Image> imageCache = new SoftCacheMap<>();

        final MainList mainList;

        ModrinthPanel() {
            try (InputStream is = getClass().getResourceAsStream("/placeholder.png")) {
                placeholderImage = ImageIO.read(is).getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_SMOOTH);
            } catch (IOException e) {
                throw new IOError(e);
            }

            mainList = new MainList();

            setLayout(new GridBagLayout());

            add(mainList);
        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }

        private final class MainList extends JPanel implements HasLogger {
            static final int PER_PAGE = 50;

            Thread loadingThread;

            MainList() {
                super();

                final JLabel loading = new JLabel("Loading...");
                loading.setForeground(new Color(0x2a2c2e));
                loading.setFont(loading.getFont().deriveFont(48f));

                loadElements(0, results -> {
                    remove(loading);
                });

                // setCellRenderer(new ListCellRenderer<>() {
                //     Map<SearchResults.Result, Component> cachedCells = new IdentityHashMap<>();

                //     @Override
                //     public Component getListCellRendererComponent(
                //         JList<? extends SearchResults.Result> list,
                //         SearchResults.Result value,
                //         int index,
                //         boolean isSelected,
                //         boolean cellHasFocus
                //     ) {
                //         return cachedCells.computeIfAbsent(value, key -> {
                //             final JLabel icon = new JLabel(new ImageIcon(placeholderImage));
                //             return icon;
                //         });
                //     }
                // });

                // setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                // addListSelectionListener(ev -> {
                //     final SearchResults.Result project = getSelectedValue();
                //     if (project == null) return;
                //     System.out.println("Open " + project);
                //     clearSelection();
                // });

                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                add(loading);
            }

            @Override
            public Logger getLogger() {
                return LOGGER;
            }

            void loadElements(int offset, Consumer<SearchResults> onComplete) {
                if (loadingThread != null) {
                    while (loadingThread.isAlive()) {
                        Thread.onSpinWait(); // Definitely don't Thread.sleep, because this is probably the AWT event thread
                    }
                }
                loadingThread = new Thread(() -> {
                    final SearchResults results;
                    try (
                        InputStream is = SimpleHttp.createUrl(
                            MODRINTH_API_ROOT, "/search",
                            Map.of(
                                "facets", "[[\"project_type:modpack\"]]",
                                "offset", offset,
                                "limit", PER_PAGE
                            )
                        ).openStream();
                        Reader reader = new InputStreamReader(is);
                    ) {
                        results = LabrinthGson.GSON.fromJson(reader, SearchResults.class);
                    } catch (Exception e) {
                        getLogger().error("Error requesting modpacks", e);
                        return;
                    }
                    SwingUtilities.invokeLater(() -> {
                        onComplete.accept(results);
                        for (final SearchResults.Result project : results.getHits()) {
                            final JButton button = new JButton();
                            final GroupLayout layout = new GroupLayout(button);
                            button.setLayout(layout);
                            button.addActionListener(ev -> {
                                LOGGER.info("Clicked {}", project.getTitle());
                            });

                            final JLabel icon = new JLabel(new ImageIcon(placeholderImage));
                            final JLabel title = new JLabel(project.getTitle());

                            layout.setAutoCreateGaps(true);
                            layout.setAutoCreateContainerGaps(true);
                            layout.setHorizontalGroup(layout.createSequentialGroup()
                                .addComponent(icon, THUMBNAIL_SIZE, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                                .addComponent(title)
                            );
                            layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(icon, THUMBNAIL_SIZE, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                                .addComponent(title)
                            );
                            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getMaximumSize().height));
                            add(button);

                            if (project.getIconUrl() == null) continue;
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
                                SwingUtilities.invokeLater(() -> {
                                    icon.setIcon(new ImageIcon(image));
                                });
                            });
                        }
                        revalidate();
                        repaint();
                    });
                }, "ModrinthMainLoadingThread");
                loadingThread.start();
            }
        }
    }

    private final class ImportPanel extends JPanel implements HasLogger {
        ImportPanel() {
            final JButton[] installPackButtonFR = new JButton[1];
            final JTextField filePathField = new JTextField();
            filePathField.setPreferredSize(new Dimension(500, filePathField.getPreferredSize().height));
            filePathField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    installPackButtonFR[0].setEnabled(e.getDocument().getLength() > 0);
                }
            });

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
