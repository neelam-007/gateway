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
    public static final String AUTHENTICATED_USER = "Authenticated User";

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

        if(startTimeMilli >= endTimeMilli) throw new IllegalArgumentException("Start time must be before end time");
        
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
     * @param values Is a Collection of Service string oid's
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
     * SECTION H: (Optional) If serviceIds is not null or empty, then all values from the Collection are placed inside
     * an IN constraint, otherwise no SQL is added to the overall query
     *
     * SECTION I: (Optional) If isDetail is true AND operations is not null or empty, then all values from the Collection
     * are placed inside an IN constraint, otherwise no SQL is added to the overall query
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
p.objectid IN (229384) AND
     ----SECTION I----
mcmv.service_operation IN ('listProducts')
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
     * @param serviceIds if supplied the published_service_oid from service_metrics will be constrained by these values
     * @param keys the list of keys representing the mapping keys
     * @param keyValueConstraints the values which each key must be equal to, Can be null or empty
     * @param valueConstraintAndOrLike for each key and value, if a value constraint exists as the index, the index into this
     * list dictitates whether an = or like constraint is applied. Can be null or empty. Cannot have values if
     * keyValueConstraints is null or empty
     * @param resolution 1 = hourly, 2 = daily. Which resolution from service_metrics to use
     * @param isDetail if true then the service_operation's real value is used in the select, group and order by,
     * otherwise operation is selected as 1. To facilitate this service_operation is always selected as
     * SERVICE_OPERATION_VALUE so that the real column is not used when isDetail is false
     * @param operations if isDetail is true, the where clauses constrains the values of service_operation from the
     * table message_context_mapping_values, with the values in operaitons 
     * @param useUser if true the auth_user_id column from message_context_mapping_values will be included in the
     * select, group by and order by clauses
     * @param authenticatedUsers if useUser is true, the where clause will constrain the values of
     * message_context_mapping_values, with the values in authenticatedUsers 
     * @return String query
     * @throws IllegalArgumentException If all the lists are not the same size and if they are empty.
     */
    public static String createMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Collection<String> serviceIds, List<String> keys,
                                            List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            List<String> operations, boolean useUser, List<String> authenticatedUsers){

        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, useUser);

        checkResolutionParameter(resolution);
        
        if(valueConstraintAndOrLike == null) valueConstraintAndOrLike = new ArrayList<String>();

        //----SECTION A----
        StringBuilder sb = new StringBuilder(aggregateSelect);
        //----SECTION B----
        addUserToSelect(useUser, sb);
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
        //----SECTION H----
        if(serviceIds != null && !serviceIds.isEmpty()){
            addServiceIdConstraint(serviceIds, sb);
        }

        //----SECTION I----
        if(isDetail && operations != null && !operations.isEmpty()){
            addOperationConstraint(operations, sb);
        }
        //----SECTION J----
        if(useUser && authenticatedUsers != null && !authenticatedUsers.isEmpty()){
            addUserConstraint(authenticatedUsers, sb);
        }

        //----SECTION K----
        if(keyValuesSupplied){
            addMappingConstraint(keys, keyValueConstraints, valueConstraintAndOrLike, sb);
        }

        //----SECTION L----
        addGroupBy(sb);

        //----SECTION M----
        addMappingOrder(sb);

//        System.out.println(sb.toString());
        return sb.toString();
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
        List<String> sIds = new ArrayList<String>();
        sIds.add(serviceId.toString());
        List<String> operationList = new ArrayList<String>();
        if(operation != null && !operation.equals("") && !operation.equals(SQL_PLACE_HOLDER)){
            operationList.add(operation);
        }
        List<String> authUsers = new ArrayList<String>();
        if(authenticatedUser != null && !authenticatedUser.equals("") && !authenticatedUser.equals(SQL_PLACE_HOLDER)){
            authUsers.add(authenticatedUser);
        }
        return createMappingQuery(startTimeInclusiveMilli, endTimeInclusiveMilli, sIds, keys, keyValueConstraints,
                valueConstraintAndOrLike, resolution, isDetail, operationList, useUser, authUsers);
    }

    private static void addUserToSelect(boolean useUser, StringBuilder sb) {
        if(useUser){
            sb.append(", mcmv.auth_user_id AS AUTHENTICATED_USER");
        }else{
            //sb.append(",  1 AS SERVICE_OPERATION_VALUE");
            sb.append(",  '" + SQL_PLACE_HOLDER + "' AS AUTHENTICATED_USER");
        }
    }

    public static String getNoMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                            Collection<String> serviceIds, int resolution){


        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);
        if(!useTime) throw new IllegalArgumentException("Both start and end time must be specified");
        checkResolutionParameter(resolution);

        StringBuilder sb = new StringBuilder(noMappingAggregateSelect);

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

