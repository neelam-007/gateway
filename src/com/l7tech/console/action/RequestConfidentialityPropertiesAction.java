package com.l7tech.console.action;

import com.l7tech.console.tree.policy.RequestWssConfidentialityTreeNode;

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
        super(node);
    }

    public String getName() {
        return "Encrypt Request Element Properties";
    }
}
