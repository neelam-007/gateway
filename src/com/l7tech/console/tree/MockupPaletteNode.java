package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

import javax.swing.*;

/**
 * The tree node in the assertion palette corresponding to the EmailAlertAssertion.
 */
public class MockupPaletteNode extends AbstractTreeNode {
    private final String name;


    public MockupPaletteNode(String name) {
        super(null);
        this.name = name;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{};
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return null;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    protected void loadChildren() {}

    public String getName() {
        return name;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/AnalyzeGatewayLog16x16.gif";
    }
}
