package com.l7tech.console.action;

import com.l7tech.console.tree.policy.RequestXpathPolicyTreeNode;
import com.l7tech.policy.assertion.RequestXpathAssertion;

/**
 * Properties action for node type RequestXpathPolicyTreeNode.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 * $Id$
 */
public class RequestXpathPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public RequestXpathPropertiesAction(RequestXpathPolicyTreeNode node) {
        super(node, RequestXpathAssertion.class);
    }
}
