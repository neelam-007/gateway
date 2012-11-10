/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 13, 2008
 * Time: 10:11:25 AM
 */
package com.l7tech.gateway.standardreports;

import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.l7tech.util.ConfigFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.Pair;

/**
 * Test coverage for class Utilities
 * Various tests test the method getPerformanceStatisticsMappingQuery. Each test is testing a specific characteristic of the sql
 * returned based on the parameter list supplied.
 */
public class UtilitiesTest {

    private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Utilities.DATE_STRING);

    /**
     * Tests the minimum requirement for a mapping query - one key supplied.
     * Checks that only 1 case statement exists in the returned sql
     * Checks that all other mapping keys are supplied in the select sql
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_OneKey() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> idFilters = new ArrayList<ReportApi.FilterPair>();
        idFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", idFilters);

        Map<String, Set<String>> serviceIdToOp = new HashMap<String, Set<String>>();

        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdToOp, keysToFilterPairs, 1, false, true);

        //There should only be 1 CASE statement in SQL
        String sql = sqlAndParamsPair.getKey();
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
        for (int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++) {
            index = sql.indexOf("mcmk.mapping" + (i + 1) + "_key");
            Assert.assertTrue(index != -1);
        }

        //';' AS MAPPING_VALUE_2, ';' AS MAPPING_VALUE_3, ';' AS MAPPING_VALUE_4, ';' AS MAPPING_VALUE_5
        //check that all other keys were selected as the placeholder
        for (int i = 1; i < Utilities.NUM_MAPPING_KEYS; i++) {
            index = sql.indexOf("AS MAPPING_VALUE_" + (i + 1));
            Assert.assertTrue(index != -1);
        }
    }

    /**
     * Checks that all values supplied as constraints on keys are used correctly in the generated sql
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_KeyValues() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();

        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null,
                        serviceIdsToOps, keysToFilterPairs, 1, false, true);

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

        String sql = sqlAndParamsPair.getKey();
        for (Map.Entry<String, List<ReportApi.FilterPair>> me : keysToFilterPairs.entrySet()) {
            String s = me.getKey();
            for (int z = 1; z <= Utilities.NUM_MAPPING_KEYS; z++) {
                int index = sql.indexOf("mcmk.mapping" + z + "_key = ?");
                Assert.assertTrue("'mcmk.mapping" + z + "_key = ?' should be in the sql: " + sql, index != -1);
            }

            for (ReportApi.FilterPair fp : me.getValue()) {
                if (!fp.isConstraintNotRequired()) {
                    for (int z = 1; z <= Utilities.NUM_MAPPING_KEYS; z++) {
                        int index = sql.indexOf("mcmv.mapping" + z + "_value = ?");
                        Assert.assertTrue("'mcmv.mapping" + z + "_value = ?' should be in the sql: " + sql, index != -1);
                    }
                }
            }

        }
    }

    //todo [Donal] add more logic to tests correct number of brackets
    //todo [Donal] needs more coverage now that a key can have 0..* filter pairs added
    /**
     * Check that any AND / LIKE filter constraints supplied are used correctly
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_KeyValuesFilters() {

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("*GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        List<ReportApi.FilterPair> custStatusFilters = new ArrayList<ReportApi.FilterPair>();
        custStatusFilters.add(new ReportApi.FilterPair("GOLD*"));
        keysToFilterPairs.put("CUSTOMER_STATUS", custStatusFilters);


        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();

        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps,
                        keysToFilterPairs, 1, false, true);

        //(
        // ( mcmk.mapping1_key = 'IP_ADDRESS' AND mcmv.mapping1_value = '127.0.0.1' ) OR
        // ( mcmk.mapping2_key = 'IP_ADDRESS' AND mcmv.mapping2_value = '127.0.0.1' ) OR
        // ( mcmk.mapping3_key = 'IP_ADDRESS' AND mcmv.mapping3_value = '127.0.0.1' ) OR
        // ( mcmk.mapping4_key = 'IP_ADDRESS' AND mcmv.mapping4_value = '127.0.0.1' ) OR
        // ( mcmk.mapping5_key = 'IP_ADDRESS' AND mcmv.mapping5_value = '127.0.0.1' )
        // )
        // AND
        // (
        //  ( mcmk.mapping1_key = 'CUSTOMER' AND mcmv.mapping1_value LIKE 'GOLD%' ) OR
        //  ( mcmk.mapping2_key = 'CUSTOMER' AND mcmv.mapping2_value LIKE 'GOLD%' ) OR
        //  ( mcmk.mapping3_key = 'CUSTOMER' AND mcmv.mapping3_value LIKE 'GOLD%' ) OR
        //  ( mcmk.mapping4_key = 'CUSTOMER' AND mcmv.mapping4_value LIKE 'GOLD%' ) OR
        //  ( mcmk.mapping5_key = 'CUSTOMER' AND mcmv.mapping5_value LIKE 'GOLD%' ))

        String sql = sqlAndParamsPair.getKey();
        for (Map.Entry<String, List<ReportApi.FilterPair>> me : keysToFilterPairs.entrySet()) {
            for (ReportApi.FilterPair fp : me.getValue()) {
                if (!fp.isConstraintNotRequired()) {
                    for (int z = 1; z <= Utilities.NUM_MAPPING_KEYS; z++) {
                        boolean useAnd = !fp.isQueryUsingWildCard();
                        String fValue = (useAnd) ? "=" : "LIKE";
                        int index = sql.indexOf("mcmk.mapping" + z + "_key = ?");
                        Assert.assertTrue(index != -1);
                        index = sql.indexOf("mcmv.mapping" + z + "_value" + fValue + " ?");
                    }
                }
            }

        }
    }

    /**
     * Test that when isDetail is true, no keys or other parameters are required, except for resolution
     * Checks that the real value of mcmv.service_operation is selected and not a placeholder
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_OnlyDetail() {
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();

        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1,
                        true, false);


        int index = sqlAndParamsPair.getKey().indexOf("mcmv.service_operation AS SERVICE_OPERATION_VALUE");
        Assert.assertTrue(index != -1);
        //System.out.println("OnlyDetail: "+sql);
    }

    /**
     * Tests that a non usage query can have a mapping query without any keys being required
     */
    @Test
    public void testNonUsageQueryNoKeysNeeded() {
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1,
                true, false);

        boolean exception = false;
        try {
            Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1,
                    true, true);
        }
        catch (Exception ex) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);
    }

    /**
     * Checks the generated sql for the following, when AUTH_USER has been added as a key, checks that the real value
     * for user is selected and not a placeholder
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_UseUser() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> userFilters = new ArrayList<ReportApi.FilterPair>();
        userFilters.add(new ReportApi.FilterPair("Donal"));
        userFilters.add(new ReportApi.FilterPair("Ldap User 1"));
        keysToFilterPairs.put("AUTH_USER", userFilters);

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();

        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, keysToFilterPairs, 1,
                        true, false);

        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("mcmv.auth_user_id AS AUTHENTICATED_USER");
        Assert.assertTrue(index != -1);

        //AND (mcmv.auth_user_id = 'Donal'  OR mcmv.auth_user_id = 'Ldap User 1' )

        for (ReportApi.FilterPair fp : userFilters) {
            index = sql.indexOf("mcmv.auth_user_id = ?");
            Assert.assertTrue("'mcmv.auth_user_id = ?' should have been in the sql: " + sql, index != -1);
        }
        //System.out.println("OnlyDetail: "+sql);
    }

    /**
     * Checks that the resolution being supplied, is always used
     * Checks that any resolution other than 1 or 2, causes an exception
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_Resolution() {

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1,
                        true, false);

        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("sm.resolution = ?");
        Assert.assertTrue(index != -1);

        //todo [Donal] this test can no longer check the value of resolution. Needs to look in the List<Object> to test
        sqlAndParamsPair = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 2, true
                , false);
        sql = sqlAndParamsPair.getKey();

        index = sql.indexOf("sm.resolution = ?");
        Assert.assertTrue(index != -1);
        //System.out.println("Resolution: "+sql);

        boolean exception = false;
        try {
            Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 3, true
                    , false);
        } catch (IllegalArgumentException iae) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Checks that start and end time are used correctly.
     * Checks that incorrect values cause an exception
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_Time() {
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);

        Calendar cal = Calendar.getInstance(tz);
        long startTime = cal.getTimeInMillis() - 1000;
        long endTime = cal.getTimeInMillis();

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, startTime, endTime, serviceIdsToOps, null, 1,
                        true, false);

        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("sm.period_start >= ?");
        Assert.assertTrue(index != -1);

        index = sql.indexOf("sm.period_start < ?");
        Assert.assertTrue(index != -1);

        boolean exception = false;
        try {
            Utilities.getPerformanceStatisticsMappingQuery(false, endTime, startTime, serviceIdsToOps, null, 1, true, false);
        } catch (IllegalArgumentException iae) {
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
    public void testGetPerformanceStatisticsMappingQuery_ServiceIds() {


        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        serviceIdsToOps.put("12345", null);
        serviceIdsToOps.put("67890", null);

        Pair<String, List<Object>> sqlAndParamsPair = Utilities.getPerformanceStatisticsMappingQuery(false, null, null,
                serviceIdsToOps, null, 1, true, false);

        //System.out.println("Service Ids: "+sql);

        //p.objectid IN (12345, 67890)
        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index != -1);

        //todo [Donal] this test can no longer validate the values. Needs to look inside the List<Object>
//        for (String s : serviceIdsToOps.keySet()) {
//            index = sql.indexOf(s);
//            Assert.assertTrue(index != -1);
//        }

        //check no constraint when no ids supplied
        serviceIdsToOps.clear();
        sqlAndParamsPair = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);
        sql = sqlAndParamsPair.getKey();
        index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index == -1);
    }

    //todo [Donal] this test is a bit loose, could pass when they constraint has not been implemented correctly
    @Test
    public void testGetPerformanceStatisticsMappingQuery_Operations() {

        Set<String> operations = new HashSet<String>();
        operations.add("listProducts");
        operations.add("orderProduct");

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        serviceIdsToOps.put("229376", new HashSet<String>(operations));
        serviceIdsToOps.put("229378", new HashSet<String>(operations));
        serviceIdsToOps.put("229380", new HashSet<String>(operations));
        serviceIdsToOps.put("229382", new HashSet<String>(operations));
        serviceIdsToOps.put("229384", new HashSet<String>(operations));

        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true, false);

        //System.out.println("Operation: "+sql);

        //AND mcmv.service_operation IN ('listProducts','orderProduct')
        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("mcmv.service_operation IN");
        Assert.assertTrue(index != -1);

        //todo [Donal] need to look inside List<Object> or else match the correct number of ?s
//        for (String s : operations) {
//            index = sql.indexOf(s);
//            Assert.assertTrue(index != -1);
//        }

        //check no constraint when no ids supplied
        serviceIdsToOps.clear();
        sqlAndParamsPair = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);
        sql = sqlAndParamsPair.getKey();
        index = sql.indexOf("mcmv.service_operation IN");
        //System.out.println("Operation: "+sql);
        Assert.assertTrue(index == -1);

        serviceIdsToOps.put("229376", new HashSet<String>());
        serviceIdsToOps.put("229378", new HashSet<String>());
        serviceIdsToOps.put("229380", null);
        serviceIdsToOps.put("229382", null);
        serviceIdsToOps.put("229384", new HashSet<String>());

        //also check when just the serivce id's are supplied, but no operations are
        sqlAndParamsPair = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);
        sql = sqlAndParamsPair.getKey();
        index = sql.indexOf("mcmv.service_operation IN");
        //System.out.println("Operation: "+sql);
        Assert.assertTrue(index == -1);

        //make sure that a constraint is provided, if at least one service has operations provided
        serviceIdsToOps.clear();
        serviceIdsToOps.put("229376", new HashSet<String>());
        serviceIdsToOps.put("229378", new HashSet<String>(operations));
        serviceIdsToOps.put("229380", null);
        serviceIdsToOps.put("229382", null);
        serviceIdsToOps.put("229384", new HashSet<String>());

        sqlAndParamsPair = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);
        sql = sqlAndParamsPair.getKey();
        index = sql.indexOf("mcmv.service_operation IN");
        //System.out.println("Operation: "+sql);
        Assert.assertTrue(index != -1);

    }

    //todo [Donal] update with policy violations and routing failures
    /**
     * Confirms that all fields which should be selected, are.
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_SelectFields() {

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true, false);

        //System.out.println("Select Fields: "+sql);

        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf(Utilities.SERVICE_ID);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_NAME);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.ROUTING_URI);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.THROUGHPUT);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.POLICY_VIOLATIONS);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.ROUTING_FAILURES);
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

    //todo [Donal] finish test
    @Test
    public void testgetUsageDistinctMappingQuery() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> filters = new ArrayList<ReportApi.FilterPair>();
        filters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", filters);
        keysToFilterPairs.put("CUSTOMER", filters);


        Pair<String, List<Object>> sqlAndParamsPair = Utilities.getDistinctMappingQuery(null, null, null,
                keysToFilterPairs, 2, true, true);

        keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);
        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        sqlAndParamsPair = Utilities.getDistinctMappingQuery(null, null, null, keysToFilterPairs, 2, true, false);
    }

    //todo [Donal] finish test
    @Test
    public void testGetUsageQuery() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> filters = new ArrayList<ReportApi.FilterPair>();
        filters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", filters);
        keysToFilterPairs.put("CUSTOMER", filters);

        Utilities.getUsageQuery(null, null, null, keysToFilterPairs, 2, false);
    }

//    public void testGetUsageColumnHeader(){
//        List<String> keys = new ArrayList<String>();
//        keys.add("IP_ADDRESS");
//        keys.add("CUSTOMER");
//
//        List<String> values = new ArrayList<String>();
//        values.add("127.0.0.1");
//        values.add("GOLD");
//
//        String authUser = "Donal";
//        String header = Utilities.getUsageColumnHeader(authUser, keys, values.toArray(new String[]{}));
////        System.out.println(header);
//    }

    /**
     * Checks that the gruop by order has all columns required present, and in the correct order.
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_GroupBy() {
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true, false);
        //System.out.println("Group by: "+sql);
        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("GROUP BY p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER , MAPPING_VALUE_1, " +
                "MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5");
        Assert.assertTrue(index != -1);
    }

    /**
     * Checks that the gruop by order has all columns required present, and in the correct order.
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_OrderBy() {
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true, false);
//        System.out.println("Order by: "+sql);
        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("ORDER BY AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, " +
                "MAPPING_VALUE_4, MAPPING_VALUE_5 ,p.objectid, SERVICE_OPERATION_VALUE");
        Assert.assertTrue(index != -1);
    }

    @Test
    public void testGetUsageMasterIntervalQuery() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> filters = new ArrayList<ReportApi.FilterPair>();
        filters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", filters);
        keysToFilterPairs.put("CUSTOMER", filters);

        String sql = Utilities.getUsageMasterIntervalQuery(null, null, null, keysToFilterPairs, 2, true).getKey();
        System.out.println(sql);

        sql = Utilities.getUsageMasterIntervalQuery(null, null, null, keysToFilterPairs, 2, false).getKey();
        System.out.println(sql);
    }

    /**
     * When the mapping query is a distinct query, check that all the expected fields are returned.
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_SelectDistinctFields() {

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getPerformanceStatisticsMappingQuery(true, null, null, serviceIdsToOps, null, 1, true,
                        false);

        String sql = sqlAndParamsPair.getKey();
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
    public void testCreateValueList() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        ipFilters.add(new ReportApi.FilterPair("127.0.0.2"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        custFilters.add(new ReportApi.FilterPair("SILVER"));
        keysToFilterPairs.put("CUSTOMER", custFilters);


        List<String> values = new ArrayList<String>();
        values.add("127.0.0.1");
        values.add("GOLD");
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);

        LinkedHashMap<String, List<ReportApi.FilterPair>> valueMap = Utilities.createDistinctKeyToFilterMap(keysToFilterPairs, values.toArray(new String[]{}), null, false);
        Assert.assertTrue(valueMap.keySet().size() == 2);

        int index = 0;
        for (String s : valueMap.keySet()) {
            List<ReportApi.FilterPair> fP = valueMap.get(s);
            Assert.assertTrue("List should only have 1 FilterPair", fP.size() == 1);
            Assert.assertTrue(values.get(index) + " should equal fP.get(0).getFilterValue()", values.get(index).equals(fP.get(0).getFilterValue()));
            index++;
        }

        values.clear();
        values.add("127.0.0.1");
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);

        boolean exception = false;
        try {
            Utilities.createDistinctKeyToFilterMap(keysToFilterPairs, values.toArray(new String[]{}), null, false);
        } catch (IllegalArgumentException iae) {
            exception = true;
        }
        Assert.assertTrue(exception);

        //test auth user
        keysToFilterPairs.clear();
        List<ReportApi.FilterPair> authFilters = new ArrayList<ReportApi.FilterPair>();
        authFilters.add(new ReportApi.FilterPair("Donal"));
        authFilters.add(new ReportApi.FilterPair("Ldap User 1"));
        keysToFilterPairs.put("AUTH_USER", authFilters);

        values.clear();
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);

        valueMap = Utilities.createDistinctKeyToFilterMap(keysToFilterPairs, values.toArray(new String[]{}), "Donal", false);
        Assert.assertTrue(valueMap.keySet().size() == 1);
        List<ReportApi.FilterPair> aFp = valueMap.get("AUTH_USER");
        Assert.assertTrue("AUTH_USER's only FilterPair should equal Donal, it was :" + aFp.get(0).getFilterValue(),
                aFp.get(0).getFilterValue().equals("Donal"));

        //test only is detail
        keysToFilterPairs.clear();
        valueMap = Utilities.createDistinctKeyToFilterMap(keysToFilterPairs, values.toArray(new String[]{}), null, true);
        Assert.assertTrue("valueMap should be empty", valueMap.isEmpty());
    }

    /**
     * Tests that the value returned from getAbsoluteMilliSeconds is correct
     */
    @Test
    public void testGetAbsoluteMilliSeconds_TimeZone() throws Exception {
        String date = "2008/11/28 23:00";
        long parisTime = Utilities.getAbsoluteMilliSeconds(date, "Europe/Paris");
        date = "2008/11/28 14:00";
        long canadaTime = Utilities.getAbsoluteMilliSeconds(date, "Canada/Pacific");
        Assert.assertTrue("GMT equivilents of 23:00 Paris time and 14:00 Canada time should equal", parisTime == canadaTime);
    }

    /**
     * Test that the Calendar instance returned from getCalendarForTimeUnit has the correct settings
     * based on the unit of time supplied
     *
     * @throws Exception
     */
    @Test
    public void testGetCalendarForTimeUnit() throws Exception {
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);

        Calendar control = Calendar.getInstance(tz);
        control.set(Calendar.MINUTE, 0);
        control.set(Calendar.SECOND, 0);
        control.set(Calendar.MILLISECOND, 0);

        Calendar cal = Utilities.getCalendarForTimeUnit(Utilities.UNIT_OF_TIME.HOUR, timeZone);
        Assert.assertTrue(control.getTimeInMillis() == cal.getTimeInMillis());

        cal = Utilities.getCalendarForTimeUnit(Utilities.UNIT_OF_TIME.DAY, timeZone);
        control = Calendar.getInstance(tz);
        control.set(Calendar.HOUR_OF_DAY, 0);
        control.set(Calendar.MINUTE, 0);
        control.set(Calendar.SECOND, 0);
        control.set(Calendar.MILLISECOND, 0);
        Assert.assertTrue(control.getTimeInMillis() == cal.getTimeInMillis());

        cal = Utilities.getCalendarForTimeUnit(Utilities.UNIT_OF_TIME.WEEK, timeZone);
        control = Calendar.getInstance(tz);
        control.set(Calendar.HOUR_OF_DAY, 0);
        control.set(Calendar.MINUTE, 0);
        control.set(Calendar.SECOND, 0);
        control.set(Calendar.MILLISECOND, 0);
        Assert.assertTrue(control.getTimeInMillis() == cal.getTimeInMillis());

        cal = Utilities.getCalendarForTimeUnit(Utilities.UNIT_OF_TIME.MONTH, timeZone);
        control = Calendar.getInstance(tz);
        control.set(Calendar.DAY_OF_MONTH, 1);
        control.set(Calendar.HOUR_OF_DAY, 0);
        control.set(Calendar.MINUTE, 0);
        control.set(Calendar.SECOND, 0);
        control.set(Calendar.MILLISECOND, 0);
        Assert.assertTrue(control.getTimeInMillis() == cal.getTimeInMillis());
    }


    /**
     * Specific tests for Utilities.getIntervalDisplayDate when the unit of time parameter is DAY
     */
    @Test
    public void testGetIntervalDisplayDate_Day() throws ParseException {
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);

        String startDate = "2008/08/31 14:12";
        DATE_FORMAT.setTimeZone(tz);
        Date d = DATE_FORMAT.parse(startDate);

        long startTime = d.getTime();
        d = DATE_FORMAT.parse("2008/09/01 14:12");
        long endTime = d.getTime();

        //Test day when it goes over a month bounday - when only 1 day is interval
        String timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 1, timeZone);
        String expected = "Sun Aug 31";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        //when 2 day interval
        d = DATE_FORMAT.parse("2008/09/02 14:12");
        endTime = d.getTime();
        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 2, timeZone);
        expected = "Sun Aug 31-Sep 2";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        //Test day when it goes over a year boundary - when only 1 day interval
        d = DATE_FORMAT.parse("2008/12/31 14:12");
        startTime = d.getTime();
        d = DATE_FORMAT.parse("2009/01/01 14:12");
        endTime = d.getTime();
        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 1, timeZone);
        expected = "Wed Dec 31 '09";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        //when 2 day interval
        d = DATE_FORMAT.parse("2008/12/31 14:12");
        startTime = d.getTime();
        d = DATE_FORMAT.parse("2009/01/02 14:12");
        endTime = d.getTime();
        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 2, timeZone);
        expected = "Wed Dec 31-Jan 2 '09";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));
    }

    /**
     * Test that the display string returned from getIntervalDisplayDate are correct
     *
     * @throws Exception
     */
    @Ignore(value = "See bug 13477. Logic needs to be updated for day light savings")
    @Test
    public void testGetIntervalDisplayDate() throws Exception {
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);

        String startDate = "2008/08/01 14:12";
        DATE_FORMAT.setTimeZone(tz);

        Date d = DATE_FORMAT.parse(startDate);
        long startTime = d.getTime();
        d = DATE_FORMAT.parse("2008/08/01 15:12");
        long endTime = d.getTime();

        String timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.HOUR, 1, timeZone);
        String expected = "08/01 14:12 - 15:12";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/01 16:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.HOUR, 2, timeZone);
        expected = "08/01 14:12 - 16:12";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/02 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 1, timeZone);
        expected = "Fri Aug 1";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/04 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 3, timeZone);
        expected = "Fri Aug 1-4";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/08 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.WEEK, 1, timeZone);
        expected = "08/01 - 08/08";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/15 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.WEEK, 2, timeZone);
        expected = "08/01 - 08/15";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/31 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.MONTH, 1, timeZone);
        expected = "2008 Aug";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals("2008 Aug"));

        d = DATE_FORMAT.parse("2008/09/30 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.MONTH, 2, timeZone);
        expected = "2008 Aug";
        Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals("2008 Aug"));

        boolean exception = false;
        try {
            d = DATE_FORMAT.parse("2008/08/01 16:12");
            endTime = d.getTime();
            timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.HOUR, 1, timeZone);
            expected = "08/01 14:12 - 16:12";
            Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));
        } catch (Exception e) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);

        exception = false;
        try {
            d = DATE_FORMAT.parse("2008/08/04 14:12");
            endTime = d.getTime();
            timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 2, timeZone);
            expected = "Fri 08/01";
            Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals("Fri 08/01"));
        } catch (Exception e) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);

        exception = false;
        try {
            d = DATE_FORMAT.parse("2008/08/15 14:12");
            endTime = d.getTime();
            timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.WEEK, 1, timeZone);
            expected = "08/01 - 08/15";
            Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals(expected));
        } catch (Exception e) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);

        exception = false;
        try {
            d = DATE_FORMAT.parse("2008/09/30 14:12");
            endTime = d.getTime();
            timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.MONTH, 1, timeZone);
            expected = "2008 Aug";
            Assert.assertTrue("Interval display string should equal: " + expected + " it was: " + timeDisplay, timeDisplay.equals("2008 Aug"));
        } catch (Exception e) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);
    }

    /**
     * Check that the intervals returned for a time period are correct. These intervals are used to drive all
     * sub queries for a specific service or set of mapping values.
     * Checks that the returned list has the correct number of intervals for each interval unit of time tested
     * Checks that IllegalArgumentException is thrown if the arguments are incorrect
     */
    @Test
    public void testGetIntervalsForTimePeriod() throws Exception {
        String timeZone = "Canada/Pacific";
        String startDate = "2008/10/12 00:00";
        String endDate = "2008/10/13 00:00";
        DATE_FORMAT.setTimeZone(Utilities.getTimeZone(timeZone));
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        //Hour
        List<Long> intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.UNIT_OF_TIME.HOUR, timeZone);

        //00:00 - 00:00, 24 hours but 25 is the interval size as it is 0 based and end time is exclusive.
        Assert.assertTrue(intervals.size() == 25);

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.UNIT_OF_TIME.HOUR, timeZone);

        Assert.assertTrue(intervals.size() == 13);

        //Day
        startDate = "2008/10/01 00:00";
        endDate = "2008/10/13 00:00";
        d = DATE_FORMAT.parse(startDate);
        timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        timePeriodEndExclusive = d.getTime();

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.UNIT_OF_TIME.DAY, timeZone);

        Assert.assertTrue(intervals.size() == 13);

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.UNIT_OF_TIME.DAY, timeZone);

        Assert.assertTrue(intervals.size() == 5);

        //Week
        startDate = "2008/09/01 00:00";
        endDate = "2008/10/13 00:00";
        d = DATE_FORMAT.parse(startDate);
        timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        timePeriodEndExclusive = d.getTime();

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.UNIT_OF_TIME.WEEK, timeZone);
        Assert.assertTrue(intervals.size() == 7);

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.UNIT_OF_TIME.WEEK, timeZone);
        Assert.assertTrue(intervals.size() == 4);

        //Month
        startDate = "2008/01/01 00:00";
        endDate = "2008/10/01 00:00";
        d = DATE_FORMAT.parse(startDate);
        timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        timePeriodEndExclusive = d.getTime();

        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.UNIT_OF_TIME.MONTH, timeZone);

        Assert.assertTrue(intervals.size() == 10);
        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.UNIT_OF_TIME.MONTH, timeZone);

        Assert.assertTrue(intervals.size() == 4);
    }

    /**
     * Tests the display string generated for the context mapping section of the report info section on a report.
     * Tests the string generated based on the various parameter combinations which this method accecpts
     */
    @Test
    public void testGetMappingReportInfoDisplayString() {
        String val = Utilities.getMappingReportInfoDisplayString(null, true, false, false);
        Assert.assertTrue(val.equals(Utilities.onlyIsDetailDisplayText));

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> filters = new ArrayList<ReportApi.FilterPair>();
        ReportApi.FilterPair authFilterPair1 = new ReportApi.FilterPair("Donal");
        filters.add(authFilterPair1);
        keysToFilterPairs.put("AUTH_USER", filters);

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, true, false);
        String expected = Utilities.AUTHENTICATED_USER_DISPLAY + ": (" + authFilterPair1.getFilterValue() + ")\n";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, true, true);
        expected = Utilities.AUTHENTICATED_USER_DISPLAY + ": (" + authFilterPair1.getFilterValue() + ")<br>";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        ReportApi.FilterPair authFilterPair2 = new ReportApi.FilterPair("Ldap User 1");
        filters.add(authFilterPair2);
        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, true, false);
        expected = Utilities.AUTHENTICATED_USER_DISPLAY + ": (" + authFilterPair1.getFilterValue() + ", " + authFilterPair2.getFilterValue() + ")\n";
        Assert.assertTrue(val.equals(expected));

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, true, true);
        expected = Utilities.AUTHENTICATED_USER_DISPLAY + ": (" + authFilterPair1.getFilterValue() + ", " + authFilterPair2.getFilterValue() + ")<br>";
        Assert.assertTrue(val.equals(expected));

        //exception, when all params are null or false
        boolean exception = false;
        try {
            Utilities.getMappingReportInfoDisplayString(null, false, false, false);
        } catch (IllegalArgumentException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, false, false);
        expected = "IP_ADDRESS\nCUSTOMER\n";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, false, true);
        expected = "IP_ADDRESS<br>CUSTOMER<br>";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, false, false);
        expected = "IP_ADDRESS (127.0.0.1)\nCUSTOMER (GOLD)\n";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, false, true);
        expected = "IP_ADDRESS (127.0.0.1)<br>CUSTOMER (GOLD)<br>";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, false, true);
        expected = "IP_ADDRESS (127.0.0.1)<br>CUSTOMER (GOLD)<br>";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        val = Utilities.getMappingReportInfoDisplayString(null, true, false, false);
        expected = Utilities.onlyIsDetailDisplayText;
        Assert.assertTrue("Is detail only query: " + expected + " actual: " + val, val.equals(expected));

    }

    /**
     * Tests the display string generated for each table of data for each distinct set of mapping values found.
     * Tests the string generated based on the various parameter combinations which this method accecpts
     */
    @Test
    public void testGetMappingValueDisplayString() {
        boolean exception = false;
        try {
            Utilities.getMappingValueDisplayString(null, null, null, false, null);
        } catch (NullPointerException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        exception = false;
        try {
            Utilities.getMappingValueDisplayString(null, Utilities.SQL_PLACE_HOLDER, null, false, null);
        } catch (NullPointerException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        String preFix = "Group values: ";
        LinkedHashMap<String, List<ReportApi.FilterPair>> emptyMap = new LinkedHashMap<String, List<ReportApi.FilterPair>>();

        String pH = Utilities.SQL_PLACE_HOLDER;
        String[] emptyStringArray = new String[]{pH, pH, pH, pH, pH};

        String val = Utilities.getMappingValueDisplayString(emptyMap, "Donal", emptyStringArray, true, preFix);
        Assert.assertTrue(val.equals(preFix + Utilities.AUTHENTICATED_USER_DISPLAY + ": Donal"));

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        keysToFilterPairs.put("CUSTOMER", custFilters);

        val = Utilities.getMappingValueDisplayString(keysToFilterPairs, Utilities.SQL_PLACE_HOLDER, new String[]{"127.0.0.1", "GOLD", pH, pH, pH}, true, preFix);
        String expected = preFix + "IP_ADDRESS: 127.0.0.1, CUSTOMER: GOLD";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        val = Utilities.getMappingValueDisplayString(keysToFilterPairs, "Donal", new String[]{"127.0.0.1", "GOLD", pH, pH, pH}, true, preFix);
        expected = preFix + Utilities.AUTHENTICATED_USER_DISPLAY + ": Donal, IP_ADDRESS: 127.0.0.1, CUSTOMER: GOLD";
        Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));

        try {
            ConfigFactory.clearCachedConfig();
            System.setProperty("com.l7tech.gateway.standardreports.truncate_mappings", "TRUE");
            System.setProperty("com.l7tech.gateway.standardreports.mapping_key_max_size", "100");
            System.setProperty("com.l7tech.gateway.standardreports.mapping_value_max_size", "20");

            val = Utilities.getMappingValueDisplayString(keysToFilterPairs, "Donal", new String[]{"127.0.0.1", "GOLD STATUS CUSTOMER WHICH IS LONG STATUS", pH, pH, pH}, true, preFix);
            expected = preFix + Utilities.AUTHENTICATED_USER_DISPLAY + ": Donal, IP_ADDRESS: 127.0.0.1, CUSTOMER: GOLD STAT...G STATUS";
            Assert.assertTrue("Expected: " + expected + " actual: " + val, val.equals(expected));
        } finally {
            System.clearProperty("com.l7tech.gateway.standardreports.truncate_mappings");
            System.clearProperty("com.l7tech.gateway.standardreports.mapping_key_max_size");
            System.clearProperty("com.l7tech.gateway.standardreports.mapping_value_max_size");
        }

        exception = false;
        try {
            Utilities.getMappingValueDisplayString(keysToFilterPairs, Utilities.SQL_PLACE_HOLDER, new String[]{"127.0.0.1", pH, pH, pH, pH}, true, preFix);
        } catch (IllegalStateException e) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testGetMilliSecondAsStringDate() throws Exception {
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);

        String parseDate = "2008/10/13 16:38";
        String expectedDate = "Oct 13, 2008 16:38";
        //MMM dd, yyyy HH:mm
        DATE_FORMAT.setTimeZone(tz);
        Date d = DATE_FORMAT.parse(parseDate);
        long timeMili = d.getTime();

        String milliAsDate = Utilities.getMilliSecondAsStringDate(timeMili, timeZone);
        Assert.assertTrue("milliAsDate should equal: " + expectedDate + ", actual value was: " + milliAsDate, expectedDate.equals(milliAsDate));
    }

    @Test
    public void testGetMillisForEndTimePeriod() {
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);
        Calendar calendar = Calendar.getInstance(tz);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long endTimeMilli = Utilities.getMillisForEndTimePeriod(Utilities.UNIT_OF_TIME.HOUR, timeZone);
        Assert.assertTrue(calendar.getTimeInMillis() == endTimeMilli);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        endTimeMilli = Utilities.getMillisForEndTimePeriod(Utilities.UNIT_OF_TIME.DAY, timeZone);
        Assert.assertTrue(calendar.getTimeInMillis() == endTimeMilli);
        endTimeMilli = Utilities.getMillisForEndTimePeriod(Utilities.UNIT_OF_TIME.WEEK, timeZone);
        Assert.assertTrue(calendar.getTimeInMillis() == endTimeMilli);

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        endTimeMilli = Utilities.getMillisForEndTimePeriod(Utilities.UNIT_OF_TIME.MONTH, timeZone);
        Assert.assertTrue(calendar.getTimeInMillis() == endTimeMilli);
    }

    @Test
    public void testGetNoMappingQuery_ValidInputs() throws ParseException {
        boolean exception = false;
        try {
            Utilities.getNoMappingQuery(false, null, null, null, 1);
        } catch (IllegalArgumentException e) {
            exception = true;
        }
        Assert.assertTrue(exception);

        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        exception = false;
        try {
            Utilities.getNoMappingQuery(false, endTime, startTime, null, 1);
        } catch (IllegalArgumentException e) {
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

        Pair<String, List<Object>> sqlAndParamsPair = Utilities.getNoMappingQuery(false, startTime, endTime, null, 1);
        String sql = sqlAndParamsPair.getKey();

        int index = sql.indexOf(Utilities.SERVICE_ID);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.SERVICE_NAME);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.ROUTING_URI);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.THROUGHPUT);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.POLICY_VIOLATIONS);
        Assert.assertTrue(index != -1);
        index = sql.indexOf(Utilities.ROUTING_FAILURES);
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
    public void testGetNoMappingQuery_Time() {

        Calendar cal = Calendar.getInstance();
        long startTime = cal.getTimeInMillis() - 1000;
        long endTime = cal.getTimeInMillis();

        Pair<String, List<Object>> sqlAndParamsPair = Utilities.getNoMappingQuery(false, startTime, endTime, null, 1);
        String sql = sqlAndParamsPair.getKey();

        int index = sql.indexOf("sm.period_start >= ?");
        Assert.assertTrue(index != -1);

        index = sql.indexOf("sm.period_start < ?");
        Assert.assertTrue(index != -1);

        boolean exception = false;
        try {
            Utilities.getNoMappingQuery(false, endTime, startTime, null, 1);
        } catch (IllegalArgumentException iae) {
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

        Pair<String, List<Object>> sqlAndParamsPair = Utilities.getNoMappingQuery(false, startTime, endTime, null, 1);
        String sql = sqlAndParamsPair.getKey();

        //todo [Donal] need to look inside List<Object> to validate value of resolution
        int index = sql.indexOf("sm.resolution = ?");
        Assert.assertTrue(index != -1);

        sqlAndParamsPair = Utilities.getNoMappingQuery(false, startTime, endTime, null, 2);
        sql = sqlAndParamsPair.getKey();
        index = sql.indexOf("sm.resolution = ?");
        Assert.assertTrue(index != -1);

        boolean exception = false;
        try {
            Utilities.getNoMappingQuery(false, startTime, endTime, null, 3);
        } catch (IllegalArgumentException iae) {
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

        Pair<String, List<Object>> sqlAndParamsPair =
                Utilities.getNoMappingQuery(false, startTime, endTime, serviceIds, 1);
        String sql = sqlAndParamsPair.getKey();

        //p.objectid IN (12345, 67890)
        int index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index != -1);

        //todo [Donal] need to look inside to validate
//        for (String s : serviceIds) {
//            index = sql.indexOf(s);
//            Assert.assertTrue(index != -1);
//        }

        //check no constraint when no ids supplied
        sqlAndParamsPair = Utilities.getNoMappingQuery(false, startTime, endTime, null, 1);
        sql = sqlAndParamsPair.getKey();

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

        Pair<String, List<Object>> sqlAndParamsPair = Utilities.getNoMappingQuery(false, startTime, endTime, null, 1);
        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("GROUP BY p.objectid");
        Assert.assertTrue(index != -1);
    }

    @Test
    public void testGetNoMappingQuery_SelectDistinctFields() throws ParseException {

        String startDate = "2008/10/01 00:00";
        long startTime = DATE_FORMAT.parse(startDate).getTime();
        String endDate = "2008/10/13 00:00";
        long endTime = DATE_FORMAT.parse(endDate).getTime();

        Pair<String, List<Object>> sqlAndParamsPair = Utilities.getNoMappingQuery(true, startTime, endTime, null, 1);
        String sql = sqlAndParamsPair.getKey();

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

        Pair<String, List<Object>> sqlAndParamsPair = Utilities.getNoMappingQuery(true, startTime, endTime, null, 1);
        String sql = sqlAndParamsPair.getKey();
        int index = sql.indexOf("ORDER BY p.objectid");
        Assert.assertTrue(index != -1);
    }

    /**
     * Confirm that the relative calculation for each unit of time allowed, yields the correct milli second time
     * in the past
     */
    @Test
    public void testGetRelativeMilliSecondsInPast() {

        //if this test fails it could be because the clock changed hour between
        //when the Calendar instance was retrieved and before the first calls to getRelativeMilliSecondsInPast
        //Calendar is retrieved for each time unit tested, to minimize the chance of this happening

        //hour
        String timeZone = "Europe/London";
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        Calendar calendar = Calendar.getInstance(tz);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.HOUR_OF_DAY, -1);
        long timeInPast = Utilities.getRelativeMilliSecondsInPast(1, Utilities.UNIT_OF_TIME.HOUR, timeZone);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);

        calendar.add(Calendar.HOUR_OF_DAY, -1);
        timeInPast = Utilities.getRelativeMilliSecondsInPast(2, Utilities.UNIT_OF_TIME.HOUR, timeZone);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);

        //day
        calendar = Calendar.getInstance(tz);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        calendar.add(Calendar.DAY_OF_MONTH, -2);
        timeInPast = Utilities.getRelativeMilliSecondsInPast(2, Utilities.UNIT_OF_TIME.DAY, timeZone);

        System.out.println("Calendar date: " + DATE_FORMAT.format(calendar.getTime()));
        Calendar testCal = Calendar.getInstance(tz);
        testCal.setTimeInMillis(timeInPast);
        System.out.println("Test Calendar date: " + DATE_FORMAT.format(testCal.getTime()));
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);

        //week
        calendar = Calendar.getInstance(tz);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        calendar.add(Calendar.WEEK_OF_YEAR, -2);
        timeInPast = Utilities.getRelativeMilliSecondsInPast(2, Utilities.UNIT_OF_TIME.WEEK, timeZone);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);

        //month
        calendar = Calendar.getInstance(tz);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        calendar.add(Calendar.MONTH, -2);
        timeInPast = Utilities.getRelativeMilliSecondsInPast(2, Utilities.UNIT_OF_TIME.MONTH, timeZone);
        Assert.assertTrue(calendar.getTimeInMillis() == timeInPast);
    }

    /**
     * This test was written as the second value of the calendar inside of Utilities was not being set to 0, which
     * was yielding inconsistent results in reports. This confirms that calling getRelativeMilliSecondsInPast will
     * be consistent. This test will fail if it runs across midnight for the specified timezone
     * @throws InterruptedException
     */
