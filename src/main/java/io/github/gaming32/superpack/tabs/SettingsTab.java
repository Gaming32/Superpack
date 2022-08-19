package io.github.gaming32.superpack.tabs;

import java.awt.Desktop;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.gaming32.superpack.Superpack;
import io.github.gaming32.superpack.SuperpackMainFrame;
import io.github.gaming32.superpack.util.GeneralUtil;
import io.github.gaming32.superpack.util.HasLogger;

public final class SettingsTab extends JPanel implements HasLogger {
    public static final Logger LOGGER = LoggerFactory.getLogger(SettingsTab.class);

    @SuppressWarnings("unused")
    private final SuperpackMainFrame parent;

    private final JLabel cacheSize;
    private Thread cacheManageThread;

    public SettingsTab(SuperpackMainFrame parent) {
        this.parent = parent;

        setLayout(new GridBagLayout());

        {
            final JPanel cacheSettings = new JPanel();

            cacheSize = new JLabel("Cache size: Calculating...");
            calculateCacheSize();

            final JButton openCache = new JButton("Open cache folder...");
            openCache.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(Superpack.cacheDir);
                } catch (Exception ioe) {
                    GeneralUtil.showErrorMessage(this, ioe);
                }
            });

            final JButton clearCache = new JButton("Clear cache");
            clearCache.addActionListener(e -> {
                cacheManageThread = new Thread(() -> {
                    try {
                        GeneralUtil.rmdir(Superpack.cacheDir.toPath());
                        Superpack.cacheDir.mkdirs();
                        Superpack.downloadCacheDir.mkdirs();
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
            add(cacheSettings);
        }
    }

    public void calculateCacheSize() {
        cacheManageThread = new Thread(() -> {
            long size;
            try {
                size = GeneralUtil.getDirectorySize(Superpack.cacheDir.toPath());
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
