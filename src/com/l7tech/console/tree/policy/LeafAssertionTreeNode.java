/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Leaf policy nodes extend this node
 */
abstract class LeafAssertionTreeNode extends AssertionTreeNode {
    private static final Logger log =
      Logger.getLogger(LeafAssertionTreeNode.class.getName());

    /**
     * Instantiate the new <code>LeafAssertionTreeNode</code>
     * with the given assertion.
     *
     * @param assertion the assertion
     */
    public LeafAssertionTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    protected void loadChildren() {
    }

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     */
    public boolean receive(AbstractTreeNode node) {
        if (true == super.receive(node)) {
            return true;
        }
        JTree tree = (JTree)TopComponents.getInstance().getComponent(PolicyTree.NAME);
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            Assertion[] nass = node.asAssertions();
            if (nass != null) {
                for (int i = 0; i < nass.length; i++) {
                    Assertion nas = nass[i];
                    AssertionTreeNode as = AssertionTreeNodeFactory.asTreeNode(nas);
                    final MutableTreeNode parent = (MutableTreeNode)getParent();
                    int index = parent.getIndex(this);
                    if (index == -1) {
                        throw new IllegalStateException("Unknown node to the three model " + this);
                    }
                    model.insertNodeInto(as, parent, index + (i + 1));
                }
            } else {
                log.log(Level.WARNING, "The node has no associated assertion " + node);
            }
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
        return true;
    }

    /**
     * True whether this node acceptrs a node
     *
     * @param node the node to accept
     * @return true if policy template node
     */
    public boolean accept(AbstractTreeNode node) {
        return node instanceof PolicyTemplateNode || getParent() != null;
    }
}
