/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.MouseListener;

/**
 * A JTextField that supports the context menu cut/copy/paste controls.
 *
 * User: mike
 * Date: Oct 1, 2003
 * Time: 1:24:06 PM
 */
public class ContextMenuTextField extends JTextField {
    private MouseListener contextMenuMouseListener;
    private boolean contextMenuEnabled = false;
    private boolean contextMenuAutoSelectAll = false;

    // ----------------------------------------------------------------------
    // constructors

    public ContextMenuTextField() {
        setContextMenuEnabled(true);
    }

    public ContextMenuTextField(String text) {
        super(text);
        setContextMenuEnabled(true);
    }

    public ContextMenuTextField(int columns) {
        super(columns);
        setContextMenuEnabled(true);
    }

    public ContextMenuTextField(String text, int columns) {
        super(text, columns);
        setContextMenuEnabled(true);
    }

    public ContextMenuTextField(Document doc, String text, int columns) {
        super(doc, text, columns);
        setContextMenuEnabled(true);
    }

    // ----------------------------------------------------------------------

    private MouseListener getContextMenuMouseListener() {
        if (contextMenuMouseListener == null) {
            contextMenuMouseListener = Utilities.createContextMenuMouseListener(this, new Utilities.DefaultContextMenuFactory() {
                protected boolean shouldIncludeMenu(JTextComponent tc, String menuText) {
                    if (menuText.equals(Utilities.CONTEXT_SELECT_ALL))
                        return !contextMenuAutoSelectAll;
                    return true;
                }
            });
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
