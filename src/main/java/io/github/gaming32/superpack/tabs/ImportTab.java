package io.github.gaming32.superpack.tabs;

import io.github.gaming32.superpack.FileDialogs;
import io.github.gaming32.superpack.SuperpackMainFrame;
import io.github.gaming32.superpack.util.GeneralUtilKt;
import io.github.gaming32.superpack.util.HasLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public final class ImportTab extends JPanel implements HasLogger {
    public static final Logger LOGGER = LoggerFactory.getLogger(ImportTab.class);

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final SuperpackMainFrame parent;

    public ImportTab(SuperpackMainFrame parent) {
        this.parent = parent;

        final JButton[] installPackButtonFR = new JButton[1];
        final JTextField filePathField = new JTextField();
        filePathField.setPreferredSize(new Dimension(500, filePathField.getPreferredSize().height));
        GeneralUtilKt.addDocumentListener(filePathField, ev ->
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
                GeneralUtilKt.showErrorMessage(this, "Pack file does not exist:\n" + filePathField.getText(), parent.getTitle());
                return;
            }
            try {
                parent.openInstallPack(new InstallPackTab(parent, packFile));
            } catch (IOException e) {
                GeneralUtilKt.showErrorMessage(this, e);
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
