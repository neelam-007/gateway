package com.l7tech.util;

import org.apache.commons.lang.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.l7tech.util.Functions.map;

/**
 * Stores configuration of date formats and provides a utility method to parse a date time / timestamp string based
 * on the beans configuration.
 */
public class DateTimeConfigUtils {

    public static final String TIMESTAMP = "<Timestamp>";
    public static final String MILLISECOND_TIMESTAMP = "<Millisecond Timestamp>";
    public static final String SECOND_TIMESTAMP = "<Second Timestamp>";

    /**
     * Parse a String into a Date object. The String may represent a string formatted date string or it may be a string
     * representation of a second or millisecond timestamp.
     *
     * Timestamps can always be parsed however strings can only be parsed if there is a matching pair set via
     * {@link DateTimeConfigUtils#setAutoDateFormats(java.util.List)}
     *
     * Thread safe.
     *
     * @param dateTimeString Input string which may represent a formatted date string or a timestamp (10 or 13 digits)
     * @return Date value configured to the date / time represented by dateTimeString.
     * @throws java.text.ParseException If the format supplied / selected used to parse dateTimeString is not able to.
     * @throws UnknownDateFormatException if a format cannot automatically be found to match the dateTimeString.
     * @throws InvalidDateFormatException if the supplied format is invalid.
     */
    @NotNull
    public Date parseDateFromString(@NotNull final String dateTimeString)
            throws ParseException, UnknownDateFormatException, InvalidDateFormatException {
        String formatToUse = null;
        // find format
        final List<Pair<String, Pattern>> autoDateFormats = getAutoDateFormats();
        for (Pair<String, Pattern> autoPair : autoDateFormats) {
            if (autoPair.right.matcher(dateTimeString).matches()) {
                formatToUse = autoPair.left;
                break;
            }
        }

        final Date returnDate;
        if (formatToUse == null) {
            // now it's safe to check if it's a timestamp as no format was matched
            if (isTimestamp(dateTimeString)) {
                returnDate = parseTimestamp(dateTimeString);
            } else {
                throw new UnknownDateFormatException("Unknown date format: '" + dateTimeString + "'");
            }
        } else {
            final SimpleDateFormat dateFormat;
            try {
                dateFormat = new SimpleDateFormat(formatToUse);
                // this will be overwritten when the string being parsed contains timezone information
                dateFormat.setTimeZone(DateUtils.getZuluTimeZone());
                // we want strict behavior to avoid mis interpreting a date value
                final boolean lenient;
                if (config == null) {
                    lenient = ConfigFactory.getBooleanProperty("com.l7tech.util.lenientDateFormat", false);
                } else {
                    lenient = config.getBooleanProperty("com.l7tech.util.lenientDateFormat", false);
                }
                dateFormat.setLenient(lenient);
            } catch (IllegalArgumentException e) {
                //invalid simple date format
                throw new InvalidDateFormatException("Invalid format: " + formatToUse);
            }

            returnDate = dateFormat.parse(dateTimeString);
        }

        return returnDate;
    }

    /**
     * Parse a timestamp. May be either milliseconds or seconds.
     * @param timeStamp If timeStamp is a sequence of 10 or 13 digits, then it will be interpreted as being
     * a long value of either seconds or milliseconds from epoch.
     * @return parsed Date
     * @throws UnknownDateFormatException if format is not a valid timestamp
     */
    @NotNull
    public static Date parseTimestamp(final String timeStamp) throws UnknownDateFormatException {
        return parseTimestamp(timeStamp, null);
    }

    /**
     * Parse a timestamp. Must be milliseconds.
     * @param timeStamp input timestamp string which must tbe of 13 digits.
     * @return parsed Date
     * @throws UnknownDateFormatException if format is not a valid millisecond timestamp
     */
    @NotNull
    public static Date parseMilliTimestamp(final String timeStamp) throws UnknownDateFormatException {
        return parseTimestamp(timeStamp, true);
    }

    /**
     * Parse a timestamp. Must be seconds.
     * @param timeStamp input timestamp string which must tbe of 10 digits.
     * @return parsed Date
     * @throws UnknownDateFormatException if format is not a valid second timestamp
     */
    @NotNull
    public static Date parseSecondTimestamp(final String timeStamp) throws UnknownDateFormatException {
        return parseTimestamp(timeStamp, false);
    }

    /**
     * True if the format string is for timestamps.
     *
     * @param format String format
     * @return true if format is one of {@link #TIMESTAMP}, {@link #MILLISECOND_TIMESTAMP} or {@link #SECOND_TIMESTAMP}
     */
    public static boolean isTimestampFormat(final String format) {
        return format.equalsIgnoreCase(TIMESTAMP) ||
                format.equalsIgnoreCase(MILLISECOND_TIMESTAMP) ||
                format.equalsIgnoreCase(SECOND_TIMESTAMP);
    }

