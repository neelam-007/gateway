package com.l7tech.console.tree;

import com.l7tech.util.WeakSet;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.util.Set;

/**
 * Class <code>JtreeGroup</code> is a JTree where additonal trees may
 * be attached.
 * <p>
 * It is used as a central coordionation point when multiple trees
 * work together.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class JtreeGroup extends JTree {
    /**
     * Returns a <code>JTree</code> with a sample model.
     * The default model used by the tree defines a leaf node as any node
     * without children.
     *
     * @return a <code>JTree</code> with the default model,
     *		which defines a leaf node as any node without children
     * @see DefaultTreeModel#asksAllowsChildren
     */
    public JtreeGroup() {
        this(null);
    }

    /**
     * Returns a <code>JTree</code> with the specified
     * <code>TreeNode</code> as its root,
     * which displays the root node.
     * By default, the tree defines a leaf node as any node without children.
     *
     * @param root  a <code>TreeNode</code> object
     * @return a <code>JTree</code> with the specified root node
     * @see DefaultTreeModel#asksAllowsChildren
     */
    public JtreeGroup(TreeNode root) {
        super(root);
    }

    /**
     * Sets the <code>TreeModel</code> that will provide the data.
     *
     * @param newModel the <code>TreeModel</code> that is to provide the data
     *        bound: true
     *  description: The TreeModel that will provide the data.
     */
    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
    }

    /**
     * Add the tree to the
     *
     * @param tree the
     */
    public void addJTree(JTree tree) {
        synchronized (associatedTrees) {
            associatedTrees.add(tree);
        }
    }

    private Set associatedTrees = new WeakSet();
}
