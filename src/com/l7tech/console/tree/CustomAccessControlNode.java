package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.CustomAssertion;

import javax.swing.*;

/**
 * The class represents an entity gui node element in the
 * TreeModel.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class CustomAccessControlNode extends AbstractTreeNode {
    /**
     * construct the <CODE>CustomProviderNode</CODE> instance for a given
     * <CODE>id</CODE>
     * 
     * @param ca the e represented by this <CODE>EntityHeaderNode</CODE>
     */
    public CustomAccessControlNode(CustomAssertionHolder ca) {
        super(ca);
        setAllowsChildren(false);
    }

    /**
     * @return always false, custom assertions cannot be deleted from the palette
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Get the set of actions associated with this node.
     * This returns actions that are used buy entity nodes
     * such .
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{};
    }

    /**
     * Test if the node can be deleted. Default for entites
     * is <code>true</code>
     * 
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return false;
    }

    /**
     * @return the assertion this node represents
     */
    public Assertion asAssertion() {
        return (Assertion)super.getUserObject();
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
    }

    public String getName() {
        CustomAssertionHolder cha = (CustomAssertionHolder)asAssertion();
        final CustomAssertion ca = cha.getCustomAssertion();
        String name = ca.getName();
        if (name == null) {
            name = "Unspecified custom assertion (class '" + ca.getClass() + "')";
        }
        return name;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/custom.gif";
    }

    /**
     * Override toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName() + "\n").
          append(super.toString());
        return sb.toString();
    }
}
