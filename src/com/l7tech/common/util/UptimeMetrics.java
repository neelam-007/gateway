/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import org.apache.log4j.Category;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Class holding information gathered by UptimeMonitor.
 * User: mike
 * Date: Sep 17, 2003
 * Time: 10:04:47 AM
 */
public class UptimeMetrics {
    private static final Category log = Category.getInstance(UptimeMetrics.class);
    private static Pattern findDaysHoursMinutes = Pattern.compile("^.*? up (\\d+) days?\\, (\\d+)\\:(\\d+)\\,.*$", Pattern.DOTALL);
    private static Pattern findDays = Pattern.compile("^.*? (\\d+) day.*$", Pattern.DOTALL);
    private static Pattern findHours = Pattern.compile("^.*? (\\d+) hr.*$", Pattern.DOTALL);
    private static Pattern findMinutes = Pattern.compile("^.*? (\\d+) min.*$", Pattern.DOTALL);
    private static Pattern findLoads = Pattern.compile("^.*?\\s+load averages?: ([0-9.]+), ([0-9.]+), ([0-9.]+).*$", Pattern.DOTALL);

    private final String rawUptimeOutput;
    private final long timestamp;
    private final int days;
    private final int hours;
    private final int minutes;
    private final double load1;
    private final double load2;
    private final double load3;

    UptimeMetrics(String rawUptimeOutput, long timestamp, int days, int hours, int minutes, double load1, double load2, double load3) {
        this.rawUptimeOutput = rawUptimeOutput;
        this.timestamp = timestamp;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.load1 = load1;
        this.load2 = load2;
        this.load3 = load3;
    }

    /**
     * Parse the result from running uptime and extract the interesting information.
     * <pre>
     * Example uptime formats:
     *   Red Hat 9       | 11:22:20  up 28 days, 18:57,  1 user,  load average: 0.00, 0.00, 0.00
     *   FreeBSD 4.8     |11:36AM  up 10 days, 14 hrs, 2 users, load averages: 0.00, 0.00, 0.00
     *   SunOS 5.9       |10:47am  up 27 day(s), 50 mins,  1 user,	load average: 0.18, 0.26, 0.20
     *   cygwin          | 11:47:39 up 5 days, 18:40,  0 users,  load average: 0.00, 0.00, 0.00
     * </pre>
     * @param rawUptimeOutput the uptime output to examine
     * @param timestamp the date and time this output was gathered, as from System.currentTimeMillis()
     */
    public UptimeMetrics(String rawUptimeOutput, long timestamp) {
        this.rawUptimeOutput = rawUptimeOutput;
        this.timestamp = timestamp;
        UptimeMetrics g = parseUptimeOutput(rawUptimeOutput);
        this.days = g.days;
        this.hours = g.hours;
        this.minutes = g.minutes;
        this.load1 = g.load1;
        this.load2 = g.load2;
        this.load3 = g.load3;
    }

    /**
     * Parse the result from running uptime and extract the interesting information.
     * This assumes the uptime output was just gathered, and so uses the current time for the timestamp.
     * <pre>
     * Example uptime formats:
     *   Red Hat 9       | 11:22:20  up 28 days, 18:57,  1 user,  load average: 0.00, 0.00, 0.00
     *   FreeBSD 4.8     |11:36AM  up 10 days, 14 hrs, 2 users, load averages: 0.00, 0.00, 0.00
     *   SunOS 5.9       |10:47am  up 27 day(s), 50 mins,  1 user,	load average: 0.18, 0.26, 0.20
     *   cygwin          | 11:47:39 up 5 days, 18:40,  0 users,  load average: 0.00, 0.00, 0.00
     * </pre>
     * @param rawUptimeOutput the uptime output to examine
     */
    public UptimeMetrics(String rawUptimeOutput) {
        this(rawUptimeOutput, System.currentTimeMillis());
    }

    /**
     * Parse the result of running uptime, and produce an UptimeMetrics object.
     * <pre>
     * Example uptime formats:
     *   Red Hat 9       | 11:22:20  up 28 days, 18:57,  1 user,  load average: 0.00, 0.00, 0.00
     *   FreeBSD 4.8     |11:36AM  up 10 days, 14 hrs, 2 users, load averages: 0.00, 0.00, 0.00
     *   SunOS 5.9       |10:47am  up 27 day(s), 50 mins,  1 user,	load average: 0.18, 0.26, 0.20
     *   cygwin          | 11:47:39 up 5 days, 18:40,  0 users,  load average: 0.00, 0.00, 0.00
     * </pre>
     *
     * @param result  The string to parse, ie " 10:12:24 up 5 days, 17:05,  0 users,  load average: 0.00, 0.00, 0.00\n"
     * @return  An UptimeMetrics instance containing the pertinent information from this string.
     */
    private static UptimeMetrics parseUptimeOutput(String result) {
        long now = System.currentTimeMillis();

        int days = 0;
        int hours = 0;
        int minutes = 0;
        Matcher matchDaysHoursMinutes = findDaysHoursMinutes.matcher(result);
        if (matchDaysHoursMinutes.matches()) {
            days = strToInt(matchDaysHoursMinutes.group(1));
            hours = strToInt(matchDaysHoursMinutes.group(2));
            minutes = strToInt(matchDaysHoursMinutes.group(3));
        } else {
            Matcher matchDays = findDays.matcher(result);
            if (matchDays.matches())
                days = strToInt(matchDays.group(1));
            Matcher matchHours = findHours.matcher(result);
            if (matchHours.matches())
                hours = strToInt(matchHours.group(1));
            Matcher matchMinutes = findMinutes.matcher(result);
            if (matchMinutes.matches())
                minutes = strToInt(matchMinutes.group(1));
        }

        double load1 = 0.0;
        double load2 = 0.0;
        double load3 = 0.0;
        Matcher matchLoads = findLoads.matcher(result);
        if (matchLoads.matches()) {
            load1 = strToDouble(matchLoads.group(1));
            load2 = strToDouble(matchLoads.group(2));
            load3 = strToDouble(matchLoads.group(3));
        }

        UptimeMetrics um = new UptimeMetrics(result, now, days, hours, minutes, load1, load2, load3);
        return um;
    }

    private static int strToInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double strToDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Return a string form of this UptimeMetrics object, suitable for use in a log.
     * @return
     */
    public String toString() {
        return "UptimeMetrics: " + timestamp + ": " + rawUptimeOutput;
    }

    /**
     * Get the raw output from the uptime executable.  This method may return anything at all,
     * including null, even if the other methods yield useful data.  It is included only as a
     * diagnostic aid.
     *
     * @return  The raw output string from uptime, if available.
     */
    public String getRawUptimeOutput() {
        return rawUptimeOutput;
    }

    /**
     * Return an indication of whether this data is "stale."  Currently, data will be considered
     * stale if it is over one minute old.  The comparison is against the current time whenever
     * this method is called -- this means that even though we have no mutators, isStale() may
     * return false and later true on a given instance of UptimeMetrics.
     *
     * @return True if this data is too old; false otherwise.
     */
    public boolean isStale() {
        return System.currentTimeMillis() - timestamp > 1000 * 60;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getDays() {
        return days;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public double getLoad1() {
        return load1;
    }

    public double getLoad2() {
        return load2;
    }

    public double getLoad3() {
        return load3;
    }
}
