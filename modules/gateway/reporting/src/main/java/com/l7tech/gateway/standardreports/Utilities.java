/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 29, 2008
 * Time: 11:21:44 AM
 * Utility functions used by the implementation of standard reports with Jasper reporting engine
 */
package com.l7tech.gateway.standardreports;

import org.w3c.dom.*;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;

import com.l7tech.common.io.XmlUtil;


public class Utilities {

    public static final String DATE_STRING = "yyyy/MM/dd HH:mm";
    private static final String HOUR_DATE_STRING = "HH:mm";
    private static final String DAY_HOUR_DATE_STRING = "MM/dd HH:mm";
    private static final String DAY_DATE_STRING = "E MM/dd";
    private static final String DAY_MONTH_DATE_STRING = "M E MM/dd";
    private static final String WEEK_DATE_STRING = "MM/dd";
    private static final String WEEK_YEAR_DATE_STRING = "yyyy/MM/dd";
    private static final String MONTH_DATE_STRING = "yyyy MMM";

    /**
     * The ';' character is used as a placeholder for sql column values, primiarly because no operation name of
     * value ';' is valid in a wsdl.
     */
    public static final String SQL_PLACE_HOLDER =  ";";
    public static final String ROW_TOTAL_STYLE = "UsageRowTotal";
    public static final String USAGE_TABLE_HEADING_STYLE = "UsageTableHeading";
    private static final String IS_CONTEXT_MAPPING = "isContextMapping";
    private static final String CHART_LEGEND = "chartLegend";
    private static final String CHART_HEIGHT = "chartHeight";
    private static final String CHART_LEGEND_FRAME_YPOS = "chartLegendFrameYPos";
    private static final String CHART_LEGEND_HEIGHT = "chartLegendHeight";
    private static final String BAND_HEIGHT = "bandHeight";
    private static final String CHART_FRAME_HEIGHT = "chartFrameHeight";
    private static final String PAGE_HEIGHT = "pageHeight";
    private static final String CHART_ELEMENT = "chartElement";
    private static final int CONSTANT_HEADER_HEIGHT = 54;
    private static final int FRAME_MIN_WIDTH = 820;

    private static final Logger logger = Logger.getLogger(Utilities.class.getName());

    public static enum UNIT_OF_TIME {
        HOUR, DAY, WEEK, MONTH
    }

    public static UNIT_OF_TIME getUnitFromString(String unitOfTime){
        for(UNIT_OF_TIME u: UNIT_OF_TIME.values()){
            if(u.toString().equals(unitOfTime)){
                return u;
            }
        }
        throw new IllegalArgumentException("No unit of time found for param: " + unitOfTime);
    }


    public static final int NUM_MAPPING_KEYS = 5;
    public static final String AUTHENTICATED_USER_DISPLAY = "Authenticated User";

    //SQL select fields
    public static final String SERVICE_ID = "SERVICE_ID";
    public static final String SERVICE_NAME = "SERVICE_NAME";
    public static final String ROUTING_URI = "ROUTING_URI";
    public static final String THROUGHPUT = "THROUGHPUT";
    public static final String FRTM = "FRTM";
    public static final String FRTMX = "FRTMX";
    public static final String FRTA = "FRTA";
    public static final String BRTM = "BRTM";
    public static final String BRTMX = "BRTMX";
    public static final String AP = "AP";

    public static final String CONSTANT_GROUP = "CONSTANT_GROUP";
    public static final String AUTHENTICATED_USER = "AUTHENTICATED_USER";
    public static final String SERVICE_OPERATION_VALUE = "SERVICE_OPERATION_VALUE";
    public static final String MAPPING_VALUE_1 = "MAPPING_VALUE_1";
    public static final String MAPPING_VALUE_2 = "MAPPING_VALUE_2";
    public static final String MAPPING_VALUE_3 = "MAPPING_VALUE_3";
    public static final String MAPPING_VALUE_4 = "MAPPING_VALUE_4";
    public static final String MAPPING_VALUE_5 = "MAPPING_VALUE_5";

    private final static String distinctFrom = "SELECT distinct p.objectid as SERVICE_ID, p.name as SERVICE_NAME, " +
            "p.routing_uri as ROUTING_URI ,'1' as CONSTANT_GROUP";

    private final static String aggregateSelect = "SELECT p.objectid as SERVICE_ID, " +
            "p.name as SERVICE_NAME, p.routing_uri as ROUTING_URI, " +
            "SUM(if({0}.attempted, {0}.attempted,0)) as ATTEMPTED, "+
            "SUM(if({0}.completed, {0}.completed,0)) as THROUGHPUT," +
            "if(SUM({0}.attempted), if(SUM({0}.authorized), SUM({0}.attempted)-SUM({0}.authorized),0),0)  as POLICY_VIOLATIONS, "+
            "if(SUM({0}.authorized), if(SUM({0}.completed), SUM({0}.authorized)-SUM({0}.completed),0),0)  as ROUTING_FAILURES, "+
            " MIN({0}.front_min) as FRTM, " +
            "MAX({0}.front_max) as FRTMX, if(SUM({0}.front_sum), if(SUM({0}.attempted), " +
            "SUM({0}.front_sum)/SUM({0}.attempted),0), 0) as FRTA, MIN({0}.back_min) as BRTM, " +
            "MAX({0}.back_max) as BRTMX, if(SUM({0}.back_sum), if(SUM({0}.completed), " +
            "SUM({0}.back_sum)/SUM({0}.completed),0), 0) as BRTA, " +
            "if(SUM({0}.attempted), ( 1.0 - ( ( (SUM({0}.authorized) - SUM({0}.completed)) / SUM({0}.attempted) ) ) ) , 0) as 'AP'" +
            " ,'1' as CONSTANT_GROUP ";

    private final static String usageAggregateSelect = "SELECT p.objectid as SERVICE_ID, " +
            "p.name as SERVICE_NAME, p.routing_uri as ROUTING_URI, " +
            "SUM(if(smd.completed, smd.completed,0)) as USAGE_SUM,'1' as CONSTANT_GROUP ";

    private final static String mappingJoin = " FROM service_metrics sm, published_service p, service_metrics_details smd," +
            " message_context_mapping_values mcmv, message_context_mapping_keys mcmk WHERE p.objectid = sm.published_service_oid " +
            "AND sm.objectid = smd.service_metrics_oid AND smd.mapping_values_oid = mcmv.objectid AND mcmv.mapping_keys_oid = mcmk.objectid ";

    private final static String noMappingJoin = " FROM service_metrics sm, published_service p WHERE " +
            "p.objectid = sm.published_service_oid ";

    public final static String onlyIsDetailDisplayText = "Detail Report";

    public final static String VARIABLES = "variables";
    public final static String VARIABLE = "variable";

    public final static String CONSTANT_HEADER = "constantHeader";
    public static final String COLUMN_WIDTH = "columnWidth";
    public static final String PAGE_WIDTH = "pageWidth";
    public static final String FRAME_WIDTH = "frameWidth";
    public static final String SERVICE_AND_OPERATION_FOOTER = "serviceAndOperationFooter";
    public static final String SERVICE_ID_FOOTER = "serviceIdFooter";
    public static final String CONSTANT_FOOTER = "constantFooter";
    public static final String LEFT_MARGIN = "leftMargin";
    public static final String RIGHT_MARGIN = "rightMargin";
    private static final String TABLE_CELL_STYLE = "TableCell";
    public static final int FIELD_HEIGHT = 18;
    public static final int RIGHT_MARGIN_WIDTH = 15;
    public static final int LEFT_MARGIN_WIDTH = RIGHT_MARGIN_WIDTH;
    public static final int TOTAL_COLUMN_WIDTH = 80;
    public static final int DATA_COLUMN_WIDTH = 80;
    public static final String SERVICE_HEADER = "serviceHeader";
    public static final String SUB_REPORT = "subReport";
    public static final String RETURN_VALUE = "returnValue";
    public static final String SUMMARY = "summary";
    private static final String SUB_REPORT_WIDTH = "subReportWidth";
    public static final String NO_DATA = "noData";
    public static final int SUB_INTERVAL_STATIC_WIDTH = 113;
    public static final int MAPPING_VALUE_FIELD_HEIGHT = 36;
    //service text field is 5 from left margin
    public static final int CONSTANT_HEADER_START_X = 113;
    public static final int SERVICE_HEADER_X_POS = 50;

    public static final Map<String, UsageReportHelper> helperMap = new HashMap<String, UsageReportHelper>();

    public static void addHelper(String uuid, UsageReportHelper helper){
        helperMap.put(uuid, helper);
    }

    public static UsageReportHelper getHelper(String uuid){
        if(!helperMap.containsKey(uuid)){
            throw new IllegalArgumentException("String: " + uuid+" not found in map");
        }

        return helperMap.get(uuid);
    }

    /**
     * Relative time is calculated to a fixed point of time depending on the unit of time supplied. For day, week
     * and month 00:00 of the current day is used (end of time period is not exclusive) minus the unitOfTime x
     * numberOfUnits. The start of the time period is inclusive.
     * E.g. if the current date is (yyyy-mm-dd hh:mm) 2008-09-22 17:48 and the unitOfTime is HOUR and the numerOfUnits
     * is 1, then the relative calculation will be done from 2008-09-22 17:00 (x) and the number of milliseconds
     * returned will be (x - (numberOfUnits x unitOfTime)). In this example if you convert the returned long
     * into a date, it would report it's time as 2008-09-22 16:00
     * For day, week and month the time used for the relative calculation is 00:00 of the current day.
     * In calculations a week = 7 days.
     * Month uses whole months, not including any time from the current month.
     * @param numberOfUnits How many unitOfTime to use
     * @param unitOfTime valid values are HOUR, DAY, WEEK and MONTH
     * @return
     */
    public static long getRelativeMilliSecondsInPast(int numberOfUnits, UNIT_OF_TIME unitOfTime){
        Calendar calendar = getCalendarForTimeUnit(unitOfTime);

        int calendarTimeUnit = getCalendarTimeUnit(unitOfTime);
        calendar.add(calendarTimeUnit, numberOfUnits * -1);
        return calendar.getTimeInMillis();
    }

    private static int getCalendarTimeUnit(UNIT_OF_TIME unitOfTime) {
        switch( unitOfTime){
            case HOUR:
                return Calendar.HOUR_OF_DAY;
            case DAY:
                return Calendar.DAY_OF_MONTH;
            case WEEK:
                return Calendar.WEEK_OF_YEAR;
            case MONTH:
                return Calendar.MONTH;
        }
        throw new IllegalArgumentException("Invalid unitOfTime");
    }

    /**
     * Get the resolution to use in summary queries.
     * If the difference between the startTimeMilli and endTimeMilli > hourRetentionPeriod * num milli seconds in a day,
     * then the daily bin resolution is used, otherwise hourly is used.
     * @param startTimeMilli start of time period, in milliseconds, since epoch
     * @param endTimeMilli end of time period, in milliseconds, since epoch
     * @param hourRetentionPeriod SSG's current hourly bin max retention policy value, number of days hourly data is
     * kept for
     * @return
     */
    public static Integer getSummaryResolutionFromTimePeriod(Integer hourRetentionPeriod, Long startTimeMilli, Long endTimeMilli){

        if(startTimeMilli >= endTimeMilli) throw new IllegalArgumentException("Start time must be before end time");
        
        long duration = endTimeMilli - startTimeMilli;
        long dayMillis = 86400000L;//day milli seconds
        long maxHourRenentionMilli = dayMillis * hourRetentionPeriod;
        if(duration > maxHourRenentionMilli){
            return new Integer(2);
       }else{
            return new Integer(1);
        }
    }

    /**
     * Get the resolution to use in interval queries.
     * This method delegates to getSummaryResolutionFromTimePeriod after checking that if the relativeTimeUnit is
     * UNIT_OF_TIME.HOUR, that the relative time period does not exceed the maximum retention period for hourly bins
     * @param startTimeMilli start of time period, in milliseconds, since epoch
     * @param endTimeMilli end of time period, in milliseconds, since epoch
     * @param hourRetentionPeriod SSG's current hourly bin max retention policy value, number of days hourly data is
     * kept for
     * @param
     * @return
     */
    public static Integer getIntervalResolutionFromTimePeriod(UNIT_OF_TIME intervalTimeUnit, Integer hourRetentionPeriod,
                                                              Long startTimeMilli, Long endTimeMilli){
        if(startTimeMilli >= endTimeMilli) throw new IllegalArgumentException("Start time must be before end time");

        if(intervalTimeUnit == UNIT_OF_TIME.HOUR){
            //If the interval is in hours, then we have to use the hourly bin
            long dayMillis = 86400000L;//day milli seconds
            long maxHourRenentionMilli = dayMillis * hourRetentionPeriod;
            
            long totalRelativeTime = endTimeMilli - startTimeMilli;

            if(totalRelativeTime > maxHourRenentionMilli){
//                throw new IllegalStateException("The relative time period extends beyond the maximum retention time for" +
//                        " hourly metrics");
            }
            return new Integer(1);

        }
        
        return getSummaryResolutionFromTimePeriod(hourRetentionPeriod, startTimeMilli, endTimeMilli);
    }

