/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.policy;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.proxy.policy.assertion.ClientAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Tree cell renderer that knows how to display policy trees.
 * @author mike
 * @version 1.0
 */
public class PolicyTreeCellRenderer extends DefaultTreeCellRenderer {
    public PolicyTreeCellRenderer() {
    }

    /**
     * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value,
                                           sel, expanded,
                                           leaf, row, hasFocus);

        this.setBackgroundNonSelectionColor(tree.getBackground());

        if (!(value instanceof DefaultMutableTreeNode)) {
            System.err.println("ERROR: Got tree node: " + value);
            return this;
        }

        value = ((DefaultMutableTreeNode) value).getUserObject();

        if (!(value instanceof ClientAssertion)) {
            System.err.println("ERROR: Got user object: " + value);
            return this;
        }

        ClientAssertion assertion = (ClientAssertion) value;
        setText(assertion.getName());
        String iconResource = assertion.iconResource(expanded);
        if (iconResource != null) {
            Image image = ImageCache.getInstance().getIcon(iconResource);
            if (image != null) {
                Icon icon = new ImageIcon(image);
                if (icon != null)
                    setIcon(icon);
            }
        }

        return this;
    }
}
