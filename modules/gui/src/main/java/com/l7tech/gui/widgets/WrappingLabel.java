/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui.widgets;

import javax.swing.*;
import java.awt.*;

/**
 * Sort of like a JLabel, but supports wrapping the text inside it.
 *
 * User: mike
 * Date: Sep 9, 2003
 * Time: 2:03:08 PM
 */
public class WrappingLabel extends ContextMenuTextArea {
    private int setLines = 0;

    /**
     * Create a new WrappingLabel with no text.
     */
    public WrappingLabel() {
        super();
        initWrappingText();
    }

    /**
     * Create a new WrappingLabel with the specified label text, and no preferred size.
     */
    public WrappingLabel(String text) {
        super(text);
        initWrappingText();
    }

    /**
     * Create a new WrappingLabel with the specified label text, that will have its preferredSize
     * set to a width of (textWidth / lines) and a height of (textHeight * lines).
     *
     * @param text
     */
    public WrappingLabel(String text, int lines) {
        super(text);
        initWrappingText();
        this.setLines = lines;
        initWrappingTextPreferredSize();
    }

    private void initWrappingText() {
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setContextMenuEnabled(false); // off by default, for labels
        setContextMenuAutoSelectAll(true); // when it is turned on, people want to select the whole thing
        setBackground((Color)UIManager.get("Label.background"));
        setForeground((Color)UIManager.get("Label.foreground"));
        setFont((Font)UIManager.get("Label.font"));
    }

    private void initWrappingTextPreferredSize() {
        if (setLines > 0) {
            int fw = getFontMetrics(getFont()).stringWidth(getText());
            int fh = getFontMetrics(getFont()).getHeight();
            setPreferredSize(new Dimension(fw / setLines, fh * setLines));
            invalidate();
        } else {
            setPreferredSize(null);
        }
    }

    public void setText(String text) {
        super.setText(text);
        if (setLines != 0)
            initWrappingTextPreferredSize();
    }

    public void setLines(int lines) {
        this.setLines = lines;
        initWrappingTextPreferredSize();
    }

    public int getLines() {
        return setLines;
    }
}
