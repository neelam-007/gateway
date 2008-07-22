package com.l7tech.console.action;

import com.l7tech.console.tree.policy.RequestWssConfidentialityTreeNode;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;

/**
 * Action to edit properties of a RequestWssConfidentialityTreeNode node.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 22, 2004<br/>
 * $Id$
 */
public class RequestConfidentialityPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public RequestConfidentialityPropertiesAction(RequestWssConfidentialityTreeNode node) {
        super(node, RequestWssConfidentiality.class);
    }

    public String getName() {
        return "Encrypt Request Element Properties";
    }
}