//    @Test
//    public void testGetMilliSecondsInPastRepeatable() throws InterruptedException {
//        long value = Utilities.getRelativeMilliSecondsInPast(1, Utilities.UNIT_OF_TIME.DAY, "Canada/Pacific");
//        Calendar cal = Calendar.getInstance();
//        cal.setTimeInMillis(value);
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SS");
//        String valueStr = dateFormat.format(cal.getTime());
//        //11 ensure we go past the second boundary
//        for(int i = 0; i < 11; i++){
//            long testValue = Utilities.getRelativeMilliSecondsInPast(1, Utilities.UNIT_OF_TIME.DAY, "Canada/Pacific");
//            Thread.sleep(100);
//            cal.setTimeInMillis(testValue);
//            String testStr = dateFormat.format(cal.getTime());
//            Assert.assertTrue(i+
//                    " Long values should match. Expected: " + value+"("+valueStr+") actual : " + testValue+"("+testStr+")"
//                    , testValue == value);
//        }
//    }

    /**
     * Ensure for various start and end times, that the correct resolution is used in sql queries
     */
    @Test
    public void testGetSummaryResolutionFromTimePeriod() throws ParseException {
        String timeZone = "Canada/Pacific";
        Calendar cal = Calendar.getInstance();
        long startTime = cal.getTimeInMillis();

        //time not important - if relative and hour then resolution must be 1
        int resolution = Utilities.getSummaryResolutionFromTimePeriod(startTime, startTime+ 1, timeZone, true, Utilities.UNIT_OF_TIME.HOUR);
        Assert.assertTrue(resolution == 1);

        // resolution of 1 required when hour is not 00:00 for start time
        cal.set(Calendar.HOUR_OF_DAY, 2);
        resolution = Utilities.getSummaryResolutionFromTimePeriod(cal.getTimeInMillis(), cal.getTimeInMillis() + 1, timeZone, false, null);
        Assert.assertTrue(resolution == 1);

        // resolution of 1 required when end time is not 00:00
        cal.set(Calendar.HOUR_OF_DAY, 0);
        startTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        long endTime = cal.getTimeInMillis();
        resolution = Utilities.getSummaryResolutionFromTimePeriod(startTime, endTime, timeZone, false, null);
        Assert.assertTrue(resolution == 1);

        // resolution of 2 when hour is 00:00 for start time not relative
        cal.set(Calendar.HOUR_OF_DAY, 0);
        startTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        endTime = cal.getTimeInMillis();

        resolution = Utilities.getSummaryResolutionFromTimePeriod(startTime, endTime, timeZone, false, null);
        Assert.assertTrue(resolution == 2);

        boolean exception = false;
        try {
            Utilities.getSummaryResolutionFromTimePeriod(endTime, startTime, timeZone, false, null);
        } catch (IllegalArgumentException iae) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Ensure for various start and end times, that the correct resolution is used in sql queries
     */
    @Test
    public void testGetIntervalResolutionFromTimePeriod() throws ParseException {
        String timeZone = "Canada/Pacific";
        Calendar cal = Calendar.getInstance();
        long startTime = cal.getTimeInMillis();
        cal.add(Calendar.HOUR_OF_DAY, 6);
        long endTime = cal.getTimeInMillis();
        // Hourly always required for an hourly interval
        int resolution = Utilities.getIntervalResolutionFromTimePeriod(Utilities.UNIT_OF_TIME.HOUR, startTime, endTime, timeZone, false, null);
        Assert.assertEquals(1, resolution);

        cal.add(Calendar.DAY_OF_MONTH, -40);
        startTime = cal.getTimeInMillis();

        resolution = Utilities.getIntervalResolutionFromTimePeriod(Utilities.UNIT_OF_TIME.HOUR, startTime, endTime, timeZone, true, Utilities.UNIT_OF_TIME.DAY);
        Assert.assertEquals(1, resolution);

        resolution = Utilities.getIntervalResolutionFromTimePeriod(Utilities.UNIT_OF_TIME.DAY, startTime, endTime, timeZone, true, Utilities.UNIT_OF_TIME.DAY);
        Assert.assertEquals(2, resolution);

        boolean exception = false;
        try {
            Utilities.getSummaryResolutionFromTimePeriod(endTime, startTime, timeZone, false, null);
        } catch (IllegalArgumentException iae) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Test the returned string is formatted correctly
     */
    @Test
    public void testGetStringNamesFromCollection() {
        List<String> strings = new ArrayList<String>();
        strings.add("one");
        strings.add("two");

        String val = Utilities.getStringNamesFromCollectionEscaped(strings);
        Assert.assertTrue(val.split(",").length == 2);

        strings.remove(1);
        val = Utilities.getStringNamesFromCollectionEscaped(strings);
        int index = val.indexOf(",");
        Assert.assertTrue(index == -1);

    }

    @Test
    public void testIsPlaceHolderValue() {
        String val = Utilities.SQL_PLACE_HOLDER;
        Assert.assertTrue(Utilities.isPlaceHolderValue(val));
    }


    @Test
    public void testGetIntervalAsString() {
        Utilities.UNIT_OF_TIME unitOfTime = Utilities.UNIT_OF_TIME.HOUR;
        String expectedValue = "Hour";
        String actualValue = Utilities.getIntervalAsString(unitOfTime, 1);
        Assert.assertTrue("expectedValue is: " + expectedValue + " actual value was " + actualValue, expectedValue.equals(actualValue));

        expectedValue = "Hours";
        actualValue = Utilities.getIntervalAsString(unitOfTime, 2);
        Assert.assertTrue("expectedValue is: " + expectedValue + " actual value was " + actualValue, expectedValue.equals(actualValue));
    }

    /**
     * Unnecessary test which I wrote as a sanity check to ensure that any iteration on a linked hash map
     * would be ordered
     */
    @Test
    public void testLinkedMapInterationOrder() {

        LinkedHashMap<String, Object> lM = new LinkedHashMap<String, Object>();
        lM.put("Donal", "test1");
        lM.put("aDonal", "test2");
        lM.put("zDonal", "test3");

        String[] control = new String[]{"Donal", "aDonal", "zDonal"};

        for (int i = 0; i < 100; i++) {
            int index = 0;
            for (String s : lM.keySet()) {
                //System.out.println(s);
                Assert.assertTrue(s + " should equal " + control[index], s.equals(control[index]));
                index++;
            }
        }
    }

    @Test
    public void testGetServiceAndIdDisplayString() {

        Map<String, Set<String>> serviceIdToOpMap = new HashMap<String, Set<String>>();
        Map<String, String> serviceIdToNameMap = new HashMap<String, String>();

        //using linked has set for predictability in 
        Set<String> srvAOps = new LinkedHashSet<String>();
        srvAOps.add("Op1");
        srvAOps.add("Op2");
        srvAOps.add("Op3");

        Set<String> srvBOps = new LinkedHashSet<String>();
        srvBOps.add("Op4");
        srvBOps.add("Op5");
        srvBOps.add("Op6");

        serviceIdToOpMap.put("123", srvAOps);
        serviceIdToOpMap.put("456", srvBOps);

        serviceIdToNameMap.put("123", "Service 1");
        serviceIdToNameMap.put("456", "Service 2");

        //first condition - printOperations = false
        String expected = "Service 1, Service 2";
        String actual = Utilities.getServiceAndIdDisplayString(null, serviceIdToNameMap, false);
        Assert.assertEquals("expectedValue is: " + expected + " actual value was " + actual, expected, actual);

        //should result in the empty string, see comment in getServiceAndIdDisplayString and usages for more info
        expected = "";
        actual = Utilities.getServiceAndIdDisplayString(null, serviceIdToNameMap, true);
        Assert.assertEquals("expectedValue is: " + expected + " actual value was " + actual, actual, expected);

        expected = "Service 1, Service 2";
        actual = Utilities.getServiceAndIdDisplayString(serviceIdToOpMap, serviceIdToNameMap, false);
        Assert.assertEquals("expectedValue is: " + expected + " actual value was " + actual, expected, actual);

        expected = "Service 1 -> Op1, Op2, Op3<br>Service 2 -> Op4, Op5, Op6";
        actual = Utilities.getServiceAndIdDisplayString(serviceIdToOpMap, serviceIdToNameMap, true);
        Assert.assertEquals("expectedValue is: " + expected + " actual value was " + actual, expected, actual);

        srvBOps.clear();
        expected = "Service 1 -> Op1, Op2, Op3<br>Service 2 -> All";
        actual = Utilities.getServiceAndIdDisplayString(serviceIdToOpMap, serviceIdToNameMap, true);
        Assert.assertEquals("expectedValue is: " + expected + " actual value was " + actual, expected, actual);

    }

    @Test
    public void testTruncateUsageGroupHeading() throws Exception {
        String input = "";
        String output = Utilities.truncateUsageGroupHeading(input);
        Assert.assertNotNull(output);

        input = "User1\nValue1 which is a long value\nValue2 which is also a long value";
        output = Utilities.truncateUsageGroupHeading(input);
        System.out.println(output);
        Assert.assertEquals("User1\nValue1 wh...ng value\nValue2 wh...ng value", output);

    }
}

