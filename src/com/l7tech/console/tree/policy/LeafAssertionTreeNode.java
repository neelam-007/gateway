/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;

/**
 * Leaf policy nodes extend this node
 */
abstract class LeafAssertionTreeNode extends AssertionTreeNode {
    public LeafAssertionTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    protected void loadChildren() {
    }

    /**
     * By default, the leaf node never accepts a node.
     *
     * @param node the node to accept
     * @return always false
     */
    public boolean accept(AbstractTreeNode node) {
        return node instanceof PolicyTemplateNode;
    }
}
