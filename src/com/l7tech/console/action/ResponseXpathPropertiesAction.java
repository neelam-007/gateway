package com.l7tech.console.action;

import com.l7tech.console.tree.policy.ResponseXpathPolicyTreeNode;

/**
 * Properties action for node type ResponseXpathPolicyTreeNode.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 22, 2004<br/>
 * $Id$
 */
public class ResponseXpathPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public ResponseXpathPropertiesAction(ResponseXpathPolicyTreeNode node) {
        super(node);
    }

    public String getName() {
        return "Evaluate Response XPath Properties";
    }
}
