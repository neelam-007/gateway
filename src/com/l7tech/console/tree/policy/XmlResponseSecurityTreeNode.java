package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;

/**
 * Class <code>XmlDsigResAssertionTreeNode</code> specifies the policy
 * element that represents the soap rsponse signing requirement.
 * <p>
 * @author flascell
 */
public class XmlResponseSecurityTreeNode extends XmlSecurityTreeNode {

    public XmlResponseSecurityTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getBaseName() {
        return "XML Response Security";
    }
}