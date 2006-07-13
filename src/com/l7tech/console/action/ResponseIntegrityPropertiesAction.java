package com.l7tech.console.action;

import com.l7tech.console.tree.policy.ResponseWssIntegrityTreeNode;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;

/**
 * Action to edit properties of a ResponseWssIntegrityTreeNode node.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 22, 2004<br/>
 * $Id$
 */
public class ResponseIntegrityPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public ResponseIntegrityPropertiesAction(ResponseWssIntegrityTreeNode node) {
        super(node, ResponseWssIntegrity.class);
    }

    public String getName() {
        return "Sign Response Element Properties";
    }
}
