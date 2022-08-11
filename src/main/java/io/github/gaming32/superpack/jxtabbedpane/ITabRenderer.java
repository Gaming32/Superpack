package io.github.gaming32.superpack.jxtabbedpane;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTabbedPane;

public interface ITabRenderer {

    public Component getTabRendererComponent(JTabbedPane tabbedPane, String text, Icon icon, int tabIndex);

}
