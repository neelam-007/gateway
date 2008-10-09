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

    /**
     * The ';' character is used as a placeholder for sql column values, primiarly because no operation name of
     * value ';' is valid in a wsdl.
     */
    public static final String SQL_PLACE_HOLDER =  ";";

    public static final String HOUR = "HOUR";
    public static final String DAY = "DAY";
    public static final String WEEK = "WEEK";
    public static final String MONTH = "MONTH";
    private static final int NUM_MAPPING_KEYS = 5;

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
     * Get the resolution to use in queries. Used in a summary report
     * If the difference between the startTimeMilli and endTimeMilli > hourRetentionPeriod * num milli seconds in a day,
     * then the daily bin resolution is used, otherwise hourly is used.
     * @param startTimeMilli start of time period, in milliseconds, since epoch
     * @param endTimeMilli end of time period, in milliseconds, since epoch
     * @param hourRetentionPeriod SSG's current hourly bin max retention policy value
     * @return
     */
    public static Integer getResolutionFromTimePeriod(Integer hourRetentionPeriod, Long startTimeMilli, Long endTimeMilli){

        long duration = endTimeMilli - startTimeMilli;
        long dayMillis = getMilliSecondsForTimeUnit(Utilities.DAY);
        long maxHourRenentionMilli = dayMillis * hourRetentionPeriod;
        if(duration > maxHourRenentionMilli){
            return new Integer(2);
        }else{
            return new Integer(1);
        }
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

    /**
     * Get the absolute period of time for either HOUR, DAY or WEEK
     * @param unitOfTime HOUR, DAY or WEEK. Month not supported as a month does not have a fixed number of milliseconds
     * @return long representing the absolute time unit in milli seconds
     * @throws IllegalArgumentException if an incorrect unit of time is supplied
     */
    private static long getMilliSecondsForTimeUnit(String unitOfTime){

        if(unitOfTime.equals("HOUR")){
            return 3600000L;
        }else if(unitOfTime.equals("DAY")){
            return 86400000L;
        }else if(unitOfTime.equals("WEEK")){
            return 604800000L;
        }else{
            throw new IllegalArgumentException("Unsupported unitOfTime: " + unitOfTime+ " Supported units are " +
                    "HOUR, DAY and WEEK");
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
     *
     * This is the general query we are going to create.
     * What's dynamic in this query is the key's from message_context_message_keys table which we are including
     * in the query.
     * There are 4 main areas in this query:-
     * SECTION A: Select out the metrics as normal
     * SECTION B: For every key included in the query, we need its corresponding mapping_value from
     * message_context_message_values table. As the key can be used in any index we need to pick the correct column
     * to use for each key and give this derived column a value so we can group by it in section D
     * SECTION C: For every key included we need to add an OR block which as an entire block are added as an AND
     * constraint. All 5 mapping key's are checked, and corresponding values if we are filtering mapping values.
     * if none of the keys match the key specified the overall AND constraint will fail and the row will not be included
     * SECTION D: For every key included, we need to group by it's value derived in section B. This allows us to
     * get the data grouped to produce a data set for each distinct set of values together, for every key supplied
     *                       //todo [Donal] update the sql here, as it's now out of date
       **********SECTION A**********
     SELECT count(*) as count, p.objectid,
         SUM(if(smd.completed, smd.completed,0)) as THROUGHPUT,
         MIN(smd.front_min) as FRTM,
         MAX(smd.front_max) as FRTMX,
         if(SUM(smd.front_sum), if(SUM(smd.attempted), SUM(smd.front_sum)/SUM(smd.attempted),0), 0) as FRTA,
         MIN(smd.back_min) as BRTM,
         MAX(smd.back_max) as BRTMX,
         if(SUM(smd.back_sum), if(SUM(smd.completed), SUM(smd.back_sum)/SUM(smd.completed),0), 0) as BRTA,
         if(SUM(smd.attempted), ( 1.0 - ( ( (SUM(smd.authorized) - SUM(smd.completed)) / SUM(smd.attempted) ) ) ) , 0) as 'AP',

        **********SECTION B**********
     CASE WHEN mcmk.mapping1_key = 'IP_ADDRESS' THEN mcmv.mapping1_value
     WHEN mcmk.mapping1_key = 'IP_ADDRESS' THEN mcmv.mapping1_value
     WHEN mcmk.mapping2_key = 'IP_ADDRESS' THEN mcmv.mapping2_value
     WHEN mcmk.mapping3_key = 'IP_ADDRESS' THEN mcmv.mapping3_value
     WHEN mcmk.mapping4_key = 'IP_ADDRESS' THEN mcmv.mapping4_value
     WHEN mcmk.mapping5_key = 'IP_ADDRESS' THEN mcmv.mapping5_value
     END as IP_ADDRESS,
     CASE WHEN mcmk.mapping1_key = 'Customer' THEN mcmv.mapping1_value
     WHEN mcmk.mapping1_key = 'Customer' THEN mcmv.mapping1_value
     WHEN mcmk.mapping2_key = 'Customer' THEN mcmv.mapping2_value
     WHEN mcmk.mapping3_key = 'Customer' THEN mcmv.mapping3_value
     WHEN mcmk.mapping4_key = 'Customer' THEN mcmv.mapping4_value
     WHEN mcmk.mapping5_key = 'Customer' THEN mcmv.mapping5_value
     END as Customer
     FROM service_metrics sm, published_service p, service_metrics_details smd, message_context_mapping_values mcmv, message_context_mapping_keys mcmk
     WHERE p.objectid = sm.published_service_oid
     AND sm.objectid = smd.service_metrics_oid
     AND smd.mapping_values_oid = mcmv.objectid
     AND mcmv.mapping_keys_oid = mcmk.objectid
        **********SECTION C**********
     AND (
         (mcmk.mapping1_key = 'IP_ADDRESS')
         OR
         (mcmk.mapping2_key = 'IP_ADDRESS')
         OR
         (mcmk.mapping3_key = 'IP_ADDRESS')
         OR
         (mcmk.mapping4_key = 'IP_ADDRESS')
         OR
         (mcmk.mapping5_key = 'IP_ADDRESS')
         )
     AND
     (
         (mcmk.mapping1_key = 'Customer' )
         OR
         (mcmk.mapping2_key = 'Customer' )
         OR
         (mcmk.mapping3_key = 'Customer' )
         OR
         (mcmk.mapping4_key = 'Customer' )
         OR
         (mcmk.mapping5_key = 'Customer' )
     )
        **********SECTION D**********
     GROUP BY p.objectid, IP_ADDRESS, Customer


    /**
     * @param startTimeInclusiveMilli
     * @param endTimeInclusiveMilli
     * @param serviceIds
     * @param keys the list of keys representing the mapping keys
     * @param keyValueConstraints the values which each key must be equal to, Can be null or empty
     * @param valueConstraintAndOrLike for each key and value, if a value constraint exists as the index, the index into this
     * list dictitates whether an = or like constraint is applied. Can be null or empty. Cannot have values if
     * keyValueConstraints is null or empty
     * @param resolution 1 = hourly, 2 = daily. Which resolution from service_metrics to use
     * @param isDetail if true then the service_operation's real value is used in the select, group and order by,
     * otherwise operation is selected as 1. To facilitate this service_operation is always selected as
     * SERVICE_OPERATION_VALUE so that the real column is not used when isDetail is false
     * @return String query
     * @throws IllegalArgumentException If all the lists are not the same size and if they are empty.
     */
    public static String createMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Collection<String> serviceIds, List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            List<String> operations){

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, operations);

        if(valueConstraintAndOrLike == null) valueConstraintAndOrLike = new ArrayList<String>();
        
        StringBuilder sb = new StringBuilder(aggregateSelect);
        
        addOperationToSelect(isDetail, sb);

        addCaseSQL(keys, sb);

        sb.append(mappingJoin);

        addResolutionConstraint(resolution, sb);

        if(useTime){
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb);
        }

        if(serviceIds != null && !serviceIds.isEmpty()){
            addServiceIdConstraint(serviceIds, sb);
        }

        if(isDetail && operations != null && !operations.isEmpty()){
            addOperationConstraint(operations, sb);
        }

        if(keyValuesSupplied){
            addMappingConstraint(keys, keyValueConstraints, valueConstraintAndOrLike, sb);
        }

        addGroupBy(sb);

        addMappingOrder(sb);

        System.out.println(sb.toString());
        return sb.toString();
    }

    /**
     * Helper method to turn single service id and operation value into collections
     * @param startTimeInclusiveMilli
     * @param endTimeInclusiveMilli
     * @param serviceId
     * @param keys
     * @param keyValueConstraints
     * @param valueConstraintAndOrLike
     * @param resolution
     * @param isDetail
     * @param operation
     * @return
     */
    public static String createMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Long serviceId, List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            String operation){

        if(serviceId == null) throw new IllegalArgumentException("Service Id must be supplied");
        List<String> sIds = new ArrayList<String>();
        sIds.add(serviceId.toString());
        List<String> operationList = null;
        if(operation != null){
            operationList = new ArrayList<String>();
            operationList.add(operation);
        }
        return createMappingQuery(startTimeInclusiveMilli, endTimeInclusiveMilli, sIds, keys, keyValueConstraints,
                valueConstraintAndOrLike, resolution, isDetail, operationList);
    }

    private static void addOperationToSelect(boolean isDetail, StringBuilder sb) {
        if(isDetail){
            sb.append(",  mcmv.service_operation AS SERVICE_OPERATION_VALUE");
        }else{
            //sb.append(",  1 AS SERVICE_OPERATION_VALUE");
            sb.append(",  '" + SQL_PLACE_HOLDER + "' AS SERVICE_OPERATION_VALUE");
        }
    }

    private final static String distinctFrom = "SELECT distinct p.objectid as SERVICE_ID, p.name as SERVICE_NAME, " +
            "p.routing_uri as ROUTING_URI ";

    private final static String aggregateSelect = "SELECT count(*) as count, p.objectid as SERVICE_ID, " +
            "p.name as SERVICE_NAME, p.routing_uri as ROUTING_URI, " +
            "SUM(if(smd.completed, smd.completed,0)) as THROUGHPUT, MIN(smd.front_min) as FRTM, " +
            "MAX(smd.front_max) as FRTMX, if(SUM(smd.front_sum), if(SUM(smd.attempted), " +
            "SUM(smd.front_sum)/SUM(smd.attempted),0), 0) as FRTA, MIN(smd.back_min) as BRTM, " +
            "MAX(smd.back_max) as BRTMX, if(SUM(smd.back_sum), if(SUM(smd.completed), " +
            "SUM(smd.back_sum)/SUM(smd.completed),0), 0) as BRTA, " +
            "if(SUM(smd.attempted), ( 1.0 - ( ( (SUM(smd.authorized) - SUM(smd.completed)) / SUM(smd.attempted) ) ) ) , 0) as 'AP' ";

    private final static String mappingJoin = " FROM service_metrics sm, published_service p, service_metrics_details smd," +
            " message_context_mapping_values mcmv, message_context_mapping_keys mcmk WHERE p.objectid = sm.published_service_oid " +
            "AND sm.objectid = smd.service_metrics_oid AND smd.mapping_values_oid = mcmv.objectid AND mcmv.mapping_keys_oid = mcmk.objectid ";

    /**
     *
     * @param startTimeInclusiveMilli start of the time period to query for inclusive, can be null, so long as
     * endTimeInclusiveMilli is also null
     * @param endTimeInclusiveMilli end of the time period to query for exclusive, can be null, so long as
     * startTimeInclusiveMilli is also null
     * @param serviceIds service ids to constrain query with
     * @param keys mapping keys to use, must be at least 1
     * @param keyValueConstraints values to constrain possible key values with
     * @param valueConstraintAndOrLike and or like in sql for any values supplied as constraints on keys, can be null
     * and empty. If it is, then AND is used by default.
     * @param resolution hourly or daily = 1 or 2
     * @param isDetail true when the sql represents a detail master report, which means service_operation is included
     * in the distinct select list. When isDetail is true, the values in the operations list is used as a filter constraint.
     * @param operations if isDetail is true and operations is non null and non empty, then the sql returned
     * will be filtered by the values in operations
     * @return String sql
     * @throws NullPointerException if startIntervalMilliSeconds or endIntervalMilliSeconds is null
     */
    public static String createMasterMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                  Collection<String> serviceIds, List<String> keys,
                                                  List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            List<String> operations){
        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, operations);

        if(valueConstraintAndOrLike == null) valueConstraintAndOrLike = new ArrayList<String>();
        
        StringBuilder sb = new StringBuilder(distinctFrom);

        addCaseSQL(keys,sb);

        addOperationToSelect(isDetail, sb);

        sb.append(mappingJoin);

        addResolutionConstraint(resolution, sb);

        if(useTime){
            addTimeConstraint(startTimeInclusiveMilli, endTimeInclusiveMilli, sb);
        }

        if(serviceIds != null && !serviceIds.isEmpty()){
            addServiceIdConstraint(serviceIds, sb);
        }

        if(isDetail && operations != null && !operations.isEmpty()){
            addOperationConstraint(operations, sb);
        }
        
        if(keyValuesSupplied){//then the lengths have to match from above constraint
            addMappingConstraint(keys, keyValueConstraints, valueConstraintAndOrLike, sb);
        }

        addMappingOrder(sb);

        System.out.println(sb.toString());
        return sb.toString();
    }

    /**
     * Convert a list of string params into a list. This is needed by a sub query which is going to select out
     * aggregate values for SPECIFIC values of keys for a specific interval.
     * We are only interested in values of args, which have an index within the size of keys. Keys will hold up to
     * 5 values, and args WILL hold 5 values, currently. However some of args will just be placeholders, with the value
     * '1'. This is as all queries always include all mapping_value_x (x:1-5), so when less than 5 keys are used, then
     * their select value is just 1, so it has no affect on group and order by operations.
     * If any of the string values equal '1' indicating a placeholder, then null is added into that location
     * in the returned list
     * @param args String values for the keys
     * @return List representation of the args
     */
    public static List<String> createValueList(List<String> keys, String... args){

        List<String> returnList = new ArrayList<String>();

        for (int i = 0; i < args.length && i < keys.size(); i++) {
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
        sb.append(" GROUP BY p.objectid, SERVICE_OPERATION_VALUE ");
        for(int i = 0; i < NUM_MAPPING_KEYS; i++){
            sb.append(", ").append("MAPPING_VALUE_" + (i+1));
        }
    }

    private static boolean checkMappingQueryParams(List<String> keys, List<String> keyValueConstraints,
                                                   List<String> valueConstraintAndOrLike,
                                                   boolean isDetail,
                                                   List<String> operations) {
        if(keys == null || keys.isEmpty()){
            if(!isDetail){
                throw new IllegalArgumentException("Mapping queries require at least one value in the keys list");
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
            //if keyValueConstraint are not supplied, then we can't have valueConstraintAndOrLike supplied either
            if(valueConstraintAndOrLike != null && !valueConstraintAndOrLike.isEmpty()){
                throw new IllegalArgumentException("Cannot supply valueConstraintAndOrLike with values if no values in" +
                        " keyValueConstraints have been supplied, on which they would be applied");
            }
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

    private static void addMappingOrder(StringBuilder sb) {
        sb.append(" ORDER BY ");
        for(int i = 0; i < NUM_MAPPING_KEYS; i++){
            if(i != 0) sb.append(", ");
            sb.append("MAPPING_VALUE_" + (i+1));
        }
        sb.append(" ,p.objectid, SERVICE_OPERATION_VALUE ");
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
            sb.append("( mcmk.mapping").append(i).append("_key ");
            sb.append(" = '").append(key).append("' ");

            if(value != null && !value.equals("")){
                sb.append(" AND mcmv.mapping").append(i).append("_value ");
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
//        System.out.print(Utilities.getStringNamesFromCollection(l));

//        String date = Utilities.getMilliSecondAsStringDate(1222239651000L);
//        System.out.println("Date: " + date);

        List<String > keys  = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("Customer");

        List<String> values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("Gold");

        List<String> useAnd = new ArrayList<String>();
        useAnd.add("AND");
        useAnd.add("AND");

        String sql = createMappingQuery(null, null, null, keys, null, null,2, false, new ArrayList<String>());
        System.out.println("Mapping sql is: " + sql);

        sql = createMappingQuery(null, null, null, keys, null, null,2, true, new ArrayList<String>());
        System.out.println("Mapping sql with operation is: " + sql);

        sql = createMasterMappingQuery(null, null, new ArrayList<String>(), keys, null, null,2, false, null);
        System.out.println("Master sql is: " + sql);
        sql = createMasterMappingQuery(null, null, new ArrayList<String>(), keys, null, null,2, true, null);
        System.out.println("Master sql with operaitons is: " + sql);

//        List<String> valuesList = createValueList(keys, new String[]{"one", "two", "three"});
//        for(String s: valuesList){
//            System.out.println(s);
//        }

        values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("Bronze");
        sql = createMappingQuery(null, null, 229376L, keys, values, null,2, false, null);
        System.out.println("Subreport no detail sql is: " + sql);

        List<String> operations = new ArrayList<String>();
        operations.add("listProducts");
        operations.add("listOrders");

        sql = createMappingQuery(null, null, new ArrayList<String>(), keys, values, null,2, true, operations);
        System.out.println("Operation sql specific keys and value is: " + sql);

        sql = createMappingQuery(null, null, new ArrayList<String>(), null, null, null,2, true, operations);
        System.out.println("Operation sql is: " + sql);

        sql = createMappingQuery(null, null, new ArrayList<String>(), null, null, null,2, true, null);
        System.out.println("Empty operation sql is: " + sql);

    }

}