/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.util.ISO8601Date;
import com.l7tech.policy.variable.BuiltinVariables;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

class TimeVariableUtils {
    static final String UTCDOT = BuiltinVariables.TIMESUFFIX_ZONE_UTC + ".";
    static final String LOCALDOT = BuiltinVariables.TIMESUFFIX_ZONE_LOCAL + ".";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    static Object getTimeValue( final String prefix, final String name, final LazyLong lazyTime) {
        String suffix = name.substring(prefix.length());
        if (suffix.startsWith(".")) suffix = suffix.substring(1);
        String format, zone;
        if (suffix.length() > 0) {
            String lsuf = suffix.toLowerCase();
            if (BuiltinVariables.TIMESUFFIX_ZONE_UTC.equals(lsuf) || BuiltinVariables.TIMESUFFIX_ZONE_LOCAL.equals(lsuf)) {
                zone = lsuf;
                format = BuiltinVariables.TIMESUFFIX_FORMAT_ISO8601;
            } else if (lsuf.startsWith(UTCDOT) || lsuf.startsWith(LOCALDOT)) {
                int ppos = lsuf.indexOf(".");
                zone = lsuf.substring(0,ppos);
                format = suffix.substring(ppos+1);
                if (format.length() == 0) format = BuiltinVariables.TIMESUFFIX_FORMAT_ISO8601;
            } else {
                zone = BuiltinVariables.TIMESUFFIX_ZONE_UTC;
                format = suffix;
            }
        } else {
            zone = BuiltinVariables.TIMESUFFIX_ZONE_UTC;
            format = BuiltinVariables.TIMESUFFIX_FORMAT_ISO8601;
        }

        Date date = new Date(lazyTime.get());
        if (BuiltinVariables.TIMESUFFIX_FORMAT_ISO8601.equalsIgnoreCase(format)) {
            if (BuiltinVariables.TIMESUFFIX_ZONE_UTC.equalsIgnoreCase(zone)) {
                return ISO8601Date.format(date);
            } else {
                TimeZone tz = ("local".equals(zone)) ? TimeZone.getDefault() : UTC;
                return ISO8601Date.format(date, -1, tz);
            }
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            if ( BuiltinVariables.TIMESUFFIX_ZONE_UTC.equalsIgnoreCase(zone) ) {
                sdf.setTimeZone( UTC );                
            }
            return sdf.format(date);
        }
    }

    interface LazyLong {
        long get();
    }
}
