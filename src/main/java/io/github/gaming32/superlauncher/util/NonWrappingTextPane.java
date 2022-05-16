package io.github.gaming32.superlauncher.util;

import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

/**
 * http://www.java2s.com/Code/Java/Swing-JFC/NonWrappingWrapTextPane.htm
 */
public class NonWrappingTextPane extends JTextPane {
    public NonWrappingTextPane() {
        super();
    }

    public NonWrappingTextPane(StyledDocument doc) {
        super(doc);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getUI().getPreferredSize(this).width <= getParent().getSize().width;
    }
}
