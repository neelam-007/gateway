package com.l7tech.console.tree.policy;


import com.l7tech.console.action.SamlPropertiesAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;

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
        StringBuffer sb = new StringBuffer("[");
        int nprops = 0;
        if (ss.isValidateValidityPeriod()) {
            if (nprops > 0) {
                sb.append(", ");
            }
            sb.append("check period");
            nprops++;
        }

        if (ss.isValidateSignature()) {
            if (nprops > 0) {
                sb.append(", ");
            }
            sb.append("require signature");
            nprops++;
        }

        if (nprops == 0) {
            sb.append("no properties set");
        }
        sb.append("]");
        return "SAML Properties " + sb.toString();
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(new SamlPropertiesAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new SamlPropertiesAction(this);
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