package com.l7tech.console.tree;

import com.l7tech.console.action.RefreshAction;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;

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

    public void performAction() {
        if (tree == null) {
            logger.warning("No tree assigned, ignoring the refresh action");
            return;
        }
        boolean isMultipleSelection  = tree.getSelectionCount() > 1;
        try {
            logger.finest("refreshing tree node type " + node.getClass());
            SwingUtilities.getWindowAncestor(tree);
            tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            AbstractTreeNode rootNode = (AbstractTreeNode) model.getRoot();

            TreeNode parent = node.getParent();
            int index = model.getIndexOfChild(parent, node);

            rootNode.hasLoadedChildren = false;
            model.reload();

            if (parent != null && index >= 0) {
                TreeNode updatedNode = (TreeNode)model.getChild(parent, index);
                TreeNode newParent = updatedNode.getParent();

                if (parent == newParent && !isMultipleSelection) {
                    setInitialSelection(updatedNode);
                }
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

