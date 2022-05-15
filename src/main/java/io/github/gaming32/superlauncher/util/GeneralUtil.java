package io.github.gaming32.superlauncher.util;

import java.awt.Component;

import javax.swing.JOptionPane;

public final class GeneralUtil {
    private GeneralUtil() {
    }

    public static void showErrorMessage(Component owner, Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(owner, t.getMessage(), t.getClass().getName(), JOptionPane.ERROR_MESSAGE);
    }
}
