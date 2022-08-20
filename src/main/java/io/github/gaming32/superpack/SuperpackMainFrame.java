package io.github.gaming32.superpack;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.superpack.jxtabbedpane.AbstractTabRenderer;
import io.github.gaming32.superpack.jxtabbedpane.JXTabbedPane;
import io.github.gaming32.superpack.labrinth.ModrinthId;
import io.github.gaming32.superpack.tabs.ImportTab;
import io.github.gaming32.superpack.tabs.InstallPackTab;
import io.github.gaming32.superpack.tabs.ModrinthTab;
import io.github.gaming32.superpack.tabs.SettingsTab;
import io.github.gaming32.superpack.util.HasLogger;
import lombok.val;

public final class SuperpackMainFrame extends JFrame implements HasLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperpackMainFrame.class);

    private final List<Consumer<String>> iconThemeListeners = new ArrayList<>();
    private final Consumer<Boolean> themeListener = isDark -> SwingUtilities.invokeLater(() -> {
        if (isDark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(this);
        for (val iconListener : iconThemeListeners) {
            iconListener.accept(isDark ? "/dark" : "/light");
        }
    });
    public final OsThemeDetector themeDetector;

    private final JXTabbedPane tabbedPane;

    private final JScrollPane modrinthTabScroll;
    private final ModrinthTab modrinthTab;
    private InstallPackTab installPackTab = null;

    public SuperpackMainFrame(OsThemeDetector themeDetector) {
        super(Superpack.APP_NAME);
        this.themeDetector = themeDetector;
        themeDetector.registerListener(themeListener);

        tabbedPane = new JXTabbedPane(JTabbedPane.LEFT);
        final AbstractTabRenderer tabRenderer = (AbstractTabRenderer)tabbedPane.getTabRenderer();
        tabRenderer.setPrototypeText("Import from file");
        tabRenderer.setHorizontalTextAlignment(SwingConstants.LEADING);

        {
            modrinthTabScroll = new JScrollPane(
                modrinthTab = new ModrinthTab(this),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );
            modrinthTabScroll.setBorder(BorderFactory.createEmptyBorder());
            modrinthTabScroll.getVerticalScrollBar().setUnitIncrement(16);
            tabbedPane.addTab("Modrinth", modrinthTabScroll);
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
        tabbedPane.addTab("Import from file", new ImportTab(this));
        tabbedPane.addTab("Settings", new SettingsTab(this));

        tabbedPane.addChangeListener(ev -> {
            final Component component = tabbedPane.getSelectedComponent();
            if (component instanceof SettingsTab) {
                ((SettingsTab)component).calculateCacheSize();
            }
        });

        {
            final boolean isDark = themeDetector.isDark();
            for (val iconListener : iconThemeListeners) {
                iconListener.accept(isDark ? "/dark" : "/light");
            }
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(tabbedPane);
        pack();
        setSize(960, 540);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void dispose() {
        super.dispose();
        themeDetector.removeListener(themeListener);
        if (installPackTab != null) {
            installPackTab.close();
        }
    }

    public void openInstallPack(InstallPackTab tab) {
        if (installPackTab != null) {
            final int tabIndex = tabbedPane.indexOfComponent(installPackTab);
            installPackTab.close();
            installPackTab = tab;
            tabbedPane.setComponentAt(tabIndex, tab);
            tabbedPane.setSelectedIndex(tabIndex);
        } else {
            installPackTab = tab;
            tabbedPane.addTab("Install Pack", tab);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        }
    }

    public void openOnModrinth(ModrinthId projectId) {
        tabbedPane.setSelectedComponent(modrinthTabScroll);
        modrinthTab.openOnModrinth(projectId);
    }
}
