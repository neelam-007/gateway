package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.DateUtils;
import com.l7tech.util.ExceptionUtils;

import java.util.*;

/**
 * Created via reflection in ExpandVariables
 *
 * @author darmstrong
 */
public class DateTimeSelector implements ExpandVariables.Selector<Date> {

    @Override
    public Selection select(String contextName, Date context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {

        String errorMsg;
        try {
            if (SUFFIX_MILLIS.equalsIgnoreCase(name)) {
                return new Selection(context.getTime());
            }

            if (SUFFIX_SECONDS.equalsIgnoreCase(name)) {
                return new Selection(context.getTime() / 1000L);
            }

            final TimeZone selectedTimeZone = DateUtils.getTimeZone(name);
            if (selectedTimeZone != null) {
                return new Selection(DateUtils.getFormattedString(context, selectedTimeZone, DateUtils.ISO8601_PATTERN));
            }

            // is it a built in suffix?
            if (builtInSuffixFormats.keySet().contains(name.toLowerCase())) {
                return new Selection(DateUtils.getFormattedString(context, DateUtils.getZuluTimeZone(), builtInSuffixFormats.get(name.toLowerCase())));
            }

            // it's not a timezone
            // perhaps it's a timezone + format
            if (name.contains(".")) {
                // The '.' character is valid in a pattern so it may mean either a suffix separator or a pattern character
                final int periodIndex = name.indexOf(".");
                String maybeTimezone = name.substring(0, periodIndex);
                final TimeZone isTzd = DateUtils.getTimeZone(maybeTimezone);
                if (isTzd != null) {
                    // the rest is a pattern
                    return new Selection(DateUtils.getFormattedString(context, isTzd, getPattern(name.substring(periodIndex + 1))));
                } else {
                    // it's a pattern
                    return new Selection(DateUtils.getFormattedString(context, DateUtils.getZuluTimeZone(), getPattern(name)));
                }
            } else {
                // it's a pattern - already checked if it's a timezone
                return new Selection(DateUtils.getFormattedString(context, DateUtils.getZuluTimeZone(), getPattern(name)));
            }

        } catch (DateUtils.UnknownTimeZoneException e) {
            errorMsg = ExceptionUtils.getMessage(e);
        } catch (DateUtils.InvalidPatternException e) {
            errorMsg = ExceptionUtils.getMessage(e);
        }

        String msg = handler.handleBadVariable(name + " for date time." + ((errorMsg.isEmpty())?"": " " + errorMsg));
        if (strict) throw new IllegalArgumentException(msg);
        return NOT_PRESENT;
    }

    @Override
    public Class<Date> getContextObjectClass() {
        return Date.class;
    }

    // - PRIVATE

    private static final String SUFFIX_MILLIS = "millis";
    private static final String SUFFIX_SECONDS = "seconds";
    private static final String SUFFIX_FORMAT_ISO8601 = "iso8601";
    private static final String SUFFIX_FORMAT_RFC1123 = "rfc1123";
    private static final String SUFFIX_FORMAT_RFC850 = "rfc850";
    private static final String SUFFIX_FORMAT_ASCTIME = "asctime";

    // - PACKAGE

    /**
     * All keys are in lower case
     */
    final static Map<String, String> builtInSuffixFormats = CollectionUtils.<String, String>mapBuilder().
            put(SUFFIX_FORMAT_ISO8601, DateUtils.ISO8601_PATTERN).
            put(SUFFIX_FORMAT_RFC1123, DateUtils.RFC1123_DEFAULT_PATTERN).
            put(SUFFIX_FORMAT_RFC850, DateUtils.RFC850_DEFAULT_PATTERN).
            put(SUFFIX_FORMAT_ASCTIME, DateUtils.ASCTIME_DEFAULT_PATTERN).
            unmodifiableMap();

    /**
     * Get a pattern for the input.
     *
     * See bug 12557. Not validating pattern here as historically invalid formats have been supported.
     *
     * @param input pattern or pattern suffix
     * @return pattern string to use
     */
    String getPattern(final String input) {
        if (builtInSuffixFormats.keySet().contains(input.toLowerCase())) {
            return builtInSuffixFormats.get(input.toLowerCase());
        } else {
            return input;
        }
    }

}
