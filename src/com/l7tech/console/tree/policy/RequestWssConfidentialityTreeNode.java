package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;

/**
 * Class <code>XmlDsigReqAssertionTreeNode</code> specifies the policy
 * element that represents the soap request signing requirement.
 * <p>
 * @author flascell
 */
public class RequestWssConfidentialityTreeNode extends XmlSecurityTreeNode {

    public RequestWssConfidentialityTreeNode(RequestWssIntegrity assertion) {
        super(assertion);
        data = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getBaseName() {
        return "Encrypt request element " + data.getXpathExpression().getExpression();
    }
    private RequestWssIntegrity data;
}