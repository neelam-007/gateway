package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.RateLimitAssertion;
import com.l7tech.console.action.RateLimitAssertionPropertiesAction;
import com.l7tech.console.panels.RateLimitAssertionPropertiesDialog;

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
        RateLimitAssertion ass = getAssertion();
        int concurrency = ass.getMaxConcurrency();
        StringBuffer sb = new StringBuffer("Rate Limit: ");
        sb.append(ass.isHardLimit() ? "up to " : "average ");
        sb.append(ass.getMaxRequestsPerSecond()).
                append(" msg/sec");
        if (ass.isShapeRequests()) sb.append(", shaped,");
        sb.append(" per ").append(prettyPrintCounterName(ass.getCounterName()));
        if (concurrency > 0) sb.append(" (concurrency ").append(concurrency).append(")");
        return sb.toString();
    }

    private String prettyPrintCounterName(String counterName) {
        String pretty = RateLimitAssertionPropertiesDialog.findCounterNameKey(counterName, null);
        return pretty != null ? pretty : ("\"" + counterName + "\"");
    }

    private RateLimitAssertion getAssertion() {
        return (RateLimitAssertion)getUserObject();
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/disconnect.gif";
    }

    public boolean canDelete() {
        return true;
    }

    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return list.toArray(new Action[0]);
    }

    public Action getPreferredAction() {
        return new RateLimitAssertionPropertiesAction(this);
    }
}
