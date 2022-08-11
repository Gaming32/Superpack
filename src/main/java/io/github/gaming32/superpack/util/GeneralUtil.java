package io.github.gaming32.superpack.util;

import java.awt.Component;

import javax.swing.JOptionPane;

import io.github.gaming32.superpack.SuperpackMain;

public final class GeneralUtil {
    private GeneralUtil() {
    }

    public static void showErrorMessage(Component owner, Throwable t) {
        showErrorMessage(owner, "Error", t);
    }

    public static void showErrorMessage(Component owner, String logMessage, Throwable t) {
        SuperpackMain.LOGGER.error(logMessage, t);
        JOptionPane.showMessageDialog(owner, t.getMessage(), t.getClass().getName(), JOptionPane.ERROR_MESSAGE);
    }

    public static void showErrorMessage(Component owner, String message) {
        SuperpackMain.LOGGER.error(message);
        JOptionPane.showMessageDialog(owner, message, SuperpackMain.APP_NAME, JOptionPane.ERROR_MESSAGE);
    }
}
