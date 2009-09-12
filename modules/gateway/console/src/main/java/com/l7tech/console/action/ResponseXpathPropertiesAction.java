package com.l7tech.console.action;

import com.l7tech.console.tree.policy.ResponseXpathPolicyTreeNode;
import com.l7tech.policy.assertion.ResponseXpathAssertion;

/**
 * Properties action for node type ResponseXpathPolicyTreeNode.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 * $Id$
 */
public class ResponseXpathPropertiesAction extends XpathBasedAssertionPropertiesAction {
    public ResponseXpathPropertiesAction(ResponseXpathPolicyTreeNode node) {
        super(node, ResponseXpathAssertion.class);
    }
}