    /**
     * Get the date string representation of a time value in milliseconds
     * @param timeMilliSeconds the number of milliseconds since epoch
     * @return a date in the format yyyy/MM/dd HH:mm
     */
    public static String getMilliSecondAsStringDate(Long timeMilliSeconds){
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_STRING);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMilliSeconds);
        return DATE_FORMAT.format(cal.getTime());
    }

    public static String getIntervalAsString(UNIT_OF_TIME unitOfTime, int numIntervalUnits){
        StringBuilder sb = new StringBuilder();
        String unit = unitOfTime.toString();
        sb.append(unit.substring(0,1).toUpperCase());
        sb.append(unit.substring(1, unit.length()).toLowerCase());
        if(numIntervalUnits > 1) sb.append("s");
        return sb.toString();
    }
    /**
     * Get the date to display on a report. The timeMilliSecond value since epoch will be converted into a suitable
     * format to use in the report as the interval information.
     * When an interval crosses a major time boundary for the intervalUnitOfTime supplied and if the timeMillilSeconds
     * represents the start of the interval, based on startofInterval being true, then the string returned will be
     * modified to highlight to the user the crossing of the time boundary
     * E.g. across midnight when the interval unit of time is 1 hour
     * @param startIntervalMilliSeconds milli second value since epoch
     * @param endIntervalMilliSeconds milli second value since epoch 
     * @param intervalUnitOfTime HOUR, DAY, WEEK or MONTH
     * @return String representing the date, to be displayed to the viewer of the report
     */
    public static String getIntervalDisplayDate(Long startIntervalMilliSeconds, Long endIntervalMilliSeconds,
                                        UNIT_OF_TIME intervalUnitOfTime){
        
        Calendar calStart = Calendar.getInstance();
        calStart.setTimeInMillis(startIntervalMilliSeconds);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTimeInMillis(endIntervalMilliSeconds);

        SimpleDateFormat WEEK_DATE_FORMAT = new SimpleDateFormat(WEEK_DATE_STRING);
        //todo [Donal] could validate that the end time is an hour, day..etc from the start time
        switch( intervalUnitOfTime){
            case HOUR:
                SimpleDateFormat HOUR_DATE_FORMAT = new SimpleDateFormat(HOUR_DATE_STRING);
                SimpleDateFormat DAY_HOUR_DATE_FORMAT = new SimpleDateFormat(DAY_HOUR_DATE_STRING);
                return DAY_HOUR_DATE_FORMAT.format(calStart.getTime()) + " - " +
                            HOUR_DATE_FORMAT.format(calEnd.getTime());
            case DAY:
                if(calStart.get(Calendar.MONTH) == Calendar.JANUARY){
                    SimpleDateFormat DAY_MONTH_DATE_FORMAT = new SimpleDateFormat(DAY_MONTH_DATE_STRING);
                    return DAY_MONTH_DATE_FORMAT.format(calStart.getTime());
                }
                SimpleDateFormat DAY_DATE_FORMAT = new SimpleDateFormat(DAY_DATE_STRING);
                return DAY_DATE_FORMAT.format(calStart.getTime());
            case WEEK:
                if(calStart.get(Calendar.MONTH) == Calendar.JANUARY){
                    SimpleDateFormat WEEK_YEAR_DATE_FORMAT = new SimpleDateFormat(WEEK_YEAR_DATE_STRING);
                    return WEEK_YEAR_DATE_FORMAT.format(calStart.getTime())+ " - " +
                            WEEK_DATE_FORMAT.format(calEnd.getTime());
                }
                return WEEK_DATE_FORMAT.format(calStart.getTime())+ " - " +
                            WEEK_DATE_FORMAT.format(calEnd.getTime());
            case MONTH:
                SimpleDateFormat MONTH_DATE_FORMAT = new SimpleDateFormat(MONTH_DATE_STRING);
                return MONTH_DATE_FORMAT.format(calStart.getTime());

        }
        return null;
    }

    /**
     * Get the number of milliseconds representing the end of the period represtented by the unitOfTime
     * For Hour this is the end of the previous hour, for Day and Week it's 00:00 today. For Months it's also the
     * 1st day of the Month
     * @param unitOfTime
     * @return
     */
    public static long getMillisForEndTimePeriod(UNIT_OF_TIME unitOfTime){
        return Utilities.getCalendarForTimeUnit(unitOfTime).getTimeInMillis();
    }

    private static void checkUnitOfTime(String unitSupplied, String [] allowableUnits){
        for(String s: allowableUnits){
            if(unitSupplied.equals(s)){
                return;
            }
        }
        StringBuilder allUnits = new StringBuilder();
        for(String s: allowableUnits){
            allUnits.append(s).append(" ");
        }
        throw new IllegalArgumentException(unitSupplied + " is not a supported unit of time. Allowable units are: "
                + allUnits.toString());
    }

    public static Calendar getCalendarForTimeUnit(UNIT_OF_TIME unitOfTime){
        Calendar calendar = Calendar.getInstance();

        //Set the calendar to be the correct end of time period
        if(unitOfTime != UNIT_OF_TIME.HOUR){
            calendar.set(Calendar.HOUR_OF_DAY, 0);
        }

        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //if the unit is month we also want to set the calendar at the start of this month, end time is exclusive
        //which means that a query will capture the entire previous month
        if(unitOfTime == UNIT_OF_TIME.MONTH){
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }

        return calendar;
    }

    /**
     * Return a millisecond value from the start of epoch up to the date
     * represented by the date parameter.
     * @param date The format MUST BE in the format 'yyyy/MM/dd HH:mm'
     * @return The number of milliseconds since epoch represented by the supplied date
     */
    public static long getAbsoluteMilliSeconds(String date) throws ParseException {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_STRING);
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
     * @param timePeriodEndExclusive end of time period
     * @param intervalNumberOfUnits The length of an interval is numberOfUnits x unitOfTime
     * @param intervalUnitOfTime valid values are HOUR, DAY, WEEK and MONTH
     * @return List<Long> the ordered list of long's representing the start of each interval. The last long represents
     * the end of the last interval.
     */
    public static List<Long> getIntervalsForTimePeriod(Long timePeriodStartInclusive, Long timePeriodEndExclusive,
                                                               int intervalNumberOfUnits, UNIT_OF_TIME intervalUnitOfTime){
        if(timePeriodStartInclusive >= timePeriodEndExclusive){
            Calendar test = Calendar.getInstance();
            test.setTimeInMillis(timePeriodStartInclusive);
            SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_STRING);
            String startDate =  DATE_FORMAT.format(test.getTime());
            test.setTimeInMillis(timePeriodEndExclusive);
            String endDate =  DATE_FORMAT.format(test.getTime());

            throw new IllegalArgumentException("End of time period must be after the time period start time: start: " +
            startDate+" value = "+ timePeriodStartInclusive+" end: " + endDate+" value = " + timePeriodEndExclusive);
        }

