package com.l7tech.console.action;

import com.l7tech.console.tree.policy.RequestWssIntegrityTreeNode;

/**
 * Action to edit properties of a RequestWssIntegrityTreeNode node.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 22, 2004<br/>
 * $Id$
 */
public class RequestIntegrityPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public RequestIntegrityPropertiesAction(RequestWssIntegrityTreeNode node) {
        super(node);
    }

    public String getName() {
        return "Sign Request Element Properties";
    }
}
