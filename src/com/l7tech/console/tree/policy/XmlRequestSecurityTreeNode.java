package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;

/**
 * Class <code>XmlDsigReqAssertionTreeNode</code> specifies the policy
 * element that represents the soap request signing requirement.
 * <p>
 * @author flascell
 */
public class XmlRequestSecurityTreeNode extends XmlSecurityTreeNode {

    public XmlRequestSecurityTreeNode(Assertion assertion) {
        super(assertion);
        if (!(assertion instanceof XmlRequestSecurity)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return the node name that is displayed
     */
    public String getBaseName() {
        XmlRequestSecurity xass = (XmlRequestSecurity)asAssertion();
        if (xass.hasAuthenticationElement())
            return "XML Request Security - Digital Signature Authentication";
        else 
            return "XML Request Security - Message Elements Signed";
    }
}