//        checkUnitOfTime(intervalUnitOfTime, new String[]{HOUR, DAY, WEEK, MONTH});
        if(intervalNumberOfUnits <= 0) throw new IllegalArgumentException("intervalNumberOfUnits must greater than 0");

        int calendarUnitOfTime = getCalendarTimeUnit(intervalUnitOfTime);

        List<Long> returnList = new ArrayList<Long>();

        Calendar endOfTimePeriod = Calendar.getInstance();
        endOfTimePeriod.setTimeInMillis(timePeriodEndExclusive);

        Calendar startOfTimePeriod = Calendar.getInstance();
        startOfTimePeriod.setTimeInMillis(timePeriodStartInclusive);

        Calendar temp = Calendar.getInstance();
        temp.setTimeInMillis(timePeriodStartInclusive);
        temp.add(calendarUnitOfTime, intervalNumberOfUnits);

        //in this case there is only one interval
        if(temp.getTimeInMillis() >= timePeriodEndExclusive){
            returnList.add(timePeriodStartInclusive);
            returnList.add(timePeriodEndExclusive);
            return returnList;
        }

        while(startOfTimePeriod.getTimeInMillis() <= endOfTimePeriod.getTimeInMillis()){
            returnList.add(startOfTimePeriod.getTimeInMillis());
            if (startOfTimePeriod.getTimeInMillis() == timePeriodEndExclusive) break;
            startOfTimePeriod.add(calendarUnitOfTime, intervalNumberOfUnits);
        }

        if(startOfTimePeriod.getTimeInMillis() != endOfTimePeriod.getTimeInMillis()){
            returnList.add(timePeriodEndExclusive);
        }
        return returnList;
    }

    /**
     * Create a string representation of all string values contained in the values collection
     * @param values The Collection of strings to be placed into a single string for user info
     * @return string with all the strings from values concat'ed with " " between them
     */
    public static String getStringNamesFromCollection(Collection values){
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

    /**
     * Get a distinct query, which for all the input params, will return sql to get the set of
     * values represented by the inputs. This query never includes operation, although it is a mapping
     * value, it has a special meaning and is never used in conjunction with the other mapping values
     * @param startTimeInclusiveMilli
     * @param endTimeInclusiveMilli
     * @param serviceIdToOperations
     * @param keys
     * @param keyValueConstraints
     * @param valueConstraintAndOrLike
     * @param resolution
     * @param isDetail
     * @param useUser
     * @param authenticatedUsers
     * @return
     */
    public static String getUsageDistinctMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Map<String, Set<String>> serviceIdToOperations, List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            boolean useUser, List<String> authenticatedUsers){

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, useUser);

        boolean serviceIdsOk = (serviceIdToOperations != null && !serviceIdToOperations.keySet().isEmpty());
        
        checkResolutionParameter(resolution);

        if(valueConstraintAndOrLike == null) valueConstraintAndOrLike = new ArrayList<String>();

        StringBuilder sb = new StringBuilder("SELECT DISTINCT ");

        addUserToSelect(false, useUser, sb);
        addCaseSQL(keys, sb);
        sb.append(mappingJoin);
        addResolutionConstraint(resolution, sb);
        if(useTime){
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb);
        }

        boolean isBlankedOpQuery = isBlanketOperationQuery(serviceIdToOperations);
        //not a detail query and service id's are ok
        // OR is a detail query, and we have blanked operation requirements
        if(serviceIdsOk && ( (!isDetail) || (isBlankedOpQuery && isDetail))){
            addServiceIdConstraint(serviceIdToOperations.keySet(), sb);
        }
        //else isDetail and were going to use operations
        else if(serviceIdsOk && !isBlankedOpQuery && isDetail){
            //new method to constrain serivce id and operation together
            addServiceAndOperationConstraint(serviceIdToOperations, sb);
        }

        if(useUser && authenticatedUsers != null && !authenticatedUsers.isEmpty()){
            addUserConstraint(authenticatedUsers, sb);
        }
        if(keyValuesSupplied){
            addMappingConstraint(keys, keyValueConstraints, valueConstraintAndOrLike, sb);
        }
        addUsageDistinctMappingOrder(sb);
        
        return sb.toString();
    }

    /**
     * Usage interval reports are driven by the set of service ids and operations which match the search criteria.
     * Mapping values are not needed in the output of this query as they do not mean anything at the master report
     * level. All parameters will supplied will be used as constraints on the query. The order of this query is simply
     * service id followed by operation, which may be a place holder
     * @return
     */
    public static String getUsageMasterIntervalQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Map<String, Set<String>> serviceIdToOperations, List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            boolean useUser, List<String> authenticatedUsers){

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, useUser);

        boolean serviceIdsOk = (serviceIdToOperations != null && !serviceIdToOperations.keySet().isEmpty());
        
        checkResolutionParameter(resolution);

        if(valueConstraintAndOrLike == null) valueConstraintAndOrLike = new ArrayList<String>();

        StringBuilder sb = new StringBuilder(distinctFrom);

        addOperationToSelect(isDetail, sb);

        sb.append(mappingJoin);

        addResolutionConstraint(resolution, sb);

        if(useTime){
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb);
        }

        boolean isBlankedOpQuery = isBlanketOperationQuery(serviceIdToOperations);
        //not a detail query and service id's are ok
        // OR is a detail query, and we have blanked operation requirements
        if(serviceIdsOk && ( (!isDetail) || (isBlankedOpQuery && isDetail))){
            addServiceIdConstraint(serviceIdToOperations.keySet(), sb);
        }
        //else isDetail and were going to use operations
        else if(serviceIdsOk && !isBlankedOpQuery && isDetail){
            //new method to constrain serivce id and operation together
            addServiceAndOperationConstraint(serviceIdToOperations, sb);
        }

        if(useUser && authenticatedUsers != null && !authenticatedUsers.isEmpty()){
            addUserConstraint(authenticatedUsers, sb);
        }

        if(keyValuesSupplied){
            addMappingConstraint(keys, keyValueConstraints, valueConstraintAndOrLike, sb);
        }

        sb.append(" ORDER BY SERVICE_ID, SERVICE_OPERATION_VALUE");

        return sb.toString();
    }

    /**
     * Usage query is only interested in one value - the sum of requests - constrained by all inputs
     * This query is used by both the summary and interval usage reports. When it's used by the base sub report
     * in an interval query the serviceIds and operations list have only 1 value each, as at that level we are
     * querying for a particular service and possibly an operation, for those queries
     * @param startTimeInclusiveMilli
     * @param endTimeInclusiveMilli
     * @param serviceIdToOperations
     * @param keys
     * @param keyValueConstraints
     * @param valueConstraintAndOrLike
     * @param resolution
     * @param isDetail
     * @param useUser
     * @param authenticatedUsers
     * @return
     */
    public static String getUsageQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Map<String, Set<String>> serviceIdToOperations, List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            boolean useUser, List<String> authenticatedUsers){

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, useUser);

        boolean serviceIdsOk = (serviceIdToOperations != null && !serviceIdToOperations.keySet().isEmpty());

        checkResolutionParameter(resolution);

        if(valueConstraintAndOrLike == null) valueConstraintAndOrLike = new ArrayList<String>();

        //----SECTION A----
        StringBuilder sb = new StringBuilder(usageAggregateSelect);

        //----SECTION B----
        addUserToSelect(true, useUser, sb);
        //----SECTION C----
        addOperationToSelect(isDetail, sb);
        //----SECTION D's----
        addCaseSQL(keys, sb);
        //----SECTION E----
        sb.append(mappingJoin);
        //----SECTION F----
        addResolutionConstraint(resolution, sb);

        //----SECTION G----
        if(useTime){
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb);
        }

        //----SECTION H & I----
        //Service ids only constrained here, if isDetail is false, otherwise operation and services are constrained
        //together below

        boolean isBlankedOpQuery = isBlanketOperationQuery(serviceIdToOperations);
        //not a detail query and service id's are ok
        // OR is a detail query, and we have blanked operation requirements
        if(serviceIdsOk && ( (!isDetail) || (isBlankedOpQuery && isDetail))){
            addServiceIdConstraint(serviceIdToOperations.keySet(), sb);
        }
        //else isDetail and were going to use operations
        else if(serviceIdsOk && !isBlankedOpQuery && isDetail){
            //new method to constrain serivce id and operation together
            addServiceAndOperationConstraint(serviceIdToOperations, sb);
        }

        //----SECTION J----
        if(useUser && authenticatedUsers != null && !authenticatedUsers.isEmpty()){
            addUserConstraint(authenticatedUsers, sb);
        }

        //----SECTION K----
        if(keyValuesSupplied){
            addMappingConstraint(keys, keyValueConstraints, valueConstraintAndOrLike, sb);
        }

        addGroupBy(sb);

        //----SECTION M----
        addUsageMappingOrder(sb);

        return sb.toString();
    }

    public static String getUsageQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Long serviceId, List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            String operation, boolean useUser, List<String> authenticatedUsers){
        if(serviceId == null) throw new NullPointerException("serviceId cannot be null");
        if(operation == null || operation.equals("")){
            throw new IllegalArgumentException("operation can be null or empty");
        }
        
        Set<String> operations = new HashSet<String>();
        if(!operation.equals(Utilities.SQL_PLACE_HOLDER)) operations.add(operation);
        Map<String, Set<String>> serviceIdToOperations = new HashMap<String, Set<String>>();
        serviceIdToOperations.put(String.valueOf(serviceId), operations);

        return getUsageQuery(startTimeInclusiveMilli, endTimeInclusiveMilli, serviceIdToOperations, keys, keyValueConstraints,
                valueConstraintAndOrLike, resolution, isDetail, useUser, authenticatedUsers);

    }

    /**
     * Create the sql required to get performance statistics for a specific period of time, for a possible specifc set of
     * service id's, operations, mapping keys and values, mapping values AND or LIKE logic and authenticated users.
     * Below is an example query. The comments in the code relate to the section's listed in the query here in the
     * javadoc
     * PLACEHOLDERS: Any where below where ';' could be selected is done for the following reasons:-
     * 1) The reporting software will always get fields, for which it has defined variables
     * 2) Group by can always include this column, so long as the placeholder value is the same for all columns, the
     * results are unaltered
     * 3) Order by can always include this column, so long as the placeholder value is the same for all columns, the
     * results are unaltered
     * 
     * SECTION A: The logic for determing the performance statistics for a specific interval of time is hard coded, and
     * has no need to change at runtime. This hardcoded query also contains logic to make processing easier.
     *
     * SECTION B: AUTHENTICATED_USER column ALWAYS appears in the select statement HOWEVER it either has the real value
     * of mcmv.auth_user_id OR it is selected as ';'.
     *
     * SECTION C: SERVICE_OPERATION_VALUE column ALWAYS appears in the select statement HOWEVER it either has the real value
     * of mcmv.auth_user_id OR it is selected as ';'.
     *
     * SECTION D 1: For every key in the List<String> keys, a case statement is created. It is very important to understand
     * how this section works, as it explains why these queries work when the entries in message_context_message_values
     * (mcmv) can contain keys from message_context_message_keys (mcmk) in any order.
     * Although the keys can appear in any order in mcmv, we select these values out of any of the mappingx_key (x:1-5)
     * columns and place it into a derived column with the value MAPPING_VALUE_X (x:1-5). The order of the keys in
     * List<String> keys, determines what MAPPING_VALUE_X column it applies to.
     * Within each case statement we are looking for the existence of a specific key in any of 5 column locations. The
     * key WILL ALWAYS exist due to the WHERE constraint that follows. The WHERE constraint guarantees that any rows
     * found from the joins in the from clause, will ALWAYS contain rows which have ALL of the keys in List<String> keys
     * Note: The implementation of service_metric_detail bins, normalizes the keys used by any mcmv bin instance. This 
     * means that although the keys can be in any order in a message context assertion, any assertion with the same
     * keys in any order, will always use the same key from mcmk.
     *
     * SECTION D 2: We ALWAYS select out every MAPPINGX_VALUE (X:1-5) values from mcmv. After we have created a CASE
     * statement for each key in List<String> keys, the remaining unused values are selected out with the place holder
     * value of ';'.
     *
     * SECTION E: The tables used in the query are not dynamic and neither are any of the joins
     *
     * SECTION F: The value of resolution can be either 1 (Hourly) or 2 (Daily)
     *
     * SECTION G: The time period for the query is for A SPECIFIC interval. The interval is inclusive of the start time
     * and exclusive of the end time.
     *
     * SECTION H & I: (Optional) Sections H & I determine if and how service id's and operations are constrained. The
     * general rule is that an operation is never constrained without it also being constrained to a service.
     * serviceIdToOperations is a map of service ids to a list of operations. There is domain logic applied depending
     * on what the values of the keys in the map are, and whether isDetail is true or false:-
     * H) When any of the keys has a non null and non empty list of operations, then the query produced is for a set
     * of services, with each service id constrained by specific operations. If any service in the map contains a null
     * or empty list of operations, it is simply left out of the query. isDetail must be true for this behaviour to happen.
     * I) When all of the keys have null or empty lists of operations, then the query is only constrained by service ids.
     * If isDetail is true, then this turns the query into a blanket operation query, in which all operations for the
     * selected services are returned.  
     *
     * If serviceIdToOperations is null or empty, then no constraint is put on services or operations.
     *
     * SECTION J: (Optional) If useUser is true AND authenticatedUsers is not null or empty, then all values from the
     * Collection are placed inside an IN constraint, otherwise no SQL is added to the overall query
     *
     * SECTION K: This section compliments the CASE queries in the select clause. For every key in List<String> keys,
     * for which a CASE statement was created, it's guaranteed that a corresponding AND block is created here.
     * For each key, the AND block ensures that any matching rows, contain the key in any of the key locations 1-5.
     *
     * SECTION K 1: If it is determined that valid values for value constraints have been supplied, see
     * keyValuesSupplied variable below, then the AND block also includes a constraint on the mcmk value column. Note
     * that there is an implicit logical relationship between a mappingx_value and mappingx_key columns, even though
     * there is no referential relationship. For each possible location of a key, the corresponding value column is
     * checked, to confirm that when the key has a specific value, that the value matches the supplied filter constraint.
     * Whether the constraint is expressed as AND or LIKE is determined by the values of valueConstraintAndOrLike.
     * NOTE: It is outside the logic of this method to do any wild card translating on the filter values contained in
     * keyValueConstraints. If a key's value is to be constrained by LIKE, then the value must have the sql wildcard
     * characters, '%' and '_', at the correct locations already, if required
     *
     * SECTION L: The group by order is important. Performance Statistics information can never be grouped across
     * services, although it can be aggreated across services, after grouping. The major group element is therefore
     * service id, followed by operation. This guarantees that any resulting row is always at the service level, and
     * from there it can be further broken down by operation, and then mapping value. The mapping values can in
     * reality be in any order here however due to how the keys are processed in the CASE statements, being determined
     * from the List<String> keys supplied, the first X mapping values are NEVER placeholders, placeholders always come
     * last.  
     *
     * SECTION M: The order by order is important. Mapping values are ALWAYS ordered first, AUTHENTICATED_USER IS A
     * mapping value. They are the major order aspect, by which we want to view data. We want to look at data in terms
     * of a set of mapping values, which represent the concept of an individual requestor type, of a service.
     * Note that due to how the keys are processed in the CASE statements, being determined from the List<String> keys
     * supplied, the first X mapping values are NEVER placeholders, placeholders always come last.
     * Following the mapping values is the service id and the operation. Service id must come before operation, as it
     * is a bigger group type. We want to either view the mapping data at the service level and from there possibly the
     * operation level.
     *
     * <pre>
SELECT
     ----SECTION A----
p.objectid as SERVICE_ID,
p.name as SERVICE_NAME,
p.routing_uri as ROUTING_URI,
SUM(if(smd.completed, smd.completed,0)) as THROUGHPUT,
MIN(smd.front_min) as FRTM,
MAX(smd.front_max) as FRTMX,
if(SUM(smd.front_sum), if(SUM(smd.attempted), SUM(smd.front_sum)/SUM(smd.attempted),0), 0) as FRTA,
MIN(smd.back_min) as BRTM,
MAX(smd.back_max) as BRTMX,
if(SUM(smd.back_sum), if(SUM(smd.completed), SUM(smd.back_sum)/SUM(smd.completed),0), 0) as BRTA,
if(SUM(smd.attempted), ( 1.0 - ( ( (SUM(smd.authorized) - SUM(smd.completed)) / SUM(smd.attempted) ) ) ) , 0) as 'AP' ,
     ----SECTION B----
mcmv.auth_user_id AS AUTHENTICATED_USER,
     ----SECTION C----
mcmv.service_operation AS SERVICE_OPERATION_VALUE,
     ----SECTION D 1----
CASE
	WHEN mcmk.mapping1_key = 'IP_ADDRESS' THEN mcmv.mapping1_value
	WHEN mcmk.mapping2_key = 'IP_ADDRESS' THEN mcmv.mapping2_value
	WHEN mcmk.mapping3_key = 'IP_ADDRESS' THEN mcmv.mapping3_value
	WHEN mcmk.mapping4_key = 'IP_ADDRESS' THEN mcmv.mapping4_value
	WHEN mcmk.mapping5_key = 'IP_ADDRESS' THEN mcmv.mapping5_value
END AS MAPPING_VALUE_1,
CASE
	WHEN mcmk.mapping1_key = 'CUSTOMER' THEN mcmv.mapping1_value
	WHEN mcmk.mapping2_key = 'CUSTOMER' THEN mcmv.mapping2_value
	WHEN mcmk.mapping3_key = 'CUSTOMER' THEN mcmv.mapping3_value
	WHEN mcmk.mapping4_key = 'CUSTOMER' THEN mcmv.mapping4_value
	WHEN mcmk.mapping5_key = 'CUSTOMER' THEN mcmv.mapping5_value
END AS MAPPING_VALUE_2,
     ----SECTION D 2----
';' AS MAPPING_VALUE_3,
';' AS MAPPING_VALUE_4,
';' AS MAPPING_VALUE_5
     ----SECTION E----
FROM
service_metrics sm, published_service p, service_metrics_details smd, message_context_mapping_values mcmv, message_context_mapping_keys mcmk
WHERE
p.objectid = sm.published_service_oid AND
sm.objectid = smd.service_metrics_oid AND
smd.mapping_values_oid = mcmv.objectid AND
mcmv.mapping_keys_oid = mcmk.objectid  AND
     ----SECTION F----
sm.resolution = 2  AND
     ----SECTION G----
sm.period_start >=1220252459000 AND
sm.period_start <1222844459000 AND
     ----SECTION H----
     AND
     (
         (  p.objectid = 229384 AND mcmv.service_operation IN ('listProducts','orderProduct') )  OR
         (  p.objectid = 229382 AND mcmv.service_operation IN ('listProducts','orderProduct') )  OR
         (  p.objectid = 229380 AND mcmv.service_operation IN ('listProducts','orderProduct') )  OR
         (  p.objectid = 229376 AND mcmv.service_operation IN ('listProducts','orderProduct') )  OR
         (  p.objectid = 229378 AND mcmv.service_operation IN ('listProducts','orderProduct') )
     )
    SECTIONS H AND I ARE MUTUALLY EXCLUSIVE
    ----SECTION I----
    p.objectid IN (229384, 229382, 229380, 229376, 229378)

     ----SECTION J----
AND mcmv.auth_user_id IN ('Ldap User 1')  AND
    ----SECTION K----
(
	( mcmk.mapping1_key  = 'IP_ADDRESS'
     ----SECTION K 1----
     AND mcmv.mapping1_value  = '127.0.0.2'
     )
	OR
	( mcmk.mapping2_key  = 'IP_ADDRESS'  AND mcmv.mapping2_value  = '127.0.0.2' )
	OR
	( mcmk.mapping3_key  = 'IP_ADDRESS'  AND mcmv.mapping3_value  = '127.0.0.2' )
	OR
	( mcmk.mapping4_key  = 'IP_ADDRESS'  AND mcmv.mapping4_value  = '127.0.0.2' )
	OR
	( mcmk.mapping5_key  = 'IP_ADDRESS'  AND mcmv.mapping5_value  = '127.0.0.2' )
) AND
(
	( mcmk.mapping1_key  = 'CUSTOMER'  AND mcmv.mapping1_value  = 'Silver' )
	OR
	( mcmk.mapping2_key  = 'CUSTOMER'  AND mcmv.mapping2_value  = 'Silver' )
	OR
	( mcmk.mapping3_key  = 'CUSTOMER'  AND mcmv.mapping3_value  = 'Silver' )
	OR
	( mcmk.mapping4_key  = 'CUSTOMER'  AND mcmv.mapping4_value  = 'Silver' )
	OR
	( mcmk.mapping5_key  = 'CUSTOMER'  AND mcmv.mapping5_value  = 'Silver' )
)
     ----SECTION L----
GROUP BY p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER , MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5
     ----SECTION M----
ORDER BY AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5 ,p.objectid, SERVICE_OPERATION_VALUE
</pre>
     * @param startTimeInclusiveMilli time_period start time inclusive
     * @param endTimeInclusiveMilli time_period end time exclsuvie
     * @param serviceIdToOperations if supplied the published_service_oid from service_metrics will be constrained by these keys.
     * If the values for a key is a list of operations, then the constraint for that service will include those operations.
     * If any service has a non null and non empty list of operations, then services will only be returned which have operations
     * specified. if all values are null or empty, then the query is constrained with just service id's, and all operations data
     * will come back for each service supplied
     * @param keys the list of keys representing the mapping keys
     * @param keyValueConstraints the values which each key must be equal to, Can be null or empty
     * @param valueConstraintAndOrLike for each key and value, if a value constraint exists as the index, the index into this
     * list dictitates whether an = or like constraint is applied. Can be null or empty. Cannot have values if
     * keyValueConstraints is null or empty
     * @param resolution 1 = hourly, 2 = daily. Which resolution from service_metrics to use
     * @param isDetail if true then the service_operation's real value is used in the select, group and order by,
     * otherwise operation is selected as 1. To facilitate this service_operation is always selected as
     * SERVICE_OPERATION_VALUE so that the real column is not used when isDetail is false
     * table message_context_mapping_values, with the values in operaitons
     * @param useUser if true the auth_user_id column from message_context_mapping_values will be included in the
     * select, group by and order by clauses
     * @param authenticatedUsers if useUser is true, the where clause will constrain the values of
     * message_context_mapping_values, with the values in authenticatedUsers 
     * @return String query
     * @throws IllegalArgumentException If all the lists are not the same size and if they are empty.
     */
    public static String createMappingQuery(boolean isMasterQuery, Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                               Map<String, Set<String>> serviceIdToOperations,
                                            List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            boolean useUser, List<String> authenticatedUsers){

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, useUser);

        boolean serviceIdsOk = (serviceIdToOperations != null && !serviceIdToOperations.keySet().isEmpty());
        //if(!serviceIdsOk) throw new IllegalArgumentException("There must be at least one service id specified as a key in serviceIdToOperations");
        
        checkResolutionParameter(resolution);

        if(valueConstraintAndOrLike == null) valueConstraintAndOrLike = new ArrayList<String>();

        //----SECTION A----
        StringBuilder sb = null;
        if(isMasterQuery){
            sb = new StringBuilder(distinctFrom);
        }else{
            String select = MessageFormat.format(aggregateSelect ,"smd");
            sb = new StringBuilder(select);
        }
        //----SECTION B----
        addUserToSelect(true, useUser, sb);
        //----SECTION C----
        addOperationToSelect(isDetail, sb);
        //----SECTION D's----
        addCaseSQL(keys, sb);
        //----SECTION E----
        sb.append(mappingJoin);
        //----SECTION F----
        addResolutionConstraint(resolution, sb);

        //----SECTION G----
        if(useTime){
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb);
        }

        //----SECTION H & I----
        //Service ids only constrained here, if isDetail is false, otherwise operation and services are constrained
        //together below

        boolean isBlankedOpQuery = isBlanketOperationQuery(serviceIdToOperations);
        //not a detail query and service id's are ok
        // OR is a detail query, and we have blanked operation requirements
        if(serviceIdsOk && ( (!isDetail) || (isBlankedOpQuery && isDetail))){
            addServiceIdConstraint(serviceIdToOperations.keySet(), sb);            
        }
        //else isDetail and were going to use operations
        else if(serviceIdsOk && !isBlankedOpQuery && isDetail){
            //new method to constrain serivce id and operation together
            addServiceAndOperationConstraint(serviceIdToOperations, sb);
        }

        //----SECTION J----
        if(useUser && authenticatedUsers != null && !authenticatedUsers.isEmpty()){
            addUserConstraint(authenticatedUsers, sb);
        }

        //----SECTION K----
        if(keyValuesSupplied){
            addMappingConstraint(keys, keyValueConstraints, valueConstraintAndOrLike, sb);
        }

        if(!isMasterQuery){
            //----SECTION L----
            addGroupBy(sb);
        }

        //----SECTION M----
        addMappingOrder(sb);

        logger.log(Level.INFO, sb.toString());
        return sb.toString();
    }

    /**
     * This method should only be called when some of the service id's map to one or more operations. Operations are
     * therefore only included in a query when they can be explicitly constrained by a service id. This ensures that
     * when a selection of operations are made by the user, that operations from other services with the same name won't
     * also be included in report output.
     * @param serviceIdToOperations
     * @param sb
     */
    public static void addServiceAndOperationConstraint(Map<String, Set<String>> serviceIdToOperations, StringBuilder sb) {
        int index = 0;
        //and surrounds the entire constraint
        sb.append(" AND (");

        for(Map.Entry<String, Set<String>> me: serviceIdToOperations.entrySet()){
            //we know this statement is not true for all elements in the map, but it may be true for some
            //if a service has no op's listed, then it's simply ignored, could log a warning
            //see isBlankedOperationQuery
            if(me.getValue() == null || me.getValue().isEmpty()) continue;

            if(index > 0) sb.append(" OR ");

            sb.append("( ");
            sb.append(" p.objectid = ").append(me.getKey());
            sb.append(" AND mcmv.service_operation IN (");

            int opIndex = 0;
            for(String op: me.getValue()){
                if(opIndex != 0) sb.append(",");
                sb.append("'" + op + "'");
                opIndex++;
            }

            sb.append(")");//close in IN
            sb.append(" ) ");//close the OR
            index++;
        }
        sb.append(" ) ");
    }

    /**
     * The map serviceIdToOperations lists all service ids and any operations they map to. If a single service id maps
     * to an operation then the result is true. If every single service id key maps to a null or empty list, then false
     * is returned, as the query will not need to constrain the service id's with operation information 
     * @param serviceIdToOperations
     * @return
     */
    private static boolean isBlanketOperationQuery(Map<String, Set<String>> serviceIdToOperations) {
//        if(serviceIdToOperations == null) throw new NullPointerException("serviceIdToOperations cannot be null");
//        if(serviceIdToOperations.isEmpty()) throw new IllegalArgumentException("serviceIdToOperations must contain at least one key");

        if(serviceIdToOperations == null || serviceIdToOperations.isEmpty()){
            return true;
        }
        
        for(Map.Entry<String, Set<String>> me: serviceIdToOperations.entrySet()){
            if(me.getValue() == null) continue;
            if(!me.getValue().isEmpty()){
                return false;
            }
        }

        return true;
    }

    /**
     * Convenience method called from sub reports. Instead of taking in collections of service ids, operations and
     * authenticated users, it takes in string values, places them in a collection and then calls createMappingQuery,
     * which this method delegates to.
     * See createMappingQuery for an explanation of how the query returned is created and how it works.
     * @param startTimeInclusiveMilli start of the time period to query for inclusive, can be null, so long as
     * endTimeInclusiveMilli is also null
     * @param endTimeInclusiveMilli end of the time period to query for exclusive, can be null, so long as
     * startTimeInclusiveMilli is also null
     * @param serviceId service ids to constrain query with
     * @param keys mapping keys to use, must be at least 1
     * @param keyValueConstraints values to constrain possible key values with
     * @param valueConstraintAndOrLike and or like in sql for any values supplied as constraints on keys, can be null
     * and empty. If it is, then AND is used by default.
     * @param resolution hourly or daily = 1 or 2
     * @param isDetail true when the sql represents a detail master report, which means service_operation is included
     * in the distinct select list. When isDetail is true, the values in the operations list is used as a filter constraint.
     * @param operation if isDetail is true and operations is non null and non empty, then the sql returned
     * will be filtered by the values in operations
     * @param useUser if true the auth_user_id column from message_context_mapping_values will be included in the
     * select, group by and order by clauses
     * @param authenticatedUser if useUser is true, the where clause will constrain the values of
     * message_context_mapping_values, with the value of authenticatedUsers 
     * @return String sql
     * @throws NullPointerException if startIntervalMilliSeconds or endIntervalMilliSeconds is null
     * @return String sql query
     */
    public static String createMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Long serviceId, List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            String operation, boolean useUser, String authenticatedUser){

        if(serviceId == null) throw new IllegalArgumentException("Service Id must be supplied");
        Set<String> operationSet = new HashSet<String>();
        if(operation != null && !operation.equals("") && !operation.equals(SQL_PLACE_HOLDER)){
            operationSet.add(operation);
        }
        Map<String, Set<String>> serviceIdToOperations = new HashMap<String, Set<String>>();
        serviceIdToOperations.put(serviceId.toString(), operationSet);

        List<String> authUsers = new ArrayList<String>();
        if(authenticatedUser != null && !authenticatedUser.equals("") && !authenticatedUser.equals(SQL_PLACE_HOLDER)){
            authUsers.add(authenticatedUser);
        }
        return createMappingQuery(false, startTimeInclusiveMilli, endTimeInclusiveMilli, serviceIdToOperations, keys,
                keyValueConstraints, valueConstraintAndOrLike, resolution, isDetail, useUser, authUsers);
    }

    private static void addUserToSelect(boolean addComma, boolean useUser, StringBuilder sb) {
        if(addComma) sb.append(",");
        if(useUser){
            sb.append(" mcmv.auth_user_id AS ").append(AUTHENTICATED_USER);
        }else{
            sb.append(" '" + SQL_PLACE_HOLDER + "' AS ").append(AUTHENTICATED_USER);
        }
    }

    public static String getNoMappingQuery(boolean isMasterQuery, Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Collection<String> serviceIds, int resolution){


        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);
        if(!useTime) throw new IllegalArgumentException("Both start and end time must be specified");
        checkResolutionParameter(resolution);

        StringBuilder sb = null;
        if(isMasterQuery){
            sb = new StringBuilder(distinctFrom);
        }else{
            String select = MessageFormat.format(aggregateSelect ,"sm");
            sb = new StringBuilder(select);
        }

        //fill in place holder's
        sb.append(", '"+SQL_PLACE_HOLDER+ "' AS AUTHENTICATED_USER");
        sb.append(", '"+SQL_PLACE_HOLDER+ "' AS SERVICE_OPERATION_VALUE");
        sb.append(", '"+SQL_PLACE_HOLDER+ "' AS MAPPING_VALUE_1");
        sb.append(", '"+SQL_PLACE_HOLDER+ "' AS MAPPING_VALUE_2");
        sb.append(", '"+SQL_PLACE_HOLDER+ "' AS MAPPING_VALUE_3");
        sb.append(", '"+SQL_PLACE_HOLDER+ "' AS MAPPING_VALUE_4");
        sb.append(", '"+SQL_PLACE_HOLDER+ "' AS MAPPING_VALUE_5");

        sb.append(noMappingJoin);

        addResolutionConstraint(resolution, sb);

        addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb);

        if(serviceIds != null && !serviceIds.isEmpty()){
            addServiceIdConstraint(serviceIds, sb);
        }

        if(isMasterQuery){
            sb.append(" ORDER BY p.objectid ");
        }else{
            sb.append(" GROUP BY p.objectid ");
        }
        //System.out.println("Sql: " + sb.toString());
        return sb.toString();
    }

    public static String getNoMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Long serviceId, int resolution){

        if(serviceId == null) throw new IllegalArgumentException("Service id must be supplied");
        List<String> sIds = new ArrayList<String>();
        sIds.add(serviceId.toString());

        return getNoMappingQuery(false, startTimeInclusiveMilli, endTimeInclusiveMilli, sIds, resolution);
    }

    private static void addOperationToSelect(boolean isDetail, StringBuilder sb) {
        if(isDetail){
            sb.append(",  mcmv.service_operation AS SERVICE_OPERATION_VALUE");
        }else{
            //sb.append(",  1 AS SERVICE_OPERATION_VALUE");
            sb.append(",  '" + SQL_PLACE_HOLDER + "' AS SERVICE_OPERATION_VALUE");
        }
    }

    /**
     * Convert a list of string params into a list. This is a convenience method for sub queries, which is going to
     * select out aggregate values for SPECIFIC values of keys for a specific interval. The sub query is fed values
     * for each mapping value from it's master report. Before it can call createMappingQuery it needs to know what
     * specific values to constrain the keys for, for this sub query.
     * We are only interested in values of args, which have an index within the size of keys. Keys will have up to
     * 5 values, and args WILL have 5 values, currently. However some of args will just be placeholders, with the value
     * SQL_PLACE_HOLDER. This is as all queries always include all mapping_value_x (x:1-5), so when less than 5 keys are used, then
     * their select value is just SQL_PLACE_HOLDER, so it has no affect on group and order by operations.
     * If any of the string values equal SQL_PLACE_HOLDER indicating a placeholder, then null is added into that location
     * in the returned list
     * @param args String values for the keys
     * @return List representation of the args
     * @throws IllegalArgumentException if any index from keys results in a null or SQL_PLACE_HOLDER value from args
     */
    public static List<String> createValueList(List<String> keys, String... args){

        if(keys.size() > args.length) throw new IllegalArgumentException("Parameter keys must never be greater in size " +
                "than parameter args");

        List<String> returnList = new ArrayList<String>();

        for (int i = 0; i < keys.size(); i++) {
            String s = args[i];
            if (s == null || s.equals(SQL_PLACE_HOLDER)) throw new IllegalArgumentException("Any value of args with a valid index " +
                    "into keys, must contain a real value and not null or " + SQL_PLACE_HOLDER);
            returnList.add(s);
        }

        return returnList;
    }

    private static void addResolutionConstraint(int resolution, StringBuilder sb) {
        sb.append(" AND sm.resolution = " + resolution+" ");
    }

    private static void addTimeConstraint(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli, StringBuilder sb) {
        sb.append(" AND sm.period_start >=").append(startTimeInclusiveMilli);
        sb.append(" AND sm.period_start <").append(endTimeInclusiveMilli);
    }

    private static void addGroupBy(StringBuilder sb) {
        sb.append(" GROUP BY p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER ");
        for(int i = 0; i < NUM_MAPPING_KEYS; i++){
            sb.append(", ").append("MAPPING_VALUE_" + (i+1));
        }
    }

    private static boolean checkMappingQueryParams(List<String> keys, List<String> keyValueConstraints,
                                                   List<String> valueConstraintAndOrLike,
                                                   boolean isDetail,
                                                   boolean useUser) {
        //we need at least one key. However both user and operation are technically keys, so if we have either
        //a user or an operation, they we have conceptually a key
        boolean keysOk = true;
        if(keys == null || keys.isEmpty()){
            if(!useUser){
                if(!isDetail){
                    throw new IllegalArgumentException("Non detail mapping queries require at least one value in " +
                            "the keys list");

                }
            }
            keysOk = false;
        }else{
            //ensure all keys are unique
            Set<String> s = new HashSet<String>(keys);
            if(s.size() != keys.size()){
                throw new IllegalArgumentException("keys may not contain any duplicates");
            }
        }

        boolean keyValuesSupplied = false;
        if(keyValueConstraints != null && !keyValueConstraints.isEmpty()){
            if(keys.size() != keyValueConstraints.size()){
                throw new IllegalArgumentException("The length of keys must match the length of the keyValueConstraints");
            }
            if(valueConstraintAndOrLike != null && !valueConstraintAndOrLike.isEmpty()){
                if(valueConstraintAndOrLike.size() != keyValueConstraints.size()){
                    throw new IllegalArgumentException("The length of valueConstraintAndOrLike must match the length of the keyValueConstraints");
                }
            }
            keyValuesSupplied = true;
        }else{
            //only throw an exception if keys are not null or empty
            if(keysOk) throw new IllegalArgumentException("The size of keyValueConstraints must match the size of keys");            
            //if keyValueConstraint are not supplied, then we can't have valueConstraintAndOrLike supplied either
//            if(valueConstraintAndOrLike != null && !valueConstraintAndOrLike.isEmpty()){
//                throw new IllegalArgumentException("Cannot supply valueConstraintAndOrLike with values if no values in" +
//                        " keyValueConstraints have been supplied, on which they would be applied");
//            }
        }
        return keyValuesSupplied;
    }

    /**
     * Find out if the time paramters should be included in the query
     * @param startTimeInclusiveMilli
     * @param endTimeInclusiveMilli
     * @return
     * @throws IllegalArgumentException if both params are not both null or both not null, or of startTimeInclusiveMilli
     * is >= endIntervalMilliSeconds 
     */
    private static boolean checkTimeParameters(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli) {
        boolean bothNull = (startTimeInclusiveMilli == null) && (endTimeInclusiveMilli == null);
        boolean bothNotNull = (startTimeInclusiveMilli != null) && (endTimeInclusiveMilli != null);
        if(!(bothNull || bothNotNull)){
            throw new IllegalArgumentException("startTimeInclusiveMilli and endTimeInclusiveMilli must both be null" +
                    "or not null");
        }
        if(bothNotNull){
            if(startTimeInclusiveMilli >= endTimeInclusiveMilli){
                throw new IllegalArgumentException("startTimeInclusiveMilli must be < than endTimeInclusiveMilli");
            }
            return true;
        }
        return false;
    }

    private static void checkResolutionParameter(int resolution){
        if(resolution != 1 && resolution != 2){
            throw new IllegalArgumentException("Resolution can only be 1 (Hourly) or 2 (Daily)");
        }
    }

    private static void addMappingOrder(StringBuilder sb) {
        sb.append(" ORDER BY AUTHENTICATED_USER, ");
        for(int i = 0; i < NUM_MAPPING_KEYS; i++){
            if(i != 0) sb.append(", ");
            sb.append("MAPPING_VALUE_" + (i+1));
        }
        sb.append(" ,p.objectid, SERVICE_OPERATION_VALUE ");
    }

    private static void addUsageMappingOrder(StringBuilder sb) {
        sb.append(" ORDER BY p.objectid, SERVICE_OPERATION_VALUE ");
        sb.append(" ,AUTHENTICATED_USER, ");
        for(int i = 0; i < NUM_MAPPING_KEYS; i++){
            if(i != 0) sb.append(", ");
            sb.append("MAPPING_VALUE_" + (i+1));
        }
    }

    private static void addUsageDistinctMappingOrder(StringBuilder sb) {
        sb.append(" ORDER BY AUTHENTICATED_USER, ");
        for(int i = 0; i < NUM_MAPPING_KEYS; i++){
            if(i != 0) sb.append(", ");
            sb.append("MAPPING_VALUE_" + (i+1));
        }
    }

    private static void addMappingConstraint(List<String> keys, List<String> keyValueConstraints, List<String> valueConstraintAndOrLike, StringBuilder sb) {
        for(int i = 0; i < keys.size(); i++){
            boolean useAnd = true;
            if( i < valueConstraintAndOrLike.size()){
                useAnd = valueConstraintAndOrLike.get(i) == null || valueConstraintAndOrLike.get(i).equalsIgnoreCase("AND");                
            }

            sb.append(" AND (").append( createOrKeyValueBlock(keys.get(i), keyValueConstraints.get(i), useAnd) );
            sb.append(")");
        }
    }

    private static void addUserConstraint(List<String> authenticatedUsers, StringBuilder sb) {
        sb.append(" AND mcmv.auth_user_id IN (");
        for (int i = 0; i < authenticatedUsers.size(); i++) {
            String s = authenticatedUsers.get(i);
            if(i != 0) sb.append(",");
            sb.append("'" + s + "'");
        }
        sb.append(") ");
    }

    private static void addOperationConstraint(List<String> operations, StringBuilder sb) {
        sb.append(" AND mcmv.service_operation IN (");
        for (int i = 0; i < operations.size(); i++) {
            String s = operations.get(i);
            if(i != 0) sb.append(",");
            sb.append("'" + s + "'");
        }
        sb.append(") ");
    }

    private static void addServiceIdConstraint(Collection<String> serviceIds, StringBuilder sb) {
        sb.append(" AND p.objectid IN (");
        boolean first = true;
        for(String s: serviceIds){
            if(!first) sb.append(", ");
            else first = false;
            sb.append(s);
        }
        sb.append(")");
    }

    private static void addCaseSQL(List<String> keys, StringBuilder sb) {
        int max = 0;
        if(keys != null && !keys.isEmpty()){
            for(int i = 0; i < keys.size(); i++,max++){
                sb.append(",").append(addCaseSQLForKey(keys.get(i), i+1));
            }
        }

        //if were not using all 5 possible mappings, then we need to create the missing to help jasper report impl
        for(int i = max+1; i <= NUM_MAPPING_KEYS; i++){
            //sb.append(", 1 AS MAPPING_VALUE_"+i);
            sb.append(", '"+SQL_PLACE_HOLDER+"' AS MAPPING_VALUE_"+i);
        }
    }


    private static String addCaseSQLForKey(String key, int index){
       /*CASE WHEN mcmk.mapping1_key = 'IP_ADDRESS' THEN mcmv.mapping1_value
WHEN mcmk.mapping1_key = 'IP_ADDRESS' THEN mcmv.mapping1_value
WHEN mcmk.mapping2_key = 'IP_ADDRESS' THEN mcmv.mapping2_value
WHEN mcmk.mapping3_key = 'IP_ADDRESS' THEN mcmv.mapping3_value
WHEN mcmk.mapping4_key = 'IP_ADDRESS' THEN mcmv.mapping4_value
WHEN mcmk.mapping5_key = 'IP_ADDRESS' THEN mcmv.mapping5_value
END as IP_ADDRESS,
*/
        StringBuilder sb = new StringBuilder(" CASE ");
        for(int i = 1; i <= NUM_MAPPING_KEYS; i++){
            sb.append(" WHEN mcmk.mapping"+i+"_key = ").append("'"+key+"'");
            sb.append(" THEN mcmv.mapping"+i+"_value");
        }
        sb.append(" END AS MAPPING_VALUE_" + index);
        return sb.toString();
    }

    /**
     *
     * @param key
     * @param value can be null, when it is then we are just constraining rows which the the correct key with any value
     * @param andValue
     * @return
     */
    private static String createOrKeyValueBlock(String key, String value, boolean andValue){
/*(
Value is included in all or none, comment is just illustrative
	(mcmk.mapping1_key = 'IP_ADDRESS' AND mcmk.mapping1_value = '127.0.0.1')
	OR
	(mcmk.mapping2_key = 'IP_ADDRESS')
	OR
	(mcmk.mapping3_key = 'IP_ADDRESS')
	OR
	(mcmk.mapping4_key = 'IP_ADDRESS')
	OR
	(mcmk.mapping5_key = 'IP_ADDRESS')
    )*/
        StringBuilder sb = new StringBuilder();
        for(int i = 1; i <= NUM_MAPPING_KEYS; i++){
            if(i != 1){
                sb.append(" OR ");
            }
            sb.append("( mcmk.mapping").append(i).append("_key");
            sb.append(" = '").append(key).append("' ");

            if(value != null && !value.equals("")){
                sb.append("AND mcmv.mapping").append(i).append("_value");
                if(andValue){
                    sb.append(" = '").append(value).append("' ");
                }else{
                    sb.append(" LIKE '").append(value).append("' ");
                }

            }
            sb.append(")");
        }
        return sb.toString();
    }

    public static boolean isPlaceHolderValue(String testVal){
        if(testVal == null || testVal.equals("")) return false;
        else return testVal.equals(SQL_PLACE_HOLDER);
    }

    /**
     * Calls getMappingValueDisplayString to get how the authUser, keys and keyValues are displayed as a string. Uses
     * that string to look up the group from displayStringToMappingGroup, this is the value that will be shown as
     * the category value on a chart.
     * see Utilities.getMappingValueDisplayString()
     * @param displayStringToMappingGroup
     * @param authUser
     * @param keys
     * @param keyValues
     * @return
     */
    public static String getCategoryMappingDisplayString(Map<String, String> displayStringToMappingGroup, 
                                                               String authUser, List<String> keys, String [] keyValues){

        String displayString = getMappingValueDisplayString(keys, authUser, keyValues, false, null);
        //System.out.println("getCategoryMappingDisplayString: " + displayString);
        if(!displayStringToMappingGroup.containsKey(displayString)) throw new IllegalArgumentException("Group for " +
                "display string not found: " + displayString);

        //System.out.println("Found: " + displayStringToMappingGroup.get(displayString));
        return displayStringToMappingGroup.get(displayString);
    }

    public static String getServiceDisplayString(String serviceName, String serviceRoutingURI){
        return serviceName+"["+serviceRoutingURI+"]";
    }

    public static String getServiceFromDisplayString(Map<String, String> displayStringToService, String serviceName,
                                                 String routingURI){

        String displayString = getServiceDisplayString(serviceName, routingURI);
        //System.out.println("getServiceFromDisplayString: " + displayString);
        if(!displayStringToService.containsKey(displayString)) throw new IllegalArgumentException("Service for " +
                "display string not found: " + displayString);

        //System.out.println("Found: " + displayStringToService.get(displayString));
        return displayStringToService.get(displayString);
    }


    /**
     * Creates a display string from the supplied parameters. Any values which are the place holder are ignored.
     * The display string starts with the authenticated user, if valid, then moves through the keys list and for each
     * key it will display its value.
     * @param authUser value for the authenticated user, can be the place holder value
     * @param keyValues array of Strings. Array is used as it's easier from with Jasper reports than using a
     * Collection
     * @param keys the keys chosen by the user
     * @return display String
     * @throws IllegalArgumentException if the length of keyValues is less than the size of keys
     * @throws NullPointerException if any argument is null or empty for it's type
     * @throws IllegalStateException if keyValues ever has the place holder value for any value from keys
     */
    public static String getMappingValueDisplayString(List<String> keys, String authUser, String[] keyValues, boolean includePreFix, String prefix) {
        if(authUser == null || authUser.equals(""))
            throw new NullPointerException("authUser must have a non null and non empty value. " +
                    "It can be the placeholder value");//as it always exists in select

        if(authUser.equals(SQL_PLACE_HOLDER) && (keys == null || keys.isEmpty())
                && (keyValues == null || keyValues.length == 0)){
            throw new IllegalArgumentException("authUser must be supplied (non null, emtpy and not a placeholder or" +
                    " valid keys and values must be supplied");
        }
        if(includePreFix){
            if(prefix == null || prefix.equals("")) throw new IllegalArgumentException("If includePreFix is true, prefix " +
                    "cannot be null or the empty string");
        }

        if(keyValues == null){
            keyValues = new String[]{};
        }
        if(keys == null){
            keys = new ArrayList<String>();
        }
        if(keyValues.length < keys.size()) throw new IllegalArgumentException("Length of keyValues must equal length of keys");


        StringBuilder sb = new StringBuilder();
        boolean firstComma = false;
        if(!authUser.equals(SQL_PLACE_HOLDER)){
            sb.append("Authenticated User: " + authUser);
            firstComma = true;
        }

        for (int i1 = 0; i1 < keys.size(); i1++) {
            String s = keys.get(i1);
            if(keyValues[i1].equals(SQL_PLACE_HOLDER)){
                throw new IllegalStateException("Place holder should not be found as the value for a valid key");
            }
            if(firstComma){
                sb.append(", ");
                firstComma = false;
            }
            if(i1 != 0){
                sb.append(", ");
            }
            sb.append(s+": " + keyValues[i1]);
        }
        if(sb.toString().equals("")){
            return "Detail Report";
        }else{
            if(includePreFix) sb.insert(0, prefix);
            return sb.toString();
        }
    }

    /**
     * From the auth user, keys, values and filter constraints get a string which displays this information for the user
     * in the report info section of a report
     * @param authUsers Auth user string, can be null or empty
     * @param keys mapping keys, can be null or empty so long as isDetail or useUser is true
     * @param keyValueConstraints
     * @param isDetail
     * @param useUser
     * @return String for displaying in report info section of report
     */
    public static String getMappingReportInfoDisplayString(List<String> authUsers, List<String> keys,
                                                           List<String> keyValueConstraints,
                                                           boolean isDetail,
                                                           boolean useUser){
        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, null, isDetail, useUser);

        StringBuilder sb = new StringBuilder();
        boolean firstComma = false;
        if(useUser){
            sb.append(AUTHENTICATED_USER_DISPLAY);
            firstComma = true;
        }
        if(useUser && authUsers != null && !authUsers.isEmpty()){
            sb.append(": (");
            for (int i = 0; i < authUsers.size(); i++) {
                String s = authUsers.get(i);
                if(i != 0) sb.append(", ");
                sb.append(s);
            }
            sb.append(")");
        }

        if(keys == null){
            //The only constraint on the params is that if all are null, then isDetail or useUser must be true,
            //however if sb is empty here, then useUser was false, in which case it's a detail query. Show something
            //instead of nothing for this corner case.
            if(sb.toString().equals("")){
                return onlyIsDetailDisplayText;                
            }else{
                return sb.toString();
            }
        }
        
        for (int i1 = 0; i1 < keys.size(); i1++) {
            String s = keys.get(i1);
            //valueConstraintAndOrLike
            if(firstComma){
                sb.append(", ");
                firstComma = false;
            }
            if(i1 != 0){
                sb.append(", ");
            }
            sb.append(s);
            if(keyValuesSupplied){
                String value = keyValueConstraints.get(i1);
                if(value != null && !value.equals("")) sb.append(" (" + keyValueConstraints.get(i1) + ")");
            }
        }
        if(sb.toString().equals("")) return "None";
        return sb.toString();
    }
