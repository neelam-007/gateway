/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utilities related to date handling.
 */
public class DateUtils {

    // - PUBLIC

    public static final String ISO8601_DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    public static final String RFC1123_DEFAULT_PATTERN = "E, dd MM yyyy hh:mm:ss Z";
    public static final String RFC850_DEFAULT_PATTERN = "EEEE, dd-MM-yy hh:mm:ss Z";
    public static final String ASCTIME_DEFAULT_PATTERN = "E MMM d hh:mm:ss yyyy";

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
            return getFormattedString(date, null, null);
        } catch (UnknownTimeZoneException e) {
            // can't happen - coding error
            throw new RuntimeException(e);
        } catch (InvalidPatternException e) {
            // can't happen - coding error
            throw new RuntimeException(e);
        }
    }

    /**
     * Format a Date
     *
     * @param date Date to format
     * @param timeZoneIdentifier time zone identifier. If null then UTC is used. Only values for which {@link DateUtils#isSupportedTimezoneDesignator(String)} returns
     * true can be used.
     * @param format Format to use. If null then the default ISO8601 format is used.
     * @return formatted string
     * @throws UnknownTimeZoneException if timezone is not supported
     * @throws InvalidPatternException if the format is invalid
     */
    @NotNull
    public static String getFormattedString(@NotNull final Date date,
                                            @Nullable final String timeZoneIdentifier,
                                            @Nullable final String format)
            throws UnknownTimeZoneException, InvalidPatternException {

        final SimpleDateFormat dateFormat;
        try {
            dateFormat = new SimpleDateFormat(format == null ? ISO8601_DEFAULT_PATTERN : format);
        } catch (IllegalArgumentException e) {
            throw new InvalidPatternException(ExceptionUtils.getMessage(e));
        }
        dateFormat.setLenient(false);
        if (timeZoneIdentifier == null || "utc".equalsIgnoreCase(timeZoneIdentifier)) {
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else if (!"local".equalsIgnoreCase(timeZoneIdentifier)) {
            throw new UnknownTimeZoneException("Unknown timezone: " + timeZoneIdentifier);
        }

        return dateFormat.format(date);
    }

    public static boolean isSupportedTimezoneDesignator(@NotNull final String tzd) {
        return timeZoneDesignators.contains(tzd);
    }

    public static class UnknownTimeZoneException extends Exception{
        public UnknownTimeZoneException(String message) {
            super(message);
        }
    }

    public static class InvalidPatternException extends Exception{
        public InvalidPatternException(String message) {
            super(message);
        }
    }

    // - PRIVATE

    private final static Set<String> timeZoneDesignators = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("utc", "local")));
}