    /**
     * Parse a timestamp according to a format. Only formats for which {@link #isTimestampFormat(String)} returns true
     * can be parsed.
     *
     * @param timestampFormat format to use.
     * @param input timestamp string
     * @return date. Never null
     * @throws UnknownDateFormatException
     */
    @NotNull
    public static Date parseForTimestamp(final String timestampFormat, final String input) throws UnknownDateFormatException {

        if (!isTimestampFormat(timestampFormat)) {
            throw new UnknownDateFormatException("Unknown timestamp format: " + timestampFormat);
        }

        final Date date;
        if (timestampFormat.equalsIgnoreCase(TIMESTAMP)) {
            date = parseTimestamp(input);
        } else if (timestampFormat.equalsIgnoreCase(MILLISECOND_TIMESTAMP)) {
            date = parseMilliTimestamp(input);
        } else {
            date = parseSecondTimestamp(input);
        }

        return date;
    }

    /**
     * Set the configured custom date formats.
     *
     * @param customFormats All custom date formats.
     */
    public void setCustomDateFormats(@NotNull List<String> customFormats) {
        customDateFormatsRef.set(Collections.unmodifiableList(customFormats));
    }

    /**
     * Set the list of date formats this class can parse.
     *
     * @param autoDateFormats list of paris of simple date format to the strictly matching pattern.
     */
    public void setAutoDateFormats(@NotNull List<Pair<String, Pattern>> autoDateFormats) {
        autoFormatsRef.set(Collections.unmodifiableList(autoDateFormats));
    }

    /**
     * Get all configured simple date formats. These are the values configured in the custom formats cluster property.
     *
     * @return List of all configured date formats. List contains all configured custom formats. Never null. May be empty.
     */
    @NotNull
    public List<String> getConfiguredDateFormats(){
        return CollectionUtils.toList(customDateFormatsRef.get());
    }

    /**
     * This will be used at runtime.
     *
     * @return All auto formats date formats. Never null. May be empty.
     */
    @NotNull
    public List<Pair<String, Pattern>> getAutoDateFormats() {
        return autoFormatsRef.get();
    }

    public static class UnknownDateFormatException extends Exception{
        public UnknownDateFormatException(String message) {
            super(message);
        }
    }

    public static class InvalidDateFormatException extends Exception {
        public InvalidDateFormatException(String message) {
            super(message);
        }
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    // - PRIVATE
    private AtomicReference<List<String>> customDateFormatsRef =
            new AtomicReference<List<String>>(new ArrayList<String>());

    private AtomicReference<List<Pair<String, Pattern>>> autoFormatsRef =
            new AtomicReference<List<Pair<String, Pattern>>>(new ArrayList<Pair<String, Pattern>>());

    private Config config;

    private static boolean isTimestamp(final String timeString) {
        final int length = timeString.length();
        return (length == 10 || length == 13) && NumberUtils.isDigits(timeString);
    }

    /**
     * Parse either a millisecond (13 digits) or second (10 digits) timestamp.
     *
     * @param timeStamp String representation of a timestamp
     * @param millis if null, then either seconds or milliseconds is supported. If true the only milliseconds is
     * expected and if false then only seconds is supported.
     * @return parsed date, never null.
     * @throws UnknownDateFormatException if the timestamp is not valid or does not match requirements from millis parameter.
     */
    @NotNull
    private static Date parseTimestamp(final String timeStamp, @Nullable final Boolean millis) throws UnknownDateFormatException {
        final boolean isMillis = millis == null || millis;
        final boolean isSeconds = millis == null || !millis;

        final String typeMsg = (isMillis && isSeconds) ? "" : (isMillis) ? " millisecond" : " second";
        final String exceptionMsg = "Invalid" + typeMsg + " timestamp: '" + timeStamp + "'";

        if (!isTimestamp(timeStamp)) {
            throw new UnknownDateFormatException(exceptionMsg);
        }

        final Date returnDate;
        final Calendar calendar = Calendar.getInstance();
        try {
            final Long maybeTimestamp = Long.valueOf(timeStamp);
            if (timeStamp.length() == 13 && isMillis) {
                calendar.setTimeInMillis(maybeTimestamp);
            } else if (timeStamp.length() == 10 && isSeconds) {
                // should not throw
                calendar.setTimeInMillis(Long.valueOf(timeStamp + "000"));
            } else {
                throw new UnknownDateFormatException(exceptionMsg);
            }
            returnDate = calendar.getTime();
        } catch (NumberFormatException e) {
            throw new UnknownDateFormatException(exceptionMsg);
        }

        return returnDate;
    }
}
