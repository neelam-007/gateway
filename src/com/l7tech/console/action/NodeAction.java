package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;

import javax.swing.*;

/**
 * The <code>NodeAction</code> is the action that is
 * associated with tree nodes.
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class NodeAction extends BaseAction {
    protected AbstractTreeNode node;
    protected JTree tree;

    /**
     * constructor accepting the node that this action will
     * act on.
     * The tree will be set to <b>null<b/>
     *
     * @param node the node this action will acto on
     */
    public NodeAction(AbstractTreeNode node) {
        this(node, null);
    }

    /**
     * full constructor. Construct the node action with the
     * node and the tree parameters.
     *
     * @param node the node that this action will act on
     * @param tree the tree where the node lives
     */
    public NodeAction(AbstractTreeNode node, JTree tree) {
        this.node = node;
        this.tree = tree;
    }

    /**
     * set the tree that this node is associated with
     *
     * @param tree the tree this node is associated with
     */
    public final void setTree(JTree tree) {
        JTree ot = tree;
        this.tree = tree;
        this.firePropertyChange("tree", ot, tree);
    }

}
