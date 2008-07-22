package com.l7tech.server.wsdm.util;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.StringTokenizer;

/**
 * ISO8601Duration similar to ISO8601Date but for durations
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 8, 2008<br/>
 */
public class ISO8601Duration {
    private int plusMinus;
    private double years;
    private double months;
    private double days;
    private double hours;
    private double minutes;
    private double seconds;

    /**
     * Instatiates a Duration from an ISO 8601 format duration string
     * @param isodur the iso8601 formatted duration string
     * @throws  ParseException if the input does not follow the standard syntax
     */
    public ISO8601Duration(String isodur) throws ParseException {
        boolean isTime = false;
        String value;
        String delim;
        plusMinus = 1;
        years = -1.0;
        months = -1.0;
        days = -1.0;
        hours = -1.0;
        minutes = -1.0;
        seconds = -1.0;
        // DURATION FORMAT IS: (-)PnYnMnDTnHnMnS
        StringTokenizer st = new StringTokenizer(isodur, "-PYMDTHS", true);
        try {
            value = st.nextToken();
            if (value.equals("-")) {
                plusMinus = -1;
                value = st.nextToken();
            }
            // DURATION MUST START WITH A "P"
            if (!value.equals("P"))
                throw new ParseException(isodur + " : " + value + " : no P deliminator for duration", 0);
            // GET NEXT FIELD
            while (st.hasMoreTokens()) {
                // VALUE
                value = st.nextToken();
                if (value.equals("T")) {
                    if (!st.hasMoreTokens())
                        throw new ParseException(isodur + " : " + value + ": no values after duration T delimitor", 0);
                    value = st.nextToken();
                    isTime = true;
                }
                // DELIMINATOR
                if (!st.hasMoreTokens())
                    throw new ParseException(isodur + " : " + value + "No deliminator for duration", 0);
                delim = st.nextToken();
                // YEAR
                if (delim.equals("Y")) {
                    years = Double.parseDouble(value);
                }
                // MONTH
                else if (delim.equals("M") && !isTime) {
                    months = Double.parseDouble(value);
                    if (months != (double) ((int) months))
                        throw new ParseException("Cannot process decimal months!", 0);
                }
                // DAYS
                else if (delim.equals("D")) {
                    days = Double.parseDouble(value);
                }
                // HOURS
                else if (delim.equals("H")) {
                    hours = Double.parseDouble(value);
                    isTime = true;
                }
                // MINUTES
                else if (delim.equals("M") && isTime) {
                    minutes = Double.parseDouble(value);
                }
                // SECONDS
                else if (delim.equals("S")) {
                    seconds = Double.parseDouble(value);
                } else {
                    throw new ParseException(isodur + ": what duration delimiter is " + delim + "?", 0);
                }
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("[" + ex.getMessage() + "] is not valid", 0);
        }
    }

    public long inSeconds() {
        double output = 0.0;
        if (years > 0) output += years * 365.254 * 3600 * 24;
        if (months > 0) output += months * 30.43 * 3600 * 24;
        if (days > 0) output += days * 3600 * 24;
        if (hours > 0) output += hours * 3600;
        if (minutes > 0) output += minutes * 60;
        if (seconds > 0) output += seconds;
        return new Double(output * plusMinus).longValue();
    }

    public static String durationFromSecs(long secs) {
        if (secs <= 60) {
            return "PT" + secs + "S";
        } else if (secs <= 3600) {
            long minutes = secs/60;
            long seconds = secs%60;
            return "PT" + minutes + "M" + seconds + "S";
        } else if (secs <= 86400) {
            long hrs = secs/3600;
            long tmp = secs%3600;
            long minutes = tmp/60;
            long seconds = tmp%60;
            return "PT" + hrs + "H" + minutes + "M" + seconds + "S";
        } else {
            long days = secs/86400;
            long tmp = secs%86400;
            long hrs = tmp/3600;
            tmp = tmp%3600;
            long minutes = tmp/60;
            long seconds = tmp%60;
            return "P" + days + "DT" + hrs + "H" + minutes + "M" + seconds + "S";
        }
    }

    /**
     * Add Duration to a Calendar
     *
     * @param cal the cal to add the duration to
     * @return modified Calendar
     * @throws java.io.IOException if problem
     */
    public Calendar addTo(Calendar cal) throws IOException {
        int iyears, imonths, idays, ihours, imins, isecs, millis;

        if (years > 0) {
            iyears = (int) years;
            //imonths = 0;            // AVOID USING MONTHS
            idays = (int) (365.254 * (years
                    - iyears));
            ihours = (int) (24.0 * 365.254 * (years
                    - iyears
                    - idays / 365.254));
            imins = (int) (60.0 * 24.0 * 365.254 * (years
                    - iyears
                    - idays / 365.254
                    - ihours / 24.0 / 365.254));
            isecs = (int) (60.0 * 60.0 * 24.0 * 365.254 * (years
                    - iyears
                    - idays / 365.254
                    - ihours / 24.0 / 365.254
                    - imins / 60.0 / 24.0 / 365.254));
            millis = (int) (1000.0 * 60.0 * 60.0 * 24.0 * 365.254 * (years
                    - iyears
                    - idays / 365.254
                    - ihours / 24.0 / 365.254
                    - imins / 60.0 / 24.0 / 365.254
                    - isecs / 60.0 / 60.0 / 24.0 / 365.254));
            cal.add(Calendar.YEAR, plusMinus * iyears);
            cal.add(Calendar.DAY_OF_YEAR, plusMinus * idays);
            cal.add(Calendar.HOUR, plusMinus * ihours);
            cal.add(Calendar.MINUTE, plusMinus * imins);
            cal.add(Calendar.SECOND, plusMinus * isecs);
            cal.add(Calendar.MILLISECOND, plusMinus * millis);
        }

        if (months > 0) {
            imonths = (int) months;
            if (months != (double) imonths)
                throw new IOException(
                        "Cannot add decimal months!");
            cal.add(Calendar.MONTH, plusMinus * imonths);
        }

        if (days > 0) {
            idays = (int) days;
            ihours = (int) (24.0 * (days
                    - idays));
            imins = (int) (60.0 * 24.0 * (days
                    - idays
                    - ihours / 24.0));
            isecs = (int) (60.0 * 60.0 * 24.0 * (days
                    - idays
                    - ihours / 24.0
                    - imins / 24.0 / 60.0));
            millis = (int) (1000.0 * 60.0 * 60.0 * 24.0 * (days
                    - idays
                    - ihours / 24.0
                    - imins / 60.0 / 24.0
                    - isecs / 60.0 / 60.0 / 24.0));
            cal.add(Calendar.DAY_OF_YEAR, plusMinus * idays);
            cal.add(Calendar.HOUR, plusMinus * ihours);
            cal.add(Calendar.MINUTE, plusMinus * imins);
            cal.add(Calendar.SECOND, plusMinus * isecs);
            cal.add(Calendar.MILLISECOND, plusMinus * millis);
        }

        if (hours > 0) {
            ihours = (int) hours;
            imins = (int) (60.0 * (hours
                    - ihours));
            isecs = (int) (60.0 * 60.0 * (hours
                    - ihours
                    - imins / 60.0));
            millis = (int) (1000.0 * 60.0 * 60.0 * (hours
                    - ihours
                    - imins / 60.0
                    - isecs / 60.0 / 60.0));
            cal.add(Calendar.HOUR, plusMinus * ihours);
            cal.add(Calendar.MINUTE, plusMinus * imins);
            cal.add(Calendar.SECOND, plusMinus * isecs);
            cal.add(Calendar.MILLISECOND, plusMinus * millis);
        }

        if (minutes > 0) {
            imins = (int) minutes;
            isecs = (int) (60.0 * (minutes
                    - imins));
            millis = (int) (1000.0 * 60.0 * (minutes
                    - imins
                    - isecs / 60.0));
            cal.add(Calendar.MINUTE, plusMinus * imins);
            cal.add(Calendar.SECOND, plusMinus * isecs);
            cal.add(Calendar.MILLISECOND, plusMinus * millis);
        }

        if (seconds > 0) {
            isecs = (int) seconds;
            millis = (int) (1000.0 * (seconds
                    - isecs));
            cal.add(Calendar.SECOND, plusMinus * isecs);
            cal.add(Calendar.MILLISECOND, plusMinus * millis);
        }
        return cal;
    }

    /**
     * Generate a string representation of an ISO 8601 duration
     *
     * @return a string representing the duration in the ISO 8601 format
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        // OPTIONAL SIGN

        if (plusMinus == -1)
            buffer.append("-");

        // REQUIRED "P"

        buffer.append("P");

        if (years > 0) {
            if (years == (double) ((int) years))
                buffer.append((int) years);
            else
                buffer.append(years);
            buffer.append("Y");
        }
        if (months > 0) {
            if (months == (double) ((int) months))
                buffer.append((int) months);
            else
                buffer.append(months);
            buffer.append("M");
        }
        if (days > 0) {
            if (days == (double) ((int) days))
                buffer.append((int) days);
            else
                buffer.append(days);
            buffer.append("D");
        }

        // DATE-TIME SEPARATOR (IF NEEDED)

        if ((years > 0 || months > 0 || days > 0) &&
                (hours > 0 || minutes > 0 || seconds > 0))
            buffer.append("T");


        if (hours > 0) {
            if (hours == (double) ((int) hours))
                buffer.append((int) hours);
            else
                buffer.append(hours);
            buffer.append("H");
        }
        if (minutes > 0) {
            if (minutes == (double) ((int) minutes))
                buffer.append((int) minutes);
            else
                buffer.append(minutes);
            buffer.append("M");
        }
        if (seconds > 0) {
            buffer.append(seconds);
            buffer.append("S");
        }

        return buffer.toString();
    }
}
