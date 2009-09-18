// ISO8601Date.java
// $Id$
// (c) COPYRIGHT MIT, INRIA and Keio, 2000.
// Please first read the full copyright statement in file COPYRIGHT.html
package com.l7tech.util;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Date parser for ISO 8601 format 
 * http://www.w3.org/TR/1998/NOTE-datetime-19980827
 * @author  Benoit Mahe (bmahe@w3.org)
 */
public class ISO8601Date {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static boolean check(StringTokenizer st, String token) throws ParseException {
        try {
            if (st.nextToken().equals(token)) {
                return true;
            } else {
                throw new ParseException("Missing ["+token+"]", 0);
            }
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    private static Calendar getCalendar(String isodate) throws ParseException {
        // YYYY-MM-DDThh:mm:ss.sTZD
        StringTokenizer st = new StringTokenizer(isodate, "-T:.+Z", true);

        Calendar calendar = new GregorianCalendar(UTC);
        calendar.clear();
        try {
            // Year
            if (st.hasMoreTokens()) {
                int year = Integer.parseInt(st.nextToken());
                calendar.set(Calendar.YEAR, year);
            } else {
                return calendar;
            }
            // Month
            if (check(st, "-") && (st.hasMoreTokens())) {
                int month = Integer.parseInt(st.nextToken()) -1;
                calendar.set(Calendar.MONTH, month);
            } else {
                return calendar;
            }
            // Day
            if (check(st, "-") && (st.hasMoreTokens())) {
                int day = Integer.parseInt(st.nextToken());
                calendar.set(Calendar.DAY_OF_MONTH, day);
            } else {
                return calendar;
            }
            // Hour
            if (check(st, "T") && (st.hasMoreTokens())) {
                int hour = Integer.parseInt(st.nextToken());
                calendar.set(Calendar.HOUR_OF_DAY, hour);
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar;
            }
            // Minutes
            if (check(st, ":") && (st.hasMoreTokens())) {
                int minutes = Integer.parseInt(st.nextToken());
                calendar.set(Calendar.MINUTE, minutes);
            } else {
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar;
            }

            //
            // Not mandatory now
            //

            // Secondes
            if (! st.hasMoreTokens()) {
                return calendar;
            }
            String tok = st.nextToken();
            if (tok.equals(":")) { // secondes
                if (st.hasMoreTokens()) {
                    int secondes = Integer.parseInt(st.nextToken());
                    calendar.set(Calendar.SECOND, secondes);
                    if (! st.hasMoreTokens()) {
                        return calendar;
                    }
                    // frac sec
                    tok = st.nextToken();
                    if (tok.equals(".")) {
                        // bug fixed, thx to Martin Bottcher
                        String nt = st.nextToken();
                        while(nt.length() < 3) {
                            nt += "0";
                        }
                        if (nt.length() > 3) assertIsNumbers(nt.substring(3));
                        nt = nt.substring( 0, 3 ); //Cut trailing chars..
                        int millisec = Integer.parseInt(nt);
                        //int millisec = Integer.parseInt(st.nextToken()) * 10;
                        calendar.set(Calendar.MILLISECOND, millisec);
                        if (! st.hasMoreTokens()) {
                            return calendar;
                        }
                        tok = st.nextToken();
                    } else {
                        calendar.set(Calendar.MILLISECOND, 0);
                    }
                } else {
                    throw new ParseException("No secondes specified", 0);
                }
            } else {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }
            // Timezone
            if (! tok.equals("Z")) { // UTC
                if (! (tok.equals("+") || tok.equals("-"))) {
                    throw new ParseException("only Z, + or - allowed", 0);
                }
                boolean plus = tok.equals("+");
                if (! st.hasMoreTokens()) {
                    throw new ParseException("Missing hour field", 0);
                }
                int tzhour = Integer.parseInt(st.nextToken());
                int tzmin  = 0;
                if (check(st, ":") && (st.hasMoreTokens())) {
                    tzmin = Integer.parseInt(st.nextToken());
                } else {
                    throw new ParseException("Missing minute field", 0);
                }
                if (plus) {
                    calendar.add(Calendar.HOUR, tzhour);
                    calendar.add(Calendar.MINUTE, tzmin);
                } else {
                    calendar.add(Calendar.HOUR, -tzhour);
                    calendar.add(Calendar.MINUTE, -tzmin);
                }
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("["+ex.getMessage()+
                                     "] is not an integer", 0);
        }
        if (st.hasMoreTokens()) throw new ParseException("Contains extra material after timezone", 0);
        return calendar;
    }

    private static final Pattern MATCH_NOT_NUMBER = Pattern.compile("[^0-9]");
    private static void assertIsNumbers(String s) throws ParseException {
        if (MATCH_NOT_NUMBER.matcher(s).find())
            throw new ParseException("contains non-numeric characters before timezone", 0);
    }

    /**
     * Parse the given string in ISO 8601 format and build a Date object.
     * @param isodate the date in ISO 8601 format
     * @return a Date instance
     * @exception ParseException if the date is not valid
     */
    public static Date parse(String isodate) throws ParseException
    {
        Calendar calendar = getCalendar(isodate);
        return calendar.getTime();
    }

    private static String twoDigit(int i) {
        if (i >=0 && i < 10) {
            return "0"+String.valueOf(i);
        }
        return String.valueOf(i);
    }

    private static String threeDigit(int i) {
        if (i >=0 && i < 10)
            return "00" + String.valueOf(i);
        if (i >=10 && i < 100)
            return "0" + String.valueOf(i);
        return String.valueOf(i);
    }

    public static String format(Date date) {
        return format(date, true, -1, UTC);
    }

    public static String format(Date date, long nanos) {
        return format(date, true, nanos, UTC);
    }

    public static String format(Date date, boolean millis, long nanos) {
        return format(date, millis, nanos, UTC);
    }

    /**
     * Generate an ISO 8601 date
     *
     * @param date a Date instance
     * @param nanos nanoseconds to include, or -1 to use only millisecond-granular timestamp.
     *              Will be taken modulo 1000000.
     * @return a string representing the date in the ISO 8601 format
     */
    public static String format( final Date date, final long nanos, final TimeZone tz ) {
        return format( date, true, nanos, tz );
    }

    /**
     * Generate an ISO 8601 date
     *
     * @param date a Date instance
     * @param millis true to include milliseconds
     * @param nanos nanoseconds to include, or -1 to use only millisecond-granular timestamp.
     *              Will be taken modulo 1000000.
     * @return a string representing the date in the ISO 8601 format
     */
    public static String format( final Date date, final boolean millis, final long nanos, final TimeZone tz) {
        Calendar calendar = new GregorianCalendar(tz);
        calendar.setTime(date);
        StringBuffer buffer = new StringBuffer();
        buffer.append(calendar.get(Calendar.YEAR));
        buffer.append("-");
        buffer.append(twoDigit(calendar.get(Calendar.MONTH) + 1));
        buffer.append("-");
        buffer.append(twoDigit(calendar.get(Calendar.DAY_OF_MONTH)));
        buffer.append("T");
        buffer.append(twoDigit(calendar.get(Calendar.HOUR_OF_DAY)));
        buffer.append(":");
        buffer.append(twoDigit(calendar.get(Calendar.MINUTE)));
        buffer.append(":");
        buffer.append(twoDigit(calendar.get(Calendar.SECOND)));
        if ( millis ) {
            buffer.append(".");
            buffer.append(threeDigit(calendar.get(Calendar.MILLISECOND)));
            if (nanos >= 0) {
                buffer.append(threeDigit((int)((nanos % 1000000L) / 1000L)));
                buffer.append(threeDigit((int)(nanos % 1000L)));
            }
        }
        if (tz == UTC)
            buffer.append("Z");
        else {
            int offsec = tz.getOffset(date.getTime()) / 1000;
            int h = offsec / 3600;
            int m = offsec - (h * 3600);
            String hours = Integer.toString(Math.abs(h));
            String minutes = Integer.toString(Math.abs(m));

            buffer.append(offsec < 0 ? "-" : "+");
            if (hours.length() == 1) buffer.append("0");
            buffer.append(hours);
            buffer.append(":");
            if (minutes.length() == 1) buffer.append("0");
            buffer.append(minutes);
        }
        return buffer.toString();
    }
}
