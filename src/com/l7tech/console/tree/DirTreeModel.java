package com.l7tech.console.tree;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;


/**
 * DirTreeModel extends DefaultTreeModel to override the reload()
 * method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 */
public class DirTreeModel extends DefaultTreeModel {
    /**
     * Creates a new instance of DirTreeModel with newRoot set
     * to the root of this model.
     *
     * @param newRoot
     */
    public DirTreeModel(TreeNode newRoot) {
        super(newRoot);
    }

    /**
     * Invoke this method if you've modified the TreeNode upon which
     * this model depends. The model will notify all of its listeners
     * that the model has changed below the node node.
     *
     * @param node   modifed node
     */
    public void reload(TreeNode node) {
        if (node != null) {
            ((EntityTreeNode)node).loadChildren(true);
            super.reload(node);
        }
    }
}

