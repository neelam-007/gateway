/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 29, 2008
 * Time: 11:21:44 AM
 * Utility functions used by the implementation of standard reports with Jasper reporting engine
 */
package com.l7tech.gateway.standardreports;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.MessageFormat;
import java.text.DateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;

import com.l7tech.util.*;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.gateway.common.mapping.MessageContextMapping;

public class Utilities {

    private static final String REPORT_DISPLAY_DATE_STRING = "MMM dd, yyyy HH:mm";
    public static final String DATE_STRING = "yyyy/MM/dd HH:mm";
    private static final String HOUR_DATE_STRING = "HH:mm";
    private static final String DAY_HOUR_DATE_STRING = "MM/dd HH:mm";
    //private static final String DAY_DATE_STRING = "E MM/dd";
    private static final String DAY_DATE_STRING = "E MMM d";
    private static final String DAY_DATE_STRING_END = "MMM d";
    private static final String DAY_DATE_ONLY = "d";
    private static final String YEAR_TWO_DIGIT = "yy";
    private static final String WEEK_DATE_STRING = "MM/dd";
    private static final String WEEK_YEAR_DATE_STRING = "yyyy/MM/dd";
    private static final String MONTH_DATE_STRING = "yyyy MMM";

    /**
     * The ';' character is used as a placeholder for sql column values, primiarly because no operation name of
     * value ';' is valid in a wsdl.
     */
    static final String SQL_PLACE_HOLDER = ";";

    private static final Logger logger = Logger.getLogger(Utilities.class.getName());
    private static final long HOUR_IN_MILLISECONDS = 3600000L;
    private static final long DAY_IN_MILLISECONDS = 86400000L;
    private static final long WEEK_IN_MILLISECONDS = 604800000L;
    private static final long MONTH_32DAYS_IN_MILLISECONDS = 2764800000L;
    public static final Integer MAPPING_KEY_MAX_SIZE = 100;
    private static final int SHORT_OPERATION_LENGTH = 20;
    public static final int LONG_OPERATION_LENGTH = 60;
    public static final Integer USAGE_HEADING_VALUE_MAX_SIZE = SHORT_OPERATION_LENGTH;
    public static final int SERVICE_DISPLAY_NAME_LENGTH = 60;

    private static final int OPERATION_STRING_MAX_SIZE = 40;
    public static final int ROUTING_URI_LENGTH = OPERATION_STRING_MAX_SIZE;

    public static enum UNIT_OF_TIME {
        HOUR, DAY, WEEK, MONTH
    }

    public static UNIT_OF_TIME getUnitFromString(String unitOfTime) {
        for (UNIT_OF_TIME u : UNIT_OF_TIME.values()) {
            if (u.toString().equals(unitOfTime)) {
                return u;
            }
        }
        throw new IllegalArgumentException("No unit of time found for param: " + unitOfTime);
    }


    public static final int NUM_MAPPING_KEYS = 5;
    static final String AUTHENTICATED_USER_DISPLAY = "Authenticated User";

    //SQL select fields
    static final String SERVICE_ID = "SERVICE_ID";
    public static final String SERVICE_NAME = "SERVICE_NAME";
    public static final String ROUTING_URI = "ROUTING_URI";
    static final String THROUGHPUT = "THROUGHPUT";
    static final String POLICY_VIOLATIONS = "POLICY_VIOLATIONS";
    static final String ROUTING_FAILURES = "ROUTING_FAILURES";
    static final String FRTM = "FRTM";
    static final String FRTMX = "FRTMX";
    static final String FRTA = "FRTA";
    static final String BRTM = "BRTM";
    static final String BRTMX = "BRTMX";
    static final String AP = "AP";

    static final String CONSTANT_GROUP = "CONSTANT_GROUP";
    public static final String AUTHENTICATED_USER = "AUTHENTICATED_USER";
    static final String SERVICE_OPERATION_VALUE = "SERVICE_OPERATION_VALUE";
    static final String MAPPING_VALUE_1 = "MAPPING_VALUE_1";
    static final String MAPPING_VALUE_2 = "MAPPING_VALUE_2";
    static final String MAPPING_VALUE_3 = "MAPPING_VALUE_3";
    static final String MAPPING_VALUE_4 = "MAPPING_VALUE_4";
    static final String MAPPING_VALUE_5 = "MAPPING_VALUE_5";

    private final static String distinctFrom = "SELECT distinct p.objectid as SERVICE_ID, p.name as SERVICE_NAME, " +
            "p.routing_uri as ROUTING_URI ,'1' as CONSTANT_GROUP";

    private final static String aggregateSelect = "SELECT p.objectid as SERVICE_ID, " +
            "p.name as SERVICE_NAME, p.routing_uri as ROUTING_URI, " +
            "SUM({0}.attempted) as ATTEMPTED, " +
            "SUM({0}.authorized) as AUTHORIZED, " +
            "SUM({0}.front_sum) as FRONT_SUM, " +
            "SUM({0}.back_sum) as BACK_SUM, " +
            "SUM({0}.completed) as THROUGHPUT," +
            "SUM({0}.completed) as COMPLETED," +
            "SUM({0}.attempted)-SUM({0}.authorized) as POLICY_VIOLATIONS, " +
            "SUM({0}.authorized)-SUM({0}.completed) as ROUTING_FAILURES, " +
            " MIN({0}.front_min) as FRTM, " +
            "MAX({0}.front_max) as FRTMX, if(SUM({0}.front_sum), if(SUM({0}.attempted), " +
            "SUM({0}.front_sum)/SUM({0}.attempted),0), 0) as FRTA, MIN({0}.back_min) as BRTM, " +
            "MAX({0}.back_max) as BRTMX, if(SUM({0}.back_sum), if(SUM({0}.completed), " +
            "SUM({0}.back_sum)/SUM({0}.completed),0), 0) as BRTA, " +
            "if(SUM({0}.attempted), ( 1.0 - ( ( (SUM({0}.authorized) - SUM({0}.completed)) / SUM({0}.attempted) ) ) ) , 0) as 'AP'" +
            " ,'1' as CONSTANT_GROUP ";

    private final static String usageAggregateSelect = "SELECT p.objectid as SERVICE_ID, " +
            "p.name as SERVICE_NAME, p.routing_uri as ROUTING_URI, " +
            "SUM(if(smd.completed, smd.completed,0)) as USAGE_SUM,'1' as CONSTANT_GROUP ";

    private final static String mappingJoin = " FROM service_metrics sm, published_service p, service_metrics_details smd," +
            " message_context_mapping_values mcmv, message_context_mapping_keys mcmk WHERE p.objectid = sm.published_service_oid " +
            "AND sm.objectid = smd.service_metrics_oid AND smd.mapping_values_oid = mcmv.objectid AND mcmv.mapping_keys_oid = mcmk.objectid ";

    private final static String noMappingJoin = " FROM service_metrics sm, published_service p WHERE " +
            "p.objectid = sm.published_service_oid ";

    final static String onlyIsDetailDisplayText = "Detail Report";


    /**
     * Relative time is calculated from a fixed point of time in the past depending on the unit of time supplied.
     * See getCalendarForTimeUnit for details on how a Calendar is configured depending on the UNIT_OF_TIME supplied.
     * After the calendar has been retrieved for the UNIT_OF_TIME, the numberOfUnits is subtracted from the calendar,
     * using the Calendar.add method. See getCalendarTimeUnit for where the correct Calendar time unit field is
     * retrieved for the UNIT_OF_TIME supplied.
     * <br>
     * The return value should only be used for the inclusive start time of a time period / interval
     *
     * @param numberOfUnits How many unitOfTime to use
     * @param timeZone      when determing the end of the previous day, week or month
     * @param unitOfTime    valid values are HOUR, DAY, WEEK and MONTH
     * @return long milli seconds since epoch in the past for the start of the time period in the past represented by
     *         the numberOfUnits and unitOfTime
     */
    public static long getRelativeMilliSecondsInPast(int numberOfUnits, UNIT_OF_TIME unitOfTime, String timeZone) {
        if (numberOfUnits < 1) throw new IllegalArgumentException("numberOfUnits cannot be less than 1");

        Calendar calendar = getCalendarForTimeUnit(unitOfTime, timeZone);

        int calendarTimeUnit = getCalendarTimeUnit(unitOfTime);
        calendar.add(calendarTimeUnit, numberOfUnits * -1);
        return calendar.getTimeInMillis();
    }

    /**
     * Get the correct Calendar field which represents the unitOfTime supplied. The returned int can be used
     * as the calendar field parameter to Calendar.add()
     *
     * @param unitOfTime what unit of time we want the corresponding Calendar field for
     * @return the calendar field which represents the unitOfTime parameter
     * @throws IllegalArgumentException if the unit of time cannot be mapped to a calendar field
     */
    private static int getCalendarTimeUnit(UNIT_OF_TIME unitOfTime) {
        switch (unitOfTime) {
            case HOUR:
                return Calendar.HOUR_OF_DAY;
            case DAY:
                return Calendar.DAY_OF_MONTH;
            case WEEK:
                return Calendar.WEEK_OF_YEAR;
            case MONTH:
                return Calendar.MONTH;
        }
        throw new IllegalArgumentException(unitOfTime.toString() + " is not supported");
    }

    /**
     * Get the resolution to use in queries. Hourly is returned when ever an Hourly resolution is required. Otherwise
     * daily is returned.
     *
     * Hourly resolution is required when either:
     * <ul>
     *     <li>Time is relative with HOUR as its unit</li>
     *     <li>Time does not start at the beginning of day or end of day.</li>
     * </ul>
     *
     * So hourly is required when ever we cannot safely use daily bins. Any fraction of a day requires hourly bins.
     *
     * Do not call from interval reports. Call {@link #getIntervalResolutionFromTimePeriod(com.l7tech.gateway.standardreports.Utilities.UNIT_OF_TIME, Long, Long, String, Boolean, com.l7tech.gateway.standardreports.Utilities.UNIT_OF_TIME)} instead
     *
     * @param startTimeMilli start of time period, in milliseconds, since epoch
     * @param endTimeMilli   end of time period, in milliseconds, since epoch
     * @param timeZone       which timezone the epoch timeMillilSeconds parameter should be converted into
     * @param isRelative true if the reports configuration uses a relative time
     * @param timeUnit time unit if isRelative is true, can be null otherwise
     * @return resolution value. Either 1 for hourly or 2 for daily.
     */
    public static Integer getSummaryResolutionFromTimePeriod(final Long startTimeMilli,
                                                             final Long endTimeMilli,
                                                             final String timeZone,
                                                             Boolean isRelative,
                                                             UNIT_OF_TIME timeUnit) {
        validateStartBeforeEndTime(startTimeMilli, endTimeMilli);

        // Hourly unit of time - then must use HOURLY
        if (isRelative && timeUnit == UNIT_OF_TIME.HOUR) {
            return 1;
        } else if (!isRelative) {
            // If is relative and not hourly then guaranteed to not be fractional (fractional means not a full day).
            // Fractional time requires HOURLY resolution
            Calendar calendar = Calendar.getInstance(getTimeZone(timeZone));
            calendar.setTimeInMillis(startTimeMilli);
            final int startHour = calendar.get(Calendar.HOUR_OF_DAY);

            calendar.setTimeInMillis(endTimeMilli);
            final int endHour = calendar.get(Calendar.HOUR_OF_DAY);

            if (startHour != 0 || endHour != 0) {
                return 1;
            }
        }

        final boolean forceHourly = ConfigFactory.getBooleanProperty("com.l7tech.gateway.standardreports.forcehourlyresolution", false);
        return (forceHourly)? 1: 2;
    }

    /**
     * Get the resolution to use in interval queries.       <br>
     * This method delegates to {@link #getSummaryResolutionFromTimePeriod(Long, Long, String, Boolean, com.l7tech.gateway.standardreports.Utilities.UNIT_OF_TIME)}
     * after checking if the relativeTimeUnit is UNIT_OF_TIME.HOUR
     *
     * @param intervalTimeUnit What interval is required. HOUR, DAY, WEEK or MONTH
     * @param startTimeMilli   start of time period, in milliseconds, since epoch
     * @param endTimeMilli     end of time period, in milliseconds, since epoch
     * @param timeZone         which timezone the epoch timeMillilSeconds parameter should be converted into
     * @return Integer 1 for hourly metric bin or 2 for daily metric bin. If UNIT_OF_TIME was hour, then the return
     *         value can only ever be 1.
     * @throws IllegalArgumentException if the start time >= end time
     */
    public static Integer getIntervalResolutionFromTimePeriod(UNIT_OF_TIME intervalTimeUnit,
                                                              Long startTimeMilli,
                                                              Long endTimeMilli,
                                                              final String timeZone,
                                                              Boolean isRelative,
                                                              UNIT_OF_TIME relativeTimeUnit) {
        validateStartBeforeEndTime(startTimeMilli, endTimeMilli);

        if (intervalTimeUnit == UNIT_OF_TIME.HOUR) {
            return 1;
        }

        return getSummaryResolutionFromTimePeriod(startTimeMilli, endTimeMilli, timeZone, isRelative, relativeTimeUnit);
    }

    /**
     * Validates that startTimeMilli < endTimeMilli. A helper method for all functions which accept a start and a end
     * time
     *
     * @param startTimeMilli milliseconds since epoch
     * @param endTimeMilli   milliseconds since epoch
     * @throws IllegalArgumentException if startTimeMilli >= endTimeMilli
     */
    private static void validateStartBeforeEndTime(long startTimeMilli, long endTimeMilli) {
        if (startTimeMilli >= endTimeMilli) throw new IllegalArgumentException("Start time must be before end time");
    }

    /**
     * Get the date string representation of a time value in milliseconds, for a specific timezone
     *
     * @param timeMilliSeconds the number of milliseconds since epoch
     * @param timeZone         which timezone the epoch timeMillilSeconds parameter should be converted into
     * @return a date in the format MMM dd, yyyy HH:mm, this format was chosen as to minimize confusion across local's
     *         without having to deal explicitly with locals in this function. The format should not cause confusion due to the
     *         string month representation and the 4 digit year
     */
    public static String getMilliSecondAsStringDate(Long timeMilliSeconds, String timeZone) {
        TimeZone tz = getTimeZone(timeZone);
        SimpleDateFormat dateFormat = new SimpleDateFormat(REPORT_DISPLAY_DATE_STRING);
        dateFormat.setTimeZone(tz);
        //timezone not really needed as we don't modify the calendar with add() operations
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(timeMilliSeconds);
        return dateFormat.format(cal.getTime());
    }

    /**
     * Get a string representation of the interval selection represented by unitOfTime and numIntervalUnits. The
     * formatted string will have the correct pluralisation if the numberOfUnits is > 1.
     *
     * @param unitOfTime       the unit of time. This parameter is used for it's toString value
     * @param numIntervalUnits if > 1 then the unitOfTime.toString will have a 's' appended to it
     * @return the string representation of the interval represented by the supplied paramaters
     * @throws NullPointerException     if unitOfTime is null
     * @throws IllegalArgumentException if numIntervalUnits < 1
     */
    public static String getIntervalAsString(UNIT_OF_TIME unitOfTime, int numIntervalUnits) {
        if (unitOfTime == null) throw new NullPointerException("unitOfTime cannot be null");
        if (numIntervalUnits < 1) throw new IllegalArgumentException("numIntervalUnits must be greater than 0");

        StringBuilder sb = new StringBuilder();
        String unit = unitOfTime.toString();
        sb.append(unit.substring(0, 1).toUpperCase());
        sb.append(unit.substring(1, unit.length()).toLowerCase());
        if (numIntervalUnits > 1) sb.append("s");
        return sb.toString();
    }

