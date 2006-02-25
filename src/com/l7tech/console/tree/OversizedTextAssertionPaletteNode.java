/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.OversizedTextAssertion;

import javax.swing.*;

/**
 * Policy palette tree node for OversizedTextAssertion.
 */
public class OversizedTextAssertionPaletteNode extends AbstractTreeNode {
    public OversizedTextAssertionPaletteNode() {
        super(null);
    }

    protected void loadChildren() {
    }

    public String getName() {
        return "Oversized Element Protection";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/SQLProtection16x16.gif";
    }

    public Assertion asAssertion() {
        return new OversizedTextAssertion();
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return super.getActions();
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return true;
    }
}
