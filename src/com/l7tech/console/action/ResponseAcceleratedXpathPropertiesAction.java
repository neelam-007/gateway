package com.l7tech.console.action;

import com.l7tech.console.tree.policy.ResponseAcceleratedXpathPolicyTreeNode;

/**
 * Properties action for node type ResponseXpathPolicyTreeNode.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 22, 2004<br/>
 * $Id$
 */
public class ResponseAcceleratedXpathPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public ResponseAcceleratedXpathPropertiesAction(ResponseAcceleratedXpathPolicyTreeNode node) {
        super(node);
    }

    public String getName() {
        return "Evaluate Response XPath Properties (Hardware Accelerated)";
    }
}
