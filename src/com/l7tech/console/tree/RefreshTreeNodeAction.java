package com.l7tech.console.tree;

import com.l7tech.console.action.RefreshAction;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;
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
     *
     * @param node
     */
    public RefreshTreeNodeAction(AbstractTreeNode node) {
        super(node);
        if (node == null) {
            throw new IllegalArgumentException();
        }
    }

    protected void performAction() {
        if (tree == null) {
            logger.warning("No tree assigned, ignoring the refresh action");
            return;
        }
        try {
            logger.finest("refreshing tree node type " + node.getClass());
            SwingUtilities.getWindowAncestor(tree);
            tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            TreePath treePath = new TreePath(node.getPath());
            if (tree.isExpanded(treePath)) {
                node.hasLoadedChildren = false;
                model.reload(node);
                setInitialSelection(getInitialSelectedNode());
            }
        } finally {
            tree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Set the initial selection
     */
    protected void setInitialSelection(TreeNode selectNode) {
        if (selectNode == null) {
            logger.fine("Could not select the node, select node is null");
            return;
        }
        if (tree == null) {
            logger.warning("No tree assigned to the action (null)");
            return;
        }
        final TreePath path = new TreePath(((DefaultMutableTreeNode)selectNode).getPath());
        tree.setSelectionPath(path);
        tree.requestFocusInWindow();
    }

    /**
     * Return the node that will be initially selected. The method
     * is protected, to allow customizing the selection by subclasses.
     * Default value is if the tree root is visible returns root, otherwise
     * first root child.
     *
     * @return the inital node to select or null
     */
    protected TreeNode getInitialSelectedNode() {
        if (tree == null || tree.getModel() == null) {
            logger.warning("No tree assigned or tree model null");
            return null;
        }
        TreeNode root = (TreeNode)tree.getModel().getRoot();
        TreeNode selectNode = root;
        if (!tree.isRootVisible()) {
            if (root.getChildCount() > 0) {
                selectNode = root.getChildAt(0);
            }
        }
        return selectNode;
    }

}

