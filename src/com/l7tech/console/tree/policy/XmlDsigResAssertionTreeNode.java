package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;

/**
 * Class <code>XmlDsigResAssertionTreeNode</code> specifies the policy
 * element that represents the soap rsponse signing requirement.
 * <p>
 * @author flascell
 */
public class XmlDsigResAssertionTreeNode extends LeafAssertionTreeNode {

    public XmlDsigResAssertionTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "SOAP response signed";
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[0];
    }

    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
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