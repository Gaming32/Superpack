package io.github.gaming32.superpack;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jthemedetecor.OsThemeDetector;

import io.github.gaming32.superpack.util.HasLogger;

public final class ProgressDialog extends JDialog implements HasLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressDialog.class);

    private final Consumer<Boolean> themeListener = isDark -> {
        if (SuperpackSettings.INSTANCE.getTheme().isAffectedBySystem()) {
            SwingUtilities.invokeLater(() -> {
                SuperpackSettings.INSTANCE.getTheme().systemThemeChanged(isDark);
                SwingUtilities.updateComponentTreeUI(this);
            });
        }
    };
    private final OsThemeDetector themeDetector;

    private final JLabel note;
    private final JProgressBar progressBar;
    private boolean openedOnce;

    public ProgressDialog(Frame owner, OsThemeDetector themeDetector, String title, String note) {
        super(owner, title, true);
        this.themeDetector = themeDetector;
        themeDetector.registerListener(themeListener);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.note = new JLabel(note);
        this.note.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        final JPanel cancelPanel = new JPanel();
        cancelPanel.setLayout(new BorderLayout());
        cancelPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        // final JButton cancelButton = new JButton("Cancel");
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(ev -> dispose());
        cancelPanel.add(cancelButton, BorderLayout.EAST);
        cancelPanel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        final JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        root.add(Box.createVerticalGlue());
        root.add(this.note);
        root.add(Box.createVerticalStrut(5));
        root.add(progressBar);
        root.add(Box.createVerticalStrut(5));
        root.add(cancelPanel);
        root.add(Box.createVerticalGlue());

        setContentPane(root);
        pack();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void setVisible(boolean value) {
        if (value) {
            openedOnce = true;
            themeDetector.registerListener(themeListener);
        } else {
            themeDetector.removeListener(themeListener);
        }
        super.setVisible(value);
    }

    public JLabel getNote() {
        return note;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public void setNote(String note) {
        this.note.setText(note);
        pack();
    }

    public void setProgress(int value) {
        progressBar.setValue(value);
    }

    public void setMaximum(int value) {
        progressBar.setMaximum(value);
    }

    public void setIndeterminate(boolean value) {
        progressBar.setIndeterminate(value);
    }

    public void setString(String s) {
        progressBar.setString(s);
    }

    public boolean cancelled() {
        return openedOnce && !isVisible();
    }
}
