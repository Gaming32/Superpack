package io.github.gaming32.superpack.tabs;

import io.github.gaming32.superpack.Superpack;
import io.github.gaming32.superpack.SuperpackMainFrame;
import io.github.gaming32.superpack.SuperpackSettings;
import io.github.gaming32.superpack.themes.Theme;
import io.github.gaming32.superpack.themes.Themes;
import io.github.gaming32.superpack.util.GeneralUtil;
import io.github.gaming32.superpack.util.HasLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;

public final class SettingsTab extends JPanel implements HasLogger, SelectedTabHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(SettingsTab.class);

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final SuperpackMainFrame parent;

    private final JLabel cacheSize;
    private Thread cacheManageThread;

    public SettingsTab(SuperpackMainFrame parent) {
        this.parent = parent;

        GridBagConstraints gbc;
        setLayout(new GridBagLayout());

        {
            final JPanel generalSettings = new JPanel();

            final JLabel themeWarningLabel = new JLabel("Warning: Theme changes may cause visual issues until a restart");
            themeWarningLabel.setForeground(Color.RED);
            themeWarningLabel.setVisible(false);

            final JLabel themeLabel = new JLabel("Theme:");

            final JComboBox<Theme> theme = new JComboBox<>(Themes.getThemes());
            theme.setSelectedItem(SuperpackSettings.INSTANCE.getTheme());
            theme.addActionListener(ev -> {
                themeWarningLabel.setVisible(true);
                //noinspection ConstantConditions
                Superpack.setTheme((Theme)theme.getSelectedItem());
                SwingUtilities.updateComponentTreeUI(parent);
            });

            final GroupLayout layout = new GroupLayout(generalSettings);
            generalSettings.setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(themeLabel)
                    .addComponent(theme)
                )
                .addComponent(themeWarningLabel)
            );
            layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.CENTER)
                    .addComponent(themeLabel)
                    .addComponent(theme)
                )
                .addComponent(themeWarningLabel)
            );
            generalSettings.setBorder(BorderFactory.createTitledBorder("General settings"));

            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(3, 3, 3, 3);
            add(generalSettings, gbc);
        }

        {
            final JPanel cacheSettings = new JPanel();

            cacheSize = new JLabel("Cache size: Calculating...");
            calculateCacheSize();

            final JButton openCache = new JButton("Open cache folder...");
            openCache.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(Superpack.CACHE_DIR);
                } catch (Exception ioe) {
                    GeneralUtil.showErrorMessage(this, ioe);
                }
            });

            final JButton clearCache = new JButton("Clear cache");
            clearCache.addActionListener(e -> {
                cacheManageThread = new Thread(() -> {
                    try {
                        GeneralUtil.rmdir(Superpack.CACHE_DIR.toPath());
                        //noinspection ResultOfMethodCallIgnored
                        Superpack.CACHE_DIR.mkdirs();
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
            cacheSettings.setBorder(BorderFactory.createTitledBorder("Cache settings"));

            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.insets = new Insets(3, 3, 3, 3);
            add(cacheSettings, gbc);
        }
    }

    private void calculateCacheSize() {
        cacheManageThread = new Thread(() -> {
            long size;
            try {
                size = GeneralUtil.getDirectorySize(Superpack.CACHE_DIR.toPath());
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
            SwingUtilities.invokeLater(() -> cacheSize.setText("Cache size: " + GeneralUtil.getHumanFileSizeExtended(size)));
        }, "CalculateCacheSize");
        cacheManageThread.setDaemon(true);
        cacheManageThread.start();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void onSelected() {
        calculateCacheSize();
    }
}
