/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utilities related to date handling.
 */
public class DateUtils {

    // - PUBLIC

    public static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
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
            return getFormattedString(date, getTimeZone("utc"), null);
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
     * @param timeZone if not null, the TimeZone to use when formatting th edate
     * @param format Format to use. If null then the default ISO8601 format is used.
     * @return formatted string
     * @throws UnknownTimeZoneException if timezone is not supported
     * @throws InvalidPatternException if the format is invalid
     */
    @NotNull
    public static String getFormattedString(@NotNull final Date date,
                                            @Nullable final TimeZone timeZone,
                                            @Nullable final String format)
            throws UnknownTimeZoneException, InvalidPatternException {

        final SimpleDateFormat dateFormat;
        try {
            dateFormat = new SimpleDateFormat(format == null ? ISO8601_PATTERN : format);
        } catch (IllegalArgumentException e) {
            throw new InvalidPatternException(ExceptionUtils.getMessage(e));
        }
        dateFormat.setLenient(false);
        if (timeZone != null) {
            dateFormat.setTimeZone(timeZone);
        }

        return dateFormat.format(date);
    }

    @Nullable
    public static TimeZone getTimeZone(@NotNull String tzd) {

        TimeZone returnZone = null;
        final boolean isGatewayTimeZone = gatewayTimeZones.contains(tzd.toLowerCase());
        if (isGatewayTimeZone) {
            if (tzd.equalsIgnoreCase("utc")) {
                returnZone =  TimeZone.getTimeZone("UTC");
            } else if (tzd.equalsIgnoreCase("local")) {
                returnZone = TimeZone.getDefault();
            }
        } else if (validTimezones.contains(tzd)) {
            // is it a built in timezone
            returnZone = TimeZone.getTimeZone(tzd);
        } else if (numericTimeZonePattern.matcher(tzd).matches()) {
            final String sign = tzd.substring(0, 1);
            final int hours = Integer.valueOf(tzd.substring(1, 3));
            final int minutes = (tzd.length() > 3) ? Integer.valueOf((tzd.contains(":")) ? tzd.substring(4, 6) : tzd.substring(3, 5)) : 0;

            final int rawOffset = (sign.charAt(0) == '-' ? -1 : 1) * ((hours * TimeUnit.HOURS.getMultiplier()) + (minutes * TimeUnit.MINUTES.getMultiplier()));
            // note we cannot 'pick' a timezone using the rawOffset. It is likely that TimeZone.getAvailableId(offset) will
            // find a timezone, but that timezone may be in a day light savings period, in which case using it may cause a date
            // with a different offset from GMT to be produced and may not match the offset requested.
            returnZone = TimeZone.getDefault();
            // this is lenient - we could verify that at least one timezone exists for this offset, this is more flexible.
            returnZone.setRawOffset(rawOffset);
        }

        return returnZone;
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

    private final static Set<String> gatewayTimeZones = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("utc", "local")));

    private final static Set<String> validTimezones =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(TimeZone.getAvailableIDs())));

    /**
     * Match either + or - 0000 or 00:00 or 00
     */
    private final static Pattern numericTimeZonePattern = Pattern.compile("(?:\\+|-)\\d\\d(?:\\:?\\d\\d)?");
}
