package com.l7tech.console.action;

import com.l7tech.console.tree.policy.RequestAcceleratedXpathPolicyTreeNode;

/**
 * Properties action for node type RequestXpathPolicyTreeNode.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 22, 2004<br/>
 * $Id$
 */
public class RequestAcceleratedXpathPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public RequestAcceleratedXpathPropertiesAction(RequestAcceleratedXpathPolicyTreeNode node) {
        super(node);
    }

    public String getName() {
        return "Evaluate Request XPath Properties (Hardware Accelerated)";
    }
}
