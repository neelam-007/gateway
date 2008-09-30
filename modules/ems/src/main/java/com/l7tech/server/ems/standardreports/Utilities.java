/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 29, 2008
 * Time: 11:21:44 AM
 * Utility functions used by the implementation of standard reports with Jasper reporting engine
 */
package com.l7tech.server.ems.standardreports;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;


public class Utilities {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private static final SimpleDateFormat HOUR_DATE_FORMAT = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat DAY_HOUR_DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");
    private static final SimpleDateFormat DAY_DATE_FORMAT = new SimpleDateFormat("E MM/dd");
    private static final SimpleDateFormat DAY_MONTH_DATE_FORMAT = new SimpleDateFormat("M E MM/dd");
    private static final SimpleDateFormat WEEK_DATE_FORMAT = new SimpleDateFormat("MM/dd");
    private static final SimpleDateFormat WEEK_YEAR_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat MONTH_DATE_FORMAT = new SimpleDateFormat("yyyy MMM");

    public static final String HOUR = "HOUR";
    public static final String DAY = "DAY";
    public static final String WEEK = "WEEK";
    public static final String MONTH = "MONTH";

    /**
     * Relative time is calculated to a fixed point of time depending on the unit of time supplied. For day, week
     * and month 00:00 of the current day is used (end of time period is not exclusive) minus the unitOfTime x
     * numberOfUnits. The start of the time period is inclusive.
     * week = 7 days, month = 30 days
     * E.g. if the current date is (yyyy-mm-dd hh:mm) 2008-09-22 17:48 and the unitOfTime is HOUR and the numerOfUnits
     * is 1, then the relative calculation will be done from 2008-09-22 17:00 (x) and the number of milliseconds
     * returned will be (x - (numberOfUnits x unitOfTime)) - epoch. In this example if ou convert the returned long
     * into a date, it would report it's time as 2008-09-22 16:00
     * For day, week and month the time used for the relative calculation is 00:00 of the current day.
     * In calculations a week = 7 days.
     * Month will use whole months, not including any time from the current month.
     * @param numberOfUnits How many unitOfTime to use
     * @param unitOfTime valid values are HOUR, DAY, WEEK and MONTH
     * @return
     */
    public static long getRelativeMilliSecondsInPast(int numberOfUnits, String unitOfTime){
        Calendar calendar = getCalendarForTimeUnit(unitOfTime);
        long calTime = calendar.getTimeInMillis();

        if(!unitOfTime.equals(MONTH)){
            long unitMilliSeconds = getMilliSecondsForTimeUnit(unitOfTime);
            long totalNumMilliSecs = numberOfUnits * unitMilliSeconds;
            return calTime - totalNumMilliSecs;
        }

        Calendar monthCal = Calendar.getInstance();
        monthCal.setTimeInMillis(calTime);
        monthCal.add(Calendar.MONTH, numberOfUnits * -1);
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        return monthCal.getTimeInMillis();
    }

