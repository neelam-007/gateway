/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.XpathBasedAssertionTreeNode;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase;

/**
 * Action that lets the manager admin change the {@link com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext}
 * for an assertion of type {@link com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase}.
 *
 * @author flascelles@layer7-tech.com
 */
public class EditXmlSecurityRecipientContextAction extends NodeAction {
    private XmlSecurityAssertionBase assertion;

    public EditXmlSecurityRecipientContextAction(XpathBasedAssertionTreeNode node) {
        super(node);
        assertion = (XmlSecurityAssertionBase)node.asAssertion();
    }

    public String getName() {
        return "View/Edit XML Security Recipient";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        // todo (plug in the new gui element)
    }
}
