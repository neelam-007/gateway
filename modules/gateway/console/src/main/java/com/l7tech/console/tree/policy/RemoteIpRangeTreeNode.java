package com.l7tech.console.tree.policy;

import com.l7tech.console.action.RemoteIpRangePropertiesAction;
import com.l7tech.policy.assertion.RemoteIpRange;

import javax.swing.*;

/**
 * Policy tree node for RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 * $Id$<br/>
 *
 */
public class RemoteIpRangeTreeNode extends LeafAssertionTreeNode<RemoteIpRange> {
    public RemoteIpRangeTreeNode(RemoteIpRange assertion) {
        super(assertion);
    }
    public String getName(final boolean decorate) {
        final String assertionName = "Allow / Forbid access to IP Address Range";

        StringBuilder sb = new StringBuilder();
        sb.append((assertion.isAllowRange())? "Allow": "Forbid");
        sb.append(" access to IP Address Range");
        sb.append(" [");
        sb.append(assertion.getStartIp());
        sb.append("/");
        sb.append(assertion.getNetworkMask());
        sb.append("]");
        return (decorate)? sb.toString(): assertionName;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/network.gif";
    }

    public Action getPreferredAction() {
        return new RemoteIpRangePropertiesAction(this);
    }
}