    /**
     * Get the date string representation of a time value in milliseconds
     * @param timeMilliSeconds the number of milliseconds since epoch
     * @return a date in the format yyyy-MM-dd HH:mm
     */
    public static String getMilliSecondAsStringDate(Long timeMilliSeconds){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMilliSeconds);
        return DATE_FORMAT.format(cal.getTime());
    }

    public static String getMilliSecondAsHourDate(Long timeMilliSeconds){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMilliSeconds);
        return HOUR_DATE_FORMAT.format(cal.getTime());
    }

    /**
     * Get the date to display on a report. The timeMilliSecond value since epoch will be converted into a suitable
     * format to use in the report as the interval information.
     * When an interval crosses a major time boundary for the intervalUnitOfTime supplied and if the timeMillilSeconds
     * represents the start of the interval, based on startofInterval being true, then the string returned will be
     * modified to highlight to the user the crossing of the time boundary
     * E.g. across midnight when the interval unit of time is 1 hour
     * @param startIntervalMilliSeconds milli second value since epoch
     * @param intervalUnitOfTime HOUR, DAY, WEEK or MONTH
     * @return
     */
    public static String getIntervalDisplayDate(Long startIntervalMilliSeconds, Long endIntervalMilliSeconds,
                                        String intervalUnitOfTime){
        Calendar calStart = Calendar.getInstance();
        calStart.setTimeInMillis(startIntervalMilliSeconds);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTimeInMillis(endIntervalMilliSeconds);


        if(intervalUnitOfTime.equals(HOUR)){
            return DAY_HOUR_DATE_FORMAT.format(calStart.getTime()) + " - " +
                        HOUR_DATE_FORMAT.format(calEnd.getTime());
        }else if(intervalUnitOfTime.equals(DAY)){
            if(calStart.get(Calendar.MONTH) == Calendar.JANUARY){
                return DAY_MONTH_DATE_FORMAT.format(calStart.getTime());
            }
            return DAY_DATE_FORMAT.format(calStart.getTime());
        }else if(intervalUnitOfTime.equals(WEEK)){
            if(calStart.get(Calendar.MONTH) == Calendar.JANUARY){
                return WEEK_YEAR_DATE_FORMAT.format(calStart.getTime())+ " - " +
                        WEEK_DATE_FORMAT.format(calEnd.getTime());
            }
            return WEEK_DATE_FORMAT.format(calStart.getTime())+ " - " +
                        WEEK_DATE_FORMAT.format(calEnd.getTime());
        }else if(intervalUnitOfTime.equals(MONTH)){
            return MONTH_DATE_FORMAT.format(calStart.getTime());
        }
        return null;
    }

    /**
     * Get the number of milliseconds representing the end of the period represtented by the unitOfTime
     * For Hour this is the end of the previous hour, for Day, Week and Month it's 00:00 today
     * @param unitOfTime
     * @return
     */
    public static long getMillisForEndTimePeriod(String unitOfTime){
        return Utilities.getCalendarForTimeUnit(unitOfTime).getTimeInMillis();
    }

    public static Calendar getCalendarForTimeUnit(String unitOfTime){
        Calendar calendar = Calendar.getInstance();

        //Set the calendar to be the correct end of time period
        if(!unitOfTime.equals(HOUR)){
            calendar.set(Calendar.HOUR_OF_DAY, 0);
        }

        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //if the unit is month we also want to set the calendar at the start of this month, end time is exclusive
        //which means that a query will capture the entire previous month
        if(unitOfTime.equals(MONTH)){
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }

        return calendar;
    }

    private static long getMilliSecondsForTimeUnit(String unitOfTime){

        if(unitOfTime.equals("HOUR")){
            return 3600000L;
        }else if(unitOfTime.equals("DAY")){
            return 86400000L;
        }else if(unitOfTime.equals("WEEK")){
            return 604800000L;
        }else{
            return Calendar.getInstance().getTimeInMillis();
        }
    }

    /**
     * Return a millisecond value from the start of epoch up to the date
     * represented by the date parameter.
     * @param date The format MUST BE in the format 'yyyy/MM/dd HH:mm'
     * @return The number of milliseconds since epoch represented by the supplied date
     */
    public static long getAbsoluteMilliSeconds(String date) throws ParseException {
        Date d = DATE_FORMAT.parse(date);
        return d.getTime();
    }

    /**
     * For the specified relative time period, get the distinct list of intervals which make up that time period
     * The first long in the returned list is the very start of the time period and the last long is the end
     * of the very last interval.
     * The returned List should be used as follows: Interval 0 = list(i) >= interval < list(i+1) therefore an interval
     * is inclusive of it's start and exclusive of it's end.
     * Note: The last interval may be shorter than expected if the interval does not divide evenly into the time period
     * @param timePeriodStartInclusive When does the time period start. See Utilities.getRelativeMilliSeconds() for
     * how to get the timePeriodStartInclusive value
     * @param intervalNumberOfUnits The length of an interval is numberOfUnits x unitOfTime
     * @param intervalUnitOfTime valid values are HOUR, DAY, WEEK and MONTH
     * @return List<Long> the ordered list of long's representing the start of each interval. The last long represents
     * the end of the last interval.
     */
    public static List<Long> getIntervalsForTimePeriod(Long timePeriodStartInclusive, Long timePeriodEndExclusive,
                                                               int intervalNumberOfUnits, String intervalUnitOfTime){
        if(timePeriodStartInclusive >= timePeriodEndExclusive){
            Calendar test = Calendar.getInstance();
            test.setTimeInMillis(timePeriodStartInclusive);
            String startDate =  DATE_FORMAT.format(test.getTime());
            test.setTimeInMillis(timePeriodEndExclusive);
            String endDate =  DATE_FORMAT.format(test.getTime());

            throw new IllegalArgumentException("End of time period must be after the time period start time: start: " +
            startDate+" value = "+ timePeriodStartInclusive+" end: " + endDate+" value = " + timePeriodEndExclusive);
        }

        List<Long> returnList = new ArrayList<Long>();

        if(!intervalUnitOfTime.equals(MONTH)){
            long unitMilliSeconds = getMilliSecondsForTimeUnit(intervalUnitOfTime);
            long currentIntervalStart = timePeriodStartInclusive;
            long intervalLength = unitMilliSeconds * intervalNumberOfUnits;
            //in this case there is only one interval
            if(currentIntervalStart + intervalLength >= timePeriodEndExclusive){
                returnList.add(currentIntervalStart);
                returnList.add(timePeriodEndExclusive);
                return returnList;
            }

            while(currentIntervalStart <= timePeriodEndExclusive){
                returnList.add(currentIntervalStart);
                if (currentIntervalStart == timePeriodEndExclusive) break;
                currentIntervalStart += intervalLength;
            }

            //Catch any difference due to timeperiod / interval having a remainder
            if (currentIntervalStart != timePeriodEndExclusive) {
                returnList.add(timePeriodEndExclusive);
            }
            return returnList;
        }

        //The code for month is the same logic as for the other time units
        //but as the length of a month varies can't deal with an absolute time interval value
        //and instead use Calendar functions to move through time.
        //This could prob be refactored for one set of logic using only calendar functions

        Calendar endOfTimePeriod = Calendar.getInstance();
        endOfTimePeriod.setTimeInMillis(timePeriodEndExclusive);

        Calendar startOfTimePeriod = Calendar.getInstance();
        startOfTimePeriod.setTimeInMillis(timePeriodStartInclusive);

        Calendar temp = Calendar.getInstance();
        temp.setTimeInMillis(timePeriodStartInclusive);
        temp.add(Calendar.MONTH, intervalNumberOfUnits);

        //in this case there is only one interval
        if(temp.getTimeInMillis() >= timePeriodEndExclusive){
            returnList.add(timePeriodStartInclusive);
            returnList.add(timePeriodEndExclusive);
            return returnList;
        }

        while(startOfTimePeriod.getTimeInMillis() <= endOfTimePeriod.getTimeInMillis()){
            returnList.add(startOfTimePeriod.getTimeInMillis());
            if (startOfTimePeriod.getTimeInMillis() == timePeriodEndExclusive) break;
            startOfTimePeriod.add(Calendar.MONTH, intervalNumberOfUnits);
        }

        if(startOfTimePeriod.getTimeInMillis() != endOfTimePeriod.getTimeInMillis()){
            returnList.add(timePeriodEndExclusive);
        }
        return returnList;
    }

    /**
     * Convert the strings in the supplied collection into a valid SQL 'IN' style query e.g.
     * (1,2,3,4) where 1,2,3 and 4 are service oid's.
     * @param Collection of Service string oid's
     * @return String to be used as the 'x' -> select * from y where where objectid in x
     */
    public static String getServiceIdInQuery(Collection values){
        if(values.isEmpty()) return "(0)";

        Iterator iter = values.iterator();
        StringBuilder sb = new StringBuilder("(");
        int i = 0;
        while(iter.hasNext()){
            if(i != 0) sb.append(",");
            sb.append(iter.next());
            i++;
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Create a string representation of all string values contained in the values collection
     * @param values The Collection of strings to be placed into a single string for user info
     * @return string with all the strings from values concat'ed with " " between them
     */
    public static String getServiceNamesFromCollection(Collection values){
        if(values.isEmpty()) return "";

        Iterator iter = values.iterator();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (iter.hasNext()) {
            if(i != 0) sb.append(", ");
            sb.append(iter.next());
            i++;
        }
        return sb.toString();
    }

    public static void main(String [] args) throws ParseException {
//        String date = "2008-09-18 15:08";
//        System.out.println(Utilities.getAbsoluteMilliSeconds(date));
//        date = "2008-09-18 15:09";
//        System.out.println(Utilities.getAbsoluteMilliSeconds(date));

//        long endOfLastHour = Utilities.getRelativeMilliSecondsInPast(1, "HOUR");
//
//        Calendar cal = Calendar.getInstance();
//        cal.setTimeInMillis(endOfLastHour);
//        Date d = cal.getTime();
//        //System.out.println(DATE_FORMAT.format(d));
//
//        long startOfYesterday = Utilities.getRelativeMilliSecondsInPast(1, "DAY");
//        cal.setTimeInMillis(startOfYesterday);
//        d = cal.getTime();
//        //System.out.println(DATE_FORMAT.format(d));
//
//        long timePeriodEnd = Utilities.getCalendarForTimeUnit("DAY").getTimeInMillis();
//        List<Long> intervals = Utilities.getIntervalsForTimePeriod(startOfYesterday, timePeriodEnd, 1, "HOUR");
//
//        for(Long l: intervals){
//            cal.setTimeInMillis(l);
//            d = cal.getTime();
//            System.out.println(DATE_FORMAT.format(d));
//        }

//        String intervalStart = "2008/09/16 12:00";
//        long intervalStartMilli = Utilities.getAbsoluteMilliSeconds(intervalStart);
//        String intervalEnd = "2008/09/16 13:00";
//        long intervalEndMilli = Utilities.getAbsoluteMilliSeconds(intervalEnd);
//
//        System.out.println("Start: " + intervalStartMilli + " End: " + intervalEndMilli);
//
//        System.out.println("HOUR: " + Utilities.getIntervalDisplayDate(intervalStartMilli, intervalEndMilli, HOUR));
//        System.out.println("DAY: " + Utilities.getIntervalDisplayDate(intervalStartMilli, intervalEndMilli, DAY));
//        System.out.println("WEEK: " + Utilities.getIntervalDisplayDate(intervalStartMilli, intervalEndMilli, WEEK));
//        System.out.println("MONTH: " + Utilities.getIntervalDisplayDate(intervalStartMilli, intervalEndMilli, MONTH));
//
//        long monthTime = Utilities.getRelativeMilliSecondsInPast(2, WEEK);
//        Calendar cal = Calendar.getInstance();
//        cal.setTimeInMillis(monthTime);
//        System.out.println("Month: " + DATE_FORMAT.format(cal.getTime()));

//        long relativeStartMilli = Utilities.getRelativeMilliSecondsInPast(1, MONTH);
//        long relativeEndMilli = Utilities.getMillisForEndTimePeriod(MONTH);
//
//        List<Long> intervals = Utilities.getIntervalsForTimePeriod(relativeStartMilli, relativeEndMilli, 10, DAY);
//        Calendar cal = Calendar.getInstance();
//        for(Long l: intervals){
//            cal.setTimeInMillis(l);
//            System.out.println(DATE_FORMAT.format(cal.getTime()));
//        }

//        List<String> l = new ArrayList<String>();
//        for(int i =0; i < 10; i++){
//            l.add(""+i);
//        }
//
//        System.out.print(Utilities.getServiceIdInQuery(l));
//        System.out.print(System.getProperty("line.separator"));
//        System.out.print(Utilities.getServiceNamesFromCollection(l));

        String date = Utilities.getMilliSecondAsStringDate(1222239651000L);
        System.out.println("Date: " + date);


    }

}