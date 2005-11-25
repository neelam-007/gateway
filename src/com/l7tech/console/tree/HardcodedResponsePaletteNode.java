package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;

import javax.swing.*;

/**
 * Palette tool tree icon for the Hardcoded Response assertion.
 */
public class HardcodedResponsePaletteNode extends AbstractTreeNode {
    public HardcodedResponsePaletteNode() {
        super(null);
    }

    protected void loadChildren() {
    }

    public String getName() {
        return "Hardcoded Response";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/MessageLength-16x16.gif";
    }

    public Assertion asAssertion() {
        return new HardcodedResponseAssertion();
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
