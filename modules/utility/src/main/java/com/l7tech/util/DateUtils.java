/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities related to date handling.
 */
public class DateUtils {

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

    private static final Pattern DATE_WITH_TIMEZONE = Pattern.compile("[A-Za-z0-9-,:\\.\\s]+-(\\d{2}:\\d{2})$");
    private static String fixTimeZone(String timeString) {
        Matcher m = DATE_WITH_TIMEZONE.matcher(timeString);
        if(m.find() && m.groupCount() > 0){
            String correctTimeZone = m.group(1).replace(":", "");//remove ':' from timezone portion of the time string
            return timeString.substring(0,m.start(1)) + correctTimeZone;
        }
        return timeString;
    }

    public static Date parseDateTime(String timeString, String format) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date dateTime = null;
        if(null != timeString) {
            dateTime = dateFormat.parse(fixTimeZone(timeString));
        }
        return dateTime;
    }


    public static int compareDate(Date d1, Date d2) {
        if(d1 == null) {
            if(d2 == null)
                return 0;
            else
                return -d2.compareTo(new Date(Long.MIN_VALUE));
        } else if(d2 == null)
            return d1.compareTo(new Date(Long.MIN_VALUE));

        return d1.compareTo(d2);
    }

}
