package com.l7tech.util;

import org.apache.commons.lang.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.l7tech.util.Functions.map;

/**
 * Stores configuration of date formats and provides a utility method to parse a date time / timestamp string based
 * on the beans configuration.
 */
public class DateTimeConfigUtils {

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
                returnDate = getTimestampFromString(dateTimeString);
                if (returnDate == null) {
                    throw new UnknownDateFormatException("Invalid timestamp: " + dateTimeString);
                }
            } else {
                throw new UnknownDateFormatException("Unknown date format: " + dateTimeString);
            }
        } else {
            final SimpleDateFormat dateFormat;
            try {
                dateFormat = new SimpleDateFormat(formatToUse);
                // we want strict behavior to avoid mis interpreting a date value
                dateFormat.setLenient(false);
            } catch (IllegalArgumentException e) {
                //invalid simple date format
                throw new InvalidDateFormatException("Invalid format: " + formatToUse);
            }

            returnDate = dateFormat.parse(dateTimeString);
        }

        return returnDate;
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
     * Get all configured simple date formats. This combines the custom formats with the auto formats. Intended for
     * display to user.
     *
     * @return List of all configured date formats. List contains all configured custom formats, followed by all
     * defined auto formats. Never null. May be empty.
     */
    @NotNull
    public List<String> getConfiguredDateFormats(){
        final LinkedHashSet<String> orderedSet = new LinkedHashSet<String>(customDateFormatsRef.get());
        orderedSet.addAll(map(autoFormatsRef.get(), new Functions.Unary<String, Pair<String, Pattern>>() {
            @Override
            public String call(Pair<String, Pattern> o) {
                return o.left;
            }
        }));
        return CollectionUtils.toList(orderedSet);
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

    // - PRIVATE
    private AtomicReference<List<String>> customDateFormatsRef =
            new AtomicReference<List<String>>(new ArrayList<String>());

    private AtomicReference<List<Pair<String, Pattern>>> autoFormatsRef =
            new AtomicReference<List<Pair<String, Pattern>>>(new ArrayList<Pair<String, Pattern>>());

    private boolean isTimestamp(final String timeString) {
        final int length = timeString.length();
        return (length == 10 || length == 13) && NumberUtils.isDigits(timeString);
    }

    /**
     * Get a Date from a string which may represent a timestamp.
     *
     * @param timeStamp If timeStamp is a sequence of 10 or 13 digits, then it will be interpreted as being
     * a long value of either seconds or milliseconds from epoch.
     * @return Date if the timeStamp represented a timestamp which could be interpreted, otherwise null.
     */
    @Nullable
    private Date getTimestampFromString(final String timeStamp) {
        Date returnDate = null;
        final Calendar calendar = Calendar.getInstance();
        try {
            boolean validTimestamp = true;
            final Long maybeTimestamp = Long.valueOf(timeStamp);
            if (timeStamp.length() == 13) {
                calendar.setTimeInMillis(maybeTimestamp);
            } else if (timeStamp.length() == 10) {
                // should not throw
                calendar.setTimeInMillis(Long.valueOf(timeStamp + "000"));
            } else {
                validTimestamp = false;
            }
            if (validTimestamp) {
                returnDate = calendar.getTime();
            }
        } catch (NumberFormatException e) {
            // not a timestamp
        }

        return returnDate;
    }
}
