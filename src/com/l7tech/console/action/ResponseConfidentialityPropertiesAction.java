package com.l7tech.console.action;

import com.l7tech.console.tree.policy.ResponseWssConfidentialityTreeNode;

/**
 * Action to edit properties of a ResponseWssConfidentialityTreeNode node.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 22, 2004<br/>
 * $Id$
 */
public class ResponseConfidentialityPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public ResponseConfidentialityPropertiesAction(ResponseWssConfidentialityTreeNode node) {
        super(node);
    }

    public String getName() {
        return "Encrypt Response Element Properties";
    }
}
