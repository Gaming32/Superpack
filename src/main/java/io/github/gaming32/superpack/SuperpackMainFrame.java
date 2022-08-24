package io.github.gaming32.superpack;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
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
import io.github.gaming32.superpack.tabs.MyPacksTab;
import io.github.gaming32.superpack.tabs.SelectedTabHandler;
import io.github.gaming32.superpack.tabs.SettingsTab;
import io.github.gaming32.superpack.util.GeneralUtil;
import io.github.gaming32.superpack.util.HasLogger;

public final class SuperpackMainFrame extends JFrame implements HasLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperpackMainFrame.class);

    public final List<Consumer<String>> iconThemeListeners = new ArrayList<>();
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
    public final OsThemeDetector themeDetector;

    private final JXTabbedPane tabbedPane;

    private final JScrollPane modrinthTabScroll;
    private final ModrinthTab modrinthTab;
    private InstallPackTab installPackTab = null;

    public SuperpackMainFrame(OsThemeDetector themeDetector) {
        super(Superpack.APP_NAME);
        this.themeDetector = themeDetector;
        themeDetector.registerListener(themeListener);

        setDropTarget(new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.rejectDrag();
                    return;
                }
                try {
                    @SuppressWarnings("unchecked")
                    final List<File> files = (List<File>)dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.size() != 1) {
                        dtde.rejectDrag();
                    }
                } catch (Exception e) {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    @SuppressWarnings("unchecked")
                    final List<File> files = (List<File>)dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    openInstallPack(files.get(0));
                } catch (Exception e) {
                    dtde.rejectDrop();
                    GeneralUtil.showErrorMessage(SuperpackMainFrame.this, "Failed to DnD", e);
                }
            }
        }));

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
        {
            final JScrollPane myPacksTabScroll = new JScrollPane(
                new MyPacksTab(this),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );
            myPacksTabScroll.setBorder(BorderFactory.createEmptyBorder());
            myPacksTabScroll.getVerticalScrollBar().setUnitIncrement(16);
            tabbedPane.addTab("My Packs", myPacksTabScroll);
        }
        tabbedPane.addTab("Import from file", new ImportTab(this));
        tabbedPane.addTab("Settings", new SettingsTab(this));

        tabbedPane.addChangeListener(ev -> {
            Component component = tabbedPane.getSelectedComponent();
            if (component instanceof JScrollPane) {
                component = ((JScrollPane)component).getViewport().getView();
            }
            if (component instanceof SelectedTabHandler) {
                ((SelectedTabHandler)component).onSelected();
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

    public void openInstallPack(File file) throws IOException {
        openInstallPack(new InstallPackTab(this, file));
    }

    public void openOnModrinth(ModrinthId projectId) {
        tabbedPane.setSelectedComponent(modrinthTabScroll);
        modrinthTab.openOnModrinth(projectId);
    }
}
