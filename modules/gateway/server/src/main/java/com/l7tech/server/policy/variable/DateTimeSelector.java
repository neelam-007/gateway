package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.DateUtils;
import com.l7tech.util.ExceptionUtils;
import org.jboss.util.collection.ConcurrentSet;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * Created via reflection in ExpandVariables
 *
 * @author darmstrong
 */
public class DateTimeSelector implements ExpandVariables.Selector<Date> {
    private final static Set<String> timezones = new ConcurrentSet<String>(Arrays.asList("UTC", "local"));
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

            if (name.contains(".")) {
                // in this case we have a timezone.format syntax
                final int periodIndex = name.indexOf(".");
                final String timeZone = name.substring(0, periodIndex).toLowerCase();
                final String format = name.substring(periodIndex + 1);
                return new Selection(DateUtils.getFormattedString(context, timeZone, format));
            }

            //name can now only represent either a timezone or a timezone + format

            if (DateUtils.isSupportedTimezoneDesignator(name)) {
                return new Selection(DateUtils.getFormattedString(context, name));
            }
            // it must be a format
            return new Selection(DateUtils.getFormattedString(context, "UTC", name));
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

}
