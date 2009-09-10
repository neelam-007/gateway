/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.ThroughputQuotaPropertiesAction;
import com.l7tech.policy.assertion.sla.ThroughputQuota;

import javax.swing.*;

/**
 * Policy tree node for ThroughputQuota assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaTreeNode extends LeafAssertionTreeNode<ThroughputQuota> {
    public ThroughputQuotaTreeNode(ThroughputQuota assertion) {
        super(assertion);
    }

    public String getName(final boolean decorate) {
        final String assertionName = "Apply Throughput Quota";
        final StringBuffer buffer = new StringBuffer(assertionName);
        if (assertion.getCounterStrategy() == ThroughputQuota.DECREMENT) {
            buffer.append(": Decrement counter " + assertion.getCounterName());
        } else {
            buffer.append(": " + assertion.getCounterName() + ": " +
                       assertion.getQuota() + " per " + timeUnitStr(assertion.getTimeUnit()));
        }
        return (decorate)? buffer.toString(): assertionName;
    }

    private String timeUnitStr(int timeUnit) {
        switch (timeUnit) {
            case ThroughputQuota.PER_SECOND: return "second";
            case ThroughputQuota.PER_MINUTE: return "minute";
            case ThroughputQuota.PER_HOUR: return "hour";
            case ThroughputQuota.PER_DAY: return "day";
            case ThroughputQuota.PER_MONTH: return "month";
            default: return "something";
        }
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }

    public Action getPreferredAction() {
        return new ThroughputQuotaPropertiesAction(this);
    }
}
