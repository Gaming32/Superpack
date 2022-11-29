package io.github.gaming32.superpack.util;

import javax.swing.*;
import java.awt.*;

/**
 * Thanks to <a href="https://stackoverflow.com/a/16229082/8840278">this StackOverflow answer</a>
 */
public class PlaceholderTextField extends JTextField {
    private static final long serialVersionUID = 1L;
    private String placeholder;

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    protected void paintComponent(final Graphics pG) {
        super.paintComponent(pG);

        if (placeholder == null || placeholder.length() == 0 || getText().length() > 0) {
            return;
        }

        final Graphics2D g = (Graphics2D) pG;
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getDisabledTextColor());
        g.drawString(placeholder, getInsets().left, pG.getFontMetrics()
            .getMaxAscent() + getInsets().top);
    }

    public void setPlaceholder(final String s) {
        placeholder = s;
    }
}
