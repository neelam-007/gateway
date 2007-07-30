/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.util;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author alex
 */
public final class TimeUnit implements Serializable, Comparable {
    private static int next = 0;
    public static final TimeUnit MILLIS = new TimeUnit(next++, "milliseconds", "ms", 1);
    public static final TimeUnit SECONDS = new TimeUnit(next++, "seconds", "s", 1000);
    public static final TimeUnit MINUTES = new TimeUnit(next++, "minutes", "m", 1000 * 60);
    public static final TimeUnit HOURS = new TimeUnit(next++, "hours", "h", 1000 * 60 * 60);
    public static final TimeUnit DAYS = new TimeUnit(next++, "days", "d", 1000 * 60 * 60 * 24);

    public static final TimeUnit[] ALL = new TimeUnit[] { MILLIS, SECONDS, MINUTES, HOURS, DAYS };

    private final int num;
    private final String name;
    private final String abbreviation;
    private final int multiplier;

    private static final Map valuesByAbbrev = new HashMap();
    static {
        for (int i = 0; i < ALL.length; i++) {
            TimeUnit timeUnit = ALL[i];
            valuesByAbbrev.put(timeUnit.getAbbreviation(), timeUnit);
        }
    }

    private TimeUnit(int num, String name, String abbrev, int multiplier) {
        this.num = num;
        this.name = name;
        this.abbreviation = abbrev;
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public long toMillis(long value) {
        return value * multiplier;
    }

    public String toString() {
        return name;
    }

    public int compareTo(Object o) {
        TimeUnit that = (TimeUnit)o;
        return this.multiplier - that.multiplier;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    protected Object readResolve() throws ObjectStreamException {
        return ALL[num];
    }

    public static TimeUnit fromAbbreviation(String value) {
        return (TimeUnit)valuesByAbbrev.get(value);
    }

    private static final Pattern numberPattern = Pattern.compile("(-?\\d*\\.?\\d*)(\\p{Lower})*");

    /**
     * Parses a time duration expressed as a number (which may contain periods, commas or spaces) followed by a one- or
     * two-letter, case-insensitive unit abbreviation, e.g. "60s" is 60 seconds, resulting in 60000.
     * @param value the string to be parsed
     * @param unsuffixedUnit the TimeUnit to assume is in use when no unit suffix is present
     * @return the equivalent duration in milliseconds
     */
    public static long parse(String value, TimeUnit unsuffixedUnit) throws NumberFormatException {
        if (value == null) throw new NullPointerException();
        if (value.length() == 0) throw new NumberFormatException("Empty strings are not supported");
        if (value.length() > 20) throw new NumberFormatException("Strings with more than 20 characters are not supported");

        Matcher mat = numberPattern.matcher(value.toLowerCase().replace(",", "").replace(" ", ""));
        if (!mat.matches() && mat.groupCount() != 2)
            throw new NumberFormatException("Value doesn't match expected format");

        final String snum = mat.group(1);

        final TimeUnit unit;
        String maybeUnit = mat.group(2);
        TimeUnit tu = (TimeUnit) valuesByAbbrev.get(maybeUnit);
        if (tu == null) tu = unsuffixedUnit;
        unit = tu;

        BigDecimal bd = new BigDecimal(snum); // OK to throw NFE
        return (long) (bd.doubleValue() * unit.multiplier);
    }

    public static long parse(String value) {
        return parse(value, MILLIS);
    }

    // This method is invoked reflectively by WspEnumTypeMapping
    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public String objectToString(Object target) {
                return ((TimeUnit)target).getAbbreviation();
            }

            public Object stringToObject(String value) throws IllegalArgumentException {
                TimeUnit tu = TimeUnit.fromAbbreviation(value);
                if (tu == null) throw new IllegalArgumentException("Unknown TimeUnit abbreviation: '" + value + "'");
                return tu;
            }
        };
    }
}
