/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Sort of like a JLabel, but supports wrapping the text inside it.
 *
 * User: mike
 * Date: Sep 9, 2003
 * Time: 2:03:08 PM
 */
public class WrappingLabel extends JTextArea {
    private int setLines = 0;
    private boolean copyMenuEnabled = false;
    private MouseAdapter copyMenuMouseListener = null;
    private JPopupMenu copyMenu = null;

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

    private MouseListener getCopyMenuMouseListener() {
        if (copyMenuMouseListener == null) {
            copyMenuMouseListener = new MouseAdapter() {
                public void mouseReleased(final MouseEvent ev) {
                    if (ev.isPopupTrigger()) {
                        WrappingLabel.this.requestFocus();
                        WrappingLabel.this.selectAll();
                        getCopyMenu().show((Component) ev.getSource(), ev.getX(), ev.getY());
                    }
                }
            };
        }
        return copyMenuMouseListener;
    }

    private JPopupMenu getCopyMenu() {
        if (copyMenu == null) {
            copyMenu = new JPopupMenu();
            JMenuItem copyItem = new JMenuItem("Copy");
            copyItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    WrappingLabel.this.copy();
                }
            });
            copyMenu.add(copyItem);
        }
        return copyMenu;
    }

    public void setCopyMenuEnabled(boolean copyMenuEnabled) {
        this.copyMenuEnabled = copyMenuEnabled;
        if (copyMenuEnabled) {
            this.addMouseListener(getCopyMenuMouseListener());
        } else {
            this.removeMouseListener(getCopyMenuMouseListener());
        }
    }

    public boolean isCopyMenuEnabled() {
        return copyMenuEnabled;
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
}
