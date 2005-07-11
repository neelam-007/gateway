package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;

import javax.swing.*;


/**
 * The class represents a node element in the TreeModel.
 * It represents the AllAssertion node.
 */
public class AllNode extends AbstractTreeNode {
    /**
     * construct the <CODE>ResponseRegexNode</CODE> instance.
     */
    public AllNode() {
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
        return new AllAssertion();
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
     * specify this node image resource
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }


    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "All assertions must evaluate to true";
    }
}