//        addGroupBy(sb);
        sb.append(" GROUP BY p.objectid ");

        System.out.println(sb.toString());
        return sb.toString();
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
            "if(SUM(smd.attempted), ( 1.0 - ( ( (SUM(smd.authorized) - SUM(smd.completed)) / SUM(smd.attempted) ) ) ) , 0) as 'AP'" +
            " ,'1' as CONSTANT_GROUP ";

    private final static String noMappingAggregateSelect = "SELECT '1' as CONSTANT_GROUP, count(*) as count, p.objectid as SERVICE_ID, " +
            "p.name as SERVICE_NAME, p.routing_uri as ROUTING_URI, " +
            "SUM(if(sm.completed, sm.completed,0)) as THROUGHPUT, MIN(sm.front_min) as FRTM, " +
            "MAX(sm.front_max) as FRTMX, if(SUM(sm.front_sum), if(SUM(sm.attempted), " +
            "SUM(sm.front_sum)/SUM(sm.attempted),0), 0) as FRTA, MIN(sm.back_min) as BRTM, " +
            "MAX(sm.back_max) as BRTMX, if(SUM(sm.back_sum), if(SUM(sm.completed), " +
            "SUM(sm.back_sum)/SUM(sm.completed),0), 0) as BRTA, " +
            "if(SUM(sm.attempted), ( 1.0 - ( ( (SUM(sm.authorized) - SUM(sm.completed)) / SUM(sm.attempted) ) ) ) , 0) as 'AP' ";

    private final static String mappingJoin = " FROM service_metrics sm, published_service p, service_metrics_details smd," +
            " message_context_mapping_values mcmv, message_context_mapping_keys mcmk WHERE p.objectid = sm.published_service_oid " +
            "AND sm.objectid = smd.service_metrics_oid AND smd.mapping_values_oid = mcmv.objectid AND mcmv.mapping_keys_oid = mcmk.objectid ";

    private final static String noMappingJoin = " FROM service_metrics sm, published_service p WHERE " +
            "p.objectid = sm.published_service_oid ";

    /**
     * Creates the sql which can be used in a master jasper report. Query returns a distinct list of services, operaitons,
     * users and mappings which can be used to drive sub queries
     * The sql returned is very similar to the sql created by createMappingQuery, as is the processing below and these
     * methods could be refactored to be just a single method with an extra parameter. See createMappingQuery for more
     * information on how the sql is created.
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
     * @param useUser if true the auth_user_id column from message_context_mapping_values will be included in the
     * select, group by and order by clauses
     * @param authenticatedUsers if useUser is true, the where clause will constrain the values of
     * message_context_mapping_values, with the values in authenticatedUsers
     * @return String sql
     * @throws NullPointerException if startIntervalMilliSeconds or endIntervalMilliSeconds is null
     */
    public static String createMasterMappingQuery(Long startTimeInclusiveMilli, Long endTimeInclusiveMilli,
                                                  Collection<String> serviceIds, List<String> keys,
                                                  List<String> keyValueConstraints,
                                            List<String> valueConstraintAndOrLike, int resolution, boolean isDetail,
                                            List<String> operations, boolean useUser, List<String> authenticatedUsers){
        boolean useTime = checkTimeParameters(startTimeInclusiveMilli, endTimeInclusiveMilli);

        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, useUser);

        checkResolutionParameter(resolution);
        
        if(valueConstraintAndOrLike == null) valueConstraintAndOrLike = new ArrayList<String>();
        
        StringBuilder sb = new StringBuilder(distinctFrom);

        addUserToSelect(useUser, sb);

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

        if(useUser && authenticatedUsers != null && !authenticatedUsers.isEmpty()){
            addUserConstraint(authenticatedUsers, sb);
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
        if(keys == null || keys.isEmpty()){
            if(!useUser){
                if(!isDetail){
                    throw new IllegalArgumentException("Mapping queries require at least one value in the keys list");
                }
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
    public static String getMappingValueDisplayString(String authUser, String [] keyValues, List<String> keys){

        if(authUser == null || authUser.equals(""))
            throw new NullPointerException("authUser must have a non null and non empty value");//as it always exists in select
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
            sb.append("Authenticated User: " + authUser+ " ");
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

        System.out.println("Display mapping: " + sb.toString());
        return sb.toString();
    }

    /**
     * From the auth user, keys, values and filter constraints get a string which displays this information for the user
     * in the report info section of a report
     * @param authUsers Auth user string, can be null or empty
     * @param keys mapping keys, can be null or empty so long as isDetail or useUser is true
     * @param keyValueConstraints
     * @param valueConstraintAndOrLike
     * @param isDetail
     * @param useUser
     * @return String for displaying in report info section of report
     */
    public static String getMappingReportInfoDisplayString(List<String> authUsers, List<String> keys,
                                                           List<String> keyValueConstraints,
                                                           List<String> valueConstraintAndOrLike, boolean isDetail,
                                                           boolean useUser){
        boolean keyValuesSupplied = checkMappingQueryParams(keys,
                keyValueConstraints, valueConstraintAndOrLike, isDetail, useUser);

        System.out.println( "authUsers: " + authUsers + " keys: " + keys + "keyValueConstraints: "
                + keyValueConstraints+" valueConstraintAndOrLike: "+valueConstraintAndOrLike+
                " isDetail: " + isDetail+ " " +useUser);

        StringBuilder sb = new StringBuilder();
        boolean firstComma = false;
        if(useUser){
            sb.append(AUTHENTICATED_USER);
        }
        if(useUser && authUsers != null && !authUsers.isEmpty()){
            sb.append(": (");
            for (int i = 0; i < authUsers.size(); i++) {
                String s = authUsers.get(i);
                if(i != 0) sb.append(", ");
                sb.append(s);
            }
            sb.append(")");
            firstComma = true;
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

        System.out.println("sb: " + sb.toString());
        return sb.toString();
    }


    public static void main(String [] args) throws ParseException {
        String intervalStart = "2008/09/16 12:00";
        long intervalStartMilli = Utilities.getAbsoluteMilliSeconds(intervalStart);
        String intervalEnd = "2008/09/16 13:00";
        long intervalEndMilli = Utilities.getAbsoluteMilliSeconds(intervalEnd);

        String sql = getNoMappingQuery(intervalStartMilli, intervalEndMilli, null,1);
        System.out.println(sql);
    }

    public static void main1(String [] args) throws ParseException {
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

        List<String> authUsers = new ArrayList<String>();
        authUsers.add("Donal");
        authUsers.add("Ldap User 1");

        String displayVal = getMappingReportInfoDisplayString(null, keys, values, useAnd, false, false);
        System.out.println("Keys and values: " + displayVal);

        displayVal = getMappingReportInfoDisplayString(null, keys, values, useAnd, true, false);
        System.out.println("Keys and values, isDetail: " + displayVal);

        displayVal = getMappingReportInfoDisplayString(null, keys, values, useAnd, true, false);
        System.out.println("Keys and values, isDetail: " + displayVal);

        displayVal = getMappingReportInfoDisplayString(null, keys, values, useAnd, true, true);
        System.out.println("Keys and values, use user: " + displayVal);

        displayVal = getMappingReportInfoDisplayString(authUsers, keys, values, useAnd, true, true);
        System.out.println("Keys and values, use user with auth users: " + displayVal);

        values.clear();
        values.add(null);
        values.add("Gold");

        displayVal = getMappingReportInfoDisplayString(authUsers, keys, values, useAnd, true, true);
        System.out.println("Keys and values, no ip value: " + displayVal);

        StringBuilder sb = new StringBuilder();
        String s = null;
        sb.append(s);
        System.out.println(sb.toString());

/*
        String sql = createMappingQuery(null, null, null, keys, null, null,2, false, new ArrayList<String>(), false, null);
        System.out.println("Mapping sql is: " + sql);

        sql = createMappingQuery(null, null, null, keys, null, null,2, true, new ArrayList<String>(), false, null);
        System.out.println("Mapping sql with operation is: " + sql);

        sql = createMasterMappingQuery(null, null, new ArrayList<String>(), keys, null, null,2, false, null, false, null);
        System.out.println("Master sql is: " + sql);
        sql = createMasterMappingQuery(null, null, new ArrayList<String>(), keys, null, null,2, true, null, false, null);
        System.out.println("Master sql with operaitons is: " + sql);


        values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("Bronze");
        sql = createMappingQuery(null, null, 229376L, keys, values, null,2, false, null, false, null);
        System.out.println("Subreport no detail sql is: " + sql);

        List<String> operations = new ArrayList<String>();
        operations.add("listProducts");
        operations.add("listOrders");

        sql = createMappingQuery(null, null, new ArrayList<String>(), keys, values, null,2, true, operations, false, null);
        System.out.println("Operation sql specific keys and value is: " + sql);

        sql = createMappingQuery(null, null, new ArrayList<String>(), null, null, null,2, true, operations, false, null);
        System.out.println("Operation sql is: " + sql);

        sql = createMappingQuery(null, null, new ArrayList<String>(), null, null, null,2, true, null, false, null);
        System.out.println("Empty operation sql is: " + sql);

        sql = createMappingQuery(null, null, new ArrayList<String>(), null, null, null,2, true, null, true, null);
        System.out.println("User sql is: " + sql);

        List<String> authUsers = new ArrayList<String>();
        authUsers.add("Donal");

        sql = createMappingQuery(null, null, new ArrayList<String>(), null, null, null,2, true, null, true, authUsers);
        System.out.println("User value sql is: " + sql);

        sql = createMasterMappingQuery(null, null, new ArrayList<String>(), keys, null, null,2, true, null, true, null);
        System.out.println("Master sql with users is: " + sql);

        sql = createMasterMappingQuery(null, null, new ArrayList<String>(), keys, null, null,2, true, null, true, authUsers);
        System.out.println("Master sql with specific users is: " + sql);

        sql = createMasterMappingQuery(null, null, new ArrayList<String>(), keys, null, null,2, false, null, true, authUsers);
        System.out.println("Master sql with users and no operations is: " + sql);
*/

    }

}