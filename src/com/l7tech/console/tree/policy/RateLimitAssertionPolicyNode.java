package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.RateLimitAssertion;
import com.l7tech.console.action.RateLimitAssertionPropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Node in the policy tree that represents the RateLimitAssertion
 * @see com.l7tech.policy.assertion.RateLimitAssertion
 */
public class RateLimitAssertionPolicyNode extends LeafAssertionTreeNode {
    public RateLimitAssertionPolicyNode(RateLimitAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "Rate Limit";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/disconnect.gif";
    }

    public boolean canDelete() {
        return true;
    }

    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    public Action getPreferredAction() {
        return new RateLimitAssertionPropertiesAction(this);
    }
}
