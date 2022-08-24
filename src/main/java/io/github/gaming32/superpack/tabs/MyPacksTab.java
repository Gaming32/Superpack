package io.github.gaming32.superpack.tabs;

import static io.github.gaming32.superpack.util.GeneralUtil.THUMBNAIL_SIZE;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
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
        removeAll();
        for (final Modpack pack : MyPacks.INSTANCE.getPacks()) {
            final JPopupMenu menu = new JPopupMenu();
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

            final JButton button = new JButton();
            final GroupLayout layout = new GroupLayout(button);
            button.setLayout(layout);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            });

            final JLabel icon = new JLabel(modrinthIcon);
            if (pack.getIconUrl() != null) {
                GeneralUtil.loadProjectIcon(pack.getIconUrl(), image -> {
                    if (image != null) {
                        icon.setIcon(new ImageIcon(image));
                    }
                });
            }

            final JLabel title = new JLabel(pack.getName());
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

    @Override
    public void onSelected() {
        if (MyPacks.INSTANCE.isDirty()) {
            MyPacks.INSTANCE.clearDirty();
            loadPacks();
        }
    }
}
