package io.github.gaming32.superpack.tabs;

import static io.github.gaming32.superpack.util.GeneralUtil.THUMBNAIL_SIZE;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.gaming32.superpack.FileDialogs;
import io.github.gaming32.superpack.MyPacks;
import io.github.gaming32.superpack.MyPacks.Modpack;
import io.github.gaming32.superpack.SuperpackMainFrame;
import io.github.gaming32.superpack.util.GeneralUtil;
import io.github.gaming32.superpack.util.HasLogger;

public final class MyPacksTab extends JPanel implements HasLogger, Scrollable, SelectedTabHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyPacksTab.class);

    private final SuperpackMainFrame parent;

    private final ImageIcon modrinthIcon;

    public MyPacksTab(SuperpackMainFrame parent) {
        this.parent = parent;

        modrinthIcon = new ImageIcon(getClass().getResource("/modrinth.png"));

        setLayout(new GridBagLayout() {{
            defaultConstraints.fill = GridBagConstraints.HORIZONTAL;
            defaultConstraints.weightx = 1;
            defaultConstraints.gridx = 0;
        }});

        loadPacks();
    }

    private void loadPacks() {
        final URL hamburgerIcon = MyPacksTab.class.getResource(
            parent.themeDetector.isDark() ? "/dark/hamburger.png" : "/light/hamburger.png"
        );
        removeAll();
        for (final Modpack pack : MyPacks.INSTANCE.getPacks()) {
            final ActionListener installAction = ev -> {
                try {
                    parent.openInstallPack(pack.getPath());
                } catch (Exception e) {
                    GeneralUtil.showErrorMessage(this, "Failed to open Install Pack", e);
                }
            };

            final JPopupMenu menu = new JPopupMenu();
            menu.add("Install").addActionListener(installAction);
            menu.add("Open File Location").addActionListener(ev ->
                GeneralUtil.browseFileDirectory(this, pack.getPath())
            );
            menu.add("Save a Copy").addActionListener(ev -> {
                final File outputFile = FileDialogs.saveMrpack(this);
                if (outputFile == null) return;
                try {
                    Files.copy(pack.getPath().toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    GeneralUtil.showErrorMessage(this, "Failed to copy file", e);
                    return;
                }
                GeneralUtil.browseFileDirectory(this, outputFile);
            });
            menu.add("Remove").addActionListener(ev -> {
                MyPacks.INSTANCE.removePack(pack);
                MyPacks.INSTANCE.setDirty();
                refresh();
            });
            menu.add("Delete From Disk").addActionListener(ev -> {
                pack.getPath().delete();
                MyPacks.INSTANCE.removePack(pack);
                MyPacks.INSTANCE.setDirty();
                refresh();
            });

            final JButton button = new JButton();
            final GroupLayout layout = new GroupLayout(button);
            button.setLayout(layout);
            button.setComponentPopupMenu(menu);
            button.addActionListener(installAction);

            final JLabel icon = new JLabel(modrinthIcon);
            if (pack.getIconUrl() != null) {
                GeneralUtil.loadProjectIcon(pack.getIconUrl(), image -> {
                    if (image != null) {
                        icon.setIcon(new ImageIcon(image));
                    }
                });
            }

            final JLabel title = new JLabel("<html>" + pack.getName() + "</html>");
            title.setFont(title.getFont().deriveFont(24f));

            final JLabel description;
            if (pack.getDescription() != null) {
                description = new JLabel("<html>" + pack.getDescription() + "</html>");
            } else {
                description = null;
            }

            final JPanel details = new JPanel();
            details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
            details.setOpaque(false);
            details.add(title);
            if (description != null) {
                details.add(description);
            }

            final JButton hamburger = new JButton(new ImageIcon(hamburgerIcon));
            hamburger.setHorizontalAlignment(SwingConstants.CENTER);
            hamburger.setFocusPainted(false);
            hamburger.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(icon, THUMBNAIL_SIZE, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .addComponent(details)
                .addComponent(hamburger, 30, 35, 35)
            );
            layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(icon, THUMBNAIL_SIZE, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .addComponent(details)
                .addComponent(hamburger, 30, 35, 35)
            );
            add(button);
        }
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

    private void refresh() {
        LOGGER.debug("Maybe refreshing My Packs");
        if (MyPacks.INSTANCE.isDirty()) {
            LOGGER.info("Refreshing My Packs");
            MyPacks.INSTANCE.clearDirty();
            loadPacks();
        }
    }

    @Override
    public void onSelected() {
        refresh();
    }
}