    /**
     * Get the date to display on a report. The timeMilliSecond value since epoch will be converted into a suitable
     * format to use in the report as the interval information.
     * <br>
     * The interval string representation depends on the UNIT_OF_TIME specified in intervalUnitOfTime. Each string
     * representation carries enough information that if a major time boundary is crossed, it is clear it has happened
     * and no two intervals can be confused.
     * <br>
     * Here are the various formats, depending on the UNIT_OF_TIME specified in intervalUnitOfTime<br>
     * HOUR: MM/dd HH:MM - HH:MM<br>
     * DAY: E MMM d, when the interval num units is 1, E MMM d-d when the num interval units is > 1, E MMM d-MMM d when
     * the num interval units is > 1 and a month boundary is crossed. If a year boundary is also crossed in this condition,
     * then 'yy is apppended to the date string.<br>
     * when a new year is crossed it is highlighed by including the string month value before the day<br>
     * WEEK: MM/dd - MM/dd, when a new year is crossed it is highlighed by including the string year before the month,
     * yyyy/MM/dd - MM/dd<br>
     * MONTH: yyyy MMM<br>
     * These are not perfect and some redundant info is shown, e.g. for all weeks in January the year will also be shown
     * for WEEK. Could be made smarter by looking at what the week number is, however any unit may be plural so the
     * first week of an interval in January may be the 3rd week and not just the first week.
     *
     * @param startIntervalMilliSeconds milli second value since epoch
     * @param endIntervalMilliSeconds   milli second value since epoch
     * @param intervalUnitOfTime        HOUR, DAY, WEEK or MONTH
     * @param numberOfTimeUnits         use to help valid the interval represented between the start and end milli second
     *                                  times. also used for determing the format of the string returned. End time not included in the returned String if
     *                                  the value is <= 1
     * @param timeZone                  what timezone to use when converting the epoch start and end time values into strings
     * @return String representing the interval, to be shown alongside the interval data.
     * @throws IllegalArgumentException if the startIntervalMilliSeconds >= endIntervalMilliSeconds
     *                                  or **NOT THROWN ANYMORE** if the difference between the startIntervalMilliSeconds and the
     *                                  endIntervalMilliSeconds is greater than the difference allowed for the UNIT_OF_TIME in intervalUnitOfTime
     */
    public static String getIntervalDisplayDate(Long startIntervalMilliSeconds, Long endIntervalMilliSeconds,
                                                UNIT_OF_TIME intervalUnitOfTime, Integer numberOfTimeUnits, String timeZone) {
        validateStartBeforeEndTime(startIntervalMilliSeconds, endIntervalMilliSeconds);
//        checkTimeDifferenceWithinRange(intervalUnitOfTime, numberOfTimeUnits, startIntervalMilliSeconds, endIntervalMilliSeconds);

        TimeZone tz = getTimeZone(timeZone);
        Calendar calStart = Calendar.getInstance(tz);
        calStart.setTimeInMillis(startIntervalMilliSeconds);

        Calendar calEnd = Calendar.getInstance(tz);
        calEnd.setTimeInMillis(endIntervalMilliSeconds);

        SimpleDateFormat weekDateFormat = new SimpleDateFormat(WEEK_DATE_STRING);
        weekDateFormat.setTimeZone(tz);

        switch (intervalUnitOfTime) {
            case HOUR:
                SimpleDateFormat hourDateFormat = new SimpleDateFormat(HOUR_DATE_STRING);
                hourDateFormat.setTimeZone(tz);
                SimpleDateFormat dateHourDateFormat = new SimpleDateFormat(DAY_HOUR_DATE_STRING);
                dateHourDateFormat.setTimeZone(tz);
                return dateHourDateFormat.format(calStart.getTime()) + " - " +
                        hourDateFormat.format(calEnd.getTime());
            case DAY:
                //if the years are different from start to end date, then we are showing the end date year
                boolean showYear = calStart.get(Calendar.YEAR) != calEnd.get(Calendar.YEAR);
                //if the months are different show it for both start and end month
                boolean showBothMonths = calStart.get(Calendar.MONTH) != calEnd.get(Calendar.MONTH);

                SimpleDateFormat dateDateFormat = new SimpleDateFormat(DAY_DATE_STRING);
                dateDateFormat.setTimeZone(tz);
                SimpleDateFormat endDateFormat;
                if (showBothMonths) {
                    endDateFormat = new SimpleDateFormat(DAY_DATE_STRING_END);
                } else {
                    endDateFormat = new SimpleDateFormat(DAY_DATE_ONLY);
                }

                endDateFormat.setTimeZone(tz);

                StringBuilder sb = new StringBuilder();
                if (numberOfTimeUnits > 1) {
                    sb.append(dateDateFormat.format(calStart.getTime())).append("-").append(endDateFormat.format(calEnd.getTime()));
                } else {
                    sb.append(dateDateFormat.format(calStart.getTime()));
                }

                if (showYear && showBothMonths) {
                    //only want to show the year when were showing two months, and end month is in the new year
                    SimpleDateFormat yearFormat = new SimpleDateFormat(YEAR_TWO_DIGIT);
                    yearFormat.setTimeZone(tz);
                    sb.append(" '").append(yearFormat.format(calEnd.getTime()));
                }
                return sb.toString();

            case WEEK:
                if (calStart.get(Calendar.MONTH) == Calendar.JANUARY) {
                    SimpleDateFormat weekYearDateFormat = new SimpleDateFormat(WEEK_YEAR_DATE_STRING);
                    weekYearDateFormat.setTimeZone(tz);
                    return weekYearDateFormat.format(calStart.getTime()) + " - " +
                            weekDateFormat.format(calEnd.getTime());
                }
                return weekDateFormat.format(calStart.getTime()) + " - " +
                        weekDateFormat.format(calEnd.getTime());
            case MONTH:
                SimpleDateFormat monthDateFormat = new SimpleDateFormat(MONTH_DATE_STRING);
                monthDateFormat.setTimeZone(tz);
                return monthDateFormat.format(calStart.getTime());

        }
        return null;
    }

    /**
     * Validate that the difference between startIntervalMilliSeconds and endIntervalMilliSeconds is not greater than
     * the unit of time represented by unitOfTime
     *
     * WARNING: Does not support Day Light Savings.
     *
     * @param unitOfTime             which UNIT_OF_TIME we will use as the range
     * @param numberOfTimeUnits      used to validate the time interval represented by start and end milli second args
     * @param startRangeMilliSeconds the start of the range in milliseconds since epoch
     * @param endRangeMilliSeconds   the end of the range in milliseconds since epoch
     * @throws IllegalArgumentException if the startRangeMilliSeconds >= endRangeMilliSeconds
     */
    private static void checkTimeDifferenceWithinRange(UNIT_OF_TIME unitOfTime, int numberOfTimeUnits,
                                                       Long startRangeMilliSeconds, Long endRangeMilliSeconds) {
        validateStartBeforeEndTime(startRangeMilliSeconds, endRangeMilliSeconds);
        long diff = endRangeMilliSeconds - startRangeMilliSeconds;
        switch (unitOfTime) {
            case HOUR:
                if (diff > HOUR_IN_MILLISECONDS * numberOfTimeUnits)
                    throw new IllegalArgumentException("Difference between start and end " +
                            "range millisecond times is greater than an hour * " + numberOfTimeUnits);
                break;
            case DAY:
                if (diff > DAY_IN_MILLISECONDS * numberOfTimeUnits)
                    throw new IllegalArgumentException("Difference between start and end " +
                            "range millisecond times is greater than a day * " + numberOfTimeUnits);
                break;
            case WEEK:
                if (diff > WEEK_IN_MILLISECONDS * numberOfTimeUnits)
                    throw new IllegalArgumentException("Difference between start and end " +
                            "range millisecond times is greater than a week * " + numberOfTimeUnits);
                break;
            case MONTH:
                if (diff > MONTH_32DAYS_IN_MILLISECONDS * numberOfTimeUnits)
                    throw new IllegalArgumentException("Difference between start and end " +
                            "range millisecond times is greater than a 32 day month * " + numberOfTimeUnits);
                break;
        }
    }

    /**
     * Get the number of milliseconds representing the end of the period represtented by the unitOfTime
     * See getCalendarForTimeUnit for how a Calendar is configured based on the UNIT_OF_TIME supplied.
     * Once the Calendar is retrieved it's time in milliseconds is retrieved and that is the return value.
     * When calculating end times with the returned value the actual value should not be included. End times are
     * always exclusive in report time period and intervals.
     * <br>
     * For example a report over the last day runs 00:00 to 00:00, in code the time period is >= 00:00 and < 00:00,
     * which gives us the entire contents of the day, and nothing from the next day
     *
     * @param unitOfTime for which we want the millisecond value of. The rules for getting this value are specified in
     *                   getCalendarForTimeUnit.
     * @param timeZone   The timezone is very important for configuring the calendar. The epoch value for the end of
     *                   the last day, week and month are 100% dependant on the timezone chosen.
     * @return the millisecond value since epoch of the end of the time period specified by unitOfTime
     */
    public static long getMillisForEndTimePeriod(UNIT_OF_TIME unitOfTime, String timeZone) {
        return Utilities.getCalendarForTimeUnit(unitOfTime, timeZone).getTimeInMillis();
    }

