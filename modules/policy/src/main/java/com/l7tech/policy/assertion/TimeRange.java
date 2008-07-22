package com.l7tech.policy.assertion;

/**
 * The time range assertion performs authorization based on the time of day and
 * day of week at which the ssg receives a requests. This allows the ssg administrator
 * to only enable consumption of a published service during a time range and or for
 * a set of specified days.
 *
 * All time properties are UTC.
 *
 * For all properties relating to days, use Calendar.MONDAY, Calendar.TUESDAY, etc
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 18, 2004<br/>
 * $Id$
 * 
 */
public class TimeRange extends Assertion {

    /**
     * default constructor enables neither time of day nor day of week
     * restrictions.
     */
    public TimeRange() {
        controlTime = false;
        controlDay = false;
    }

    /**
     * construct a time range assertion that validates a time of day range only.
     * @param timeOfDayRange all values must be UTC
     */
    public TimeRange(TimeOfDayRange timeOfDayRange) {
        timeRange = timeOfDayRange;
        controlTime = true;
        controlDay = false;
    }

    /**
     * construct a time range assertion that controls day of week only.
     * @param startDayOfWeek using Calendar.MONDAY, Calendar.TUESDAY, etc
     * @param endDayOfWeek using Calendar.MONDAY, Calendar.TUESDAY, etc
     */
    public TimeRange(int startDayOfWeek, int endDayOfWeek) {
        this.startDayOfWeek = startDayOfWeek;
        this.endDayOfWeek = endDayOfWeek;
        controlTime = false;
        controlDay = true;
    }

    /**
     * construct a time range assertion that controls both time of day and day of week
     * @param timeOfDayRange all values must be UTC
     * @param startDayOfWeek using Calendar.MONDAY, Calendar.TUESDAY, etc
     * @param endDayOfWeek using Calendar.MONDAY, Calendar.TUESDAY, etc
     */
    public TimeRange(TimeOfDayRange timeOfDayRange, int startDayOfWeek, int endDayOfWeek) {
        timeRange = timeOfDayRange;
        this.startDayOfWeek = startDayOfWeek;
        this.endDayOfWeek = endDayOfWeek;
        controlTime = true;
        controlDay = true;
    }


    /**
     * whether or not this assertion controls the time of day of incoming requests.
     */
    public boolean isControlTime() {
        return controlTime;
    }

    /**
     * whether or not this assertion controls the time of day of incoming requests.
     */
    public void setControlTime(boolean controlTime) {
        this.controlTime = controlTime;
    }

    /**
     * whether or not this assertion controls the day of week of incoming requests.
     */
    public boolean isControlDay() {
        return controlDay;
    }

    /**
     * whether or not this assertion controls the day of week of incoming requests.
     */
    public void setControlDay(boolean controlDay) {
        this.controlDay = controlDay;
    }

    /**
     * the time range used for the time of day restriction (UTC).
     */
    public TimeOfDayRange getTimeRange() {
        return timeRange;
    }

    /**
     * the time range used for the time of day restriction (UTC).
     */
    public void setTimeRange(TimeOfDayRange timeRange) {
        this.timeRange = timeRange;
    }

    /**
     * the first day allowed as part of the day of week restriction
     */
    public int getStartDayOfWeek() {
        return startDayOfWeek;
    }

    /**
     * the first day allowed as part of the day of week restriction
     */
    public void setStartDayOfWeek(int startDayOfWeek) {
        this.startDayOfWeek = startDayOfWeek;
    }

    /**
     * the last day allowed as part of the day of week restriction
     */
    public int getEndDayOfWeek() {
        return endDayOfWeek;
    }

    /**
     * the last day allowed as part of the day of week restriction
     */
    public void setEndDayOfWeek(int endDayOfWeek) {
        this.endDayOfWeek = endDayOfWeek;
    }

    private boolean controlTime;
    private boolean controlDay;
    private TimeOfDayRange timeRange;
    // using Calendar.MONDAY, Calendar.TUESDAY, etc
    private int startDayOfWeek;
    // using Calendar.MONDAY, Calendar.TUESDAY, etc
    private int endDayOfWeek;
}
