package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.console.action.TimeRangePropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Policy tree node for TimeRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 19, 2004<br/>
 * $Id$
 * 
 */
public class TimeRangeTreeNode extends LeafAssertionTreeNode {
    public TimeRangeTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion instanceof TimeRange) {
            nodeAssertion = (TimeRange)assertion;
        } else
            throw new IllegalArgumentException("assertion passed must be of type " + TimeRange.class.getName());
    }
    public String getName() {
        return "Time And Day Availability";
    }

    protected String iconResource(boolean open) {
        // todo, a special icon for this assertion?
        return "com/l7tech/console/resources/time.gif";
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new TimeRangePropertiesAction(this);
    }

    public boolean canDelete() {
        return true;
    }

    public TimeRange getTimeRange() {return nodeAssertion;}

    private TimeRange nodeAssertion;
}
