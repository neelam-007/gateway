package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.console.action.SamlSecurityPropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class <code>XmlDsigReqAssertionTreeNode</code> specifies the policy
 * element that represents the soap request signing requirement.
 * <p/>
 */
public class SamlTreeNode extends LeafAssertionTreeNode {

    public SamlTreeNode(Assertion assertion) {
        super(assertion);
    }

    public String getName() {
        SamlSecurity ss = (SamlSecurity)asAssertion();
        if (ss.getConfirmationMethodType() == SamlSecurity.CONFIRMATION_METHOD_HOLDER_OF_KEY) {
            return "SAML holder-of-key authentication";
        } else if (ss.getConfirmationMethodType() == SamlSecurity.CONFIRMATION_METHOD_SENDER_VOUCHES) {
            return "SAML sender-vouches authentication";
        } else if (ss.getConfirmationMethodType() == SamlSecurity.CONFIRMATION_METHOD_WHATEVER) {
            return "SAML holder-of-key or sender-vouches authentication";
        } else {
            logger.warning("The SAML Security assertion is not configured properly.");
            return "SAML authentication (confirmation not specified)";
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
        list.add(new SamlSecurityPropertiesAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new SamlSecurityPropertiesAction(this);
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