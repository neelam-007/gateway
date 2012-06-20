package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.DateUtils;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.TimeZone;

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

            @Nullable
            final String maybeFormat;
            @Nullable
            final TimeZone timeZone;
            if (name.contains(".")) {
                final int periodIndex = name.indexOf(".");
                String maybeTimezone = name.substring(0, periodIndex);
                final TimeZone isTzd = DateUtils.getTimeZone(maybeTimezone);
                if (isTzd != null) {
                    maybeFormat = name.substring(periodIndex + 1);
                    timeZone = isTzd;
                } else {
                    maybeFormat = name;
                    timeZone = DateUtils.getTimeZone("UTC");
                }
            } else {
                final TimeZone maybeTimeZone = DateUtils.getTimeZone(name);
                if (maybeTimeZone != null) {
                    maybeFormat = null;
                    timeZone = maybeTimeZone;
                } else {
                    maybeFormat = name;
                    timeZone = DateUtils.getTimeZone("UTC");
                }
            }

            final String format;
            if (SUFFIX_FORMAT_ISO8601.equalsIgnoreCase(maybeFormat)) {
                format = DateUtils.ISO8601_PATTERN;
            } else if (SUFFIX_FORMAT_RFC1123.equalsIgnoreCase(maybeFormat)) {
                format = DateUtils.RFC1123_DEFAULT_PATTERN;
            } else if (SUFFIX_FORMAT_RFC850.equalsIgnoreCase(maybeFormat)) {
                format = DateUtils.RFC850_DEFAULT_PATTERN;
            } else if (SUFFIX_FORMAT_ASCTIME.equalsIgnoreCase(maybeFormat)) {
                format = DateUtils.ASCTIME_DEFAULT_PATTERN;
            } else {
                format = maybeFormat;
            }

            return new Selection(DateUtils.getFormattedString(context, timeZone, format));
        } catch (DateUtils.UnknownTimeZoneException e) {
            errorMsg = ExceptionUtils.getMessage(e);
        } catch (DateUtils.InvalidPatternException e) {
            errorMsg = ExceptionUtils.getMessage(e);
        }

        String msg = handler.handleBadVariable(name + " for date time. " + errorMsg);
        if (strict) throw new IllegalArgumentException(msg);
        return NOT_PRESENT;
    }

    @Override
    public Class<Date> getContextObjectClass() {
        return Date.class;
    }

    private static final String SUFFIX_MILLIS = "millis";
    private static final String SUFFIX_SECONDS = "seconds";
    private static final String SUFFIX_FORMAT_ISO8601 = "iso8601";
    private static final String SUFFIX_FORMAT_RFC1123 = "rfc1123";
    private static final String SUFFIX_FORMAT_RFC850 = "rfc850";
    private static final String SUFFIX_FORMAT_ASCTIME = "asctime";

}
