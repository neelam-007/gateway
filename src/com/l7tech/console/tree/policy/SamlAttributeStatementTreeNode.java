package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.EditSamlStatementAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class <code>SamlAttributeStatementTreeNode</code> specifies the policy
 * element that represents the Saml Attribute Statement.
 * <p/>
 */
public class SamlAttributeStatementTreeNode extends LeafAssertionTreeNode {

    private SamlAttributeStatement data;

    public SamlAttributeStatementTreeNode(Assertion assertion) {
        super(assertion);
        data = (SamlAttributeStatement)assertion;
    }

    public String getName() {
        if (!data.getRecipientContext().localRecipient()) {
            return "SAML Attribute Statement [\'" + data.getRecipientContext().getActor() + "\' actor]";
        } else {
            return "SAML Attribute Statement";
        }
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(new EditSamlStatementAction(this));
        list.add(new EditXmlSecurityRecipientContextAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new EditSamlStatementAction(this);
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
        return "com/l7tech/console/resources/SAMLAttributeStatement.gif";
    }
}