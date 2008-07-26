package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;

/**
 * The action is to disable an assertion.  The performAction is implemented in the super class.
 *
 * @auther: ghuang
 */
public class DisableAssertionAction extends DisableOrEnableAssertionAction {

    public DisableAssertionAction(AssertionTreeNode node) {
        super(node, DISABLE_ASSERTION_ACTION_IDX);
    }

    public String getName() {
        return "Disable Assertion";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/disableAssertion16.gif";
    }
}
