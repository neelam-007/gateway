package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;

/**
 * The <code>NodeAction</code> is the action that is
 * associated with tree nodes.
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class NodeAction extends BaseAction {
    protected AbstractTreeNode node;

    public NodeAction(AbstractTreeNode node) {
        this.node = node;
    }
}
