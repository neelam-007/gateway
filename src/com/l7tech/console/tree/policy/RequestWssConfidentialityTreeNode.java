package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;

/**
 * Class <code>XmlDsigReqAssertionTreeNode</code> specifies the policy
 * element that represents the soap request signing requirement.
 * <p>
 * @author flascell
 */
public class RequestWssConfidentialityTreeNode extends XmlSecurityTreeNode {

    public RequestWssConfidentialityTreeNode(Assertion assertion) {
        super(assertion);
        if (!(assertion instanceof RequestWssIntegrity)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return the node name that is displayed
     */
    public String getBaseName() {
        return "XML Request Security - Message Elements Encrypted";
    }
}