package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.policy.assertion.TimeOfDay;
import com.l7tech.policy.assertion.TimeOfDayRange;
import com.l7tech.console.action.TimeRangePropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

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
        if (nodeAssertion != null && (nodeAssertion.isControlDay() || nodeAssertion.isControlTime())) {
            String nodeName = "Available ";
            if (nodeAssertion.isControlDay()) {
                nodeName += week[nodeAssertion.getStartDayOfWeek()-1] +
                            " trough " + week[nodeAssertion.getEndDayOfWeek()-1] + " ";
            }

            if (nodeAssertion.isControlTime() && nodeAssertion.getTimeRange() != null) {
                TimeOfDayRange tr = nodeAssertion.getTimeRange();

                nodeName += "from " + timeToString(utcToLocalTime(tr.getFrom())) + " to " +
                                      timeToString(utcToLocalTime(tr.getTo()));
            }

            return nodeName;
        }
        else return "No availability defined";
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

    private String timeToString(TimeOfDay tod) {
        return (tod.getHour() < 10 ? "0" : "") + tod.getHour() +
               (tod.getMinute() < 10 ? ":0" : ":") + tod.getMinute() +
               (tod.getSecond() < 10 ? ":0" : ":") + tod.getSecond();
    }

    private TimeOfDay utcToLocalTime(TimeOfDay utc) {
        int totOffsetInMin = Calendar.getInstance().getTimeZone().getRawOffset() / (1000*60);
        int hroffset = totOffsetInMin/60;
        int minoffset = totOffsetInMin%60;
        int utchr = utc.getHour() + hroffset;
        int utcmin = utc.getMinute() + minoffset;
        while (utcmin >= 60) {
            ++utchr;
            utcmin -= 60;
        }
        while (utchr >= 24) {
            utchr -= 24;
        }
        while (utcmin < 0) {
            --utchr;
            utcmin += 60;
        }
        while (utchr < 0) {
            utchr += 24;
        }
        return new TimeOfDay(utchr, utcmin, utc.getSecond());
    }

    public TimeRange getTimeRange() {return nodeAssertion;}

    private TimeRange nodeAssertion;

    private static final String[] week = {"Sunday",
                                          "Monday",
                                          "Tuesday",
                                          "Wednesday",
                                          "Thursday",
                                          "Friday",
                                          "Saturday"};
}
