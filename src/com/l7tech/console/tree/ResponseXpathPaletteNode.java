package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ResponseXpathAssertion;

import javax.swing.*;


/**
 * The class represents a node element in the TreeModel.
 * It represents the XPath node.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ResponseXpathPaletteNode extends AbstractTreeNode {
    /**
     * construct the <CODE>RequestXpathPaletteNode</CODE> instance.
     */
    public ResponseXpathPaletteNode() {
        super(null);
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
        return new ResponseXpathAssertion();
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

    /**
     * subclasses override this method
     */
    protected void loadChildren() {}

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Evaluate response XPath expression";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }
}
