package com.l7tech.console.tree;

import com.l7tech.console.action.RefreshAction;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Class <code>RefreshTreeNodeAction</code> is the general tree node
 * refresh action.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */

public class RefreshTreeNodeAction extends RefreshAction {
    static Logger logger = Logger.getLogger(RefreshTreeNodeAction.class.getName());
    /**
     * create the refresh action for the given tree node.
     * @param node
     */
    public RefreshTreeNodeAction(AbstractTreeNode node) {
        super(node);
        if (node == null) {
            throw new IllegalArgumentException();
        }
    }

    public void performAction() {
        if (tree == null) {
            logger.warning("No tree assigned, ignoring the refresh action");
            return;
        }
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    SwingUtilities.getWindowAncestor(tree);
                    tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    TreePath treePath = new TreePath(node.getPath());
                    if (tree.isExpanded(treePath)) {
                        node.hasLoadedChildren = false;
                        model.reload(node);
                    }
                } finally {
                    tree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}

