/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.TimeRangePropertiesAction;
import com.l7tech.policy.assertion.TimeOfDay;
import com.l7tech.policy.assertion.TimeOfDayRange;
import com.l7tech.policy.assertion.TimeRange;

import javax.swing.*;
import java.util.Calendar;

/**
 * Policy tree node for TimeRange assertion.
 */
public class TimeRangeTreeNode extends LeafAssertionTreeNode<TimeRange> {
    public TimeRangeTreeNode(TimeRange assertion) {
        super(assertion);
    }

    public String getName() {
        if (!assertion.isControlDay() && !assertion.isControlTime()) return "No Availability Defined";

        String nodeName = "Available ";
        if (assertion.isControlDay()) {
            nodeName += week[assertion.getStartDayOfWeek()-1] +
                        " through " + week[assertion.getEndDayOfWeek()-1] + " ";
        }

        if (assertion.isControlTime() && assertion.getTimeRange() != null) {
            TimeOfDayRange tr = assertion.getTimeRange();

            nodeName += "from " + timeToString(utcToLocalTime(tr.getFrom())) + " to " +
                                  timeToString(utcToLocalTime(tr.getTo()));
        }

        return nodeName;
    }

    protected String iconResource(boolean open) {
        // todo, a special icon for this assertion?
        return "com/l7tech/console/resources/time.gif";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new TimeRangePropertiesAction(this);
    }

    private String timeToString(TimeOfDay tod) {
        return (tod.getHour() < 10 ? "0" : "") + tod.getHour() +
               (tod.getMinute() < 10 ? ":0" : ":") + tod.getMinute() +
               (tod.getSecond() < 10 ? ":0" : ":") + tod.getSecond();
    }

    private TimeOfDay utcToLocalTime(TimeOfDay utc) {
        int totOffsetInMin = Calendar.getInstance().getTimeZone().getOffset(System.currentTimeMillis()) / (1000*60);
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

    private static final String[] week = {"Sunday",
                                          "Monday",
                                          "Tuesday",
                                          "Wednesday",
                                          "Thursday",
                                          "Friday",
                                          "Saturday"};
}
