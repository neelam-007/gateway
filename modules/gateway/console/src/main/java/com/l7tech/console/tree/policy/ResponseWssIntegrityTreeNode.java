package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;

/**
 * Class <code>XmlDsigResAssertionTreeNode</code> specifies the policy
 * element that represents the soap rsponse signing requirement.
 * <p>
 * @author flascell
 */
public class ResponseWssIntegrityTreeNode extends XpathBasedAssertionTreeNode {

    public ResponseWssIntegrityTreeNode(ResponseWssIntegrity assertion) {
        super(assertion);
        data = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getBaseName() {
        StringBuffer name;
        if (data.getXpathExpression() == null) {
            name = new StringBuffer("Sign response element " + "[XPath expression not set]");
        } else {
            name = new StringBuffer("Sign response element " + data.getXpathExpression().getExpression());
        }
        name.append(SecurityHeaderAddressableSupport.getActorSuffix(data));
        return name.toString();
    }
    private ResponseWssIntegrity data;
}