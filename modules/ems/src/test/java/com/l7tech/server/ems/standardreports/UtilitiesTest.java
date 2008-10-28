/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 13, 2008
 * Time: 10:11:25 AM
 */
package com.l7tech.server.ems.standardreports;

import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Test;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Test coverage for class Utilities
 * Various tests test the method createMappingQuery. Each test is testing a specific characteristic of the sql
 * returned based on the parameter list supplied.
 */
public class UtilitiesTest{

    private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Utilities.DATE_STRING);
    /**
     * Tests the minimum requirement for a mapping query - one key supplied.
     * Checks that only 1 case statement exists in the returned sql
     * Checks that all other mapping keys are supplied in the select sql
     */
    @Test
    public void testCreateMappingQuery_OneKey(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        String sql =
                Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),keys ,null,null,1,false,null,false,null);

        //There should only be 1 CASE statement in SQL
        int index = sql.indexOf("CASE", 0);
        Assert.assertTrue(index != 0);

        index = sql.indexOf("CASE", index + 1);
        Assert.assertTrue(index == -1);

        //CASE  WHEN mcmk.mapping1_key = 'IP_ADDRESS' THEN mcmv.mapping1_value
        // WHEN mcmk.mapping2_key = 'IP_ADDRESS' THEN mcmv.mapping2_value
        // WHEN mcmk.mapping3_key = 'IP_ADDRESS' THEN mcmv.mapping3_value
        // WHEN mcmk.mapping4_key = 'IP_ADDRESS' THEN mcmv.mapping4_value
        // WHEN mcmk.mapping5_key = 'IP_ADDRESS' THEN mcmv.mapping5_value
        // END
        //test case has correct number of WHEN, THEN via mappingx_key
        for(int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++){
            index = sql.indexOf("mcmk.mapping"+(i+1)+"_key");
            Assert.assertTrue(index != -1);
        }

        //';' AS MAPPING_VALUE_2, ';' AS MAPPING_VALUE_3, ';' AS MAPPING_VALUE_4, ';' AS MAPPING_VALUE_5
        //check that all other keys were selected as the placeholder
        for(int i = 1; i < Utilities.NUM_MAPPING_KEYS; i++){
            index = sql.indexOf("AS MAPPING_VALUE_"+(i+1));
            Assert.assertTrue(index != -1);
        }
        //System.out.println("OneKey: "+sql);
    }

    @Test
    public void testCreateMasterMappingQuery_KeyValues(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        List<String> values = new ArrayList<String>();
//        values.add("127.0.0.1");
//        values.add("GOLD");

        String sql = Utilities.createMappingQuery(true, null,null,new ArrayList<String>(),keys ,values ,null ,1 ,false ,null
                ,false ,null);

        System.out.println(sql);

    }

    /**
     * Checks that all values supplied as constraints on keys are used correctly in the generated sql
     */
    @Test
    public void testCreateMappingQuery_KeyValues(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        List<String> values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("GOLD");

        String sql = Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),keys ,values ,null ,1 ,false ,null
                ,false ,null);

        //System.out.println("Value Sql: "+sql);

        //AND
        //(
        // ( mcmk.mapping1_key = 'IP_ADDRESS'  AND mcmv.mapping1_value = '127.0.0.1' ) OR
        // ( mcmk.mapping2_key = 'IP_ADDRESS'  AND mcmv.mapping2_value = '127.0.0.1' ) OR
        // ( mcmk.mapping3_key = 'IP_ADDRESS'  AND mcmv.mapping3_value = '127.0.0.1' ) OR
        // ( mcmk.mapping4_key = 'IP_ADDRESS'  AND mcmv.mapping4_value = '127.0.0.1' ) OR
        // ( mcmk.mapping5_key = 'IP_ADDRESS'  AND mcmv.mapping5_value = '127.0.0.1' )
        //)
        //AND
        //(
        // ( mcmk.mapping1_key = 'CUSTOMER'  AND mcmv.mapping1_value = 'GOLD' ) OR
        // ( mcmk.mapping2_key = 'CUSTOMER'  AND mcmv.mapping2_value = 'GOLD' ) OR
        // ( mcmk.mapping3_key = 'CUSTOMER'  AND mcmv.mapping3_value = 'GOLD' ) OR
        // ( mcmk.mapping4_key = 'CUSTOMER'  AND mcmv.mapping4_value = 'GOLD' ) OR
        // ( mcmk.mapping5_key = 'CUSTOMER'  AND mcmv.mapping5_value = 'GOLD' )
        // )

        for (int i = 0; i < keys.size(); i++) {
            String s = keys.get(i);
            String v = values.get(i);
            for(int z = 1; z <= Utilities.NUM_MAPPING_KEYS; z++){
                int index = sql.indexOf("mcmk.mapping"+z+"_key = '"+s+"'");
                Assert.assertTrue(index != -1);
                index = sql.indexOf("mcmv.mapping"+z+"_value = '"+v+"'");
                Assert.assertTrue(index != -1);
            }
        }
    }

    /**
     * Check that any AND / LIKE filter constraints supplied are used correctly
     */
    @Test
    public void testCreateMappingQuery_KeyValuesFilters(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");
        keys.add("CUSTOMER_STATUS");

        List<String> values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("%GOLD");
        values.add("GOLD%");

        List<String> filters = new ArrayList<String>();
        filters.add(null);
        filters.add("LIKE");
        filters.add("like");

        String sql = Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),keys ,values ,filters, 1 ,false ,null
                ,false ,null);

        //System.out.println("Filter Sql: "+sql);
        
        //(
        // ( mcmk.mapping1_key = 'IP_ADDRESS' AND mcmv.mapping1_value = '127.0.0.1' ) OR
        // ( mcmk.mapping2_key = 'IP_ADDRESS' AND mcmv.mapping2_value = '127.0.0.1' ) OR
        // ( mcmk.mapping3_key = 'IP_ADDRESS' AND mcmv.mapping3_value = '127.0.0.1' ) OR
        // ( mcmk.mapping4_key = 'IP_ADDRESS' AND mcmv.mapping4_value = '127.0.0.1' ) OR
        // ( mcmk.mapping5_key = 'IP_ADDRESS' AND mcmv.mapping5_value = '127.0.0.1' )
        // )
        // AND
        // (
        //  ( mcmk.mapping1_key = 'CUSTOMER' AND mcmv.mapping1_value LIKE 'GOLD' ) OR
        //  ( mcmk.mapping2_key = 'CUSTOMER' AND mcmv.mapping2_value LIKE 'GOLD' ) OR
        //  ( mcmk.mapping3_key = 'CUSTOMER' AND mcmv.mapping3_value LIKE 'GOLD' ) OR
        //  ( mcmk.mapping4_key = 'CUSTOMER' AND mcmv.mapping4_value LIKE 'GOLD' ) OR
        //  ( mcmk.mapping5_key = 'CUSTOMER' AND mcmv.mapping5_value LIKE 'GOLD' ))
        for (int i = 0; i < keys.size(); i++) {
            String s = keys.get(i);
            String v = values.get(i);
            for(int z = 1; z <= Utilities.NUM_MAPPING_KEYS; z++){
                boolean useAnd = (filters.get(i) == null || filters.get(i).equalsIgnoreCase("AND"));
                String fValue = (useAnd)?"=":"LIKE";
                int index = sql.indexOf("mcmk.mapping"+z+"_key = '"+s+"' AND mcmv.mapping"+z+"_value "+fValue+" '"+v+"'");
                Assert.assertTrue(index != -1);
            }
        }

        
    }

    /**
     * Test that when isDetail is true, no keys or other parameters are required, except for resolution
     * Checks that the real value of mcmv.service_operation is selected and not a placeholder
     */
    @Test
    public void testCreateMappingQuery_OnlyDetail(){
        String sql =
                Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),null ,null ,null,1,true ,null ,false ,null);

        int index = sql.indexOf("mcmv.service_operation AS SERVICE_OPERATION_VALUE");
        Assert.assertTrue(index != -1);
        //System.out.println("OnlyDetail: "+sql);
    }

    /**
     * Checks the generated sql for the following, when useUser is true and when a list of authenticated users
     * is provided:-
     * Checks that the real value for user is selected and not a placeholder
     *
     */
    @Test
    public void testCreateMappingQuery_UseUser(){
        List<String> users = new ArrayList<String>();
        users.add("Donal");
        users.add("Ldap User 1");

        String sql =
                Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),null ,null ,null,1,true ,null ,true , users);

        int index = sql.indexOf("mcmv.auth_user_id AS AUTHENTICATED_USER");
        Assert.assertTrue(index != -1);

        //mcmv.auth_user_id IN ('Donal','Ldap User 1')
        index = sql.indexOf("mcmv.auth_user_id IN");
        Assert.assertTrue(index != -1);

        for(String s: users){
            index = sql.indexOf(s);
            Assert.assertTrue(index != -1);
        }
        //System.out.println("OnlyDetail: "+sql);
    }

    /**
     * Checks that the resolution being supplied, is always used
     * Checks that any resolution other than 1 or 2, causes an exception
     */
    @Test
    public void testCreateMappingQuery_Resolution(){

        String sql =
                Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),null ,null ,null,1,true ,null ,false , null);

        int index = sql.indexOf("sm.resolution = 1");
        Assert.assertTrue(index != -1);

        sql = Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),null ,null ,null,2,true ,null ,false , null);
        index = sql.indexOf("sm.resolution = 2");
        Assert.assertTrue(index != -1);
        //System.out.println("Resolution: "+sql);

        boolean exception = false;
        try{
            Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),null ,null ,null,3 ,true ,null ,false , null);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Checks that start and end time are used correctly.
     * Checks that incorrect values cause an exception
     */
    @Test
    public void testCreateMappingQuery_Time(){

        Calendar cal = Calendar.getInstance();
        long startTime = cal.getTimeInMillis() - 1000;
        long endTime = cal.getTimeInMillis();

        String sql = Utilities.createMappingQuery(false, startTime, endTime ,new ArrayList<String>(),null ,null ,null,1,true ,
                null ,false , null);

        int index = sql.indexOf("sm.period_start >="+startTime);
        Assert.assertTrue(index != -1);

        index = sql.indexOf("sm.period_start <"+endTime);
        Assert.assertTrue(index != -1);
        
        boolean exception = false;
        try{
            Utilities.createMappingQuery(false, endTime, startTime, new ArrayList<String>(),null ,null ,null,1 ,true ,null ,false , null);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
        //System.out.println("Time: "+sql);
    }

    /**
     * Tests that any service id's provided are used in the query
     * Checks that when no ids are supplied, there is no constraint in the generated sql
     */
    @Test
    public void testCreateMappingQuery_ServiceIds(){

        List<String> serviceIds = new ArrayList<String>();
        serviceIds.add("12345");
        serviceIds.add("67890");

        String sql = Utilities.createMappingQuery(false, null, null, serviceIds, null, null, null, 1, true,
                null , false, null);

        //System.out.println("Service Ids: "+sql);

        //p.objectid IN (12345, 67890)
        int index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index != -1);

        for(String s: serviceIds){
            index = sql.indexOf(s);
            Assert.assertTrue(index != -1);
        }

        //check no constraint when no ids supplied
        sql = Utilities.createMappingQuery(false, null, null, new ArrayList<String>(), null, null, null, 1, true,
                null , false, null);
        index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index == -1);
    }

    @Test
    public void testCreateMappingQuery_Operations(){

        List<String> operations = new ArrayList<String>();
        operations.add("listProducts");
        operations.add("orderProduct");

        String sql = Utilities.createMappingQuery(false, null, null, new ArrayList<String>(), null, null, null, 1, true,
                operations, false, null);

//        System.out.println("Operation: "+sql);

            //AND mcmv.service_operation IN ('listProducts','orderProduct')
        int index = sql.indexOf("mcmv.service_operation IN");
        Assert.assertTrue(index != -1);

        for(String s: operations){
            index = sql.indexOf(s);
            Assert.assertTrue(index != -1);
        }

        //check no constraint when no ids supplied
        sql = Utilities.createMappingQuery(false, null, null, new ArrayList<String>(), null, null, null, 1, true,
                null , false, null);
        index = sql.indexOf("mcmv.service_operation IN");
        Assert.assertTrue(index == -1);
    }

    /**
     * Confirms that all fields which should be selected, are.
     */
    @Test
    public void testCreateMappingQuery_SelectFields(){

        String sql = Utilities.createMappingQuery(false, null, null, new ArrayList<String>(), null, null, null, 1, true,
                null , false, null);

        //System.out.println("Select Fields: "+sql);

        int index = sql.indexOf(Utilities.SERVICE_ID);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_NAME);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.ROUTING_URI);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.THROUGHPUT);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.FRTM);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.FRTMX);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.FRTA);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.BRTM);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.BRTMX);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.AP);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.CONSTANT_GROUP);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.AUTHENTICATED_USER);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_OPERATION_VALUE);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_1);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_2);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_3);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_4);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_5);
        Assert.assertTrue(index != -1);
    }

    @Test
    public void testgetUsageDistinctMappingQuery(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        List<String> values = new ArrayList<String>();
        values.add(null);
        values.add("GOLD");

        String sql = Utilities.getUsageDistinctMappingQuery(null, null, null, keys, null, null, 2, true, null, false, null);
        System.out.println(sql);
    }

    @Test
    public void testGetUsageSummaryQuery(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        List<String> values = new ArrayList<String>();
        values.add(null);
        values.add("GOLD");

        String sql = Utilities.getUsageSummaryQuery(null, null, null, keys, null, null, 2, false, null, false, null);
        System.out.println(sql);
    }

    public void testGetUsageColumnHeader(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        List<String> values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("GOLD");

        String authUser = "Donal"; 
//        String header = Utilities.getUsageColumnHeader(authUser, keys, values.toArray(new String[]{}));
//        System.out.println(header);
    }
    /**
     * Checks that the gruop by order has all columns required present, and in the correct order.
     */
    @Test
    public void testCreateMappingQuery_GroupBy(){
        String sql =
                Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),null ,null,null,1, true, null,false,null);
        //System.out.println("Group by: "+sql);
        int index = sql.indexOf("GROUP BY p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER , MAPPING_VALUE_1, " +
                "MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5");
        Assert.assertTrue(index != -1);
    }

    /**
     * Checks that the gruop by order has all columns required present, and in the correct order.
     */
    @Test
    public void testCreateMappingQuery_OrderBy(){
        String sql =
                Utilities.createMappingQuery(false, null,null,new ArrayList<String>(),null ,null,null,1, true, null,false,null);
//        System.out.println("Order by: "+sql);
        int index = sql.indexOf("ORDER BY AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, " +
                "MAPPING_VALUE_4, MAPPING_VALUE_5 ,p.objectid, SERVICE_OPERATION_VALUE");
        Assert.assertTrue(index != -1);
    }

    /**
     * When the mapping query is a distinct query, check that all the expected fields are returned.
     */
    @Test
    public void testCreateMappingQuery_SelectDistinctFields(){

        String sql = Utilities.createMappingQuery(true, null, null, new ArrayList<String>(), null, null, null, 1, true,
                null , false, null);

        System.out.println("Select Distinct Fields: "+sql);

        int index = sql.indexOf(Utilities.SERVICE_ID);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_NAME);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.ROUTING_URI);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.AUTHENTICATED_USER);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_OPERATION_VALUE);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_1);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_2);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_3);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_4);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_5);
        Assert.assertTrue(index != -1);
    }

    /**
     * Tests that the list created by createValueList is correct
     * Checks that IllegalArgumentException is thrown, when a key does not have a valid value
     */
    @Test
    public void testCreateValueList(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        List<String> values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("GOLD");
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);

        List<String> valueList = Utilities.createValueList(keys, values.toArray(new String[]{}));
        Assert.assertTrue(valueList.size() == 2);

        values.clear();
        values.add("127.0.0.1");
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);

        boolean exception = false;
        try{
            Utilities.createValueList(keys, values.toArray(new String[]{}));
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }



    /**
     * Tests that the value returned from getAbsoluteMilliSeconds is correct  
     */
    @Test
    public void testGetAbsoluteMilliSeconds() throws Exception{
        String date = "2008/10/13 14:12";
        Date d = DATE_FORMAT.parse(date);
        long controlTime = d.getTime();

        long testTime = Utilities.getAbsoluteMilliSeconds(date);
        Assert.assertTrue(controlTime == testTime);
    }

    /**
     * Test that the Calendar instance returned from getCalendarForTimeUnit has the correct settings
     * based on the unit of time supplied
     * @throws Exception
     */
    @Test
    public void testGetCalendarForTimeUnit() throws Exception{
        Calendar control = Calendar.getInstance();
        control.set(Calendar.MINUTE, 0);
        control.set(Calendar.MILLISECOND, 0);

        Calendar cal = Utilities.getCalendarForTimeUnit(Utilities.HOUR);
        Assert.assertTrue(control.getTimeInMillis() == cal.getTimeInMillis());

        cal = Utilities.getCalendarForTimeUnit(Utilities.DAY);
        control = Calendar.getInstance();
        control.set(Calendar.HOUR_OF_DAY, 0);
        control.set(Calendar.MINUTE, 0);
        control.set(Calendar.MILLISECOND, 0);
        Assert.assertTrue(control.getTimeInMillis() == cal.getTimeInMillis());

        cal = Utilities.getCalendarForTimeUnit(Utilities.WEEK);
        control = Calendar.getInstance();
        control.set(Calendar.HOUR_OF_DAY, 0);
        control.set(Calendar.MINUTE, 0);
        control.set(Calendar.MILLISECOND, 0);
        Assert.assertTrue(control.getTimeInMillis() == cal.getTimeInMillis());

        cal = Utilities.getCalendarForTimeUnit(Utilities.MONTH);
        control = Calendar.getInstance();
        control.set(Calendar.DAY_OF_MONTH, 1);
        control.set(Calendar.HOUR_OF_DAY, 0);
        control.set(Calendar.MINUTE, 0);
        control.set(Calendar.MILLISECOND, 0);
        Assert.assertTrue(control.getTimeInMillis() == cal.getTimeInMillis());
    }

    /**
     * Test that the display string returned from getIntervalDisplayDate are correct 
     * @throws Exception 
     */
    @Test
    public void testGetIntervalDisplayDate() throws Exception{
        String startDate = "2008/08/01 14:12";
        String endDate = "2008/10/13 15:12";
        Date d = DATE_FORMAT.parse(startDate);
        long startTime = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long endTime = d.getTime();

        String timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.HOUR);
        Assert.assertTrue(timeDisplay.equals("08/01 14:12 - 15:12"));

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.DAY);
        Assert.assertTrue(timeDisplay.equals("Fri 08/01"));
        
        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.WEEK);
        Assert.assertTrue(timeDisplay.equals("08/01 - 10/13"));

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.MONTH);
        Assert.assertTrue(timeDisplay.equals("2008 Aug"));

        //System.out.println("timeDisplay: "+ timeDisplay);
    }

    /**
     * Check that the intervals returned for a time period are correct. These intervals are used to drive all
     * sub queries for a specific service or set of mapping values.
     * Checks that the returned list has the correct number of intervals for each interval unit of time tested
     * Checks that IllegalArgumentException is thrown if the arguments are incorrect
     */
    @Test
    public void testGetIntervalsForTimePeriod() throws Exception{
        String startDate = "2008/10/12 00:00";
        String endDate = "2008/10/13 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        //Hour
        List<Long> intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.HOUR);

        //00:00 - 00:00, 24 hours but 25 is the interval size as it is 0 based and end time is exclusive.
        Assert.assertTrue(intervals.size() == 25);

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.HOUR);

        Assert.assertTrue(intervals.size() == 13);

        //Day
        startDate = "2008/10/01 00:00";
        endDate = "2008/10/13 00:00";
        d = DATE_FORMAT.parse(startDate);
        timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        timePeriodEndExclusive = d.getTime();

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.DAY);

        Assert.assertTrue(intervals.size() == 13);

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.DAY);

        Assert.assertTrue(intervals.size() == 5);

        //Week
        startDate = "2008/09/01 00:00";
        endDate = "2008/10/13 00:00";
        d = DATE_FORMAT.parse(startDate);
        timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        timePeriodEndExclusive = d.getTime();

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.WEEK);
        Assert.assertTrue(intervals.size() == 7);

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.WEEK);
        Assert.assertTrue(intervals.size() == 4);

        //Month
        startDate = "2008/01/01 00:00";
        endDate = "2008/10/01 00:00";
        d = DATE_FORMAT.parse(startDate);
        timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        timePeriodEndExclusive = d.getTime();

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.MONTH);

        Assert.assertTrue(intervals.size() == 10);
        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.MONTH);

        Assert.assertTrue(intervals.size() == 4);
    }

    /**
     * Tests the display string generated for the context mapping section of the report info section on a report.
     * Tests the string generated based on the various parameter combinations which this method accecpts
     */
    @Test
    public void testGetMappingReportInfoDisplayString(){
        String val = Utilities.getMappingReportInfoDisplayString(null, null, null, true, false);
        Assert.assertTrue(val.equals(Utilities.onlyIsDetailDisplayText));

        val = Utilities.getMappingReportInfoDisplayString(null,null, null, false, true);
        Assert.assertTrue(val.equals(Utilities.AUTHENTICATED_USER_DISPLAY));

        List<String> authUsers = new ArrayList<String>();
        authUsers.add("Donal");
        val = Utilities.getMappingReportInfoDisplayString(authUsers, null, null, false, true);
        Assert.assertTrue(val.equals(Utilities.AUTHENTICATED_USER_DISPLAY+": ("+authUsers.get(0)+")"));

        authUsers.add("Ldap User 1");
        val = Utilities.getMappingReportInfoDisplayString(authUsers, null, null, false, true);
        Assert.assertTrue(val.equals(Utilities.AUTHENTICATED_USER_DISPLAY+": ("+authUsers.get(0)+", "+authUsers.get(1)+")"));
        
        //if auth users supplied, not printed if useUser is false
        val = Utilities.getMappingReportInfoDisplayString(authUsers, null, null, true, false);
        Assert.assertTrue(val.equals(Utilities.onlyIsDetailDisplayText));

        //exception, when all params are null or false
        boolean exception = false;
        try{
            Utilities.getMappingReportInfoDisplayString(authUsers, null, null, false, false);
        }catch (IllegalArgumentException e){
            exception = true;
        }
        Assert.assertTrue(exception);

        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        List<String> values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("GOLD");

        val = Utilities.getMappingReportInfoDisplayString(authUsers, keys, null, false, false);
        Assert.assertTrue(val.equals(keys.get(0) + ", "+keys.get(1)));

        val = Utilities.getMappingReportInfoDisplayString(authUsers, keys, values, false, false);
        Assert.assertTrue(val.equals(keys.get(0) + " ("+values.get(0)+"), "+keys.get(1)+" ("+values.get(1)+")"));

        //exception when too many values
        values.add("GOLD");
        exception = false;
        try{
            Utilities.getMappingReportInfoDisplayString(authUsers, keys, values, false, false);
        }catch (IllegalArgumentException e){
            exception = true;
        }
        Assert.assertTrue(exception);
        values.remove(2);

        val = Utilities.getMappingReportInfoDisplayString(authUsers, keys, values, false, true);
        Assert.assertTrue(val.equals(Utilities.AUTHENTICATED_USER_DISPLAY+": ("+authUsers.get(0)+", "+authUsers.get(1)+"), "
                +keys.get(0) + " ("+values.get(0)+"), "+keys.get(1)+" ("+values.get(1)+")"));

    }

    /**
     * Tests the display string generated for each table of data for each distinct set of mapping values found.
     * Tests the string generated based on the various parameter combinations which this method accecpts
     */
    @Test
    public void testGetMappingValueDisplayString(){
        boolean exception = false;
        try{
            Utilities.getMappingValueDisplayString(null, null, null);
        }catch (NullPointerException e){
            exception = true;
        }
        Assert.assertTrue(exception);

        exception = false;
        try{
            Utilities.getMappingValueDisplayString(Utilities.SQL_PLACE_HOLDER, null, null);
        }catch (IllegalArgumentException e){
            exception = true;
        }
        Assert.assertTrue(exception);

        
        String val = Utilities.getMappingValueDisplayString("Donal", null, null);
        Assert.assertTrue(val.equals("Mapping Value: "+Utilities.AUTHENTICATED_USER_DISPLAY+": Donal"));

        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        List<String> values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("GOLD");

        val = Utilities.getMappingValueDisplayString(Utilities.SQL_PLACE_HOLDER, keys, values.toArray(new String[]{}));
        Assert.assertTrue(val.equals("Mapping Value: "+keys.get(0)+": "+values.get(0)+", "+keys.get(1)+": "+values.get(1)));

        val = Utilities.getMappingValueDisplayString("Donal", keys, values.toArray(new String[]{}));
        Assert.assertTrue(val.equals("Mapping Value: "+Utilities.AUTHENTICATED_USER_DISPLAY+": Donal, "+keys.get(0)+": "+values.get(0)+", "+keys.get(1)+": "+values.get(1)));
        
        values.remove(1);
        exception = false;
        try{
            Utilities.getMappingValueDisplayString(Utilities.SQL_PLACE_HOLDER, keys, values.toArray(new String[]{}));
        }catch (IllegalArgumentException e){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testGetMilliSecondAsStringDate() throws Exception{
        String date = "2008/10/13 16:38";
        Date d = DATE_FORMAT.parse(date);
        long timeMili = d.getTime();

        String milliAsDate = Utilities.getMilliSecondAsStringDate(timeMili);
        Assert.assertTrue(date.equals(milliAsDate));
    }

    @Test
    public void testGetMillisForEndTimePeriod(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long endTimeMilli = Utilities.getMillisForEndTimePeriod(Utilities.HOUR);
        Assert.assertTrue(calendar.getTimeInMillis() == endTimeMilli);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        endTimeMilli = Utilities.getMillisForEndTimePeriod(Utilities.DAY);
        Assert.assertTrue(calendar.getTimeInMillis() == endTimeMilli);
        endTimeMilli = Utilities.getMillisForEndTimePeriod(Utilities.WEEK);
        Assert.assertTrue(calendar.getTimeInMillis() == endTimeMilli);

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        endTimeMilli = Utilities.getMillisForEndTimePeriod(Utilities.MONTH);
        Assert.assertTrue(calendar.getTimeInMillis() == endTimeMilli);
    }

    @Test
    public void testGetNoMappingQuery_ValidInputs() throws ParseException {
        boolean exception = false;
        try{
            Utilities.getNoMappingQuery(false, null, null, null,1);
        }catch (IllegalArgumentException e){
            exception = true;
        }
        Assert.assertTrue(exception);

        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        exception = false;
        try{
            Utilities.getNoMappingQuery(false, endTime, startTime, null,1);
        }catch (IllegalArgumentException e){
            exception = true;
        }
        Assert.assertTrue(exception);
        
    }

    @Test
    public void testGetNoMappingQuery_SelectFields() throws ParseException {

        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        String sql = Utilities.getNoMappingQuery(false, startTime, endTime, null,1);

        int index = sql.indexOf(Utilities.SERVICE_ID);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_NAME);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.ROUTING_URI);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.THROUGHPUT);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.FRTM);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.FRTMX);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.FRTA);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.BRTM);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.BRTMX);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.AP);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.CONSTANT_GROUP);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.AUTHENTICATED_USER);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_OPERATION_VALUE);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_1);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_2);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_3);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_4);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_5);
        Assert.assertTrue(index != -1);
        
    }

    /**
     * Checks that start and end time are used correctly.
     * Checks that incorrect values cause an exception
     */
    @Test
    public void testGetNoMappingQuery_Time(){

        Calendar cal = Calendar.getInstance();
        long startTime = cal.getTimeInMillis() - 1000;
        long endTime = cal.getTimeInMillis();

        String sql = Utilities.getNoMappingQuery(false, startTime, endTime, null,1);

        int index = sql.indexOf("sm.period_start >="+startTime);
        Assert.assertTrue(index != -1);

        index = sql.indexOf("sm.period_start <"+endTime);
        Assert.assertTrue(index != -1);

        boolean exception = false;
        try{
            Utilities.getNoMappingQuery(false, endTime, startTime, null,1);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Checks that the resolution being supplied, is always used
     * Checks that any resolution other than 1 or 2, causes an exception
     */
    @Test
    public void testGetNoMappingQuery_Resolution() throws ParseException {
        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        String sql = Utilities.getNoMappingQuery(false, startTime, endTime,  null,1);

        int index = sql.indexOf("sm.resolution = 1");
        Assert.assertTrue(index != -1);

        sql = Utilities.getNoMappingQuery(false, startTime, endTime, null,2);
        index = sql.indexOf("sm.resolution = 2");
        Assert.assertTrue(index != -1);
        //System.out.println("Resolution: "+sql);

        boolean exception = false;
        try{
            Utilities.getNoMappingQuery(false, startTime, endTime, null,3);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Tests that any service id's provided are used in the query
     * Checks that when no ids are supplied, there is no constraint in the generated sql
     */
    @Test
    public void testGetNoMappingQuery_ServiceIds() throws ParseException {
        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        List<String> serviceIds = new ArrayList<String>();
        serviceIds.add("12345");
        serviceIds.add("67890");

        String sql = Utilities.getNoMappingQuery(false, startTime, endTime,  serviceIds,1);

        //System.out.println("Service Ids: "+sql);

        //p.objectid IN (12345, 67890)
        int index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index != -1);

        for(String s: serviceIds){
            index = sql.indexOf(s);
            Assert.assertTrue(index != -1);
        }

        //check no constraint when no ids supplied
        sql = Utilities.getNoMappingQuery(false, startTime, endTime, null, 1);
        
        index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index == -1);
    }

    /**
     * Checks that the group by clause has the objectid column present. Only required when isMasterQuery is false
     */
    @Test
    public void testGetNoMappingQuery_GroupBy() throws ParseException {
        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        String sql = Utilities.getNoMappingQuery(false, startTime, endTime, null, 1);
        int index = sql.indexOf("GROUP BY p.objectid");
        Assert.assertTrue(index != -1);
    }

    @Test
    public void testGetNoMappingQuery_SelectDistinctFields() throws ParseException {

        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        String sql = Utilities.getNoMappingQuery(true, startTime, endTime, null,1);
        
        int index = sql.indexOf(Utilities.SERVICE_ID);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_NAME);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.ROUTING_URI);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.AUTHENTICATED_USER);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_OPERATION_VALUE);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_1);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_2);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_3);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_4);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.MAPPING_VALUE_5);
        Assert.assertTrue(index != -1);
    }

    /**
     * Checks that the order by clause has the objectid column present.
     * Only required when isMasterQuery is true
     */
    @Test
    public void testGetNoMappingQuery_OrderBy() throws ParseException {
        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        String sql = Utilities.getNoMappingQuery(true, startTime, endTime, null, 1);
        int index = sql.indexOf("ORDER BY p.objectid");
        Assert.assertTrue(index != -1);
    }

    /**
     * Confirm that the relative calculation for each unit of time allowed, yields the correct milli second time
     * in the past
     */
    @Test
    public void testGetRelativeMilliSecondsInPast(){

        //if this test fails it could be because the clock changed hour between
        //when the Calendar instance was retrieved and before the first calls to getRelativeMilliSecondsInPast
        //Calendar is retrieved for each time unit tested, to minimize the chance of this happening

        //hour
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.HOUR_OF_DAY, -1);
        long timeInPast = Utilities.getRelativeMilliSecondsInPast(1, Utilities.HOUR);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);

        calendar.add(Calendar.HOUR_OF_DAY, -1);
        timeInPast = Utilities.getRelativeMilliSecondsInPast(2, Utilities.HOUR);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);

        //day
        calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        calendar.add(Calendar.DAY_OF_MONTH, -2);
        timeInPast = Utilities.getRelativeMilliSecondsInPast(2, Utilities.DAY);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);

        //week
        calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        calendar.add(Calendar.WEEK_OF_YEAR, -2);
        timeInPast = Utilities.getRelativeMilliSecondsInPast(2, Utilities.WEEK);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);

        //month
        calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        calendar.add(Calendar.MONTH, -2);
        timeInPast = Utilities.getRelativeMilliSecondsInPast(2, Utilities.MONTH);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);
    }

    /**
     * Ensure for various start and end times, that the correct resolution is used in sql queries
     */
    @Test
    public void testGetResolutionFromTimePeriod() throws ParseException {
        Calendar cal = Calendar.getInstance();
        long startTime = cal.getTimeInMillis();         
        cal.add(Calendar.HOUR_OF_DAY, 6);
        long endTime = cal.getTimeInMillis();
        int resolution = Utilities.getResolutionFromTimePeriod(24, startTime, endTime);
        Assert.assertTrue(resolution == 1);

        cal.add(Calendar.DAY_OF_MONTH, -40);
        startTime = cal.getTimeInMillis();
        resolution = Utilities.getResolutionFromTimePeriod(24, startTime, endTime);
        Assert.assertTrue(resolution == 2);

        boolean exception = false;
        try{
            Utilities.getResolutionFromTimePeriod(24, endTime, startTime);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Test the returned string is formatted correctly
     */
    @Test
    public void testGetStringNamesFromCollection(){
        List<String> strings = new ArrayList<String>();
        strings.add("one");
        strings.add("two");

        String val = Utilities.getStringNamesFromCollection(strings);
        Assert.assertTrue(val.split(",").length == 2);

        strings.remove(1);
        val = Utilities.getStringNamesFromCollection(strings);
        int index = val.indexOf(",");
        Assert.assertTrue(index == -1);
        
    }

    @Test
    public void testIsPlaceHolderValue(){
        String val = Utilities.SQL_PLACE_HOLDER;
        Assert.assertTrue(Utilities.isPlaceHolderValue(val));
    }

    @Test
    public void testGetUsageRuntimeDoc_NullParams(){
        boolean exception = false;
        try{
            Utilities.getUsageRuntimeDoc(false, null, null);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testGetUsageRuntimeDoc_NotNull(){
        List<String> keys = getTestKeys();
        LinkedHashSet<String> mappingValues = getTestMappingValues();

        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        Assert.assertNotNull(doc);
    }

    /**
     * Don't change the number of elements, as you will break the tests which use this as convenience
     * @return
     */
    private List<String> getTestKeys(){
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");
        return keys;        
    }

    /**
     * Don't change the number of elements, as you will break the tests which use this as convenience
     * @return
     */
    private LinkedHashSet<String> getTestMappingValues(){
        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();
        mappingValues.add("notimportant1");
        mappingValues.add("notimportant2");
        mappingValues.add("notimportant3");
        mappingValues.add("notimportant4");
        return mappingValues;
    }
    
    @Test
    public void testGetUsageRuntimeDoc_FirstLevelElements(){
        List<String> keys = getTestKeys();
        LinkedHashSet<String> mappingValues = getTestMappingValues();
        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.CONSTANT_HEADER);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.SERVICE_ID_FOOTER);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.CONSTANT_FOOTER);
        Assert.assertTrue(list.getLength() == 1);
        
        list = doc.getElementsByTagName(Utilities.COLUMN_WIDTH);
                Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.FRAME_WIDTH);
                Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.LEFT_MARGIN);
                Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.RIGHT_MARGIN);
                Assert.assertTrue(list.getLength() == 1);
    }

    @Test
    public void testGetUsageRuntimeDoc_Variables(){
        List<String> keys = getTestKeys();
        LinkedHashSet<String> mappingValues = getTestMappingValues();
        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();
        //3 sets of variables X 4 value sets from getTestMappingValues()
        Assert.assertTrue(list.getLength() == 12);
    }

    @Test
    public void testGetUsageRuntimeDoc_VariablesNames(){
        List<String> keys = getTestKeys();
        LinkedHashSet<String> mappingValues = getTestMappingValues();
        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        //Assert.assertTrue(false);
        //todo [Donal] logic here to validate the names created for variables
    }

    @Test
    public void testGetUsageRuntimeDoc_ConstantHeader(){
        List<String> keys = getTestKeys();
        LinkedHashSet<String> mappingValues = getTestMappingValues();
        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_HEADER);
        list = list.item(0).getChildNodes();
        //2 text fields which are hardcoded, and 4 value sets from getTestMappingValues() 
        Assert.assertTrue(list.getLength() == 6);
    }

    @Test
    public void testGetUsageRuntimeDoc_ServiceOperationFooter(){
        List<String> keys = getTestKeys();
        LinkedHashSet<String> mappingValues = getTestMappingValues();
        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(list.getLength() == 5);
    }

    @Test
    public void testGetUsageRuntimeDoc_SerivceIdFooter(){
        List<String> keys = getTestKeys();
        LinkedHashSet<String> mappingValues = getTestMappingValues();
        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_ID_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(list.getLength() == 5);
    }

    @Test
    public void testGetUsageRuntimeDoc_ConstantFooter(){
        List<String> keys = getTestKeys();
        LinkedHashSet<String> mappingValues = getTestMappingValues();
        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(list.getLength() == 5);
    }
    
}
