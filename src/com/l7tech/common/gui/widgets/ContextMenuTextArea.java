/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.event.MouseListener;

/**
 * A JTextArea that supports the context menu cut/copy/paste controls.
 *
 * User: mike
 * Date: Oct 1, 2003
 * Time: 12:42:08 PM
 */
public class ContextMenuTextArea extends JTextArea {
    private MouseListener contextMenuMouseListener;
    private boolean contextMenuEnabled = false;
    private boolean contextMenuAutoSelectAll = false;

    // ----------------------------------------------------------------------
    // constructors

    public ContextMenuTextArea() {
        setContextMenuEnabled(true);
    }

    public ContextMenuTextArea(String text) {
        super(text);
        setContextMenuEnabled(true);
    }

    public ContextMenuTextArea(int rows, int columns) {
        super(rows, columns);
        setContextMenuEnabled(true);
    }

    public ContextMenuTextArea(String text, int rows, int columns) {
        super(text, rows, columns);
        setContextMenuEnabled(true);
    }

    public ContextMenuTextArea(Document doc) {
        super(doc);
        setContextMenuEnabled(true);
    }

    public ContextMenuTextArea(Document doc, String text, int rows, int columns) {
        super(doc, text, rows, columns);
        setContextMenuEnabled(true);
    }

    // ----------------------------------------------------------------------

    private MouseListener getContextMenuMouseListener() {
        if (contextMenuMouseListener == null) {
            contextMenuMouseListener = Utilities.createContextMenuMouseListener(this);
        }
        return contextMenuMouseListener;
    }

    public void setContextMenuEnabled(boolean contextMenuEnabled) {
        this.contextMenuEnabled = contextMenuEnabled;
        if (contextMenuEnabled) {
            this.addMouseListener(getContextMenuMouseListener());
        } else {
            this.removeMouseListener(getContextMenuMouseListener());
        }
    }

    public boolean isContextMenuEnabled() {
        return contextMenuEnabled;
    }

    public boolean isContextMenuAutoSelectAll() {
        return contextMenuAutoSelectAll;
    }

    public void setContextMenuAutoSelectAll(boolean contextMenuAutoSelectAll) {
        this.contextMenuAutoSelectAll = contextMenuAutoSelectAll;
        putClientProperty(Utilities.PROPERTY_CONTEXT_MENU_AUTO_SELECT_ALL,
                          contextMenuAutoSelectAll ? "true" : null);
    }
}
