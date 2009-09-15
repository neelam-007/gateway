package com.l7tech.policy.assertion;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

import java.util.Calendar;

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
 * @author flascell<br/>
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

    private final static String baseName = "Limit Availability to Time/Days";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<TimeRange>(){
        @Override
        public String getAssertionName( final TimeRange assertion, final boolean decorate) {
            if(!decorate) return baseName;

            final StringBuffer buffer = new StringBuffer("Limit Availability to: ");

            if (!assertion.isControlDay() && !assertion.isControlTime()){
                buffer.append("No Availability Defined");
                return buffer.toString();
            }

            if (assertion.isControlDay()) {
                buffer.append(week[assertion.getStartDayOfWeek()-1] +
                            " through " + week[assertion.getEndDayOfWeek()-1] + " ");
            }

            if (assertion.isControlTime() && assertion.getTimeRange() != null) {
                TimeOfDayRange tr = assertion.getTimeRange();
                buffer.append("from " + timeToString(utcToLocalTime(tr.getFrom())) + " to " +
                                      timeToString(utcToLocalTime(tr.getTo())));
            }
            return buffer.toString();
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"misc"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Restrict service access by time of day and/or day of week.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/time.gif");
        
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.TimeRangePropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Time/Day Availability Properties");
        return meta;
    }

    private static String timeToString(TimeOfDay tod) {
        return (tod.getHour() < 10 ? "0" : "") + tod.getHour() +
               (tod.getMinute() < 10 ? ":0" : ":") + tod.getMinute() +
               (tod.getSecond() < 10 ? ":0" : ":") + tod.getSecond();
    }

    private static TimeOfDay utcToLocalTime(TimeOfDay utc) {
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
    
    private boolean controlTime;
    private boolean controlDay;
    private TimeOfDayRange timeRange;
    // using Calendar.MONDAY, Calendar.TUESDAY, etc
    private int startDayOfWeek;
    // using Calendar.MONDAY, Calendar.TUESDAY, etc
    private int endDayOfWeek;
}
