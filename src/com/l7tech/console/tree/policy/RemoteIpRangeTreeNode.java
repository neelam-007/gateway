package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.console.action.RemoteIpRangePropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

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
public class RemoteIpRangeTreeNode extends LeafAssertionTreeNode {
    public RemoteIpRangeTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion instanceof RemoteIpRange) {
            nodeAssertion = (RemoteIpRange)assertion;
        } else
            throw new IllegalArgumentException("assertion passed must be of type " +
              RemoteIpRange.class.getName());
    }
    public String getName() {
        String nodeName = "IP address range";
        if (nodeAssertion != null) {
            if (nodeAssertion.isAllowRange()) {
                nodeName += " includes";
            } else nodeName += " excludes";
            nodeName += " [" + nodeAssertion.getStartIp() + "/" + nodeAssertion.getNetworkMask() + "]";
        }
        return nodeName;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/network.gif";
    }

    public Action getPreferredAction() {
        return new RemoteIpRangePropertiesAction(this);
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

    public RemoteIpRange getAssertion() {return nodeAssertion;}

    private RemoteIpRange nodeAssertion;
}
