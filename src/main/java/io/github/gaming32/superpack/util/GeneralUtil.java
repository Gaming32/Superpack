package io.github.gaming32.superpack.util;

import java.awt.Component;
import java.awt.Frame;

import javax.swing.JOptionPane;

public final class GeneralUtil {
    private GeneralUtil() {
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, Throwable t) {
        showErrorMessage(owner, "Error", t);
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, String logMessage, Throwable t) {
        owner.getLogger().error(logMessage, t);
        JOptionPane.showMessageDialog(owner, t.getMessage(), t.getClass().getName(), JOptionPane.ERROR_MESSAGE);
    }

    public static <T extends Frame & HasLogger> void showErrorMessage(T owner, String message) {
        showErrorMessage(owner, message, owner.getTitle());
    }

    public static <T extends Component & HasLogger> void showErrorMessage(T owner, String message, String title) {
        owner.getLogger().error(message);
        JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