    /**
     * Get a Calendar object, correctly configured to be at exactly the end time of the UNIT_OF_TIME specified, for the
     * timezone specified.<br>
     * The rules are as follows, for the various values of UNIT_OF_TIME<br>
     * HOUR: A calendar whos minute, second and millisecond values are all set to 0. The calendar is exactly at the
     * very start of the CURRENT hour.<br>
     * DAY: A calendar who's hour, minute, second and millisecond values are all set to 0. The calendar is exactly at the
     * very start of the CURRENT day.<br>
     * WEEK:A calendar who's hour, minute, second and millisecond values are all set to 0. The calendar is exactly at the
     * very start of the CURRENT day. Note this is the same as DAY<br>
     * MONTH:A calendar who's day of month is set to 1 and hour, minute, second and millisecond values are all set to 0.
     * The calendar is exactly at the very start of the CURRENT month.<br>
     * This function is primiarly used by relative time functions. The milli second value of the returned Calendar, as is,
     * is the end of the time period / interval. To get the start use the Calendar.add method with the correct field
     * and a minus value to move back in time in fixed amounts, always arriving at the very start of the time period
     * represented by UNIT_OF_TIME.<br>
     * Note: Where ever this calendar is used, any add functions should be using the Calendar field which matches
     * the UNIT_OF_TIME the returned Calendar has been configured with. See getCalendarTimeUnit
     *
     * @param unitOfTime for which we want the millisecond value of. The rules for getting this value are specified in
     *                   getCalendarForTimeUnit.
     * @param timeZone   is very important for configuring the calendar. The epoch value for the end of
     *                   the last day, week and month are 100% dependant on the timezone chosen.
     * @return Calendar correctly configured to be at exactly the end time of the UNIT_OF_TIME specified, for the
     *         timezone specified
     */
    public static Calendar getCalendarForTimeUnit(UNIT_OF_TIME unitOfTime, String timeZone) {
        Calendar calendar = Calendar.getInstance(getTimeZone(timeZone));

        //Set the calendar to be the correct end of time period
        if (unitOfTime != UNIT_OF_TIME.HOUR) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
        }

        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //if the unit is month we also want to set the calendar at the start of this month, end time is exclusive
        //which means that a query will capture the entire previous month
        if (unitOfTime == UNIT_OF_TIME.MONTH) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }

        return calendar;
    }


    /**
     * Return a millisecond value from the start of epoch up to the date represented by the String date parameter.
     * If you specify Europe/Paris as the timezone, then the date will be parsed
     * and the GMT millisecond value represented by that paris date time will be returned.
     * Any date parsed with a timezone is returning the millisecond value since epoch relative to the specified timezone
     * If you specify 2008/11/28 23:00 with Europe/Paris and then 2008/11/28 14:00 Canada/Pacific, you will get the
     * same millisecond value, as they both translate to the same GMT value
     *
     * @param date     The format MUST BE in the format 'yyyy/MM/dd HH:mm'
     * @param timeZone timezone in which the date supplied belongs
     * @return The number of milliseconds since epoch represented by the supplied date and timezone
     * @throws ParseException if the supplied string date cannot be parsed
     */
    public static long getAbsoluteMilliSeconds(String date, String timeZone) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_STRING);
        dateFormat.setTimeZone(getTimeZone(timeZone));
        Date d = dateFormat.parse(date);
        return d.getTime();
    }

    /**
     * Convenience method to retrieve a TimeZone object from the timezone id represented by the timeZone parameter
     *
     * @param timeZone the timezone required
     * @return the correct timezone which matches the timeZone parameter. Only the correct timezone is returned. Never null
     * @throws IllegalArgumentException if the timezone requested is not found
     */
    public static TimeZone getTimeZone(String timeZone) {

        TimeZone tz = TimeZone.getTimeZone(timeZone);
        //if we ask for an invlaid timezone, no exception is thrown, we just get GMT as the default.
        //throwing an exception if the timezone found is not the same as requested
        if (!tz.getID().equals(timeZone)) {
            throw new IllegalArgumentException("Timezone '" + timeZone + "' is not valid");
        }
        return tz;
    }

    /**
     * For the specified relative time period, get the distinct list of intervals which make up that time period
     * The first long in the returned list is the very start of the time period and the last long is the end
     * of the very last interval.<br>
     * The returned List should be used as follows: Interval 0, i= 0: list(i) >= interval < list(i+1) therefore an
     * interval is inclusive of it's start and exclusive of it's end.<br>
     * Note: The last interval may be shorter than expected if the interval does not divide evenly into the time period
     *
     * @param timePeriodStartInclusive When does the time period start. See Utilities.getRelativeMilliSeconds() for
     *                                 how to get the timePeriodStartInclusive value
     * @param timePeriodEndExclusive   end of time period
     * @param intervalNumberOfUnits    The length of an interval is numberOfUnits x unitOfTime
     * @param intervalUnitOfTime       valid values are HOUR, DAY, WEEK and MONTH
     * @param timeZone                 the timezone to use when formatting the timePeriodStartInclusive and timePeriodEndExclusive
     *                                 in the case when there is an IllegalArgumentException
     * @return List<Long> the ordered list of long's representing the <em>start</em> of each interval. The last long represents
     *         the end of the last interval.
     * @throws IllegalArgumentException if the end start time period is >= end time period or if the intervalNumberOfUnits
     *                                  is <= 0
     */
    public static List<Long> getIntervalsForTimePeriod(Long timePeriodStartInclusive, Long timePeriodEndExclusive,
                                                       int intervalNumberOfUnits,
                                                       UNIT_OF_TIME intervalUnitOfTime, String timeZone) {
        TimeZone tz = getTimeZone(timeZone);
        if (timePeriodStartInclusive >= timePeriodEndExclusive) {
            Calendar test = Calendar.getInstance(tz);
            test.setTimeInMillis(timePeriodStartInclusive);
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_STRING);
            dateFormat.setTimeZone(tz);
            String startDate = dateFormat.format(test.getTime());
            test.setTimeInMillis(timePeriodEndExclusive);
            String endDate = dateFormat.format(test.getTime());

            throw new IllegalArgumentException("End of time period must be after the time period start time: start: " +
                    startDate + " value = " + timePeriodStartInclusive + " end: " + endDate + " value = " + timePeriodEndExclusive);
        }

        if (intervalNumberOfUnits <= 0) throw new IllegalArgumentException("intervalNumberOfUnits must greater than 0");

        int calendarUnitOfTime = getCalendarTimeUnit(intervalUnitOfTime);

        List<Long> returnList = new ArrayList<Long>();

        Calendar endOfTimePeriod = Calendar.getInstance(tz);
        endOfTimePeriod.setTimeInMillis(timePeriodEndExclusive);

        Calendar startOfTimePeriod = Calendar.getInstance(tz);
        startOfTimePeriod.setTimeInMillis(timePeriodStartInclusive);

        Calendar temp = Calendar.getInstance(tz);
        temp.setTimeInMillis(timePeriodStartInclusive);
        temp.add(calendarUnitOfTime, intervalNumberOfUnits);

        //in this case there is only one interval
        if (temp.getTimeInMillis() >= timePeriodEndExclusive) {
            returnList.add(timePeriodStartInclusive);
            returnList.add(timePeriodEndExclusive);
            return returnList;
        }

        while (startOfTimePeriod.getTimeInMillis() <= endOfTimePeriod.getTimeInMillis()) {
            returnList.add(startOfTimePeriod.getTimeInMillis());
            if (startOfTimePeriod.getTimeInMillis() == timePeriodEndExclusive) break;
            startOfTimePeriod.add(calendarUnitOfTime, intervalNumberOfUnits);
        }

        if (startOfTimePeriod.getTimeInMillis() != endOfTimePeriod.getTimeInMillis()) {
            returnList.add(timePeriodEndExclusive);
        }
        return returnList;
    }

    /**
     * Interval reports show the interval information in column 0. The heading for this column is 'Interval'. Hourly
     * and weekly intervals show date using MM/dd format, so for these intervals we can update the column heading
     * so that the reader of a report knows what the month and day order is.
     *
     * @param intervalUnitOfTime unit of time that an interval report is using
     * @return String to add to the 'Interval' column heading. Empty string for Day and Month intervals
     */
    public static String getIntervalDateInfoString(UNIT_OF_TIME intervalUnitOfTime) {

        if (intervalUnitOfTime == UNIT_OF_TIME.HOUR || intervalUnitOfTime == UNIT_OF_TIME.WEEK) {
            return " (mm/dd)";
        }
        return "";
    }

    /**
     * Create a string representation of all string values contained in the values collection
     *
     * @param values The Collection of strings to be placed into a single string for display purposes
     * @return string with all the strings from values concat'ed with " " between them
     */
    public static String getStringNamesFromCollectionEscaped(Collection values) {
        if (values.isEmpty()) return "";

        Iterator iter = values.iterator();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (iter.hasNext()) {
            if (i != 0) sb.append(", ");
            sb.append(escapeHtmlCharacters(TextUtils.truncStringMiddleExact(iter.next().toString(), Utilities.SERVICE_DISPLAY_NAME_LENGTH)));
            i++;
        }
        return sb.toString();
    }

    /**
     * Get the sql query, which for a time period, will return the complete list of distinct mapping value sets, for all
     * the constrains represented by the parameters.
     * <br>
     * To understand this, you need to understand that the rows of message context mapping keys, contains a row
     * for every distinct combination of mapping keys, regardless of what index they exist in a row of this table.
     * Based on the keysToFilters the possible rows are filtered such that only values are returned when the row has
     * a key for every key in keysToFilters, in any of it's mapping indexes, and if a value constraint is specified,
     * then its actual value in message context mapping values must match the constraint.
     *
     * @param startTimeInclusiveMilli start of the time period
     * @param endTimeInclusiveMilli   end of the time period
     * @param serviceIdToOperations   a map of service id to the set of operations. The data is filtered such that the
     *                                mapping keys / values must match, but so must the link to published_service table. If isDetail is true, then the
     *                                query is further constrained so that the operation value in message context mapping values is constrained to the
     *                                list of operations specified here. Operation constrains are always in relation to a service.
     * @param keysToFilters           a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                                represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                                key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                                and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                                hash map.
     * @param resolution              which resolution bin to use, hourly or daily
     * @param isDetail                is used in validating the parameters, some constrains are relative to the query being a detail
     *                                query or not.  The keysToFilters cannot be validated without knowing if the report is at the operation level.
     *                                In addition, isDetail determins whether we just constrain by service id or service id and operation
     * @param isUsage                 needed in order to validate the input parameters
     * @return Pair&lt;String, List&lt;Object&gt;&gt; A Pair of a String containing the query sql and a List&lt;Object&gt;> containing the
     *         parameters to be added to the SQL. Each entry in List<Object> matches the index of a ? character in the sql
     *         <p/>
     *         <pre>
     *                                 AUTHENTICATED_USER | MAPPING_VALUE_1 | MAPPING_VALUE_2 | MAPPING_VALUE_3 | MAPPING_VALUE_4 | MAPPING_VALUE_5
     *                                 </pre>
     *         Note operation is not included. It is a mapping key under the covers but it has special meaning. Notice how
     *         authenticated_user is returned. To the user and to business logic, authenticated user is a normal mapping key
     */
    public static Pair<String, List<Object>> getDistinctMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                                     Map<String, Set<String>> serviceIdToOperations,
                                                                     LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters, int resolution,
                                                                     boolean isDetail, boolean isUsage) {

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keysSupplied = checkMappingQueryParams(keysToFilters, isDetail, isUsage);

        boolean serviceIdsOk = (serviceIdToOperations != null && !serviceIdToOperations.keySet().isEmpty());

        checkResolutionParameter(resolution);

        boolean useUser = (keysSupplied) && isUserSupplied(keysToFilters);

        List<Object> queryParams = new ArrayList<Object>();
        StringBuilder sb = new StringBuilder("SELECT DISTINCT ");

        addUserToSelect(false, useUser, sb, queryParams);
        List<String> keys = new ArrayList<String>();
        if (keysSupplied) keys.addAll(keysToFilters.keySet());
        addCaseSQL(keys, sb, queryParams);

        sb.append(mappingJoin);
        addResolutionConstraint(resolution, sb, queryParams);
        if (useTime) {
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb, queryParams);
        }

        boolean isBlankedOpQuery = isBlanketOperationQuery(serviceIdToOperations);
        //not a detail query and service id's are ok
        // OR is a detail query, and we have blanked operation requirements
        if (serviceIdsOk && ((!isDetail) || (isBlankedOpQuery && isDetail))) {
            addServiceIdConstraint(serviceIdToOperations.keySet(), sb, queryParams);
        }
        //else isDetail and were going to use operations
        else if (serviceIdsOk && !isBlankedOpQuery && isDetail) {
            //new method to constrain serivce id and operation together
            addServiceAndOperationConstraint(serviceIdToOperations, sb, queryParams);
        }

        if (useUser) {
            List<ReportApi.FilterPair> userFilterPairs = getAuthenticatedUserFilterPairs(keysToFilters);
            if (!userFilterPairs.isEmpty()) {
                addUserConstraint(userFilterPairs, sb, queryParams);
            } else {
                addUserNotNullConstraint(sb);
            }
        }

        if (keysSupplied) {
            addMappingConstraint(keysToFilters, sb, queryParams);
        }
        addUsageDistinctMappingOrder(sb);

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "getDistinctMappingQuery: " + logCompleteSql(sb.toString(), queryParams));
        }

        return new Pair<String, List<Object>>(sb.toString(), queryParams);
    }

    /**
     * Usage interval reports are driven by the set of service ids and operations which match the search criteria.
     * Mapping values are not needed in the output of this query as they do not mean anything at the master report
     * level. All parameters will supplied will be used as constraints on the query. The order of this query is simply
     * service id followed by operation, which may be a place holder
     *
     * @param startTimeInclusiveMilli start of the time period
     * @param endTimeInclusiveMilli   end of the time period
     * @param serviceIdToOperations   a map of service id to the set of operations. The data is filtered such that the
     *                                mapping keys / values must match, but so must the link to published_service table. If isDetail is true, then the
     *                                query is further constrained so that the operation value in message context mapping values is constrained to the
     *                                list of operations specified here. Operation constrains are always in relation to a service.
     * @param keysToFilters           a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                                represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                                key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                                and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                                hash map.
     * @param resolution              which resolution bin to use, hourly or daily
     * @param isDetail                is used in validating the parameters, some constrains are relative to the query being a detail
     *                                query or not.  The keysToFilters cannot be validated without knowing if the report is at the operation level.
     *                                In addition, isDetail determins whether we just constrain by service id or service id and operation
     * @return Pair&lt;String, List&lt;Object&gt;&gt; A Pair of a String containing the query sql and a List&lt;Object&gt;> containing the
     *         parameters to be added to the SQL. Each entry in List<Object> matches the index of a ? character in the sql
     *         <p/>
     *         <pre>
     *                         SERVICE_ID | SERVICE_NAME | ROUTING_URI | CONSTANT_GROUP | SERVICE_OPERATION_VALUE
     *                         </pre>
     */
    public static Pair<String, List<Object>> getUsageMasterIntervalQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                                         Map<String, Set<String>> serviceIdToOperations,
                                                                         LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                                         int resolution, boolean isDetail) {

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keysSupplied = checkMappingQueryParams(keysToFilters, isDetail, true);

        boolean serviceIdsOk = (serviceIdToOperations != null && !serviceIdToOperations.keySet().isEmpty());

        checkResolutionParameter(resolution);

        boolean useUser = (keysSupplied) && isUserSupplied(keysToFilters);

        List<Object> queryParams = new ArrayList<Object>();

        StringBuilder sb = new StringBuilder(distinctFrom);

        addOperationToSelect(isDetail, sb, queryParams);

        sb.append(mappingJoin);

        addResolutionConstraint(resolution, sb, queryParams);

        if (useTime) {
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb, queryParams);
        }

        boolean isBlankedOpQuery = isBlanketOperationQuery(serviceIdToOperations);
        //not a detail query and service id's are ok
        // OR is a detail query, and we have blanked operation requirements
        if (serviceIdsOk && ((!isDetail) || (isBlankedOpQuery && isDetail))) {
            addServiceIdConstraint(serviceIdToOperations.keySet(), sb, queryParams);
        }
        //else isDetail and were going to use operations
        else if (serviceIdsOk && !isBlankedOpQuery && isDetail) {
            //new method to constrain serivce id and operation together
            addServiceAndOperationConstraint(serviceIdToOperations, sb, queryParams);
        }

        if (useUser) {
            List<ReportApi.FilterPair> userFilterPairs = getAuthenticatedUserFilterPairs(keysToFilters);
            if (!userFilterPairs.isEmpty()) {
                addUserConstraint(userFilterPairs, sb, queryParams);
            } else {
                addUserNotNullConstraint(sb);
            }
        }

        if (keysSupplied) {
            addMappingConstraint(keysToFilters, sb, queryParams);
        }

        sb.append(" ORDER BY SERVICE_ID, SERVICE_OPERATION_VALUE");

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "getUsageMasterIntervalQuery: " + logCompleteSql(sb.toString(), queryParams));
        }

        return new Pair<String, List<Object>>(sb.toString(), queryParams);
    }

    /**
     * Usage query is only interested in one value - the sum of throughput requests - constrained by all inputs
     * This query is used by both the summary and interval usage reports. When it's used by the base sub report
     * in an interval query the serviceIds and operations list have only 1 value each, as at that level we are
     * querying for a particular service and possibly an operation
     *
     * @param startTimeInclusiveMilli start of the time period
     * @param endTimeInclusiveMilli   end of the time period
     * @param serviceIdToOperations   a map of service id to the set of operations. The data is filtered such that the
     *                                mapping keys / values must match, but so must the link to published_service table. If isDetail is true, then the
     *                                query is further constrained so that the operation value in message context mapping values is constrained to the
     *                                list of operations specified here. Operation constrains are always in relation to a service.
     * @param keysToFilters           a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                                represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                                key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                                and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                                hash map.
     * @param resolution              which resolution bin to use, hourly or daily
     * @param isDetail                is used in validating the parameters, some constrains are relative to the query being a detail
     *                                query or not.  The keysToFilters cannot be validated without knowing if the report is at the operation level.
     *                                In addition, isDetail determins whether we just constrain by service id or service id and operation
     * @return Pair&lt;String, List&lt;Object&gt;&gt; A Pair of a String containing the query sql and a List&lt;Object&gt;> containing the
     *         parameters to be added to the SQL. Each entry in List<Object> matches the index of a ? character in the sql
     *         <p/>
     *         The sql always returns the following fields:-
     *         <pre>
     *                         SERVICE_ID | SERVICE_NAME | ROUTING_URI | USAGE_SUM | CONSTANT_GROUP | AUTHENTICATED_USER |
     *                         SERVICE_OPERATION_VALUE | MAPPING_VALUE_1 | MAPPING_VALUE_2 | MAPPING_VALUE_3 | MAPPING_VALUE_4 | MAPPING_VALUE_5
     *                         </pre>
     */
    public static Pair<String, List<Object>> getUsageQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                           Map<String, Set<String>> serviceIdToOperations,
                                                           LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                           int resolution,
                                                           boolean isDetail) {

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keysSupplied = checkMappingQueryParams(keysToFilters, isDetail, true);

        boolean serviceIdsOk = (serviceIdToOperations != null && !serviceIdToOperations.keySet().isEmpty());

        checkResolutionParameter(resolution);

        boolean useUser = (keysSupplied) && isUserSupplied(keysToFilters);

        List<Object> queryParams = new ArrayList<Object>();
        //----SECTION A----
        StringBuilder sb = new StringBuilder(usageAggregateSelect);

        //----SECTION B----
        addUserToSelect(true, useUser, sb, queryParams);
        //----SECTION C----
        addOperationToSelect(isDetail, sb, queryParams);
        //----SECTION D's----
        List<String> keys = new ArrayList<String>();
        //this is a usage query, no npe due to checkMappingQueryParams above
        keys.addAll(keysToFilters.keySet());
        addCaseSQL(keys, sb, queryParams);
        //----SECTION E----
        sb.append(mappingJoin);
        //----SECTION F----
        addResolutionConstraint(resolution, sb, queryParams);

        //----SECTION G----
        if (useTime) {
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb, queryParams);
        }

        //----SECTION H & I----
        //Service ids only constrained here, if isDetail is false, otherwise operation and services are constrained
        //together below

        boolean isBlankedOpQuery = isBlanketOperationQuery(serviceIdToOperations);
        //not a detail query and service id's are ok
        // OR is a detail query, and we have blanked operation requirements
        if (serviceIdsOk && ((!isDetail) || (isBlankedOpQuery && isDetail))) {
            addServiceIdConstraint(serviceIdToOperations.keySet(), sb, queryParams);
        }
        //else isDetail and were going to use operations
        else if (serviceIdsOk && !isBlankedOpQuery && isDetail) {
            //new method to constrain serivce id and operation together
            addServiceAndOperationConstraint(serviceIdToOperations, sb, queryParams);
        }

        //----SECTION J----
        if (useUser) {
            List<ReportApi.FilterPair> userFilterPairs = getAuthenticatedUserFilterPairs(keysToFilters);
            if (!userFilterPairs.isEmpty()) {
                addUserConstraint(userFilterPairs, sb, queryParams);
            } else {
                addUserNotNullConstraint(sb);
            }
        }

        //----SECTION K----
        if (keysSupplied) {
            addMappingConstraint(keysToFilters, sb, queryParams);
        }

        addGroupBy(sb);

        //----SECTION M----
        addUsageMappingOrder(sb);

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "getUsageQuery: " + logCompleteSql(sb.toString(), queryParams));
        }

        return new Pair<String, List<Object>>(sb.toString(), queryParams);
    }

    /**
     * This version of getUsageQuery accecpts a single serviceId and operation, as this query is called from the lowest
     * sub report when the report is an interval usage report. At that level the query is interested in the usage data
     * represented by the constraints, at the service and or possibly the operation level. This method will delegate to the
     * other implementation after converting the service id and operation into a collection.
     * <br>
     * This function is for convenience, so the report doesn't need to provide the service id and operation as a collection
     *
     * @param startTimeInclusiveMilli start of the time period
     * @param endTimeInclusiveMilli   end of the time period
     * @param serviceId               the service id which we want usage data for
     * @param keysToFilters           a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                                represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                                key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                                and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                                hash map.
     * @param resolution              which resolution bin to use, hourly or daily
     * @param isDetail                is used in validating the parameters, some constrains are relative to the query being a detail
     *                                query or not.  The keysToFilters cannot be validated without knowing if the report is at the operation level.
     *                                In addition, isDetail determins whether we just constrain by service id or service id and operation
     * @param operation               the operation, if isDetail is true, that we want usage data for
     * @return Pair<String, List<Object>> A Pair of a String containing the query sql and a List<Object> containing the
     *         parameters to be added to the SQL. Each entry in List<Object> matches the index of a ? character in the sql
     *         <p/>
     *         The sql always returns the following fields:-
     *         <pre>
     *                         SERVICE_ID | SERVICE_NAME | ROUTING_URI | USAGE_SUM | CONSTANT_GROUP | AUTHENTICATED_USER |
     *                         SERVICE_OPERATION_VALUE | MAPPING_VALUE_1 | MAPPING_VALUE_2 | MAPPING_VALUE_3 | MAPPING_VALUE_4 | MAPPING_VALUE_5
     *                         </pre>
     */
    public static Pair<String, List<Object>> getUsageQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                           Long serviceId,
                                                           LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters, int resolution,
                                                           boolean isDetail, String operation) {
        if (serviceId == null) throw new NullPointerException("serviceId cannot be null");
        if (operation == null || operation.equals("")) {
            throw new IllegalArgumentException("operation can be null or empty");
        }

        Set<String> operations = new HashSet<String>();
        if (!operation.equals(Utilities.SQL_PLACE_HOLDER)) operations.add(operation);
        Map<String, Set<String>> serviceIdToOperations = new HashMap<String, Set<String>>();
        serviceIdToOperations.put(String.valueOf(serviceId), operations);

        return getUsageQuery(startTimeInclusiveMilli, endTimeInclusiveMilli, serviceIdToOperations, keysToFilters, resolution, isDetail);

    }

    /**
     * From the keysToFilters map, determine if the AUTH_USER mapping key has been specified
     *
     * @param keysToFilters a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                      represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                      key then the isEmpty() method of FilterPair should return true.
     * @return true if the AUTH_USER is one of the mapping keys in keysToFilter, otherwise false
     */
    private static boolean isUserSupplied(Map<String, List<ReportApi.FilterPair>> keysToFilters) {
        return keysToFilters.containsKey(MessageContextMapping.MappingType.AUTH_USER.toString());
    }

    /**
     * Extract all the FilterPair's which relate to the context mapping key AUTH_USER<br>
     * This should only be called when isUserSupplied && checkMappingQueryParams is true, see usages for how it's used.
     * The presence of the key AUTH_USER, implies that we will retrieve the real value of auth user id from the mapping
     * values table, however the list returned from this method, will determine if we actually need to add any constraint
     * on the authenticated user value in the sql query that is being generated
     *
     * @param keysToFilters a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                      represent it's constraints. The AUTH_USER key is searched for, and if it is found the list of FilterPairs it
     *                      maps to will be returned, any FilterPairs whos isEmpty() method returns true, are not returned
     * @return the list of FilterPair's supplied to getPerformanceStatisticsMappingQuery, empty list if non were specified in keysToFilters
     * @throws IllegalStateException if the AUTH_USER key is not found in keysToFilters, the caller should know before
     *                               this method is called, that the key exists.
     */
    private static List<ReportApi.FilterPair> getAuthenticatedUserFilterPairs(Map<String, List<ReportApi.FilterPair>> keysToFilters) {
        for (Map.Entry<String, List<ReportApi.FilterPair>> me : keysToFilters.entrySet()) {
            if (me.getKey().equals(MessageContextMapping.MappingType.AUTH_USER.toString())) {
                List<ReportApi.FilterPair> returnList = new ArrayList<ReportApi.FilterPair>();
                for (ReportApi.FilterPair fp : me.getValue()) {
                    if (!fp.isConstraintNotRequired()) {
                        returnList.add(fp);
                    }
                }
                return returnList;
            }
        }
        throw new IllegalStateException("No authenticated user filter pairs were found");
    }

    /**
     * Create the sql required to get performance statistics for a specific period of time, for a possible specifc set of
     * service id's, operations, mapping keys and values, mapping values EQUAL or LIKE logic and authenticated users.
     * Below is an example query. The comments in the code relate to the section's listed in the query here in the
     * javadoc
     * <br>
     * Note: this query returns exactly the same columns as getNoMappingQuery when the parameter isMasterQuery has the
     * same value
     * <br>
     * PLACEHOLDERS: Any where below where ';' could be selected is done for the following reasons:-<br>
     * 1) The reporting software will always get fields, for which it has defined variables<br>
     * 2) Group by can always include this column, so long as the placeholder value is the same for all columns, the
     * results are unaltered<br>
     * 3) Order by can always include this column, so long as the placeholder value is the same for all columns, the
     * results are unaltered<br>
     * <p/>
     * <br>
     * SECTION A: The logic for determing the performance statistics for a specific interval of time is hard coded, and
     * has no need to change at runtime. This hardcoded query also contains logic to make processing easier.
     * <br>
     * SECTION B: AUTHENTICATED_USER column ALWAYS appears in the select statement HOWEVER it either has the real value
     * of mcmv.auth_user_id OR it is selected as ';'.
     * <br>
     * SECTION C: SERVICE_OPERATION_VALUE column ALWAYS appears in the select statement HOWEVER it either has the real value
     * of mcmv.auth_user_id OR it is selected as ';'.
     * <br>
     * SECTION D 1: For every key in the List<String> keys, a case statement is created. It is very important to understand
     * how this section works, as it explains why these queries work when the entries in message_context_message_values
     * (mcmv) can contain keys from message_context_message_keys (mcmk) in any order.<br>
     * Although the keys can appear in any order in mcmv, we select these values out of any of the mappingx_key (x:1-5)
     * columns and place it into a derived column with the value MAPPING_VALUE_X (x:1-5). The order of the keys in
     * List<String> keys, determines what MAPPING_VALUE_X column it applies to.<br>
     * Within each case statement we are looking for the existence of a specific key in any of 5 column locations. The
     * key WILL ALWAYS exist due to the WHERE constraint that follows. The WHERE constraint guarantees that any rows
     * found from the joins in the from clause, will ALWAYS contain rows which have ALL of the keys in List<String> keys
     * Note: The implementation of service_metric_detail bins, normalizes the keys used by any mcmv bin instance. This
     * means that although the keys can be in any order in a message context assertion, any assertion with the same
     * keys in any order, will always use the same key from mcmk.
     * <br>
     * SECTION D 2: We ALWAYS select out every MAPPINGX_VALUE (X:1-5) values from mcmv. After we have created a CASE
     * statement for each key in List<String> keys, the remaining unused values are selected out with the place holder
     * value of ';'.
     * <br>
     * SECTION E: The tables used in the query are not dynamic and neither are any of the joins
     * <br>
     * SECTION F: The value of resolution can be either 1 (Hourly) or 2 (Daily)
     * <br>
     * SECTION G: The time period for the query is for A SPECIFIC interval. The interval is inclusive of the start time
     * and exclusive of the end time.
     * <br>
     * SECTION H & I: (Optional) Sections H & I determine if and how service id's and operations are constrained. The
     * general rule is that an operation is never constrained without it also being constrained to a service.<br>
     * serviceIdToOperations is a map of service ids to a list of operations. There is domain logic applied depending
     * on what the values of the keys in the map are, and whether isDetail is true or false:-<br>
     * H) When any of the keys has a non null and non empty list of operations, then the query produced is for a set
     * of services, with each service id constrained by specific operations. If any service in the map contains a null
     * or empty list of operations, it is simply left out of the query. isDetail must be true for this behaviour to happen.
     * <br>
     * I) When all of the keys have null or empty lists of operations, then the query is only constrained by service ids.
     * If isDetail is true, then this turns the query into a blanket operation query, in which all operations for the
     * selected services are returned.
     * <br>
     * <p/>
     * If serviceIdToOperations is null or empty, then no constraint is put on services or operations.
     * <br>
     * SECTION J: (Optional) If the AUTH_USER key has been added, and it has one or more FilterPair's in keysToFilters,
     * then an AND block is created. Within the AND block, there is a constrain for every FilterPair. EQUAL or LIKE is
     * used depending on the FilterPair's isUseAnd() method.<br>
     * All the constraints are OR'd together within the AND block.
     * <br>
     * SECTION K: This section compliments the CASE queries in the select clause. For every key in keysToFilters
     * for which a CASE statement was created, it's guaranteed that a corresponding AND block is created here.<br>
     * For each key, the AND block ensures that any matching rows, contains the key in any of the key locations 1-5.
     * Note: to the user AUTH_USER is a mapping key, however if AUTH_USER is supplied as a key in keysToFilters,
     * it's handled separately in the J block above.
     * <br>
     * For each key, if it has 1 or more FilterPair's whose isEmpty method is not true, a constraint is added within
     * the AND block for the keys.
     * <br>
     * SECTION K1: If only one constraint is needed, then a single constraint is added within an AND block
     * <br>
     * SECTION K2: If a key has more than one constraint required, then the AND block contains the 2 or more constraints
     * each of which are OR'd together.
     * <br>
     * For both K1 and k2, if the FilterPair's isUseAnd() method returns true, then AND is used in the constraint,
     * otherwise LIKE is used.
     * <br>
     * <p/>
     * Note: FilterPair handles all translation of the * wildcard into the SQL % wildcard. Additionally it ensures
     * that the characters '%' and '_' can be used literally in a constraint value.
     * <br>
     * SECTION L: The group by order is important. Performance Statistics information can never be grouped across
     * services, although it can be aggreated across services, after grouping. The major group element is therefore
     * service id, followed by operation. This guarantees that any resulting row is always at the service level, and
     * from there it can be further broken down by operation, and then mapping value. The mapping values can in
     * reality be in any order here however due to how the keys are processed in the CASE statements, being determined
     * from the List<String> keys supplied, the first X mapping values are NEVER placeholders, placeholders always come
     * last.
     * <br>
     * SECTION M: The order by order is important. Mapping values are ALWAYS ordered first, AUTHENTICATED_USER IS A
     * mapping value. They are the major order aspect, by which we want to view data. We want to look at data in terms
     * of a set of mapping values, which represent the concept of an individual requestor type, of a service.<br>
     * Note that due to how the keys are processed in the CASE statements, being determined from the List<String> keys
     * supplied, the first X mapping values are NEVER placeholders, placeholders always come last.<br>
     * Following the mapping values is the service id and the operation. Service id must come before operation, as it
     * is a bigger group type. We want to either view the mapping data at the service level and from there possibly the
     * operation level.
     * <p/>
     * <pre>
     * SELECT
     * ----SECTION A----
     * SELECT p.objectid as SERVICE_ID,
     * p.name as SERVICE_NAME,
     * p.routing_uri as ROUTING_URI,
     * SUM(smd.attempted) as ATTEMPTED,
     * SUM(smd.completed) as COMPLETED,
     * SUM(smd.authorized) as AUTHORIZED,
     * SUM(smd.front_sum) as FRONT_SUM,
     * SUM(smd.back_sum) as BACK_SUM,
     * SUM(smd.completed) as THROUGHPUT,
     * SUM(smd.attempted)-SUM(smd.authorized) as POLICY_VIOLATIONS,
     * SUM(smd.authorized)-SUM(smd.completed) as ROUTING_FAILURES,
     * MIN(smd.front_min) as FRTM,
     * MAX(smd.front_max) as FRTMX,
     * if(SUM(smd.front_sum), if(SUM(smd.attempted), SUM(smd.front_sum)/SUM(smd.attempted),0), 0) as FRTA,
     * MIN(smd.back_min) as BRTM,
     * MAX(smd.back_max) as BRTMX,
     * if(SUM(smd.back_sum), if(SUM(smd.completed), SUM(smd.back_sum)/SUM(smd.completed),0), 0) as BRTA,
     * if(SUM(smd.attempted), ( 1.0 - ( ( (SUM(smd.authorized) - SUM(smd.completed)) / SUM(smd.attempted) ) ) ) , 0) as AP ,
     * 1 as CONSTANT_GROUP ,
     * ----SECTION B----
     * mcmv.auth_user_id AS AUTHENTICATED_USER,
     * ----SECTION C----
     * mcmv.service_operation AS SERVICE_OPERATION_VALUE,
     * ----SECTION D 1----
     * CASE
     * WHEN mcmk.mapping1_key = 'IP_ADDRESS' THEN mcmv.mapping1_value
     * WHEN mcmk.mapping2_key = 'IP_ADDRESS' THEN mcmv.mapping2_value
     * WHEN mcmk.mapping3_key = 'IP_ADDRESS' THEN mcmv.mapping3_value
     * WHEN mcmk.mapping4_key = 'IP_ADDRESS' THEN mcmv.mapping4_value
     * WHEN mcmk.mapping5_key = 'IP_ADDRESS' THEN mcmv.mapping5_value
     * END AS MAPPING_VALUE_1,
     * CASE
     * WHEN mcmk.mapping1_key = 'CUSTOMER' THEN mcmv.mapping1_value
     * WHEN mcmk.mapping2_key = 'CUSTOMER' THEN mcmv.mapping2_value
     * WHEN mcmk.mapping3_key = 'CUSTOMER' THEN mcmv.mapping3_value
     * WHEN mcmk.mapping4_key = 'CUSTOMER' THEN mcmv.mapping4_value
     * WHEN mcmk.mapping5_key = 'CUSTOMER' THEN mcmv.mapping5_value
     * END AS MAPPING_VALUE_2,
     * ----SECTION D 2----
     * ';' AS MAPPING_VALUE_3,
     * ';' AS MAPPING_VALUE_4,
     * ';' AS MAPPING_VALUE_5
     * ----SECTION E----
     * FROM
     * service_metrics sm, published_service p, service_metrics_details smd, message_context_mapping_values mcmv, message_context_mapping_keys mcmk
     * WHERE
     * p.objectid = sm.published_service_oid AND
     * sm.objectid = smd.service_metrics_oid AND
     * smd.mapping_values_oid = mcmv.objectid AND
     * mcmv.mapping_keys_oid = mcmk.objectid  AND
     * ----SECTION F----
     * sm.resolution = 2  AND
     * ----SECTION G----
     * sm.period_start >=1220252459000 AND
     * sm.period_start <1222844459000 AND
     * ----SECTION H----
     * AND
     * (
     * (  p.objectid = 229384 AND mcmv.service_operation IN ('listProducts','orderProduct') )  OR
     * (  p.objectid = 229382 AND mcmv.service_operation IN ('listProducts','orderProduct') )  OR
     * (  p.objectid = 229380 AND mcmv.service_operation IN ('listProducts','orderProduct') )  OR
     * (  p.objectid = 229376 AND mcmv.service_operation IN ('listProducts','orderProduct') )  OR
     * (  p.objectid = 229378 AND mcmv.service_operation IN ('listProducts','orderProduct') )
     * )
     * SECTIONS H AND I ARE MUTUALLY EXCLUSIVE
     * ----SECTION I----
     * p.objectid IN (229384, 229382, 229380, 229376, 229378)
     * <p/>
     * ----SECTION J----
     * AND
     * (
     * mcmv.auth_user_id LIKE 'Ld%'  OR mcmv.auth_user_id LIKE 'Do%'
     * )
     * ----SECTION K----
     * AND
     * (
     * ( mcmk.mapping1_key = 'IP_ADDRESS'
     * ----SECTION K1----
     * AND ( mcmv.mapping1_value LIKE '127.%'  )  )  OR
     * ---- END K1 ----
     * ( mcmk.mapping2_key = 'IP_ADDRESS'  AND ( mcmv.mapping2_value LIKE '127.%'  )  )  OR
     * ( mcmk.mapping3_key = 'IP_ADDRESS'  AND ( mcmv.mapping3_value LIKE '127.%'  )  )  OR
     * ( mcmk.mapping4_key = 'IP_ADDRESS'  AND ( mcmv.mapping4_value LIKE '127.%'  )  )  OR
     * ( mcmk.mapping5_key = 'IP_ADDRESS'  AND ( mcmv.mapping5_value LIKE '127.%'  )  )
     * )
     * AND
     * (
     * ( mcmk.mapping1_key = 'CUSTOMER'  AND
     * ----SECTION K2----
     * ( mcmv.mapping1_value = 'GOLD'  OR  mcmv.mapping1_value LIKE 'S%'  )
     * ----END SECTION K2----
     * )  OR
     * ( mcmk.mapping2_key = 'CUSTOMER'  AND ( mcmv.mapping2_value = 'GOLD'  OR  mcmv.mapping2_value LIKE 'S%'  )  )  OR
     * ( mcmk.mapping3_key = 'CUSTOMER'  AND ( mcmv.mapping3_value = 'GOLD'  OR  mcmv.mapping3_value LIKE 'S%'  )  )  OR
     * ( mcmk.mapping4_key = 'CUSTOMER'  AND ( mcmv.mapping4_value = 'GOLD'  OR  mcmv.mapping4_value LIKE 'S%'  )  )  OR
     * ( mcmk.mapping5_key = 'CUSTOMER'  AND ( mcmv.mapping5_value = 'GOLD'  OR  mcmv.mapping5_value LIKE 'S%'  )  )
     * )
     * ----SECTION L----
     * GROUP BY p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER , MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5
     * ----SECTION M----
     * ORDER BY AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5 ,p.objectid, SERVICE_OPERATION_VALUE
     * </pre>
     *
     * @param isMasterQuery           if true, this query will only return the SERVICE_ID, SERVICE_NAME, ROUTING_URI and
     *                                CONSTANT_GROUP. Set to true when you want to find out what services match the supplied criteria. This is used
     *                                to drive the master interval reports, where we will run a sub report for every serivce id found.
     * @param startTimeInclusiveMilli time_period start time inclusive
     * @param endTimeInclusiveMilli   time_period end time exclsuvie
     * @param serviceIdToOperations   if supplied the published_service_oid from service_metrics will be constrained by these keys.
     *                                If the values for a key is a list of operations, then the constraint for that service will include those operations.
     *                                If any service has a non null and non empty list of operations, then services will only be returned which have operations
     *                                specified. if all values are null or empty, then the query is constrained with just service id's, and all operations data
     *                                will come back for each service supplied
     *                                list dictitates whether an = or like constraint is applied. Can be null or empty. Cannot have values if
     *                                keyValueConstraints is null or empty
     * @param keysToFilters           a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                                represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                                key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                                and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                                hash map.
     * @param resolution              1 = hourly, 2 = daily. Which resolution from service_metrics to use
     * @param isDetail                if true then the service_operation's real value is used in the select, group and order by,
     *                                otherwise operation is selected as 1. To facilitate this service_operation is always selected as
     *                                SERVICE_OPERATION_VALUE so that the real column is not used when isDetail is false
     *                                table message_context_mapping_values, with the values in operaitons
     *                                message_context_mapping_values, with the values in authenticatedUsers
     * @param isUsage                 needed in order to validate the input parameters
     * @return Pair&lt;String, List&lt;Object&gt;&gt; A Pair of a String containing the query sql and a List&lt;Object&gt;> containing the
     *         parameters to be added to the SQL. Each entry in List<Object> matches the index of a ? character in the sql
     *         <p/>
     *         If isMasterQuery is true, then the sql query has the following columns:
     *         <pre>
     *                          SERVICE_ID, SERVICE_NAME, ROUTING_URI and CONSTANT_GROUP
     *                          </pre>
     *         If isMasterQuery is false, it has the following columns:-
     *         <pre>
     *                          SERVICE_ID | SERVICE_NAME | ROUTING_URI | ATTEMPTED | COMPLETED | AUTHORIZED | FRONT_SUM | BACK_SUM | THROUGHPUT
     *                          | POLICY_VIOLATIONS | ROUTING_FAILURES | FRTM | FRTMX | FRTA   | BRTM | BRTMX | BRTA   | AP     | CONSTANT_GROUP
     *                          | AUTHENTICATED_USER | SERVICE_OPERATION_VALUE | MAPPING_VALUE_1 | MAPPING_VALUE_2 | MAPPING_VALUE_3 |
     *                          MAPPING_VALUE_4 | MAPPING_VALUE_5
     *                          </pre>
     */
    public static Pair<String, List<Object>> getPerformanceStatisticsMappingQuery(boolean isMasterQuery, Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                                                  Map<String, Set<String>> serviceIdToOperations,
                                                                                  LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters, int resolution,
                                                                                  boolean isDetail, boolean isUsage) {

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keysSupplied = checkMappingQueryParams(keysToFilters, isDetail, isUsage);

        boolean serviceIdsOk = (serviceIdToOperations != null && !serviceIdToOperations.keySet().isEmpty());

        checkResolutionParameter(resolution);

        boolean useUser = (keysSupplied) && isUserSupplied(keysToFilters);

        List<Object> queryParams = new ArrayList<Object>();

        //----SECTION A----
        StringBuilder sb;
        if (isMasterQuery) {
            sb = new StringBuilder(distinctFrom);
        } else {
            String select = MessageFormat.format(aggregateSelect, "smd");
            sb = new StringBuilder(select);
        }
        //----SECTION B----
        addUserToSelect(true, useUser, sb, queryParams);
        //----SECTION C----
        addOperationToSelect(isDetail, sb, queryParams);
        //----SECTION D's----
        List<String> keys = new ArrayList<String>();
        if (keysSupplied) keys.addAll(keysToFilters.keySet());
        addCaseSQL(keys, sb, queryParams);
        //----SECTION E----
        sb.append(mappingJoin);
        //----SECTION F----
        addResolutionConstraint(resolution, sb, queryParams);

        //----SECTION G----
        if (useTime) {
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb, queryParams);
        }

        //----SECTION H & I----
        //Service ids only constrained here, if isDetail is false, otherwise operation and services are constrained
        //together below

        boolean isBlankedOpQuery = isBlanketOperationQuery(serviceIdToOperations);
        //not a detail query and service id's are ok
        // OR is a detail query, and we have blanked operation requirements
        if (serviceIdsOk && ((!isDetail) || (isBlankedOpQuery && isDetail))) {
            addServiceIdConstraint(serviceIdToOperations.keySet(), sb, queryParams);
        }
        //else isDetail and were going to use operations
        else if (serviceIdsOk && !isBlankedOpQuery && isDetail) {
            //new method to constrain serivce id and operation together
            addServiceAndOperationConstraint(serviceIdToOperations, sb, queryParams);
        }

        //----SECTION J----
        if (useUser) {
            List<ReportApi.FilterPair> userFilterPairs = getAuthenticatedUserFilterPairs(keysToFilters);
            if (!userFilterPairs.isEmpty()) {
                addUserConstraint(userFilterPairs, sb, queryParams);
            } else {
                addUserNotNullConstraint(sb);
            }
        }

        //----SECTION K----
        if (keysSupplied) {
            addMappingConstraint(keysToFilters, sb, queryParams);
        }

        if (!isMasterQuery) {
            //----SECTION L----
            addGroupBy(sb);
        }

        //----SECTION M----
        addMappingOrder(sb);

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "getPerformanceStatisticsMappingQuery: " + logCompleteSql(sb.toString(), queryParams));
        }

        return new Pair<String, List<Object>>(sb.toString(), queryParams);
    }

    private static String logCompleteSql(String sql, List<Object> params) {

        StringBuilder sb = new StringBuilder();
        int paramIndex = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '?') {
                Object o = params.get(paramIndex);
                paramIndex++;
                if (o instanceof String) {
                    sb.append("'").append(SqlUtils.mySqlEscapeIllegalSqlChars(o.toString())).append("'");
                } else {
                    sb.append(" ").append(o).append(" ");
                }

            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * This method should only be called when some of the service id's map to one or more operations. Operations are
     * therefore only included in a query when they can be explicitly constrained by a service id. This ensures that
     * when a selection of operations are made by the user, that operations from other services with the same name won't
     * also be included in report output.
     * <br>
     * Note: it's possible that calling this
     *
     * @param serviceIdToOperations a map of service id to the set of operations.
     * @param sb                    the string builder to add sql to
     * @param queryParams           List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                              character is added to the string sql being build in sb, before any other ? characters are added
     * @throws IllegalArgumentException if serviceIdToOperations is null or empty. Calling code should ensure that this
     *                                  function is only called when it is required. See usages
     */
    protected static void addServiceAndOperationConstraint(Map<String, Set<String>> serviceIdToOperations,
                                                           StringBuilder sb, List<Object> queryParams) {
        if (serviceIdToOperations == null || serviceIdToOperations.isEmpty()) {
            throw new IllegalArgumentException("serviceIdToOperations cannot be null and cannot be empty");
        }

        int index = 0;
        //and surrounds the entire constraint
        sb.append(" AND (");

        for (Map.Entry<String, Set<String>> me : serviceIdToOperations.entrySet()) {
            //we know this statement is not true for all elements in the map, but it may be true for some
            //if a service has no op's listed, then it's simply ignored, could log a warning
            //see isBlankedOperationQuery
            if (me.getValue() == null || me.getValue().isEmpty()) continue;

            if (index > 0) sb.append(" OR ");

            sb.append("( ");

            sb.append(" p.objectid = ? ");
            queryParams.add(Long.valueOf(me.getKey()));

            sb.append(" AND mcmv.service_operation IN (");

            int opIndex = 0;
            for (String op : me.getValue()) {
                if (opIndex != 0) sb.append(",");
                sb.append(" ? ");
                queryParams.add(op);
                opIndex++;
            }

            sb.append(")");//close in IN
            sb.append(" ) ");//close the OR
            index++;
        }
        sb.append(" ) ");
    }

    /**
     * Find out if services need to be constrained by operations. Based on the standard reports UI some of the conditions
     * this allows for does not happen, but the sql query logic deals with it.
     * <br>
     * If a selection was allowed when operations and services could be selected together, then this determines if as a
     * general policy for the sql being generated, whether or not service id's should be constrained by operations or not.
     *
     * @param serviceIdToOperations The map serviceIdToOperations lists all service ids and any operations they map to.
     *                              If a single service id maps to an operation then the result is false. If every single service id key maps to a
     *                              null or empty list, then true is returned, as the query will not need to constrain the service id's with operation
     *                              information
     * @return true if the serviceIdToOperations should be treated as a service id only constraint, or false indicating
     *         that operations should be included in the sql constraint
     */
    private static boolean isBlanketOperationQuery(Map<String, Set<String>> serviceIdToOperations) {
        if (serviceIdToOperations == null || serviceIdToOperations.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Set<String>> me : serviceIdToOperations.entrySet()) {
            if (me.getValue() == null) continue;
            if (!me.getValue().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Convenience method called from sub reports. Instead of taking in collections of service ids, operations and
     * authenticated users, it takes in string values, places them in a collection and then calls getPerformanceStatisticsMappingQuery,
     * which this method delegates to.<br>
     * See getPerformanceStatisticsMappingQuery for more details<br>
     * Note: this query returns exactly the same columns as getNoMappingQuery
     *
     * @param startTimeInclusiveMilli time_period start time inclusive
     * @param endTimeInclusiveMilli   time_period end time exclsuvie
     * @param serviceId               the service id we want a mapping query for
     * @param keysToFilterValues      a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                                represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                                key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                                and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                                hash map.
     * @param resolution              1 = hourly, 2 = daily. Which resolution from service_metrics to use
     * @param isDetail                if true then the service_operation's real value is used in the select, group and order by,
     *                                otherwise operation is selected as 1. To facilitate this service_operation is always selected as
     *                                SERVICE_OPERATION_VALUE so that the real column is not used when isDetail is false
     *                                table message_context_mapping_values, with the values in operaitons
     *                                message_context_mapping_values, with the values in authenticatedUsers
     * @param operation               if isDetail is true, the operation we want performance statistics information for
     * @param isUsage                 needed in order to validate the input parameters
     * @return String query, see getPerformanceStatisticsMappingQuery for details of the return columns
     * @throws IllegalArgumentException If all the lists are not the same size and if they are empty.
     */
    public static Pair<String, List<Object>> getPerformanceStatisticsMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                                                  Long serviceId, LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterValues,
                                                                                  int resolution, boolean isDetail, String operation, boolean isUsage) {

        if (serviceId == null) throw new IllegalArgumentException("Service Id must be supplied");
        Set<String> operationSet = new HashSet<String>();
        if (operation != null && !operation.equals("") && !operation.equals(SQL_PLACE_HOLDER)) {
            operationSet.add(operation);
        }
        Map<String, Set<String>> serviceIdToOperations = new HashMap<String, Set<String>>();
        serviceIdToOperations.put(serviceId.toString(), operationSet);

        return getPerformanceStatisticsMappingQuery(false, startTimeInclusiveMilli, endTimeInclusiveMilli, serviceIdToOperations,
                keysToFilterValues, resolution, isDetail, isUsage);
    }

    /**
     * Add sql to sb for selecting out the auth_user_id from message context mapping values. The user is ALWAYS
     * added to all select lists for performance statistics queries, however this method determines whether or not
     * the real column is selected or the placeholder is selected.
     *
     * @param addComma    should a comma be added before any sql is written to sb
     * @param useUser     should the real value of mcmv.auth_user_id be used or the sql place holder
     * @param sb          the string builder to write the sql to
     * @param queryParams List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                    character is added to the string sql being build in sb, before any other ? characters are added
     */
    private static void addUserToSelect(boolean addComma, boolean useUser, StringBuilder sb, List<Object> queryParams) {
        if (addComma) sb.append(",");
        if (useUser) {
            sb.append(" mcmv.auth_user_id AS ").append(AUTHENTICATED_USER);
        } else {
            sb.append(" ? AS ").append(AUTHENTICATED_USER);
            queryParams.add(SQL_PLACE_HOLDER);
        }
    }

    /**
     * Get the sql query for performance statistics reports with no mappings involved. The sql returned will be constrained
     * based on the input parameters
     * Note: this query returns exactly the same columns as getPerformanceStatisticsMappingQuery when the parameter isMasterQuery has the
     * same value
     *
     * @param isMasterQuery           if true, this query will only return the SERVICE_ID, SERVICE_NAME, ROUTING_URI and
     *                                CONSTANT_GROUP. Set to true when you want to find out what services match the supplied criteria. This is used
     *                                to drive the master interval reports, where we will run a sub report for every serivce id found.
     * @param startTimeInclusiveMilli start of the time period
     * @param endTimeInclusiveMilli   end of the time period
     * @param serviceIds              list of service ids to constrain the query with
     * @param resolution              which resolution bin to use, hourly or daily
     * @return String query if isMasterQuery is true, it has the following columns:
     *         SERVICE_ID, SERVICE_NAME, ROUTING_URI and CONSTANT_GROUP.
     *         If isMasterQuery is false, it has the following columns:-
     *         SERVICE_ID | SERVICE_NAME | ROUTING_URI | ATTEMPTED | COMPLETED | AUTHORIZED | FRONT_SUM | BACK_SUM | THROUGHPUT
     *         | POLICY_VIOLATIONS | ROUTING_FAILURES | FRTM | FRTMX | FRTA   | BRTM | BRTMX | BRTA   | AP     | CONSTANT_GROUP
     *         | AUTHENTICATED_USER | SERVICE_OPERATION_VALUE | MAPPING_VALUE_1 | MAPPING_VALUE_2 | MAPPING_VALUE_3 |
     *         MAPPING_VALUE_4 | MAPPING_VALUE_5
     * @throws IllegalArgumentException if both start and end time parameters have not been specified
     */
    public static Pair<String, List<Object>> getNoMappingQuery(boolean isMasterQuery, Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                               Collection<String> serviceIds, int resolution) {


        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);
        if (!useTime) throw new IllegalArgumentException("Both start and end time must be specified");
        checkResolutionParameter(resolution);

        List<Object> queryParams = new ArrayList<Object>();

        StringBuilder sb;
        if (isMasterQuery) {
            sb = new StringBuilder(distinctFrom);
        } else {
            String select = MessageFormat.format(aggregateSelect, "sm");
            sb = new StringBuilder(select);
        }

        //fill in place holder's
        sb.append(", ? AS AUTHENTICATED_USER");
        queryParams.add(SQL_PLACE_HOLDER);
        sb.append(", ? AS SERVICE_OPERATION_VALUE");
        queryParams.add(SQL_PLACE_HOLDER);
        sb.append(", ? AS MAPPING_VALUE_1");
        queryParams.add(SQL_PLACE_HOLDER);
        sb.append(", ? AS MAPPING_VALUE_2");
        queryParams.add(SQL_PLACE_HOLDER);
        sb.append(", ? AS MAPPING_VALUE_3");
        queryParams.add(SQL_PLACE_HOLDER);
        sb.append(", ? AS MAPPING_VALUE_4");
        queryParams.add(SQL_PLACE_HOLDER);
        sb.append(", ? AS MAPPING_VALUE_5");
        queryParams.add(SQL_PLACE_HOLDER);

        sb.append(noMappingJoin);

        addResolutionConstraint(resolution, sb, queryParams);

        addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb, queryParams);

        if (serviceIds != null && !serviceIds.isEmpty()) {
            addServiceIdConstraint(serviceIds, sb, queryParams);
        }

        if (isMasterQuery) {
            sb.append(" ORDER BY p.objectid ");
        } else {
            sb.append(" GROUP BY p.objectid ");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "getNoMappingQuery: " + logCompleteSql(sb.toString(), queryParams));
        }

        return new Pair<String, List<Object>>(sb.toString(), queryParams);
    }

    /**
     * Convenience method for sub reports. Delegates to getNoMappingQuery, adding the service id to a Collection
     *
     * @param startTimeInclusiveMilli start of the time period
     * @param endTimeInclusiveMilli   end of the time period
     * @param serviceId               the service id to constrain the query with
     * @param resolution              which resolution bin to use, hourly or daily
     * @return String sql. See getNoMappingQuery for details of return columns
     * @throws IllegalArgumentException if service id is null or empty
     */
    public static Pair<String, List<Object>> getNoMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                               Long serviceId, int resolution) {

        if (serviceId == null) throw new IllegalArgumentException("Service id must be supplied");
        List<String> sIds = new ArrayList<String>();
        sIds.add(serviceId.toString());

        return getNoMappingQuery(false, startTimeInclusiveMilli, endTimeInclusiveMilli, sIds, resolution);
    }

    /**
     * Add a select value so that the column SERVICE_OPERATION_VALUE is always contained in the sql generated.
     * Whether or not the value of SERVICE_OPERATION_VALUE is the sql place holder or mcmv.service_operation is
     * determined by isDetail being true or false
     *
     * @param isDetail    if true, add the real column to the select, else add the sql place holder
     * @param sb          the string builder to add the sql to
     * @param queryParams List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                    character is added to the string sql being build in sb, before any other ? characters are added
     */
    private static void addOperationToSelect(boolean isDetail, StringBuilder sb, List<Object> queryParams) {
        if (isDetail) {
            sb.append(",  mcmv.service_operation AS SERVICE_OPERATION_VALUE");
        } else {
            sb.append(",  ? AS SERVICE_OPERATION_VALUE");
            queryParams.add(SQL_PLACE_HOLDER);
        }
    }

    /**
     * Essentially create a new keysToFilterPairs representation, where each key has only one value. The returned
     * linked hash map can then be used as the input to methods like getPerformanceStatisticsMappingQuery which require a linked has map
     * representing the key and value constraints.<br>
     * This method is called from the performance statistics sub report, at which level it is querying for SPECIFIC
     * values of keys and not a general constrain as is supplied in the master reports.<br>
     * Therefore we need to convert the supplied map into a more restricted map with only 1 value per key.<br>
     * This is a convenience method for sub queries, which is going to select out aggregate values for SPECIFIC values
     * of keys for a specific interval.<br>
     * The sub query is fed values for each mapping value from it's master report as report parameters, this includes
     * AUTH_USER.
     * <br>
     * <br>
     * <p/>
     * runtimeMappingDistinctSetArray is a String array who's length should equal the number of max keys the ssg
     * currently supports, which is currently 5.
     * <br>
     * <br>
     * <p/>
     * <em>
     * The string at index i IMPLICITLY matches the key at the same index in keysToFilterPairs. THIS IS WHY THIS
     * DATA STRUCTURE IS ALWAYS A LINKED HASH MAP, AS ORDER MUST BE MAINTAINED.
     * </em>
     * <br>
     * <br>
     * <p/>
     * <em>
     * Every key in keysToFilterPairs MUST HAVE a NON NULL and NON SQL PLACEHOLDER value in
     * runtimeMappingDistinctSetArray. If not an IllegalArgumentException is thrown as this represents a serious logic
     * error in the reports.
     * </em>
     * <br>
     * indexes of runtimeMappingDistinctSetArray for which have a higher index than the last key in keysToFilterPairs,
     * is not important and should be the SQL_PLACE_HOLDER.
     * This is as all queries always include all mapping_value_x (x:1-5), so when less than 5 keys are used, then
     * their select value is just SQL_PLACE_HOLDER, so it has no affect on group and order by operations.
     * <br>
     * <em>ONLY CALL FROM PERFORMANCE STATISTICS REPORTS - DO NOT CALL FROM USAGE REPORTS</em>
     * <br>
     * Remember the values in runtimeMappingDistinctSetArray are coming directly from the database! This means that its
     * possible for these values to contain illegal sql characters which must be escaped. The returned map contains
     * values which are escaped. The returned values are always used with '=' equals to match values in the db
     *
     * @param keysToFilterPairs              a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                                       represent it's constraints. This linked hash map represents all keys and all FilterPairs used for the entire
     *                                       report. This is to be passed down to the sub report via parameters.
     * @param runtimeMappingDistinctSetArray the array of mapping key values 1- max num mappings (currently 5). This is
     *                                       an array as this function IS called from performance statistics interval sub reports, which have been fed in
     *                                       string values for each mapping values as a parameter to the report
     * @param authUser                       String value for authenticated user to use when creating queries
     * @param isDetail                       required for validating the input parameters
     * @return LinkedHashMap representation of the args. Essentially is keysToFilterPairs, for all keys, but for specific
     *         values of those keys, 1 value per key
     * @throws IllegalArgumentException if any index from keys results in a null or SQL_PLACE_HOLDER value from args
     */
    public static LinkedHashMap<String, List<ReportApi.FilterPair>> createDistinctKeyToFilterMap(
            LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs, String[] runtimeMappingDistinctSetArray,
            String authUser, boolean isDetail) {

        if (runtimeMappingDistinctSetArray.length != NUM_MAPPING_KEYS) {
            throw new IllegalArgumentException("Currently: " + NUM_MAPPING_KEYS + " are supported. This many keys must" +
                    "be supplied in runtimeMappingDistinctSetArray. Any values not required should be the sql place holder");
        }

        LinkedHashMap<String, List<ReportApi.FilterPair>> returnMap = new LinkedHashMap<String, List<ReportApi.FilterPair>>();

        if (keysToFilterPairs == null) {
            //it's a ps report calling this, so it's possible the only mapping is isDetail = true
            return returnMap;
        }

        if (keysToFilterPairs.keySet().size() > runtimeMappingDistinctSetArray.length)
            throw new IllegalArgumentException("Parameter keys must never be greater in size " +
                    "than parameter args");

        boolean keysSupplied = checkMappingQueryParams(keysToFilterPairs, isDetail, false);

        //check auth user
        boolean useUser = (keysSupplied) && isUserSupplied(keysToFilterPairs);
        if (useUser) {
            if (authUser == null || authUser.equals(SQL_PLACE_HOLDER)) {
                throw new IllegalArgumentException("Authenticated User cannot have the place holder value, when it was " +
                        "specified as a valid key");
            }
            List<ReportApi.FilterPair> authList = new ArrayList<ReportApi.FilterPair>();
            //in case values have the * symbol, tell FilterPair not to be smart
            authList.add(new ReportApi.FilterPair(authUser, true));
            returnMap.put(MessageContextMapping.MappingType.AUTH_USER.toString(), authList);
        }

        int index = 0;
        //order is really important, implied from LinkedHashMap
        for (String s : keysToFilterPairs.keySet()) {
            if (s.equals(MessageContextMapping.MappingType.AUTH_USER.toString())) continue;

            String runTimeMappingValue = runtimeMappingDistinctSetArray[index];

            if (runTimeMappingValue == null || runTimeMappingValue.equals(SQL_PLACE_HOLDER))
                throw new IllegalArgumentException("Any key in keyToFilterPairs, must contain a real value and not null or " + SQL_PLACE_HOLDER);

            List<ReportApi.FilterPair> fpList = new ArrayList<ReportApi.FilterPair>();
            //in case values have the * symbol, tell FilterPair not to be smart
            fpList.add(new ReportApi.FilterPair(runTimeMappingValue, true));
            returnMap.put(s, fpList);
            index++;
        }
        return returnMap;
    }

    /**
     * Add the resolution constraint to the sql query
     *
     * @param resolution  valid resolution 1 or 2, not rechecked here
     * @param sb          string bulder to add the sql to
     * @param queryParams List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                    character is added to the string sql being build in sb, before any other ? characters are added
     */
    private static void addResolutionConstraint(int resolution, StringBuilder sb, List<Object> queryParams) {
        sb.append(" AND sm.resolution = ? ");
        queryParams.add(resolution);
    }

    /**
     * Add the time constrain to the sql. startIntervalMilliSeconds is inclusive, endTimeInclusiveMilli is exclusive
     *
     * @param startTimeInclusiveMilli start of the time period inclusive
     * @param endTimeInclusiveMilli   end of the time period exclusive
     * @param sb                      string builder to add the sql to
     * @param queryParams             List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                                character is added to the string sql being build in sb, before any other ? characters are added
     */
    private static void addTimeConstraint(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli, StringBuilder sb, List<Object> queryParams) {
        sb.append(" AND sm.period_start >= ? ");
        queryParams.add(startTimeInclusiveMilli);
        sb.append(" AND sm.period_start < ? ");
        queryParams.add(endTimeInclusiveMilli);
    }

    /**
     * Add the group by clause. Used by both performance statistics queries and usage queries. Ensures the group by
     * is added in the correct order to ensure that the largest group is always first. The group ordering is:-<br>
     * p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3,
     * MAPPING_VALUE_4 and MAPPING_VALUE_5
     *
     * @param sb string builder to add the sql to
     */
    private static void addGroupBy(StringBuilder sb) {
        sb.append(" GROUP BY p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER ");
        for (int i = 0; i < NUM_MAPPING_KEYS; i++) {
            sb.append(", ").append("MAPPING_VALUE_").append((i + 1));
        }
    }

    /**
     * Determine for the report type and whether the report is a detail query or not, whether the keysToFilterValues
     * parameter is valid. Return boolean valud indicates whether or not valid keys have been supplied for a query
     * Keys do not need to be supplied for a performance statistics report when it's a detail report, howevere they
     * must always be supplied for a usage report.
     *
     * @param keysToFilterValues a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                           represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                           key then the isEmpty() method of FilterPair should return true.
     * @param isDetail           is the usage of the keys for a detail report?
     * @param isUsageQuery       is the usage of the keys a usage report?
     * @return boolean true when the keys were supplied and are valid, false otherwise. False will never be returned
     *         when isUsage is true and keysToFilterValues is null or empty.
     * @throws IllegalArgumentException if isUsageQuery is true, and keysToFilterValues is null or empty. Also thrown
     *                                  if isDetail is false and keysToFilterValues is null or empty
     */
    public static boolean checkMappingQueryParams(Map<String, List<ReportApi.FilterPair>> keysToFilterValues,
                                                  boolean isDetail, boolean isUsageQuery) {
        //we need at least one key. However both user and operation are technically keys, so if we have either
        //a user or an operation, they we have conceptually a key

        if (keysToFilterValues == null || keysToFilterValues.isEmpty()) {

            if (isUsageQuery) {
                throw new IllegalArgumentException("Usage queries require at least one message context key");
            }

            if (!isDetail) {
                throw new IllegalArgumentException("Non detail mapping queries require at least one message context key");
            }
            return false;
        }
        return true;
    }

    /**
     * Validate the time parameters. Time params are valid when they are both not null and the start time is < the end
     * time
     *
     * @param startTimeInclusiveMilli start of the time period inclusive
     * @param endTimeInclusiveMilli   end of the time period exclusive
     * @return boolean true when the time parameters should be included in a sql query, false when not.
     * @throws IllegalArgumentException if both params are not both null or both not null, or of startTimeInclusiveMilli
     *                                  is >= endIntervalMilliSeconds
     */
    private static boolean checkTimeParameters(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli) {
        boolean bothNull = (startTimeInclusiveMilli == null) && (endTimeInclusiveMilli == null);
        boolean bothNotNull = (startTimeInclusiveMilli != null) && (endTimeInclusiveMilli != null);
        if (!(bothNull || bothNotNull)) {
            throw new IllegalArgumentException("startTimeInclusiveMilli and endTimeInclusiveMilli must both be null" +
                    "or not null");
        }
        if (bothNotNull) {
            if (startTimeInclusiveMilli >= endTimeInclusiveMilli) {
                throw new IllegalArgumentException("startTimeInclusiveMilli must be < than endTimeInclusiveMilli");
            }
            return true;
        }
        return false;
    }

    /**
     * Validate the resolution parameter. It should only represent 1 or 2, hourly or daily
     *
     * @param resolution resolution to validate
     * @throws IllegalArgumentException if the resolution is not 1 or 2
     */
    private static void checkResolutionParameter(int resolution) {
        if (resolution != 1 && resolution != 2) {
            throw new IllegalArgumentException("Resolution can only be 1 (Hourly) or 2 (Daily)");
        }
    }

    /**
     * Add the order by query to the sql. Ensures that the order by is added in the correct order. The ordering is
     * very important and the logic of the jasper reports is 100% dependent on this ordering never changing. If this
     * function is updated, then all usages of the sql queries, which are built using this query, must have their
     * usages examined and understand how this ordering will affect the runinng of the jasper reports.<br>
     * <em>THIS IS ONLY TO BE USED WHEN GENERATING SQL FOR PERFORMANCE STATISTICS REPORTS</em>
     * The order is as follows:-<br>
     * AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5,
     * p.objectid, SERVICE_OPERATION_VALUE
     *
     * @param sb string builder to add the sql to
     */
    private static void addMappingOrder(StringBuilder sb) {
        sb.append(" ORDER BY AUTHENTICATED_USER, ");
        for (int i = 0; i < NUM_MAPPING_KEYS; i++) {
            if (i != 0) sb.append(", ");
            sb.append("MAPPING_VALUE_").append((i + 1));
        }
        sb.append(" ,p.objectid, SERVICE_OPERATION_VALUE ");
    }

    /**
     * Add the order by query to the sql. Ensures that the order by is added in the correct order. The ordering is
     * very important and the logic of the jasper reports is 100% dependent on this ordering never changing. If this
     * function is updated, then all usages of the sql queries, which are built using this query, must have their
     * usages examined and understand how this ordering will affect the runinng of the jasper reports.<br>
     * <em>THIS IS ONLY TO BE USED WHEN GENERATING SQL FOR USAGE REPORTS</em>
     * The order is as follows:-<br>
     * p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3,
     * MAPPING_VALUE_4, MAPPING_VALUE_5,
     *
     * @param sb string builder to add the sql to
     */
    private static void addUsageMappingOrder(StringBuilder sb) {
        sb.append(" ORDER BY p.objectid, SERVICE_OPERATION_VALUE ");
        sb.append(" ,AUTHENTICATED_USER, ");
        for (int i = 0; i < NUM_MAPPING_KEYS; i++) {
            if (i != 0) sb.append(", ");
            sb.append("MAPPING_VALUE_").append((i + 1));
        }
    }


    /**
     * This is currently only used by getDistinctMappingQuery, which is only used from ReportGenerator. It's used
     * to get meta data about what data the report will get at runtime, to prepare data to be given to the report
     * as a parameter. The order is not strictly required.
     *
     * @param sb string builder to add the sql to
     */
    private static void addUsageDistinctMappingOrder(StringBuilder sb) {
        sb.append(" ORDER BY AUTHENTICATED_USER, ");
        for (int i = 0; i < NUM_MAPPING_KEYS; i++) {
            if (i != 0) sb.append(", ");
            sb.append("MAPPING_VALUE_").append((i + 1));
        }
    }

    /**
     * Add key and value constraints to the sql query being generated. To understand how key and values are constrained
     * it's important to understand that there is an implicit relationship between the key at index x(x:1-5) in
     * message_context_mapping_keys and the value at index x(x:1-5) in message_context_mapping_values<br>
     * <em>When the AUTH_USER key is found, it is ignored. It is treated separately. AUTH_USER never causes a constraint
     * to be added here. When it is found, it is ignored</em><br>
     * For each key included in keysToFilters, an AND block is added. This AND block will constrain each of the
     * mapping keys 1 to 5 with the key value. Each of these constraints are OR'd within the AND block. This constraint
     * means that <em>AT LEAST ONE MAPPING KEY COLUMN MUST MATCH THE KEY</em><br>
     * <p/>
     * Each key in keysToFilters maps to 0..* FilterPair's. For every FilterPair in the list, that each key maps to,
     * who's isEmpty() method returns false, is included in the AND block as follows:-<br>
     * For each OR constaint within the AND block, it's has a sub AND block added. Within this sub AND block, the
     * corresponding mcmk.mappingX_value (x:1-5) column is constrained. There is a constraint added for each non empty
     * FilterPair, which are all OR'd together. Whether the constraint is EQUAL or LIKE is determined by each FilterPair.
     * It is possible that within a sub AND block, that the values have a mix of AND and LIKE<br>
     * <pre>
     *     AND -- this is the start of an AND block for key IP_ADDRESS
     * (mcmk.mapping1_key = 'IP_ADDRESS' AND --this is the start of the sub AND block
     * (mcmk.mapping1_value = '127.0.0.1' OR  mcmk.mapping1_value = '127.0.0.2' --there were two non empty FilterPairs))
     * OR
     * --the key is constrained for EVERY mcmk.mappingX_key(x:1-5)
     * (mcmk.mapping2_key = 'IP_ADDRESS' AND (mcmk.mapping2_value = '127.0.0.1' OR  mcmk.mapping2_value = '127.0.0.2'))
     * OR
     * (mcmk.mapping3_key = 'IP_ADDRESS' AND (mcmk.mapping3_value = '127.0.0.1' OR  mcmk.mapping3_value LIKE '127.0.%.2'))
     * OR
     * (mcmk.mapping4_key = 'IP_ADDRESS' AND (mcmk.mapping4_value = '127.0.0.1' OR  mcmk.mapping4_value = '127.0.0.2'))
     * OR
     * (mcmk.mapping5_key = 'IP_ADDRESS' AND (mcmk.mapping5_value = '127.0.0.1' OR  mcmk.mapping5_value = '127.0.0.2'))
     * )
     * </pre>
     *
     * @param keysToFilters a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                      represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                      key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                      and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                      hash map.
     * @param sb            string builder to add the sql to
     * @param queryParams   List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                      character is added to the string sql being build in sb, before any other ? characters are added
     */
    private static void addMappingConstraint(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                             StringBuilder sb, List<Object> queryParams) {
        if (keysToFilters == null || keysToFilters.isEmpty()) {
            throw new IllegalArgumentException("keysToFilters cannot be empty");
        }

        for (Map.Entry<String, List<ReportApi.FilterPair>> me : keysToFilters.entrySet()) {
            if (me.getKey().equals(MessageContextMapping.MappingType.AUTH_USER.toString())) continue;

            sb.append(" AND (");
            for (int i = 1; i <= NUM_MAPPING_KEYS; i++) {
                if (i != 1) {
                    sb.append(" OR ");
                }
                sb.append("( mcmk.mapping").append(i).append("_key");
                sb.append(" = ? ");
                queryParams.add(me.getKey());
                if (me.getValue() == null || me.getValue().isEmpty()) {
                    throw new IllegalArgumentException("Each key must have a list of FilterPairs");
                }
                StringBuilder tempBuffer = new StringBuilder();
                tempBuffer.append(" AND (");
                boolean constraintAdded = false;
                int index = 0;
                for (ReportApi.FilterPair fp : me.getValue()) {
                    if (!fp.isConstraintNotRequired()) {
                        constraintAdded = true;
                        if (index != 0) tempBuffer.append(" OR ");

                        tempBuffer.append(" mcmv.mapping").append(i).append("_value");
                        if (!fp.isQueryUsingWildCard()) {
                            tempBuffer.append(" = ? ");
                            queryParams.add(fp.getFilterValue());
                        } else {
                            tempBuffer.append(" LIKE ? ");
                            queryParams.add(fp.getFilterValue());
                        }
                    }
                    index++;
                }
                tempBuffer.append(" ) ");

                if (constraintAdded) {
                    sb.append(tempBuffer);
                }
                sb.append(" ) ");
            }
            sb.append(" ) ");
        }
    }

    /**
     * Add the AUTH_USER constraint to the sql. This creates an AND block and within it the mcmv.auth_user_id
     * is constrained using EQUALS or LIKE, depending on the FilterPair, which are all OR'd together<br>
     * <pre>
     * AND(mcmv.auth_user_id = 'Donal' OR mcmv.auth_user_id LIKE 'Ldap%')
     * </pre>
     * Note: Any caller of this function should ensure that the list authUserFilterPairs only contains FilterPairs
     * which are related to AUTH_USER mapping key. See usages for where this is done.<br>
     * Note: Any caller has already determined that calling this function is required, based on the set of mapping keys
     * submitted to the callers function.
     * //todo possibly refactor FilterPair to include the mapping key, so we can validate here that only AUTH_USER fp's are received
     *
     * @param authUserFilterPairs the list of all FilterPairs, representing the AUTH_USER mapping key
     * @param sb                  string builder to add the sql to
     * @param queryParams         List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                            character is added to the string sql being build in sb, before any other ? characters are added
     */
    private static void addUserConstraint(List<ReportApi.FilterPair> authUserFilterPairs,
                                          StringBuilder sb, List<Object> queryParams) {
        if (authUserFilterPairs.isEmpty()) throw new IllegalArgumentException("authUserFilterPairs cannot be empty");

        sb.append(" AND (");

        int index = 0;
        for (int i = 0; i < authUserFilterPairs.size(); i++) {
            ReportApi.FilterPair fp = authUserFilterPairs.get(i);
            if (fp.isConstraintNotRequired()) continue;

            if (index != 0) sb.append(" OR ");
            if (!fp.isQueryUsingWildCard()) {
                sb.append("mcmv.auth_user_id = ? ");
                queryParams.add(fp.getFilterValue());
            } else {
                sb.append("mcmv.auth_user_id LIKE ? ");
                queryParams.add(fp.getFilterValue());
            }
            index++;
        }
        sb.append(") ");
    }

    /**
     * It's possible for auth_user to have null values, when a context assertion which includes it either does not
     * get a value or it's simply not in a context assertion. In this case we want to filter out any null values
     * when we are interested in auth_user
     *
     * @param sb string builder to add the sql to
     */
    private static void addUserNotNullConstraint(StringBuilder sb) {
        sb.append(" AND mcmv.auth_user_id IS NOT NULL ");
    }

    /**
     * When no operation are required, as the report is not a detail report, then the sql only needs to be
     * constrained by the service ids. For each service id in serviceIds, it is added to an IN constraint
     * <br>
     * Note: A caller has already decided that this function is required. Calling this function means the sql
     * being generated is not for a detail report
     *
     * @param serviceIds  the list of service ids the sql should be constrained by
     * @param sb          string builder to add the sql to
     * @param queryParams List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                    character is added to the string sql being build in sb, before any other ? characters are added
     */
    private static void addServiceIdConstraint(Collection<String> serviceIds, StringBuilder sb, List<Object> queryParams) {
        sb.append(" AND p.objectid IN (");
        boolean first = true;
        for (String s : serviceIds) {
            if (!first) sb.append(", ");
            else first = false;

            sb.append(" ? ");
            queryParams.add(s);
        }
        sb.append(")");
    }

    /**
     * Performance Statistics and Usage reports <em>ALWAYS</em> call this function. Every single query created which is
     * used as a master query or sub report query in the jasper reports, <em>MUST</em> have called this function to ensure
     * that all mapping keys are always added to the SELECT list.<br>
     * <em>For every String key, which is NOT EQUAL to AUTH_USER, addCaseSQLForKey is called. AUTH_USER is handled
     * separately</em><br>
     * After all keys have been added, the SQL_PLACE_HOLDER value is added for mapping keys whose index were not used
     * due to the size of keys. This ensures that all queries ALWAYS return mapping_value_x (x:1-5)<br>
     * The reports WILL BREAK if this was to change.<br>
     * <br>
     * <em>Note the parameter is just a list of keys. This order of this list is extremelly important. What ever index
     * a key is at in this list, then that key must be in the SAME index, in any other ordered data structure used
     * in other functions within this class, for the same sql statement being generated.
     * If not the reports are not correct and the queries are junk</em>
     * <p/>
     * Sample output for a list with 2 keys:<br>
     * <pre>
     * CASE
     * WHEN mcmk.mapping1_key = 'IP_ADDRESS' THEN mcmv.mapping1_value
     * WHEN mcmk.mapping2_key = 'IP_ADDRESS' THEN mcmv.mapping2_value
     * WHEN mcmk.mapping3_key = 'IP_ADDRESS' THEN mcmv.mapping3_value
     * WHEN mcmk.mapping4_key = 'IP_ADDRESS' THEN mcmv.mapping4_value
     * WHEN mcmk.mapping5_key = 'IP_ADDRESS' THEN mcmv.mapping5_value
     * END AS MAPPING_VALUE_1,
     * CASE
     * WHEN mcmk.mapping1_key = 'CUSTOMER' THEN mcmv.mapping1_value
     * WHEN mcmk.mapping2_key = 'CUSTOMER' THEN mcmv.mapping2_value
     * WHEN mcmk.mapping3_key = 'CUSTOMER' THEN mcmv.mapping3_value
     * WHEN mcmk.mapping4_key = 'CUSTOMER' THEN mcmv.mapping4_value
     * WHEN mcmk.mapping5_key = 'CUSTOMER' THEN mcmv.mapping5_value
     * END AS MAPPING_VALUE_2,
     * ';' AS MAPPING_VALUE_3, ';' AS MAPPING_VALUE_4, ';' AS MAPPING_VALUE_5
     * <p/>
     * </pre>
     * Note how even though there are only 2 keys, that all 5 mapping value columns are created
     *
     * @param keys        the mapping keys values
     * @param sb          string builder to add the sql to
     * @param queryParams List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                    character is added to the string sql being build in sb, before any other ? characters are added
     */
    private static void addCaseSQL(List<String> keys, StringBuilder sb, List<Object> queryParams) {
        int max = 0;
        if (keys != null && !keys.isEmpty()) {
            for (String s : keys) {
                if (!s.equals(MessageContextMapping.MappingType.AUTH_USER.toString())) {
                    sb.append(",").append(addCaseSQLForKey(s, max + 1, queryParams));
                    max++;
                }
            }
        }

        //if were not using all 5 possible mappings, then we need to create the missing to help jasper report impl
        for (int i = max + 1; i <= NUM_MAPPING_KEYS; i++) {
            sb.append(", ? AS MAPPING_VALUE_").append(i);
            queryParams.add(SQL_PLACE_HOLDER);
        }
    }

    /**
     * Add a correct case statement block for the specified key, which is taking the position specified by index, in
     * the list of MAPPING_VALUE_x (x:1-5) being added to a sql select list.<br>
     * The case statement created searches for the key value in each of the mapping key indexes. When the key is found
     * then the value from the corresponding index in message_context_mapping_values is selected as the value.<br>
     * This enforces the implicit relationship between the mapping key and value columns in
     * message_context_mapping_keys and message_context_mapping_values
     * <p/>
     * Sample output for a key 'CUSTOMER' at index 2:-<br>
     * <pre>
     * CASE
     * WHEN mcmk.mapping1_key = 'CUSTOMER' THEN mcmv.mapping1_value
     * WHEN mcmk.mapping2_key = 'CUSTOMER' THEN mcmv.mapping2_value
     * WHEN mcmk.mapping3_key = 'CUSTOMER' THEN mcmv.mapping3_value
     * WHEN mcmk.mapping4_key = 'CUSTOMER' THEN mcmv.mapping4_value
     * WHEN mcmk.mapping5_key = 'CUSTOMER' THEN mcmv.mapping5_value
     * END AS MAPPING_VALUE_2
     * </pre>
     *
     * @param key         the key value to create the case statement for. The key is not validated in any way.
     * @param index       the index for the column MAPPING_VALUE_ being created. The index will be appended. If the index is 2,
     *                    then the column created is MAPPING_VALUE_2
     * @param queryParams List&lt;Object&gt; the list to add query parameters to. Values are added immediately after any ?
     *                    character is added to the string sql being build in sb, before any other ? characters are added
     * @return the String representing the complete case statement for the supplied key
     */
    private static String addCaseSQLForKey(String key, int index, List<Object> queryParams) {
        if (key == null) throw new NullPointerException("key cannot be null");
        if (key.equals("")) throw new IllegalArgumentException("key cannot be the empty string");
        if (index < 0) throw new IllegalArgumentException("index cannot be negative");

        StringBuilder sb = new StringBuilder(" CASE ");
        for (int i = 1; i <= NUM_MAPPING_KEYS; i++) {
            sb.append(" WHEN mcmk.mapping").append(i).append("_key = ? ");
            queryParams.add(key);
            sb.append(" THEN mcmv.mapping").append(i).append("_value");
        }
        sb.append(" END AS MAPPING_VALUE_").append(index);
        return sb.toString();
    }

    /**
     * Convenient method to test if a string value represents the SQL_PLACE_HOLDER value. This is predominantly indended
     * for use within actual reports, which when running through the result set for a query generated by functions in this
     * class, needs to know if it's got a real value or just a place holder. Using this function means the reports
     * don't care about what the actual SQL_PLACE_HOLDER value is.
     *
     * @param testVal String value being tested to see if its the SQL_PLACE_HOLDER value. Can be null and the empty String
     * @return true if testVal is equal to SQL_PLACE_HOLDER
     */
    public static boolean isPlaceHolderValue(String testVal) {
        return !(testVal == null || testVal.equals("")) && testVal.equals(SQL_PLACE_HOLDER);
    }

    /**
     * Called from within the Chart component of the jasper reports. See the chart definition within the jrxml files.
     * The value returned is used as the Category value. Everytime this is called, a new category is being generated
     * within the chart.<br>
     * Calls getMappingValueDisplayString to get how the authUser, keys and keyValues are displayed as a string.
     * Uses that string to then look up the group from displayStringToMappingGroup, this is the value that will be shown as
     * the category value on a chart.<br>
     * The data structure displayStringToMappingGroup is created before the report is ran. The creation of this data
     * structure must also use getMappingValueDisplayString for creating key values. This ensures that the values from
     * the report passed to this function, when turned into a key, will <em>ALWAYS</em> match a key in
     * displayStringToMappingGroup.
     * <br>
     * see Utilities.getMappingValueDisplayString()
     *
     * @param displayStringToMappingGroup the map of string values to a string group value. This string group value is
     *                                    what's used as a Category value in a chart.
     * @param authUser                    the value for AUTH_USER. May be the sql place holder value.
     * @param keysToFilters               a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                                    represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                                    key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                                    and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                                    hash map.
     * @param keyValues                   mapping key values. This <em>MUST BE OF LENGTH 5</em>, or what ever the current number of mapping
     *                                    has been updated to. Any values which do have have a valid index in keysToFilters, should be the SQL_PLACE_HOLDER
     * @return the String value to be displayed as a category in the chart used in the report output
     */
    public static String getCategoryMappingDisplayString(Map<String, String> displayStringToMappingGroup,
                                                         String authUser,
                                                         LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                         String[] keyValues) {

        String displayString = getMappingValueDisplayString(keysToFilters, authUser, keyValues, false, null);
        if (!displayStringToMappingGroup.containsKey(displayString)) throw new IllegalArgumentException("Group for " +
                "display string not found: " + displayString);

        return displayStringToMappingGroup.get(displayString);
    }

    /**
     * Used from within reports to get the value to go with 'Report Generated At:'. for the current timezone the a
     * String representing the current date and time is returned.<br>
     * <p/>
     * The format of this string is: MMM dd, yyyy HH:mm, this has been chosen so that it's clear, regardless of what
     * timezone is selected, what the date and time is, as string values are used for month. This allows us to not have
     * to deal with a setting for display date and time within reports, yet.
     *
     * @param timeZone the String id value of a timezone, which we want the current date and time for
     * @return a String representation of the current date and time in the supplied timezone
     */
    public static String getCurrentDateAndTime(String timeZone) {
        TimeZone tz = getTimeZone(timeZone);
        Calendar cal = Calendar.getInstance(tz);
        Date currentDate = cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat(REPORT_DISPLAY_DATE_STRING);
        dateFormat.setTimeZone(tz);
        return dateFormat.format(currentDate);
    }

    /**
     * Used from with reports when a service name and routing uri need to be displayed, and there is a space limitation
     * on how much data can be shown. This is currently only an issue for summary reports, as they attempt to display
     * the service name and routing uri in column 1 of tabular data.<br>
     * The service name and routing uri are both truncated in the middle using TextUtils.truncStringMiddleExact<br>
     * The returned String is HTML formatted. There is a &lt br &gt tag in between the service name and routing uri in
     * the returned String<br>
     * Both routing uri may be null or empty, service name must not be.<br>
     * <em>In the jasper report, any text element which uses this string, must be set to use HTML formatting</em>
     *
     * @param serviceName       service name to truncate
     * @param serviceRoutingURI routing uri to truncate, can be null or the emtpy string
     * @return a HTML formatted version of the truncated service name, followed by a line break, followed by the routing
     *         uri. If the servivce name and routing uri are short, they may not have been truncated at all. The font size of 1
     *         is used to ensure that the font is small, as two lines of information are going to be displayed where normally
     *         only 1 is
     * @throws NullPointerException     if service name is null
     * @throws IllegalArgumentException if service name is equal to the empty string
     */
    public static String getServiceDisplayString(String serviceName, String serviceRoutingURI) {
        if (serviceName == null) throw new NullPointerException("serviceName must be non null");
        if (serviceName.equals("")) throw new IllegalArgumentException("serviceName must not be the emtpy string");

        String serviceNameDisplay = TextUtils.truncStringMiddleExact(serviceName, 18);

        if (serviceRoutingURI == null || serviceRoutingURI.equals(""))
            return "<font size=\"1\">" + escapeHtmlCharacters(serviceNameDisplay) + "</font>";

        String displayRoutingURI = TextUtils.truncStringMiddleExact(serviceRoutingURI, 18);

        return "<font size=\"1\">" + escapeHtmlCharacters(serviceNameDisplay) + "<br>" +
                "[" + escapeHtmlCharacters(displayRoutingURI) + "]" + "</font>";
    }

    public static String escapeHtmlCharacters(String stringToEscape) {

        stringToEscape = stringToEscape.replaceAll("&", "&amp;");
        stringToEscape = stringToEscape.replaceAll("<", "&lt;");
        return stringToEscape.replaceAll(">", "&gt;");
    }

    /**
     * Used from within reports where ever a service name and routing uri are to be displayed in report output, where
     * there is room for their entire length, as their component can stretch. The returned string is in the
     * format serviceName [routingURI], if the routing uri is valid, otherwise just the service name
     *
     * @param serviceName       service name to format. Cannot be null or the empty string
     * @param serviceRoutingURI routing uri, can be null and the emtpy string
     * @return a formatted string. if the routign uri is valid: serviceName [routing uri], otherwise just the service
     *         name
     * @throws NullPointerException     if service name is null
     * @throws IllegalArgumentException if service name is equal to the empty string
     */
    public static String getServiceDisplayStringNotTruncated(String serviceName, String serviceRoutingURI) {
        if (serviceName == null) throw new NullPointerException("serviceName must be non null");
        if (serviceName.equals("")) throw new IllegalArgumentException("serviceName must not be the emtpy string");

        if (serviceRoutingURI == null || serviceRoutingURI.equals("")) return escapeHtmlCharacters(serviceName);

        return escapeHtmlCharacters(serviceName + "[" + serviceRoutingURI + "]");
    }

    public static String getServiceDisplayStringNotTruncatedNoEscape(String serviceName, String serviceRoutingURI) {
        if (serviceName == null) throw new NullPointerException("serviceName must be non null");
        if (serviceName.equals("")) throw new IllegalArgumentException("serviceName must not be the emtpy string");

        if (serviceRoutingURI == null || serviceRoutingURI.equals("")) return serviceName;

        return serviceName + "[" + serviceRoutingURI + "]";
    }

    public static String getServiceDisplayStringTruncatedNoEscape(String serviceName, String serviceRoutingURI) {
        if (serviceName == null) throw new NullPointerException("serviceName must be non null");
        if (serviceName.equals("")) throw new IllegalArgumentException("serviceName must not be the emtpy string");

        if (serviceRoutingURI == null || serviceRoutingURI.equals("")) return serviceName;

        return TextUtils.truncStringMiddleExact(serviceName, Utilities.SERVICE_DISPLAY_NAME_LENGTH) +
                "[" + TextUtils.truncStringMiddleExact(serviceRoutingURI, Utilities.ROUTING_URI_LENGTH) + "]";
    }

    public static String getServiceStringTruncatedNoEscape(String serviceName, int maxServiceNameLength) {
        if (serviceName == null) throw new NullPointerException("serviceName must be non null");
        if (serviceName.equals("")) throw new IllegalArgumentException("serviceName must not be the emtpy string");

        return TextUtils.truncStringMiddleExact(serviceName, maxServiceNameLength);
    }

    public static String getRoutingUriStringTruncatedNoEscape(String routingUri, int maxRoutingUriLength) {
        if (routingUri == null) return "";
        return TextUtils.truncStringMiddleExact(routingUri, maxRoutingUriLength);
    }

    /**
     * Get a display string representing the operation. If the operationName is too large it will be truncated in the
     * middle using TextUtils.truncStringMiddleExact.
     *
     * @param operationName operation name to truncate if too large
     * @return the truncated operation name, or the operation name unmodified, if it doesn't need truncation
     * @throws NullPointerException     if operation name is null
     * @throws IllegalArgumentException if operation name is equal to the empty string
     */
    public static String getOperationDisplayString(String operationName) {
        return getOperationDisplayString(operationName, SHORT_OPERATION_LENGTH);
    }

    public static String getOperationDisplayString(String operationName, int length) {
        if (operationName == null) throw new NullPointerException("operationName must be non null");
        if (operationName.equals("")) throw new IllegalArgumentException("operationName must not be the emtpy string");

        return escapeHtmlCharacters(TextUtils.truncStringMiddleExact(operationName, length));
    }

    /**
     * Called from within the Chart component of the jasper reports, only by performance statistics reports.
     * See the chart definition within the jrxml files.<br>
     * The value returned is used as the Category value. Everytime this is called, a new category is being generated
     * within the chart.<br>
     * Calls getServiceDisplayStringNotTruncatedNoEscape to get how the serviceName and routingURI are displayed as a string.
     * Uses that string to then look up the service identifier from displayStringToService, this is the value that will
     * be shown as the category value on a chart. e.g. Service1 or Service 1 etc...<br>
     * The data structure displayStringToService is created before the report is ran. The creation of this data
     * structure must also use getServiceDisplayStringNotTruncatedNoEscape for creating key values. This ensures that the values
     * from the report passed to this function, when turned into a key, will <em>ALWAYS</em> match a key in
     * displayStringToService.
     * <br>
     * see Utilities.getServiceDisplayStringNotTruncated()
     *
     * @param displayStringToService the map of string values to a string service identifier value. This string service
     *                               id identifier value is what's used as a Category value in a chart.
     * @param serviceName            service name, should not be null or empty string
     * @param routingURI             routing uri, can be null and empty string
     * @return the String value to be displayed as a category in the chart used in the report output
     */
    public static String getServiceFromDisplayString(Map<String, String> displayStringToService, String serviceName,
                                                     String routingURI) {

        String displayString = getServiceDisplayStringNotTruncatedNoEscape(serviceName, routingURI);
        if (!displayStringToService.containsKey(displayString)) throw new IllegalArgumentException("Service for " +
                "display string not found: " + displayString);

        return displayStringToService.get(displayString);
    }

    /**
     * Creates a display string from the supplied parameters. Any values which are the place holder are ignored.
     * The display string starts with the authenticated user, if valid, then for each key in keysToFilters it displays
     * the key and possibly it's value from keyValues
     * <p/>
     * The string at index i in keyValues, IMPLICITLY matches the key at the same index in keysToFilters. THIS IS WHY THIS
     * DATA STRUCTURE IS ALWAYS A LINKED HASH MAP, AS ORDER MUST BE MAINTAINED.
     * <p/>
     * This function is used to create a unique key string by which to identify a set of mapping values and also to
     * display the mapping values to the user in report output. As a result a prefix can be added for display purposes.
     *
     *
     * @param keysToFilters        a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                             represent it's constraints. All keys should have at least one FilterPair supplied. If no constraint was added for a
     *                             key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                             and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                             hash map.
     * @param authUser             value for the authenticated user, can be the place holder value
     * @param keyValues            array of Strings. Array is used as it's easier from with Jasper reports than using a
     *                             Collection.
     * @param includePreFix        true if the supplied prefix should be at the start of the returned string
     * @param prefix               stirng to include at the start of the returned string. If includePreFix is true, it cannot be
     *                             null or the empty string
     * @return a string representing all the supplied parameters
     * @throws IllegalArgumentException if the length of keyValues is less than the size of keys, or if truncateValues is
     *                                  true and truncateMaxSize is null or < -1
     * @throws NullPointerException     if any argument is null or empty for it's type
     * @throws IllegalStateException    if keyValues ever has the place holder value for any value from keys
     */
    public static String getMappingValueDisplayString(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                      String authUser,
                                                      String[] keyValues,
                                                      boolean includePreFix,
                                                      String prefix) {
        if (keysToFilters == null) throw new NullPointerException("keysToFilters cannot be null");

        if (authUser == null || authUser.equals(""))
            throw new NullPointerException("authUser must have a non null and non empty value. " +
                    "It can be the placeholder value");//as it always exists in select

        if (keyValues == null) {
            throw new NullPointerException("keyValues cannot be null");
        }

        if (keyValues.length != NUM_MAPPING_KEYS)
            throw new IllegalArgumentException("Length of keyValues must equal :" + NUM_MAPPING_KEYS);

        if ((authUser.equals(SQL_PLACE_HOLDER) && keysToFilters.isEmpty())
                || (authUser.equals(SQL_PLACE_HOLDER) && keysToFilters.size() == 1
                && keysToFilters.keySet().iterator().next().equals(MessageContextMapping.MappingType.AUTH_USER.toString()))) {
            throw new IllegalArgumentException("authUser must be supplied (non null, emtpy and not a placeholder) or" +
                    " some ReportApi.FilterPairs should be supplied in keysToFilters");
        }

        if (includePreFix) {
            if (prefix == null || prefix.equals(""))
                throw new IllegalArgumentException("If includePreFix is true, prefix " +
                        "cannot be null or the empty string");
        }

        final boolean truncateValues = ConfigFactory.getBooleanProperty("com.l7tech.gateway.standardreports.truncate_mappings", false);
        int truncateKeyMaxSize = ConfigFactory.getIntProperty("com.l7tech.gateway.standardreports.mapping_key_max_size", 100);
        int truncateValueMaxSize = ConfigFactory.getIntProperty("com.l7tech.gateway.standardreports.mapping_value_max_size", 20);

        if (truncateValues) {
            if (truncateKeyMaxSize < 10) {
                logger.warning("Minimum size for system property com.l7tech.gateway.standardreports.mapping_key_max_size is 10. Using value of 10.");
                truncateKeyMaxSize = 10;
            }
            if (truncateValueMaxSize < 10) {
                logger.warning("Minimum size for system property com.l7tech.gateway.standardreports.mapping_value_max_size is 10. Using value of 10.");
                truncateValueMaxSize = 10;
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean firstComma = false;
        if (!authUser.equals(SQL_PLACE_HOLDER)) {
            sb.append("Authenticated User: ");
            if (truncateValues) sb.append(TextUtils.truncStringMiddleExact(authUser, truncateValueMaxSize));
            else sb.append(authUser);
            firstComma = true;
        }

        int index = 0;
        for (Map.Entry<String, List<ReportApi.FilterPair>> me : keysToFilters.entrySet()) {
            if (me.getKey().equals(MessageContextMapping.MappingType.AUTH_USER.toString())) continue;

            if (keyValues[index].equals(SQL_PLACE_HOLDER)) {
                throw new IllegalStateException("Place holder should not be found as the value for a valid key");
            }

            if (firstComma) {
                sb.append(", ");
                firstComma = false;
            }
            if (index != 0) {
                sb.append(", ");
            }

            if (truncateValues) sb.append(TextUtils.truncStringMiddleExact(me.getKey(), truncateKeyMaxSize));
            else sb.append(me.getKey());

            sb.append(": ");

            if (truncateValues) sb.append(TextUtils.truncStringMiddleExact(keyValues[index], truncateValueMaxSize));
            else sb.append(keyValues[index]);

            index++;
        }

        if (includePreFix) {
            return prefix + escapeHtmlCharacters(sb.toString());
        }
        return sb.toString();
    }

    /**
     * From the auth user, keys, values and filter constraints get a string which displays this information for the user
     * in the report info section of a report
     *
     * @param keysToFilters all the keys to FilterPairs, which we want converted into a nice string for the user
     * @param isDetail      is used in validating the parameters, some constrains are relative to the query being a detail
     *                      query or not.  The keysToFilters cannot be validated without knowing if the report is at the operation level.
     *                      In addition, isDetail determins whether we just constrain by service id or service id and operation
     * @param isUsage       needed in order to validate the input parameters
     * @param isHtml        Returned string is new line formatted. If isHtml is true <br> is used, otherwise \n is used
     *                      also, if isHtml is true, then the output strings are HTML escaped
     * @return String for displaying in report info section of report
     */
    public static String getMappingReportInfoDisplayString(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters
            , boolean isDetail, boolean isUsage, boolean isHtml) {

        boolean keysSupplied = checkMappingQueryParams(keysToFilters, isDetail, isUsage);

        boolean useUser = (keysSupplied) && isUserSupplied(keysToFilters);

        StringBuilder sb = new StringBuilder();
        if (useUser) {
            sb.append(AUTHENTICATED_USER_DISPLAY);
        }
        if (useUser) {
            List<ReportApi.FilterPair> authUsers = keysToFilters.get(MessageContextMapping.MappingType.AUTH_USER.toString());
            StringBuilder tempBuffer = new StringBuilder();
            tempBuffer.append(": (");
            int index = 0;
            boolean valueFound = false;
            for (ReportApi.FilterPair fp : authUsers) {
                if (fp.isConstraintNotRequired()) continue;
                if (index != 0) tempBuffer.append(", ");
                tempBuffer.append((isHtml) ? escapeHtmlCharacters(fp.getDisplayValue()) : fp.getDisplayValue());
                index++;
                valueFound = true;
            }
            tempBuffer.append(")");
            if (valueFound) sb.append(tempBuffer);
            if (!isHtml) sb.append("\n");
            else sb.append("<br>");
        }

        if (keysToFilters == null || keysToFilters.isEmpty()) {
            //The only constraint on the params is that if all are null/empty, then isDetail or useUser must be true,
            //however if sb is empty here, then useUser was false, in which case it's a detail query. Show something
            //instead of nothing for this corner case.
            if (sb.toString().equals("")) {
                return onlyIsDetailDisplayText;
            } else {
                return sb.toString();
            }
        }

        for (Map.Entry<String, List<ReportApi.FilterPair>> me : keysToFilters.entrySet()) {
            if (me.getKey().equals(MessageContextMapping.MappingType.AUTH_USER.toString())) continue;

            String mappingKey = me.getKey();
            //valueConstraintAndOrLike

            String keyString = TextUtils.truncStringMiddleExact(mappingKey, MAPPING_KEY_MAX_SIZE);
            sb.append((isHtml) ? escapeHtmlCharacters(keyString) : keyString);
            StringBuilder tempBuilder = new StringBuilder();
            tempBuilder.append(" (");
            int tempIndex = 0;
            for (ReportApi.FilterPair fp : me.getValue()) {
                if (!fp.isConstraintNotRequired()) {
                    if (tempIndex != 0) tempBuilder.append(", ");
                    tempBuilder.append((isHtml) ? escapeHtmlCharacters(fp.getDisplayValue()) : fp.getDisplayValue());
                    tempIndex++;
                }
            }
            tempBuilder.append(")");
            if (tempIndex > 0) sb.append(tempBuilder);
            if (!isHtml) sb.append("\n");
            else sb.append("<br>");
        }
        return sb.toString();
    }

    public static LinkedHashMap<Integer, String> getGroupIndexToGroupString(int numGroups) {
        LinkedHashMap<Integer, String> groupIndexToGroup = new LinkedHashMap<Integer, String>();
        for (int i = 1; i <= numGroups; i++) {
            String group = "Group " + i;
            groupIndexToGroup.put(i, group);
        }
        return groupIndexToGroup;
    }

    public static LinkedHashMap<String, String> getLegendDisplayStringToGroupMap(Collection<String> mappingValuesLegend) {
        LinkedHashMap<String, String> displayStringToGroup = new LinkedHashMap<String, String>();
        int index = 1;
        for (String s : mappingValuesLegend) {
            String group = "Group " + index;
            displayStringToGroup.put(s, group);
            index++;
        }

        return displayStringToGroup;
    }


    /**
     * Called exclusively from reports and test cases
     * As this is called from reports, there are no type parameters on any of the Collection arguments
     * <p/>
     * This method escapes any HTML characters. See escapeHtmlCharacters
     *
     * @param serviceIdToOperationMap map of all service ids used in the report to the list of operations required
     * @param printOperations         this should be the result of isDetail && isContextMapping from the report's params
     * @return a String representing the supplied services and operations. If none supplied it's the empty string.
     *         If printOperations is false then no operation information is included in the returned list and the services names
     *         are not new line formatted in html. If printOperaitons is true, then the  returned string is HTML formatted with
     *         &lt;br&gt; seperating the services within the returned String
     */
    public static String getServiceAndIdDisplayString(Map serviceIdToOperationMap, Map serviceIdToNameMap, boolean printOperations) {
        if (serviceIdToNameMap == null) serviceIdToNameMap = new HashMap();

        if (!printOperations) {
            List sortedList = new ArrayList(serviceIdToNameMap.values());
            Collections.sort(sortedList);
            return getStringNamesFromCollectionEscaped(sortedList);
        }

        //This supports the case when all services are selected, not a feature yet of the reporting ui
        //when this happens the report detects the empty string and shows 'All Available'. See usages
        if (serviceIdToOperationMap == null || serviceIdToOperationMap.isEmpty()) return "";

        //this is done just for convenience in code below so it can use entrySet
        Map<String, Set<String>> sIdToOpMap = serviceIdToOperationMap;
        Map<String, String> idToDisplayString = new HashMap<String, String>();
        for (Map.Entry<String, Set<String>> me : sIdToOpMap.entrySet()) {
            String serviceName = (String) serviceIdToNameMap.get(me.getKey());
            StringBuilder sb = new StringBuilder();
            int index = 0;

            List<String> sortedList = new ArrayList<String>();
            if (me.getValue() != null) sortedList.addAll(me.getValue());
            Collections.sort(sortedList);
            for (String s : sortedList) {
                if (index != 0) sb.append(", ");
                sb.append(s);
                index++;
            }
            idToDisplayString.put(serviceName, sb.toString());
        }

        //sb.append(serviceName+": -> ");
        List<String> serviceNames = new ArrayList<String>(idToDisplayString.keySet());
        Collections.sort(serviceNames);

        StringBuilder sb = new StringBuilder();
        int rowIndex = 0;
        int maxRows = sIdToOpMap.size();
        for (String s : serviceNames) {
            sb.append(escapeHtmlCharacters(TextUtils.truncStringMiddleExact(s, Utilities.SERVICE_DISPLAY_NAME_LENGTH)));
            String operations = idToDisplayString.get(s);
            if (!operations.equals("")) {
                sb.append(" -> ").append(escapeHtmlCharacters(TextUtils.truncStringMiddleExact(operations, OPERATION_STRING_MAX_SIZE)));
            } else {
                //reports can handle a detail query with no ops supplied, implying all are selected
                //not a feature yet in reporting UI. This handles that case
                sb.append(" -> All");
            }
            if (rowIndex < maxRows - 1) sb.append("<br>");
            rowIndex++;
        }

        return sb.toString();
    }


    private static final int[] hexColours = new int[]{0xFFDC5A, 0xD6D6D6, 0xE8EDB4};

    /**
     * Some class implementing JRChartCustomizer will call this method from customize() in order to find out what
     * color the chart series should be. The chart can't access configuration, so it's assumed for the moment that
     * this class can. It specifies how many series colours it wants, and it should get back a list of strings, each
     * representing a unique html color code
     *
     * @param howMany how many unique colours are required. 3 is the maximum
     * @return a List&lt;Color&gt; of colors to use
     */
    public static List<Color> getSeriesColours(int howMany) {
        if (howMany < 1 || howMany > 3) throw new IllegalArgumentException("howMany must be >= 1 and <= 3");
        List<Color> colours = new ArrayList<Color>();
        for (int i = 0; i < hexColours.length; i++) {
            int l = hexColours[i];
            Color c = new Color(l);
            colours.add(c);
        }
        return colours;
    }

    public static String truncateUsageGroupHeading(String valuesToTruncate) {
        final String[] split = valuesToTruncate.split("\n");
        final Collection<String> stringsToTrunc = new ArrayList<String>();
        for (String s : split) {
            stringsToTrunc.add(TextUtils.truncStringMiddleExact(s, Utilities.USAGE_HEADING_VALUE_MAX_SIZE));
        }
        return TextUtils.join("\n", stringsToTrunc).toString();
    }
}

