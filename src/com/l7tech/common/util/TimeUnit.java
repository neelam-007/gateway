/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.util;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.Map;
import java.util.HashMap;

/**
 * @author alex
 */
public final class TimeUnit implements Serializable, Comparable {
    private static int next = 0;
    public static final TimeUnit MILLIS = new TimeUnit(next++, "milliseconds", "ms", 1);
    public static final TimeUnit SECONDS = new TimeUnit(next++, "seconds", "s", 1000);
    public static final TimeUnit MINUTES = new TimeUnit(next++, "minutes", "m", 1000 * 60);
    public static final TimeUnit HOURS = new TimeUnit(next++, "hours", "h", 1000 * 60 * 60);

    public static final TimeUnit[] ALL = new TimeUnit[] { MILLIS, SECONDS, MINUTES, HOURS };

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
}
