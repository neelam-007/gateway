package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;

/**
 * Class <code>XmlDsigReqAssertionTreeNode</code> specifies the policy
 * element that represents the soap request signing requirement.
 * <p>
 * @author flascell
 */
public class XmlRequestSecurityTreeNode extends XmlSecurityTreeNode {

    public XmlRequestSecurityTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getBaseName() {
        return "XML Request Security";
    }
}