//
//    public static String getUsageColumnHeader(String authUser, List<String> keys, String [] values) {
//        if(values.length < keys.size()) throw new IllegalArgumentException("values must be greater or equal to the size of keys");
//
//        StringBuilder sb = new StringBuilder();
//        if(authUser != null && !authUser.equals(SQL_PLACE_HOLDER)){
//            sb.append("User: ").append(authUser).append(" ");
//        }
//        for (int i = 0; i < keys.size(); i++) {
//            String key = keys.get(i);
//            sb.append(key);
//
//            if(values[i] == null || values[i].equals(SQL_PLACE_HOLDER)){
//                throw new IllegalArgumentException("A value for each key must be supplied");
//            }
//            sb.append(": ").append(values[i]).append(" ");
//        }
//
//        return sb.toString();
//    }

    public static String getUsageColumnHeader(String mappingValue){
        if(mappingValue == null) throw new NullPointerException("Parameter mappingValue cannot be null");
        return mappingValue.replaceAll(";","").trim();
    }


    /**
     *
     * @param useUser
     * @param keys
     * @return
     */
    public static Document getUsageSubReportRuntimeDoc(boolean useUser, List<String> keys,
                                                       LinkedHashSet<List<String>> distinctMappingSets) {
        if((keys == null || keys.isEmpty()) && !useUser){
            throw new IllegalArgumentException("keys must not be null or empty if useUser is not true");
        }

        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);
        
        if(distinctMappingValues == null || distinctMappingValues.isEmpty()){
            distinctMappingValues = new LinkedHashSet<String>();
        }

        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        int numMappingValues = distinctMappingValues.size();

        Node rootNode = doc.getFirstChild();

        //Create variables element
        Element variables = doc.createElement(VARIABLES);
        rootNode.appendChild(variables);

        for(int i = 0; i < numMappingValues; i++){
            addVariableToElement(doc, variables, "COLUMN_"+(i+1), "java.lang.Long","None", null, "Nothing", "getColumnValue", "COLUMN_"+(i+1));
        }

        Element serviceAndOperationFooterElement = doc.createElement(SERVICE_AND_OPERATION_FOOTER);
        rootNode.appendChild(serviceAndOperationFooterElement);

        int xPos = 0;
        int yPos = 0;
        
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-"+(i+1), "java.lang.Long", "$V{COLUMN_"+(i+1)+"}",
                    TABLE_CELL_STYLE, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal", "java.lang.Long", "$V{TOTAL}",
                ROW_TOTAL_STYLE, true);

        Element noDataElement = doc.createElement(NO_DATA);
        rootNode.appendChild(noDataElement);

        xPos = 0;

        for(int i = 0; i < numMappingValues; i++){
            addStaticTextToElement(doc, noDataElement, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "noDataStaticText-"+(i+1), "NA", TABLE_CELL_STYLE, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        addStaticTextToElement(doc, noDataElement, xPos, yPos, TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "noDataStaticText-Total", "NA", ROW_TOTAL_STYLE, true);
        xPos += TOTAL_COLUMN_WIDTH;

        //frame width is the same as page width for the subreport
        Element pageWidth = doc.createElement(PAGE_WIDTH);
        rootNode.appendChild(pageWidth);
        pageWidth.setTextContent(String.valueOf(xPos));

        return doc;
    }

    /**
     *
     * @param useUser
     * @param keys
     * @return
     */
    public static Document getUsageSubIntervalMasterRuntimeDoc(boolean useUser, List<String> keys,
                                                               LinkedHashSet<List<String>> distinctMappingSets) {
        if((keys == null || keys.isEmpty()) && !useUser){
            throw new IllegalArgumentException("keys must not be null or empty if useUser is not true");
        }

        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);

        if(distinctMappingValues == null || distinctMappingValues.isEmpty()){
            distinctMappingValues = new LinkedHashSet<String>();
        }

        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        int numMappingValues = distinctMappingValues.size();

        Node rootNode = doc.getFirstChild();

        //Create variables element
        Element variables = doc.createElement(VARIABLES);
        rootNode.appendChild(variables);

        //Create the COLUMN_X variables first
        for(int i = 0; i < numMappingValues; i++){
            //<variable name="COLUMN_1" class="java.lang.Long" resetType="Report" calculation="Sum">
            addVariableToElement(doc, variables, "COLUMN_"+(i+1), "java.lang.Long", "Report", null, "Sum");
        }

        //Subreport return values
        //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_1" calculation="Sum"/>
        Element subReport = doc.createElement(SUB_REPORT);
        rootNode.appendChild(subReport);

        for(int i = 0; i < numMappingValues; i++){
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_1" calculation="Sum"/>
            addSubReportReturnVariable(doc, subReport, "COLUMN_"+(i+1), "COLUMN_"+(i+1), "Sum");            
        }

        //determine how wide the sub report should be
        int subReportWidth = 0;
        for(int i = 0; i < numMappingValues; i++){
            subReportWidth += DATA_COLUMN_WIDTH;
        }
        subReportWidth += TOTAL_COLUMN_WIDTH;

        Element subReportWidthElement = doc.createElement(SUB_REPORT_WIDTH);
        rootNode.appendChild(subReportWidthElement);
        subReportWidthElement.setTextContent(String.valueOf(subReportWidth));

        int pageWidth = subReportWidth + SUB_INTERVAL_STATIC_WIDTH;
        Element pageWidthElement = doc.createElement(PAGE_WIDTH);
        rootNode.appendChild(pageWidthElement);
        pageWidthElement.setTextContent(String.valueOf(pageWidth));

        return doc;
    }

    private static String getContextKeysDiaplayString(boolean useUser, List<String> keys){
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if(useUser){
            sb.append("Auth User");
            first = false;
        }

        for (int i = 0; i < keys.size(); i++) {
            String s = keys.get(i);
            if (!first) {
                if (i != keys.size()-1 ) sb.append(", ");
                else sb.append(" and ");
            }
            first = false;
            sb.append(s);
        }
        return sb.toString();
    }


    /**
     * Get the runtime doc for performance statistics reports. Need to know what size to make the chart, create data
     * for it's legend and remove unnecessary chart elements
     * @param isContextMapping are mapping keys being used or not
     * @param groupToMappingValue a map of a shortened string to the string representing a set of mapping values
     * to display as the category value in a chart e.g. group 1 instead of IpAddress=...Customer=..., or service 1
     * instead of Warehouse [routing uri].....
     * @return
     */
    public static Document getPerfStatAnyRuntimeDoc(boolean isContextMapping,
                                                               LinkedHashMap<String,String> groupToMappingValue){
        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        Node rootNode = doc.getFirstChild();
        //Create variables element
        Element isCtxMapElement = doc.createElement(IS_CONTEXT_MAPPING);
        rootNode.appendChild(isCtxMapElement);
        if(isContextMapping){
            isCtxMapElement.setTextContent("1");
        }else{
            isCtxMapElement.setTextContent("0");
        }

        //Create all the text fields for the chart legend
        Element chartLegend = doc.createElement(CHART_LEGEND);
        rootNode.appendChild(chartLegend);
        int x = 0;
        int y = 0;
        int vSpace = 2;
        int height = 18;
        int frameWidth = FRAME_MIN_WIDTH;

        int index = 0;
        for (Map.Entry<String, String> me : groupToMappingValue.entrySet()) {
            addTextFieldToElement(doc, chartLegend, x, y, frameWidth, height, "chartLegendKey"+(index+1), "java.lang.String",
                    "<b>"+me.getKey()+":</b> " + me.getValue(), "chartLegendTextField", false);

            y += height + vSpace;
            index++;
        }

        //Chart height is minimum 130, if there are more than 2 mapping value sets then increase it
        int chartHeight = 130;
        int numMappingSets = groupToMappingValue.size();
        if(numMappingSets > 2){
            chartHeight += 30 * (numMappingSets -2);            
        }

        Element chartHeightElement = doc.createElement(CHART_HEIGHT);
        rootNode.appendChild(chartHeightElement);
        chartHeightElement.setTextContent(String.valueOf(chartHeight));

        //start of chart legend = chart height + 18 for the title of the chart frame
        int chartLegendFrameYPos = chartHeight;// + height;
        Element chartLegendYPosElement = doc.createElement(CHART_LEGEND_FRAME_YPOS);
        rootNode.appendChild(chartLegendYPosElement);
        chartLegendYPosElement.setTextContent(String.valueOf(chartLegendFrameYPos));

        //height of chart legend = num mapping sets * height + vSpace
        int chartLegendHeight = numMappingSets * (height + vSpace);
        Element chartLegendHeightElement = doc.createElement(CHART_LEGEND_HEIGHT);
        rootNode.appendChild(chartLegendHeightElement);
        chartLegendHeightElement.setTextContent(String.valueOf(chartLegendHeight));

        int chartFrameHeight = chartHeight + 18 + chartLegendHeight;
        Element chartFrameHeightElement = doc.createElement(CHART_FRAME_HEIGHT);
        rootNode.appendChild(chartFrameHeightElement);
        chartFrameHeightElement.setTextContent(String.valueOf(chartFrameHeight));

        //Calculate the height of the band
        int bandHeight = chartFrameHeight + height + height;//18 from the summary frame + 18 for a gap
        Element bandHeightElement = doc.createElement(BAND_HEIGHT);
        rootNode.appendChild(bandHeightElement);
        bandHeightElement.setTextContent(String.valueOf(bandHeight));

        int titleHeight = 186;
        int margins = 20 + 20;
        int totalFirstPageHeight = titleHeight + margins + bandHeight;
        int minPageHeight = 595;
        if(totalFirstPageHeight < minPageHeight) totalFirstPageHeight = minPageHeight;

        Element pageHeightElement = doc.createElement(PAGE_HEIGHT);
        rootNode.appendChild(pageHeightElement);
        pageHeightElement.setTextContent(String.valueOf(totalFirstPageHeight));

        return doc;
    }

    /**
     * Create a document, given the input properties, which will be used to transform the
     * template usgae report.
     * @param keys The mapping keys selected to be included in the report. Used for creating display elements
     * @param useUser if true, then the report header will include a display item for 'Authenticated User'
     * @return the Document returned is not formatted
     */
    public static Document getUsageIntervalMasterRuntimeDoc(boolean useUser, List<String> keys,
                                              LinkedHashSet<List<String>> distinctMappingSets) {
        if((keys == null || keys.isEmpty()) && !useUser){
            throw new IllegalArgumentException("keys must not be null or empty if useUser is not true");
        }

        LinkedHashSet<String> mappingValuesLegend = Utilities.getMappingLegendValues(keys, distinctMappingSets);        
        /*
        * distinctMappingValues The set of distinct mapping values, which were determined earlier based on
        * the users selection of keys, key values, time and other constraints. Each string in the set is the
        * concatanated value of authenticated user, mapping1_value, mapping2_value, mapping3_value, mapping4_value
        * and mapping5_value.
        */
        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);
        
        if(distinctMappingValues == null || distinctMappingValues.isEmpty()){
            distinctMappingValues = new LinkedHashSet<String>();
        }

        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        int numMappingValues = distinctMappingValues.size();

        Node rootNode = doc.getFirstChild();
        //Create variables element
        Element variables = doc.createElement(VARIABLES);
        rootNode.appendChild(variables);

        //Create the COLUMN_X variables first
        for(int i = 0; i < numMappingValues; i++){
            //<variable name="COLUMN_SERVICE_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE" calculation="Sum">
            addVariableToElement(doc, variables, "COLUMN_SERVICE_"+(i+1), "java.lang.Long", "Group", "SERVICE", "Sum");
        }

        for(int i = 0; i < numMappingValues; i++){
            //<variable name="COLUMN_OPERATION_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE_OPERATION" calculation="Sum">
            addVariableToElement(doc, variables, "COLUMN_OPERATION_"+(i+1), "java.lang.Long", "Group", "SERVICE_OPERATION", "Sum");
        }

        for(int i = 0; i < numMappingValues; i++){
            //<variable name="COLUMN_REPORT_1" class="java.lang.Long" resetType="Report" calculation="Sum">
            addVariableToElement(doc, variables, "COLUMN_REPORT_"+(i+1), "java.lang.Long", "Report", null, "Sum");
        }

        //serviceHeader
        Element serviceHeader = doc.createElement(SERVICE_HEADER);
        rootNode.appendChild(serviceHeader);

        int xPos = CONSTANT_HEADER_START_X;
        int yPos = 0;

        String keyDisplayValue = getContextKeysDiaplayString(useUser, keys);
        Element keyInfoElement = doc.createElement("keyInfo");
        rootNode.appendChild(keyInfoElement);
        CDATASection cData = doc.createCDATASection(keyDisplayValue);
        keyInfoElement.appendChild(cData);

        List<String> listMappingValues = new ArrayList<String>();
        listMappingValues.addAll(distinctMappingValues);

        //add a text field for each column
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, serviceHeader, xPos, yPos, DATA_COLUMN_WIDTH, MAPPING_VALUE_FIELD_HEIGHT,
                    "textField-serviceHeader-"+(i+1), "java.lang.String", listMappingValues.get(i), USAGE_TABLE_HEADING_STYLE,
                    true);
            xPos += DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceHeader, xPos, yPos, TOTAL_COLUMN_WIDTH, MAPPING_VALUE_FIELD_HEIGHT,
                "textField-serviceHeader-ServiceTotals", "java.lang.String", "Service Totals", USAGE_TABLE_HEADING_STYLE,
                true);

        xPos += TOTAL_COLUMN_WIDTH;

        int docTotalWidth = xPos + LEFT_MARGIN_WIDTH + RIGHT_MARGIN_WIDTH;
        int frameWidth = xPos;
        Element pageWidth = doc.createElement(PAGE_WIDTH);
        pageWidth.setTextContent(String.valueOf(docTotalWidth));
        Element columnWidthElement = doc.createElement(COLUMN_WIDTH);
        columnWidthElement.setTextContent(String.valueOf(frameWidth));
        Element frameWidthElement = doc.createElement(FRAME_WIDTH);
        frameWidthElement.setTextContent(String.valueOf(frameWidth));
        
        //sub report variables
        Element subReport = doc.createElement(SUB_REPORT);
        rootNode.appendChild(subReport);

        for(int i = 0; i < numMappingValues; i++){
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_SERVICE_1" calculation="Sum"/>
            addSubReportReturnVariable(doc, subReport, "COLUMN_"+(i+1), "COLUMN_SERVICE_"+(i+1), "Sum");            
        }

        for(int i = 0; i < numMappingValues; i++){
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_OPERATION_1" calculation="Sum"/>
            addSubReportReturnVariable(doc, subReport, "COLUMN_"+(i+1), "COLUMN_OPERATION_"+(i+1), "Sum");
        }
        
        for(int i = 0; i < numMappingValues; i++){
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_REPORT_1" calculation="Sum"/>
            addSubReportReturnVariable(doc, subReport, "COLUMN_"+(i+1), "COLUMN_REPORT_"+(i+1), "Sum");
        }

        //SERVICE_OPERATION footer
        Element serviceAndOperationFooterElement = doc.createElement(SERVICE_AND_OPERATION_FOOTER);
        rootNode.appendChild(serviceAndOperationFooterElement);
        //todo [Donal] this is out by 5, 113 instead of 118, affects summary and master transforms, fix when reports completed
        xPos = CONSTANT_HEADER_START_X;
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-"+(i+1), "java.lang.Long", "$V{COLUMN_OPERATION_"+(i+1)+"}",
                    ROW_TOTAL_STYLE, true);
            xPos += DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal", "java.lang.Long", "$V{ROW_OPERATION_TOTAL}",
                ROW_TOTAL_STYLE, true);

        //serviceIdFooter
        Element serviceIdFooterElement = doc.createElement(SERVICE_ID_FOOTER);
        rootNode.appendChild(serviceIdFooterElement);

        xPos = CONSTANT_HEADER_START_X;
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, serviceIdFooterElement, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceIdFooter-"+(i+1), "java.lang.Long", "$V{COLUMN_SERVICE_"+(i+1)+"}",
                    ROW_TOTAL_STYLE, true);
            xPos += DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceIdFooterElement, xPos, yPos, TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceIdFooterTotal", "java.lang.Long", "$V{ROW_SERVICE_TOTAL}", ROW_TOTAL_STYLE, true);

        //summary
        Element summaryElement = doc.createElement(SUMMARY);
        rootNode.appendChild(summaryElement);

        xPos = CONSTANT_HEADER_START_X;
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, summaryElement, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-constantFooter-"+(i+1), "java.lang.Long", "$V{COLUMN_REPORT_"+(i+1)+"}",
                    ROW_TOTAL_STYLE, true);
            xPos += DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, summaryElement, xPos, yPos, TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-constantFooterTotal", "java.lang.Long", "$V{ROW_REPORT_TOTAL}", ROW_TOTAL_STYLE, true);

        rootNode.appendChild(pageWidth);
        //columnWidth -is page width - left + right margin
        rootNode.appendChild(columnWidthElement);
        rootNode.appendChild(frameWidthElement);
        Element leftMarginElement = doc.createElement(LEFT_MARGIN);
        leftMarginElement.setTextContent(String.valueOf(LEFT_MARGIN_WIDTH));
        rootNode.appendChild(leftMarginElement);

        Element rightMarginElement = doc.createElement(RIGHT_MARGIN);
        rightMarginElement.setTextContent(String.valueOf(LEFT_MARGIN_WIDTH));
        rootNode.appendChild(rightMarginElement);

        LinkedHashMap<String, String> groupToLegendDisplayStringMap = getGroupToLegendDisplayStringMap(mappingValuesLegend);
        addChartXMLToDocument(doc, groupToLegendDisplayStringMap, frameWidth, 595);
        
        return doc;
    }

    private static LinkedHashSet<String> getMappingValues(LinkedHashSet<List<String>> distinctMappingSets){
        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();

        for(List<String> set: distinctMappingSets){
            List<String> mappingStrings = new ArrayList<String>();
            boolean first = true;
            String authUser = null;
            for(String s: set){
                if(first){
                    authUser = s;
                    first = false;
                    continue;
                }
                mappingStrings.add(s);
            }
            String mappingValue = Utilities.getMappingValueString(authUser, mappingStrings.toArray(new String[]{}));
            mappingValues.add(mappingValue);
        }

        return mappingValues;
    }

    public static LinkedHashMap<String, String> getKeyToColumnValues(LinkedHashSet<List<String>> distinctMappingSets) {
        LinkedHashSet<String> mappingValues = getMappingValues(distinctMappingSets);
        LinkedHashMap<String, String> keyToColumnName = new LinkedHashMap<String, String>();
        int count = 1;
        //System.out.println("Key to column map");
        for (String s : mappingValues) {
            keyToColumnName.put(s, "COLUMN_"+count);
            //System.out.println(s+" " + "COLUMN_"+count);
            count++;
        }
        return keyToColumnName;
    }

    public static LinkedHashMap<Integer, String> getGroupIndexToGroupString(LinkedHashSet<String> mappingValuesLegend){
        LinkedHashMap<Integer, String> groupIndexToGroup = new LinkedHashMap<Integer, String>();
        int index = 1;
        for(String s: mappingValuesLegend){
            String group = "Group "+index;
            groupIndexToGroup.put(index, group);
            index++;
        }
        return groupIndexToGroup;
    }

    public static LinkedHashMap<String, String> getLegendDisplayStringToGroupMap(LinkedHashSet<String> mappingValuesLegend){
        LinkedHashMap<String, String> displayStringToGroup = new LinkedHashMap<String, String>();
        int index = 1;
        for(String s: mappingValuesLegend){
            String group = "Group "+index;
            displayStringToGroup.put(s, group);
            index++;
        }

        return displayStringToGroup;
    }


    private static LinkedHashMap<String, String> getGroupToLegendDisplayStringMap(LinkedHashSet<String> mappingValuesLegend){
        LinkedHashMap<String, String> groupToDisplayString = new LinkedHashMap<String, String>();
        int index = 1;
        for(String s: mappingValuesLegend){
            String group = "Group "+index;
            groupToDisplayString.put(group, s);
            index++;
        }

        return groupToDisplayString;
    }

    /**
     * Create a document, given the input properties, which will be used to transform the
     * template usgae report.
     * @param keys The mapping keys selected to be included in the report. Used for creating display elements
     * @param useUser if true, then the report header will include a display item for 'Authenticated User'
     * @return the Document returned is not formatted
     */
    public static Document getUsageRuntimeDoc(boolean useUser, List<String> keys,
                                              LinkedHashSet<List<String>> distinctMappingSets) {
        if((keys == null || keys.isEmpty()) && !useUser){
            throw new IllegalArgumentException("keys must not be null or empty if useUser is not true");
        }

        LinkedHashSet<String> mappingValuesLegend = Utilities.getMappingLegendValues(keys, distinctMappingSets);

        /*
        * distinctMappingValues The set of distinct mapping values, which were determined earlier based on
        * the users selection of keys, key values, time and other constraints. Each string in the set is the
        * concatanated value of authenticated user, mapping1_value, mapping2_value, mapping3_value, mapping4_value
        * and mapping5_value.
        */

        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);

        if(distinctMappingValues == null || distinctMappingValues.isEmpty()){
            distinctMappingValues = new LinkedHashSet<String>();
        }
        
        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);

        int numMappingValues = distinctMappingValues.size();

        Node rootNode = doc.getFirstChild();
        //Create variables element
        Element variables = doc.createElement(VARIABLES);
        rootNode.appendChild(variables);

        //Create the COLUMN_X variables first
        for(int i = 0; i < numMappingValues; i++){
            addVariableToElement(doc, variables, "COLUMN_"+(i+1), "java.lang.Long", "Group", "SERVICE_AND_OPERATION",
                    "Nothing", "getColumnValue", "COLUMN_"+(i+1));
        }

        //then the COLUMN_MAPPING_TOTAL_X variables
        for(int i = 0; i < numMappingValues; i++){
            addVariableToElement(doc, variables, "COLUMN_MAPPING_TOTAL_"+(i+1), "java.lang.Long", "Group", "CONSTANT",
                    "Sum", "getVariableValue", "COLUMN_"+(i+1));
        }

        //then the COLUMN_SERVICE_TOTAL_X variables
        for(int i = 0; i < numMappingValues; i++){
            addVariableToElement(doc, variables, "COLUMN_SERVICE_TOTAL_"+(i+1), "java.lang.Long", "Group", "SERVICE_ID",
                    "Sum", "getVariableValue", "COLUMN_"+(i+1));
        }

        //create constantHeader element
        Element constantHeader = doc.createElement(CONSTANT_HEADER);
        rootNode.appendChild(constantHeader);

        //Constant header starts at x = 113
        //The widths for the entire document are determined from this first header.
        //it has slightly different make up as it has an additional text field, however all other headers
        //should always work out to the be the same width. If they are wider, the report will not compile
        int xPos = CONSTANT_HEADER_START_X;
        int yPos = 0;

        String keyDisplayValue = getContextKeysDiaplayString(useUser, keys);
        Element keyInfoElement = doc.createElement("keyInfo");
        rootNode.appendChild(keyInfoElement);
        CDATASection cData = doc.createCDATASection(keyDisplayValue);
        keyInfoElement.appendChild(cData);

        List<String> listMappingValues = new ArrayList<String>();
        listMappingValues.addAll(distinctMappingValues);

        //add a text field for each column
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, constantHeader, xPos, yPos, DATA_COLUMN_WIDTH, MAPPING_VALUE_FIELD_HEIGHT,
                    "textField-constantHeader-"+(i+1), "java.lang.String", listMappingValues.get(i), USAGE_TABLE_HEADING_STYLE,
                    false);
            xPos += DATA_COLUMN_WIDTH;
        }
        //move x pos along for width of a column

        addTextFieldToElement(doc, constantHeader, xPos, yPos, TOTAL_COLUMN_WIDTH, MAPPING_VALUE_FIELD_HEIGHT,
                "textField-constantHeader-ServiceTotals", "java.lang.String", "Service Totals", USAGE_TABLE_HEADING_STYLE,
                false);

        xPos += TOTAL_COLUMN_WIDTH;
        
        int docTotalWidth = xPos + LEFT_MARGIN_WIDTH + RIGHT_MARGIN_WIDTH;
        int frameWidth = xPos;
        Element pageWidth = doc.createElement(PAGE_WIDTH);
        pageWidth.setTextContent(String.valueOf(docTotalWidth));
        Element columnWidthElement = doc.createElement(COLUMN_WIDTH);
        columnWidthElement.setTextContent(String.valueOf(frameWidth));
        Element frameWidthElement = doc.createElement(FRAME_WIDTH);
        frameWidthElement.setTextContent(String.valueOf(frameWidth));

        //serviceAndOperationFooter
        Element serviceAndOperationFooterElement = doc.createElement(SERVICE_AND_OPERATION_FOOTER);
        rootNode.appendChild(serviceAndOperationFooterElement);
        xPos = CONSTANT_HEADER_START_X;
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-"+(i+1), "java.lang.Long", "$V{COLUMN_"+(i+1)+"}",
                    TABLE_CELL_STYLE, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal", "java.lang.Long", "$V{SERVICE_AND_OR_OPERATION_TOTAL}",
                ROW_TOTAL_STYLE, true);

        //serviceIdFooter
        Element serviceIdFooterElement = doc.createElement(SERVICE_ID_FOOTER);
        rootNode.appendChild(serviceIdFooterElement);

        xPos = CONSTANT_HEADER_START_X;
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, serviceIdFooterElement, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceIdFooter-"+(i+1), "java.lang.Long", "$V{COLUMN_SERVICE_TOTAL_"+(i+1)+"}",
                    ROW_TOTAL_STYLE, true);
            xPos += DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceIdFooterElement, xPos, yPos, TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceIdFooterTotal", "java.lang.Long", "$V{SERVICE_ONLY_TOTAL}", ROW_TOTAL_STYLE, true);

        //constantFooter
        Element constantFooterElement = doc.createElement(CONSTANT_FOOTER);
        rootNode.appendChild(constantFooterElement);

        xPos = CONSTANT_HEADER_START_X;
        for(int i = 0; i < numMappingValues; i++){
            addTextFieldToElement(doc, constantFooterElement, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-constantFooter-"+(i+1), "java.lang.Long", "$V{COLUMN_MAPPING_TOTAL_"+(i+1)+"}",
                    ROW_TOTAL_STYLE, true);
            xPos += DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, constantFooterElement, xPos, yPos, TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-constantFooterTotal", "java.lang.Long", "$V{GRAND_TOTAL}", ROW_TOTAL_STYLE, true);


        rootNode.appendChild(pageWidth);
        //columnWidth -is page width - left + right margin
        rootNode.appendChild(columnWidthElement);
        rootNode.appendChild(frameWidthElement);
        Element leftMarginElement = doc.createElement(LEFT_MARGIN);
        leftMarginElement.setTextContent(String.valueOf(LEFT_MARGIN_WIDTH));
        rootNode.appendChild(leftMarginElement);

        Element rightMarginElement = doc.createElement(RIGHT_MARGIN);
        rightMarginElement.setTextContent(String.valueOf(LEFT_MARGIN_WIDTH));
        rootNode.appendChild(rightMarginElement);

        LinkedHashMap<String, String> groupToLegendDisplayStringMap = getGroupToLegendDisplayStringMap(mappingValuesLegend);
        addChartXMLToDocument(doc, groupToLegendDisplayStringMap, frameWidth, 595);
        return doc;
    }

    /**
     *
     * @param keys
     * @param distinctMappingSets the first value of each list is alwasys the authenticated user, followed by 5
     * mapping values
     * @return
     */
    public static LinkedHashSet<String> getMappingLegendValues(List<String> keys,
                                                        LinkedHashSet<List<java.lang.String>> distinctMappingSets){
        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();

        for(List<String> set: distinctMappingSets){
            List<String> mappingStrings = new ArrayList<String>();
            boolean first = true;
            String authUser = null;
            for(String s: set){
                if(first){
                    authUser = s;
                    first = false;
                    continue;
                }
                mappingStrings.add(s);
            }
            String mappingValue = Utilities.getMappingValueDisplayString(keys,
                    authUser,
                    mappingStrings.toArray(new String[]{}), false, null);
            mappingValues.add(mappingValue);
        }

        return mappingValues;
    }

    private static void addChartXMLToDocument(Document doc, LinkedHashMap<String,String> groupToMappingValue,
                                              int frameWidth, int minPageHeight){
        //Create all the text fields for the chart legend
        Node rootNode = doc.getFirstChild();
        Element chartElement = doc.createElement(CHART_ELEMENT);
        rootNode.appendChild(chartElement);

        Element chartLegend = doc.createElement(CHART_LEGEND);
        chartElement.appendChild(chartLegend);
        int x = 0;
        int y = 0;
        int vSpace = 2;
        int height = 18;

        int index = 0;
        if(frameWidth < FRAME_MIN_WIDTH) frameWidth = FRAME_MIN_WIDTH;
        for (Map.Entry<String, String> me : groupToMappingValue.entrySet()) {

            addTextFieldToElement(doc, chartLegend, x, y, frameWidth, height, "chartLegendKey"+(index+1), "java.lang.String",
                    "<b>"+me.getKey()+":</b> " + me.getValue(), "chartLegendTextField", false);

            y += height + vSpace;
            index++;
        }

        //Chart height is minimum 130, if there are more than 2 mapping value sets then increase it
        int chartHeight = 130;
        int numMappingSets = groupToMappingValue.size();
        if(numMappingSets > 2){
            chartHeight += 30 * (numMappingSets -2);
        }

        Element chartHeightElement = doc.createElement(CHART_HEIGHT);
        chartElement.appendChild(chartHeightElement);
        chartHeightElement.setTextContent(String.valueOf(chartHeight));

        //start of chart legend = chart height + 18 for the title of the chart frame
        int chartLegendFrameYPos = chartHeight;// + height;
        Element chartLegendYPosElement = doc.createElement(CHART_LEGEND_FRAME_YPOS);
        chartElement.appendChild(chartLegendYPosElement);
        chartLegendYPosElement.setTextContent(String.valueOf(chartLegendFrameYPos));

        //height of chart legend = num mapping sets * height + vSpace
        int chartLegendHeight = numMappingSets * (height + vSpace);
        Element chartLegendHeightElement = doc.createElement(CHART_LEGEND_HEIGHT);
        chartElement.appendChild(chartLegendHeightElement);
        chartLegendHeightElement.setTextContent(String.valueOf(chartLegendHeight));

        int chartFrameHeight = chartHeight + 18 + chartLegendHeight;
        Element chartFrameHeightElement = doc.createElement(CHART_FRAME_HEIGHT);
        chartElement.appendChild(chartFrameHeightElement);
        chartFrameHeightElement.setTextContent(String.valueOf(chartFrameHeight));

        //Calculate the height of the band
        int bandHeight = chartFrameHeight + height + height;//18 from the summary frame + 18 for a gap
        Element bandHeightElement = doc.createElement(BAND_HEIGHT);
        chartElement.appendChild(bandHeightElement);
        bandHeightElement.setTextContent(String.valueOf(bandHeight));

        int titleHeight = 243;
        int margins = 20 + 20;
        int totalFirstPageHeight = titleHeight + margins + bandHeight + CONSTANT_HEADER_HEIGHT;
        if(totalFirstPageHeight < minPageHeight) totalFirstPageHeight = minPageHeight;

        Element pageHeightElement = doc.createElement(PAGE_HEIGHT);
        chartElement.appendChild(pageHeightElement);
        pageHeightElement.setTextContent(String.valueOf(totalFirstPageHeight));

    }


    public static String getMappingValueString(String authUser, String [] mappingValues){
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        if(!authUser.equals(Utilities.SQL_PLACE_HOLDER)){
            sb.append(authUser);
            first = false;
        }

        for(String s: mappingValues){
            if(!first){
                if(!s.equals(Utilities.SQL_PLACE_HOLDER)) sb.append("<br>");
            }
            first = false;
            if(!s.equals(Utilities.SQL_PLACE_HOLDER)) sb.append(s);
        }

        return sb.toString();
    }

    /**
     *
     * @param serviceIdToOperationMap
     * @param printOperations this should be the result of isDetail && isContextMapping from the report's params
     * @return
     */
    public static String getServiceAndIdDisplayString(Map serviceIdToOperationMap, Map serviceIdToNameMap, boolean printOperations){
        Map<String, Set<String>> sIdToOpMap = serviceIdToOperationMap;

        if(!printOperations){
            if(serviceIdToNameMap == null) serviceIdToNameMap = new HashMap();
            List sortedList = new ArrayList(serviceIdToNameMap.values());
            Collections.sort(sortedList);
            return getStringNamesFromCollection(sortedList);
        }

        if(serviceIdToOperationMap == null || serviceIdToOperationMap.isEmpty()) return "";
        

        Map<String, String> idToDisplayString = new HashMap<String, String>();
        for(Map.Entry<String, Set<String>> me: sIdToOpMap.entrySet()){
            String serviceName = (String) serviceIdToNameMap.get(me.getKey());
            StringBuilder sb = new StringBuilder();
            int index = 0;
            for(String s: me.getValue()){
                if(index != 0) sb.append(", ");
                sb.append(s);
                index++;
            }
            idToDisplayString.put(serviceName, sb.toString());
        }

        //sb.append(serviceName+": -> ");
        List<String> serviceNames = new ArrayList<String>(idToDisplayString.keySet());
        Collections.sort(serviceNames);

        StringBuilder sb = new StringBuilder();
        int rowIndex = 0;
        int maxRows = sIdToOpMap.size();
        for(String s: serviceNames){
            sb.append(s).append(" -> ").append(idToDisplayString.get(s));
            if(rowIndex < maxRows-1) sb.append("<br>");
            rowIndex++;
        }

        return sb.toString();
    }


    private static final int [] hexColours = new int []{0xFFDC5A, 0xD6D6D6, 0xE8EDB4};

    /**
     * Some class implementing JRChartCustomizer will call this method from customize() in order to find out what
     * color the chart series should be. The chart can't access configuration, so it's assumed for the moment that
     * this class can. It specifies how many series colours it wants, and it should get back a list of strings, each
     * representing a unique html color code
     * @param howMany
     * @return
     */
    public static List<Color> getSeriesColours(int howMany){
        List<Color> colours = new ArrayList<Color>();
        for (int i = 0; i < hexColours.length; i++) {

            int l;
            if(i > hexColours.length){
                l = hexColours[i % hexColours.length];
                l = l + (i * 20) +20;
            }else{
                l = hexColours[i];
            }
            Color c = new Color(l);
            colours.add(c);
        }
        return colours;
    }

    /**
     *         <textField isStretchWithOverflow="true" isBlankWhenNull="false" evaluationTime="Now"
                   hyperlinkType="None" hyperlinkTarget="Self">
            <reportElement
                    x="50"
                    y="0"
                    width="68"
                    height="36"
                    key="textField-MappingKeys"/>
            <box></box>
            <textElement markup="html">
                <font/>
            </textElement>
            <textFieldExpression class="java.lang.String">
                <![CDATA["IP_ADDRESS<br>CUSTOMER"]]></textFieldExpression>
        </textField>

     * @param doc
     * @param frameElement
     * @param x
     * @param y
     * @param width
     * @param height
     * @param key
     * @param textFieldExpressionClass
     * @param markedUpCData
     */
    private static void addTextFieldToElement(Document doc, Element frameElement, int x, int y, int width, int height,
                                              String key, String textFieldExpressionClass, String markedUpCData,
                                              String style, boolean opaque){
        Element textField = doc.createElement("textField");
        textField.setAttribute("isStretchWithOverflow","true");
        textField.setAttribute("isBlankWhenNull","false");
        textField.setAttribute("evaluationTime","Now");
        textField.setAttribute("hyperlinkType","None");
        textField.setAttribute("hyperlinkTarget","Self");

        Element reportElement = doc.createElement("reportElement");
        reportElement.setAttribute("x", String.valueOf(x));
        reportElement.setAttribute("y",String.valueOf(y));
        reportElement.setAttribute("width",String.valueOf(width));
        reportElement.setAttribute("height",String.valueOf(height));
        reportElement.setAttribute("key",key);
        reportElement.setAttribute("style", style);
        if(opaque) reportElement.setAttribute("mode", "Opaque");

        textField.appendChild(reportElement);

        Element boxElement = doc.createElement("box");
        textField.appendChild(boxElement);

        Element textElement = doc.createElement("textElement");
        textElement.setAttribute("markup","html");
        Element fontElement = doc.createElement("font");
        textElement.appendChild(fontElement);
        textField.appendChild(textElement);

        Element textFieldExpressionElement = doc.createElement("textFieldExpression");
        textFieldExpressionElement.setAttribute("class",textFieldExpressionClass);

        CDATASection cDataSection;
        if(textFieldExpressionClass.equals("java.lang.String")){
            cDataSection = doc.createCDATASection("\""+markedUpCData+"\"");
        }else{
            cDataSection = doc.createCDATASection(markedUpCData);            
        }

        textFieldExpressionElement.appendChild(cDataSection);
        textField.appendChild(textFieldExpressionElement);

        frameElement.appendChild(textField);
    }


    /**
     *         <staticText>
            <reportElement
                x="0"
                y="0"
                width="50"
                height="17"
                key="staticText-1"/>
            <box></box>
            <textElement>
                <font/>
            </textElement>
        <text><![CDATA[NA]]></text>
        </staticText>

     * @param doc
     * @param frameElement
     * @param x
     * @param y
     * @param width
     * @param height
     * @param key
     * @param markedUpCData
     * @param style
     */
    private static void addStaticTextToElement(Document doc, Element frameElement, int x, int y, int width, int height,
                                              String key, String markedUpCData, String style, boolean opaque){
        Element staticText = doc.createElement("staticText");

        Element reportElement = doc.createElement("reportElement");
        reportElement.setAttribute("x", String.valueOf(x));
        reportElement.setAttribute("y",String.valueOf(y));
        reportElement.setAttribute("width",String.valueOf(width));
        reportElement.setAttribute("height",String.valueOf(height));
        reportElement.setAttribute("key",key);
        reportElement.setAttribute("style", style);
        if(opaque) reportElement.setAttribute("mode", "Opaque");
        staticText.appendChild(reportElement);

        Element boxElement = doc.createElement("box");
        staticText.appendChild(boxElement);

        Element textElement = doc.createElement("textElement");
        textElement.setAttribute("markup","html");
        Element fontElement = doc.createElement("font");
        textElement.appendChild(fontElement);
        staticText.appendChild(textElement);

        Element text = doc.createElement("text");
        CDATASection cDataSection = doc.createCDATASection(markedUpCData);
        text.appendChild(cDataSection);
        staticText.appendChild(text);
        frameElement.appendChild(staticText);
    }


    /**
     *
     *         <variable name="COLUMN_1_MAPPING_TOTAL" class="java.lang.Long" resetType="Group" resetGroup="CONSTANT"
                  calculation="Sum">
            <variableExpression><![CDATA[((UsageReportHelper)$P{REPORT_SCRIPTLET})
    .getVariableValue("COLUMN_1", $F{AUTHENTICATED_USER},
    new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3},
    $F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})]]></variableExpression>
        </variable>
 
     * @param doc
     * @param variables
     * @param varName
     * @param varClass
     * @param resetType
     * @param resetGroup
     * @param calc
     * @param functionName
     */
    private static void addVariableToElement(Document doc, Element variables, String varName, String varClass,
                                             String resetType, String resetGroup, String calc, String functionName,
                                             String columnName){
        Element newVariable = doc.createElement(VARIABLE);
        newVariable.setAttribute("name", varName);
        newVariable.setAttribute("class", varClass);
        newVariable.setAttribute("resetType", resetType);
        if(resetGroup != null && !resetGroup.equals("")) newVariable.setAttribute("resetGroup", resetGroup);
        newVariable.setAttribute("calculation", calc);

        Element variableExpression = doc.createElement("variableExpression");
        String cData = "((UsageSummaryAndSubReportHelper)$P{REPORT_SCRIPTLET})."+functionName+"(\""+columnName+"\"," +
                " $F{AUTHENTICATED_USER},new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3}," +
                "$F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})";
        CDATASection cDataSection = doc.createCDATASection(cData);
        variableExpression.appendChild(cDataSection);

        newVariable.appendChild(variableExpression);
        variables.appendChild(newVariable);

    }

    /**
     * <variable name="COLUMN_SERVICE_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE" calculation="Sum">
     * @param doc
     * @param variables
     * @param varName
     * @param varClass
     * @param resetType
     * @param resetGroup
     * @param calc
     */
    private static void addVariableToElement(Document doc, Element variables, String varName, String varClass,
                                             String resetType, String resetGroup, String calc){
        Element newVariable = doc.createElement(VARIABLE);
        newVariable.setAttribute("name", varName);
        newVariable.setAttribute("class", varClass);
        newVariable.setAttribute("resetType", resetType);
        if(resetGroup != null && !resetGroup.equals("")) newVariable.setAttribute("resetGroup", resetGroup);
        newVariable.setAttribute("calculation", calc);
        variables.appendChild(newVariable);

    }

    /**
     * <returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_SERVICE_1" calculation="Sum"/>
     * @param doc
     * @param subReport
     * @param subreportVariable
     * @param toVariable
     * @param calc
     */
    private static void addSubReportReturnVariable(Document doc, Element subReport, String subreportVariable,
                                                   String toVariable, String calc){
        Element newVariable = doc.createElement(RETURN_VALUE);
        newVariable.setAttribute("subreportVariable", subreportVariable);
        newVariable.setAttribute("toVariable", toVariable);
        newVariable.setAttribute("calculation", calc);
        subReport.appendChild(newVariable);
    }
}

