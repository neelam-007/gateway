package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;

/**
 * Class <code>XmlDsigReqAssertionTreeNode</code> specifies the policy
 * element that represents the soap request signing requirement.
 * <p>
 * @author flascell
 */
public class RequestWssIntegrityTreeNode extends XpathBasedAssertionTreeNode {

    public RequestWssIntegrityTreeNode(RequestWssIntegrity assertion) {
        super(assertion);
        data = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getBaseName() {
        if (data.getXpathExpression() == null) return "Sign request element " + "[XPath expression not set]";
        return "Sign request element " + data.getXpathExpression().getExpression();
    }
    private RequestWssIntegrity data;
}