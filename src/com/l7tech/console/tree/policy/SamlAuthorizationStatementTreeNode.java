package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class <code>SamlAuthorizationStatementTreeNode</code> specifies the policy
 * element that represents the Saml Authorization Statement.
 * <p/>
 */
public class SamlAuthorizationStatementTreeNode extends LeafAssertionTreeNode {

    public SamlAuthorizationStatementTreeNode(Assertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "SAML Authorization Statement";
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return null;
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
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