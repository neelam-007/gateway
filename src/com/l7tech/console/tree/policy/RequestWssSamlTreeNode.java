package com.l7tech.console.tree.policy;


import com.l7tech.console.action.EditRequestWssSamlAction;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class <code>RequestWssSamlTreeNode</code> specifies the policy
 * element that represents the SAML Authentication Statement.
 * <p/>
 */
public class RequestWssSamlTreeNode extends LeafAssertionTreeNode {

    public RequestWssSamlTreeNode(Assertion assertion) {
        super(assertion);
    }

    public String getName() {
        RequestWssSaml data = (RequestWssSaml) getUserObject();

        String st = "Unknown Statement";
        if (data.getAuthenticationStatement() != null) {
            st = "Authentication Statement";
        } else if (data.getAttributeStatement() !=null) {
            st = "Attribute Statement";
        } else if (data.getAuthorizationStatement() !=null) {
            st = "Authorization Decision Statement";
        }

        if (data.getVersion() != null && data.getVersion().intValue() != 0) {
            st = "v" + data.getVersion().intValue() + " " + st;
        }

        if (!data.getRecipientContext().localRecipient()) {
            return "SAML "+st+" [\'" + data.getRecipientContext().getActor() + "\' actor]";
        } else {
            return "SAML "+st;
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
        return new EditRequestWssSamlAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlWithCert16.gif";
    }
}