/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.console.action.ThroughputQuotaPropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Policy tree node for ThroughputQuota assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaTreeNode extends LeafAssertionTreeNode {

    public ThroughputQuotaTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion instanceof ThroughputQuota) {
            nodeAssertion = (ThroughputQuota)assertion;
        } else
            throw new IllegalArgumentException("assertion passed must be of type " +
              ThroughputQuota.class.getName());
    }
    public String getName() {
        String nodeName = null;
        if (nodeAssertion.getCounterStrategy() == ThroughputQuota.DECREMENT) {
            nodeName = "Decrement counter " + nodeAssertion.getCounterName();
        } else {
            nodeName = "Max Throughput " + nodeAssertion.getCounterName() + ": " +
                       nodeAssertion.getQuota() + " per " + timeUnitStr(nodeAssertion.getTimeUnit());
        }
        return nodeName;
    }

    private String timeUnitStr(int timeUnit) {
        switch (timeUnit) {
            case ThroughputQuota.PER_DAY: return "day";
            case ThroughputQuota.PER_HOUR: return "hour";
            case ThroughputQuota.PER_MONTH: return "month";
            case ThroughputQuota.PER_SECOND: return "second";
            default: return "something";
        }
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }

    public Action getPreferredAction() {
        return new ThroughputQuotaPropertiesAction(this);
    }

    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    public boolean canDelete() {
        return true;
    }

    public ThroughputQuota getAssertion() {return nodeAssertion;}
    private ThroughputQuota nodeAssertion;

}
