package io.github.gaming32.superpack;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.superpack.jxtabbedpane.AbstractTabRenderer;
import io.github.gaming32.superpack.jxtabbedpane.JXTabbedPane;
import io.github.gaming32.superpack.util.GeneralUtil;

public final class SuperpackMainFrame extends JFrame {
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

        tabbedPane.addTab("Import from file", new ImportPanel());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(tabbedPane);
        pack();
        setSize(960, 540);
        setVisible(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        themeDetector.removeListener(themeListener);
    }

    private final class ImportPanel extends JPanel {
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
                    GeneralUtil.showErrorMessage(this, "Pack file does not exist:\n" + filePathField.getText());
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
    }
}
