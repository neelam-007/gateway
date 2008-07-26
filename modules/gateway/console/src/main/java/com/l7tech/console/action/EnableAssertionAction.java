package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;

/**
 * The action is to enable an assertion.  The performAction is implemented in the super class.
 *
 * @auther: ghuang
 */
public class EnableAssertionAction extends DisableOrEnableAssertionAction {

    public EnableAssertionAction(AssertionTreeNode node) {
        super(node, ENABLE_ASSERTION_ACTION_IDX);
    }

    public String getName() {
        return "Enable Assertion";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/enableAssertion16.gif";
    }
}
