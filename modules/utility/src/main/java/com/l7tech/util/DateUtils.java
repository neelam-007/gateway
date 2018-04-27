/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utilities related to date handling.
 */
public class DateUtils {

    // - PUBLIC

    public static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String RFC1123_DEFAULT_PATTERN = "E, dd MM yyyy hh:mm:ss Z";
    public static final String RFC850_DEFAULT_PATTERN = "EEEE, dd-MM-yy hh:mm:ss Z";
    public static final String ASCTIME_DEFAULT_PATTERN = "E MMM d hh:mm:ss yyyy";

    private static final String LOCAL_TZ = "local";
    private static final String UTC_TZ = "utc";

    private DateUtils() {
        // empty
    }

    /**
     * Compare the specified date to today and produce a relative date phrase like "Yesterday" or "3 years from now".
     *
     * @param date the date to compare with today, or null to return "Never"
     * @param skipIfPassed if true, return the empty string for dates that have already passed
     * @return a phrase like "Yesterday", "Never", "33 years and 1 day from now", "9 days ago".  Never null.
     */
    public static String makeRelativeDateMessage(Date date, boolean skipIfPassed) {
        if (date == null)
            return "Never";

        long now = System.currentTimeMillis();
        long then = date.getTime();
        long days = (then - now) / 1000 / 60 / 60 / 24;

        if (days < 0 && skipIfPassed)
            return "";

        if (Math.abs(days) < 10) {
            switch ((int)days) {
                case -1:
                    return "Yesterday";
                case 0:
                    return "Today";
                case 1:
                    return "Tomorrow";
                default:
            }
        }

        String ago = days < 0 ? " ago" : " from now";
        days = Math.abs(days);

        String yearsStr = "";
        if (days > 365) {
            long years = days / 365;
            days -= (years * 365);
            yearsStr = TextUtils.plural(years, "year") + " and ";
        }
        return yearsStr + TextUtils.plural(days, "day") + ago;
    }

    /**
     * Get the default formatting of a date string on the Gateway. This is backwards compatible with the Gateway's
     * gateway.time and request.time variables, which defaulted to ISO8601 with a fixed 'Z' string.
     *
     * @param date Date to format. Never null
     * @return Zulu (UTC) formatted String. Never null
     */
    @NotNull
    public static String getZuluFormattedString(@NotNull final Date date) {
        try {
            return getFormattedString(date, getTimeZone(UTC_TZ), null);
        } catch (InvalidPatternException e) {
            // can't happen - coding error
            throw new UnsupportedDateOperationException(e);
        }
    }

    /**
     * Get the default formatting of a date string on the Gateway. Uses the default timezone.
     *
     * @param date Date to format. Never null
     * @return Date formatted String in the default timezone. Never null
     */
    @NotNull
    public static String getDefaultTimeZoneFormattedString(@NotNull final Date date) {
        try {
            return getFormattedString(date, TimeZone.getDefault(), null);
        } catch (InvalidPatternException e) {
            // can't happen - coding error
            throw new UnsupportedDateOperationException(e);
        }
    }

    /**
     * Format a Date
     *
     * @param date Date to format
     * @param timeZone the TimeZone to use when formatting the date. Required.
     * @param format Format to use. If null then the default ISO8601 format is used.
     * @return formatted string
     * @throws InvalidPatternException if the format is invalid
     */
    @NotNull
    public static String getFormattedString(@NotNull final Date date,
                                            @NotNull final TimeZone timeZone,
                                            @Nullable final String format)
            throws InvalidPatternException {

        final SimpleDateFormat dateFormat;
        try {
            dateFormat = new SimpleDateFormat(format == null ? ISO8601_PATTERN : format);
        } catch (IllegalArgumentException e) {
            throw new InvalidPatternException(ExceptionUtils.getMessage(e));
        }
        dateFormat.setTimeZone(timeZone);

        return dateFormat.format(date);
    }

    @NotNull
    public static TimeZone getZuluTimeZone() {
        return TimeZone.getTimeZone("UTC");
    }

    @Nullable
    public static TimeZone getTimeZone(@NotNull String tzd) {

        TimeZone returnZone = null;
        final boolean isGatewayTimeZone = gatewayTimeZones.contains(tzd.toLowerCase());
        if (isGatewayTimeZone) {
            switch(tzd.toLowerCase()) {
                case UTC_TZ:
                    returnZone = TimeZone.getTimeZone(UTC_TZ.toUpperCase());
                    break;
                case LOCAL_TZ:
                    returnZone = TimeZone.getDefault();
                    break;
                default:
                    returnZone = null;
            }
        } else if (lowerToActualTimeZones.containsKey(tzd.toLowerCase())) {
            // it is a built in timezone
            returnZone = TimeZone.getTimeZone(lowerToActualTimeZones.get(tzd.toLowerCase()));
        } else if (numericTimeZonePattern.matcher(tzd).matches()) {
            final String sign = tzd.substring(0, 1);
            final int hours = Integer.parseInt(tzd.substring(1, 3));
            final int minutes = (tzd.length() > 3) ? Integer.parseInt((tzd.contains(":")) ? tzd.substring(4, 6) : tzd.substring(3, 5)) : 0;

            final int rawOffset = (sign.charAt(0) == '-' ? -1 : 1) * ((hours * TimeUnit.HOURS.getMultiplier()) + (minutes * TimeUnit.MINUTES.getMultiplier()));

            final TimeZone timeZone = TimeZone.getTimeZone("GMT" + tzd);
            // tzd may not be valid. If this is the case, then TimeZone will simply return GMT.
            // make sure the raw offset requested was received.
            if (timeZone.getRawOffset() == rawOffset) {
                returnZone = timeZone;
            } else {
                returnZone = null;
            }
        }

        return returnZone;
    }

    public static class InvalidPatternException extends Exception{
        InvalidPatternException(String message) {
            super(message);
        }
    }

    public static class UnsupportedDateOperationException extends RuntimeException {
        UnsupportedDateOperationException(Exception e) {
            super(e);
        }
    }

    @TestOnly
    public static Map<String, String> getLowerToActualTimeZones() {
        return lowerToActualTimeZones;
    }

    // - PRIVATE

    private static final Set<String> gatewayTimeZones = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(UTC_TZ, LOCAL_TZ)));

    private static final Map<String, String> lowerToActualTimeZones = Arrays.stream(TimeZone.getAvailableIDs()).collect(Collectors.toMap(String::toLowerCase, Function.identity()));

    /**
     * Match either + or - 0000 or 00:00 or 00
     */
    private static final Pattern numericTimeZonePattern = Pattern.compile("(?:\\+|-)\\d\\d(?:\\:?\\d\\d)?");
}
