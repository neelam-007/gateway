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
import java.text.MessageFormat;
import java.io.*;

import org.junit.Test;
import org.junit.Assert;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.ResourceMapEntityResolver;
import com.l7tech.server.management.api.node.ReportApi;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperCompileManager;

/**
 * Test coverage for class Utilities
 * Various tests test the method getPerformanceStatisticsMappingQuery. Each test is testing a specific characteristic of the sql
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
    public void testGetPerformanceStatisticsMappingQuery_OneKey(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> idFilters = new ArrayList<ReportApi.FilterPair>();
        idFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", idFilters);

        Map<String, Set<String>> serviceIdToOp = new HashMap<String,Set<String>>();

        String sql =
                Utilities.getPerformanceStatisticsMappingQuery(false, null,null,serviceIdToOp ,keysToFilterPairs,1,false, true);

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
        System.out.println("OneKey: "+sql);
    }

//    @Test
//    public void testCreateMapingQueryNew(){
//        LinkedHashMap<String, List<FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<FilterPair>>();
//        List<FilterPair> idFilters = new ArrayList<FilterPair>();
//        idFilters.add(new FilterPair("127.0.0.1", true));
//        idFilters.add(new FilterPair("127.0.0.2", true));
//        keysToFilterPairs.put("IP_ADDRESS", idFilters);
//
//        String sql =
//                Utilities.createMappingQueryNew(false, null,null,null,keysToFilterPairs ,1,false, false);
//
//        System.out.println("Sql: " + sql);
//
//        List<FilterPair> custFilters = new ArrayList<FilterPair>();
//        custFilters.add(new FilterPair("GOLD", true));
//        //custFilters.add(new FilterPair("SIL%ER", false));
//        keysToFilterPairs.put("CUSTOMER", custFilters);
//
//        sql = Utilities.createMappingQueryNew(false, null,null,null,keysToFilterPairs ,1,false, false);
//        System.out.println("Sql: " + sql);
//    }
//
//    @Test
//    public void testCreateMapingQueryNew_UseUser(){
//        LinkedHashMap<String, List<FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<FilterPair>>();
//        List<FilterPair> idFilters = new ArrayList<FilterPair>();
//        idFilters.add(new FilterPair());
//        keysToFilterPairs.put("AUTH_USER", idFilters);
//
//        String sql =
//                Utilities.createMappingQueryNew(false, null,null,null,keysToFilterPairs ,1,false, false);
//
//        System.out.println("Sql: " + sql);
//    }
//
//    @Test
//    public void testCreateMapingQueryNew_(){
//        LinkedHashMap<String, List<FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<FilterPair>>();
//        List<FilterPair> idFilters = new ArrayList<FilterPair>();
//        idFilters.add(new FilterPair("127.0.0.1", true));
//        idFilters.add(new FilterPair("127.0.0.2", true));
//        keysToFilterPairs.put("IP_ADDRESS", idFilters);
//
//        String sql =
//                Utilities.createMappingQueryNew(false, null,null,null,keysToFilterPairs ,1,false, false);
//
//        System.out.println("Sql: " + sql);
//
//        List<FilterPair> custFilters = new ArrayList<FilterPair>();
//        custFilters.add(new FilterPair("GOLD", true));
//        //custFilters.add(new FilterPair("SIL%ER", false));
//        keysToFilterPairs.put("CUSTOMER", custFilters);
//
//        sql = Utilities.createMappingQueryNew(false, null,null,null,keysToFilterPairs ,1,false, false);
//        System.out.println("Sql: " + sql);
//    }
//
//    @Test
//    public void testCreateMapingQueryNew_OnlyKeys(){
//
//        LinkedHashMap<String, List<FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<FilterPair>>();
//        List<FilterPair> idFilters = new ArrayList<FilterPair>();
//        idFilters.add(new FilterPair());
//        idFilters.add(new FilterPair());
//        keysToFilterPairs.put("IP_ADDRESS", idFilters);
//
//        String sql =
//                Utilities.createMappingQueryNew(false, null,null,null,keysToFilterPairs ,1,false, false);
//
//        System.out.println("Sql: " + sql);
//
//        List<FilterPair> custFilters = new ArrayList<FilterPair>();
//        custFilters.add(new FilterPair("GOLD", true));
//        custFilters.add(new FilterPair());
//        keysToFilterPairs.put("CUSTOMER", custFilters);
//
//        sql = Utilities.createMappingQueryNew(false, null,null,null,keysToFilterPairs ,1,false, false);
//        System.out.println("Sql: " + sql);
//
//    }
//
//    @Test
//    public void testCreateMapingQueryNew_OnlyKeysOld(){
//
//        List<String> keys = new ArrayList<String>();
//        keys.add("IP_ADDRESS");
//        keys.add("CUSTOMER");
//        List<String> values = new ArrayList<String>();
//        values.add(null);
//        values.add(null);
//
//        Map<String, Set<String>> serviceIdToOp = new HashMap<String,Set<String>>();
//
//        String sql =
//                Utilities.getPerformanceStatisticsMappingQuery(false, null,null,serviceIdToOp ,keys ,values ,null,1,false, false,null);
//        System.out.println("Sql: " + sql);
//
//    }


//    @Test
//    public void testCreateMappingQuery_DisallowDuplicateKeys(){
//        LinkedHashMap<String, List<FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<FilterPair>>();
//        List<FilterPair> idFilters = new ArrayList<FilterPair>();
//        idFilters.add(new FilterPair());
//        keysToFilterPairs.put("IP_ADDRESS", idFilters);
//        keysToFilterPairs.put("IP_ADDRESS", idFilters);
//
//        Map<String, Set<String>> serviceIdToOp = new HashMap<String,Set<String>>();
//        boolean exception = false;
//        try{
//            String sql =
//                    Utilities.getPerformanceStatisticsMappingQuery(false, null,null,serviceIdToOp ,keysToFilterPairs,1,false, true);
//        }catch(Exception e){
//            exception = true;
//        }
//
//        Assert.assertTrue("Exception should be thrown due to duplicate keys", exception);
//
//    }

    //todo [Donal] finish test
    @Test
    public void testCreateMasterMappingQuery_KeyValues(){

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        Map<String, Set<String>> serviceIdToOp = new HashMap<String,Set<String>>();

        String sql = Utilities.getPerformanceStatisticsMappingQuery(true, null,null,serviceIdToOp, keysToFilterPairs ,1 ,false
                ,true);

        System.out.println(sql);

    }

    @Test
    public void testLinkedMapInterationOrder(){

        LinkedHashMap<String, Object> lM = new LinkedHashMap<String, Object>();
        lM.put("Donal", "test1");
        lM.put("aDonal", "test2");
        lM.put("zDonal", "test3");

        String [] control = new String[]{"Donal", "aDonal", "zDonal"};

        for(int i = 0; i < 100; i++){
            int index = 0;
            for(String s: lM.keySet()){
                //System.out.println(s);
                Assert.assertTrue(s+" should equal " + control[index], s.equals(control[index]));
                index++;
            }
        }
    }

    //todo [Donal] finish test
    /**
     * Test that the relationship of a service and any of it's selected operations are constrained correct
     */
    @Test
    public void testServiceAndOperation(){

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        List<String> operations = new ArrayList<String>();
        operations.add("listProducts");
        operations.add("listOrders");

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Set<String> ops = new HashSet<String>();
        ops.add("listProducts");
        serviceIdsToOps.put("360449", new HashSet<String>(ops));
        ops.clear();
        ops.add("listOrders");
        serviceIdsToOps.put("360450", new HashSet<String>(ops));

        String sql = Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps, keysToFilterPairs ,1 ,true ,
                true);

        System.out.println(sql);

        StringBuilder sb = new StringBuilder();
        Utilities.addServiceAndOperationConstraint(serviceIdsToOps, sb);
        System.out.println(sb.toString());

    }

    /**
     * Checks that all values supplied as constraints on keys are used correctly in the generated sql
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_KeyValues(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();

        String sql = Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps,keysToFilterPairs,1 ,false
                ,true);

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

        for(Map.Entry<String, List<ReportApi.FilterPair>> me: keysToFilterPairs.entrySet()){
            String s = me.getKey();
            for(int z = 1; z <= Utilities.NUM_MAPPING_KEYS; z++){
                int index = sql.indexOf("mcmk.mapping"+z+"_key = '"+s+"'");
                Assert.assertTrue("mcmk.mapping"+z+"_key = '"+s+"' should be in the sql: " + sql, index != -1);
            }

            for(ReportApi.FilterPair fp: me.getValue()){
                if(!fp.isEmpty()){
                    for(int z = 1; z <= Utilities.NUM_MAPPING_KEYS; z++){
                        int index = sql.indexOf("mcmv.mapping"+z+"_value = '"+fp.getFilterValue()+"'");
                        Assert.assertTrue("mcmv.mapping"+z+"_value = '"+fp.getFilterValue()+"' should be in the sql: " + sql, index != -1);
                    }
                }
            }

        }
    }

    //todo [Donal] add more logic to tests correct number of brackets
    /**
     * Check that any AND / LIKE filter constraints supplied are used correctly
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_KeyValuesFilters(){

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("%GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        List<ReportApi.FilterPair> custStatusFilters = new ArrayList<ReportApi.FilterPair>();
        custStatusFilters.add(new ReportApi.FilterPair("GOLD%"));
        keysToFilterPairs.put("CUSTOMER_STATUS", custStatusFilters);


        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();

        String sql = Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps,keysToFilterPairs, 1 ,false
                ,true);

        System.out.println("Filter Sql: "+sql);

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

        for(Map.Entry<String, List<ReportApi.FilterPair>> me: keysToFilterPairs.entrySet()){
            String s = me.getKey();

            for(ReportApi.FilterPair fp: me.getValue()){
                if(!fp.isEmpty()){
                    for(int z = 1; z <= Utilities.NUM_MAPPING_KEYS; z++){
                        boolean useAnd = fp.isUseAnd();
                        String fValue = (useAnd)?"=":"LIKE";
                        int index = sql.indexOf("mcmk.mapping"+z+"_key = '"+s+"'");
                        Assert.assertTrue(index != -1);
                        index = sql.indexOf("mcmv.mapping"+z+"_value "+fValue+" '"+fp.getFilterValue()+"'");
                        Assert.assertTrue(index != -1);
                        // );
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
    public void testGetPerformanceStatisticsMappingQuery_OnlyDetail(){
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();

        String sql =
                Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps ,null,1,
                        true ,false);

        int index = sql.indexOf("mcmv.service_operation AS SERVICE_OPERATION_VALUE");
        Assert.assertTrue(index != -1);
        //System.out.println("OnlyDetail: "+sql);
    }

    /**
     * Tests that a non usage query can have a mapping query without any keys being required
     */
    @Test
    public void testNonUsageQueryNoKeysNeeded(){
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps ,null,1,
                        true ,false);

        boolean exception = false;
        try{
            Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps ,null,1,
                            true ,true);
        }
        catch(Exception ex){
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);
    }

    /**
     * Checks the generated sql for the following, when useUser is true and when a list of authenticated users
     * is provided:-
     * Checks that the real value for user is selected and not a placeholder
     *
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_UseUser(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> userFilters = new ArrayList<ReportApi.FilterPair>();
        userFilters.add(new ReportApi.FilterPair("Donal"));
        userFilters.add(new ReportApi.FilterPair("Ldap User 1"));
        keysToFilterPairs.put("AUTH_USER", userFilters);

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();

        String sql =
                Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps ,keysToFilterPairs,1,
                        true ,false);

        int index = sql.indexOf("mcmv.auth_user_id AS AUTHENTICATED_USER");
        Assert.assertTrue(index != -1);

        //AND (mcmv.auth_user_id = 'Donal'  OR mcmv.auth_user_id = 'Ldap User 1' )

        for(ReportApi.FilterPair fp: userFilters){
            index = sql.indexOf("mcmv.auth_user_id = '"+fp.getFilterValue()+"'");
            Assert.assertTrue("mcmv.auth_user_id = '"+fp.getFilterValue()+"' should have been in the sql: " + sql,index != -1);
        }
        //System.out.println("OnlyDetail: "+sql);
    }

    /**
     * Checks that the resolution being supplied, is always used
     * Checks that any resolution other than 1 or 2, causes an exception
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_Resolution(){

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        String sql =
                Utilities.getPerformanceStatisticsMappingQuery(false, null,null,serviceIdsToOps,null,1,
                        true ,false);

        int index = sql.indexOf("sm.resolution = 1");
        Assert.assertTrue(index != -1);

        sql = Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps,null,2,true
                ,false);
        index = sql.indexOf("sm.resolution = 2");
        Assert.assertTrue(index != -1);
        //System.out.println("Resolution: "+sql);

        boolean exception = false;
        try{
            Utilities.getPerformanceStatisticsMappingQuery(false, null,null, serviceIdsToOps, null,3 ,true
                    ,false);
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
    public void testGetPerformanceStatisticsMappingQuery_Time(){
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);

        Calendar cal = Calendar.getInstance(tz);
        long startTime = cal.getTimeInMillis() - 1000;
        long endTime = cal.getTimeInMillis();

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        String sql = Utilities.getPerformanceStatisticsMappingQuery(false, startTime, endTime ,serviceIdsToOps,null,1,true ,false);

        int index = sql.indexOf("sm.period_start >="+startTime);
        Assert.assertTrue(index != -1);

        index = sql.indexOf("sm.period_start <"+endTime);
        Assert.assertTrue(index != -1);

        boolean exception = false;
        try{
            Utilities.getPerformanceStatisticsMappingQuery(false, endTime, startTime, serviceIdsToOps, null,1 ,true ,false);
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
    public void testGetPerformanceStatisticsMappingQuery_ServiceIds(){


        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        serviceIdsToOps.put("12345", null);
        serviceIdsToOps.put("67890", null);

        String sql = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);

        //System.out.println("Service Ids: "+sql);

        //p.objectid IN (12345, 67890)
        int index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index != -1);

        for(String s: serviceIdsToOps.keySet()){
            index = sql.indexOf(s);
            Assert.assertTrue(index != -1);
        }

        //check no constraint when no ids supplied
        serviceIdsToOps.clear();
        sql = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);
        index = sql.indexOf("p.objectid IN");
        Assert.assertTrue(index == -1);
    }

    @Test
    public void testGetPerformanceStatisticsMappingQuery_Operations(){

        Set<String> operations = new HashSet<String>();
        operations.add("listProducts");
        operations.add("orderProduct");

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        serviceIdsToOps.put("229376", new HashSet<String>(operations));
        serviceIdsToOps.put("229378", new HashSet<String>(operations));
        serviceIdsToOps.put("229380", new HashSet<String>(operations));
        serviceIdsToOps.put("229382", new HashSet<String>(operations));
        serviceIdsToOps.put("229384", new HashSet<String>(operations));

        String sql = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);

        //System.out.println("Operation: "+sql);

            //AND mcmv.service_operation IN ('listProducts','orderProduct')
        int index = sql.indexOf("mcmv.service_operation IN");
        Assert.assertTrue(index != -1);

        for(String s: operations){
            index = sql.indexOf(s);
            Assert.assertTrue(index != -1);
        }

        //check no constraint when no ids supplied
        serviceIdsToOps.clear();
        sql = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);
        index = sql.indexOf("mcmv.service_operation IN");
        //System.out.println("Operation: "+sql);
        Assert.assertTrue(index == -1);

        serviceIdsToOps.put("229376", new HashSet<String>());
        serviceIdsToOps.put("229378", new HashSet<String>());
        serviceIdsToOps.put("229380", null);
        serviceIdsToOps.put("229382", null);
        serviceIdsToOps.put("229384", new HashSet<String>());

        //also check when just the serivce id's are supplied, but no operations are
        sql = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);
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

        sql = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);
        index = sql.indexOf("mcmv.service_operation IN");
        //System.out.println("Operation: "+sql);
        Assert.assertTrue(index != -1);

    }

    //todo [Donal] update with policy violations and routing failures
    /**
     * Confirms that all fields which should be selected, are.
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_SelectFields(){

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        String sql = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, null, 1, true,
                false);

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

    //todo [Donal] finish test
    @Test
    public void testgetUsageDistinctMappingQuery(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> filters = new ArrayList<ReportApi.FilterPair>();
        filters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", filters);
        keysToFilterPairs.put("CUSTOMER", filters);


        String sql = Utilities.getUsageDistinctMappingQuery(null, null, null, keysToFilterPairs, 2, true, true);
        System.out.println(sql);

        keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);
        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        sql = Utilities.getUsageDistinctMappingQuery(null, null, null, keysToFilterPairs, 2, true, false);
        System.out.println(sql);
    }

    @Test
    public void testGetUsageQuery(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> filters = new ArrayList<ReportApi.FilterPair>();
        filters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", filters);
        keysToFilterPairs.put("CUSTOMER", filters);

        String sql = Utilities.getUsageQuery(null, null, null, keysToFilterPairs, 2, false);
        System.out.println(sql);
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
    public void testGetPerformanceStatisticsMappingQuery_GroupBy(){
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        String sql =
                Utilities.getPerformanceStatisticsMappingQuery(false, null,null,serviceIdsToOps,null,1, true, false);
        //System.out.println("Group by: "+sql);
        int index = sql.indexOf("GROUP BY p.objectid, SERVICE_OPERATION_VALUE, AUTHENTICATED_USER , MAPPING_VALUE_1, " +
                "MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5");
        Assert.assertTrue(index != -1);
    }

    /**
     * Checks that the gruop by order has all columns required present, and in the correct order.
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_OrderBy(){
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        String sql =
                Utilities.getPerformanceStatisticsMappingQuery(false, null,null,serviceIdsToOps,null,1, true, false);
//        System.out.println("Order by: "+sql);
        int index = sql.indexOf("ORDER BY AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, " +
                "MAPPING_VALUE_4, MAPPING_VALUE_5 ,p.objectid, SERVICE_OPERATION_VALUE");
        Assert.assertTrue(index != -1);
    }

    @Test
    public void testGetUsageMasterIntervalQuery(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> filters = new ArrayList<ReportApi.FilterPair>();
        filters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", filters);
        keysToFilterPairs.put("CUSTOMER", filters);

        String sql = Utilities.getUsageMasterIntervalQuery(null, null, null, keysToFilterPairs, 2, true);
        System.out.println(sql);

        sql = Utilities.getUsageMasterIntervalQuery(null, null, null, keysToFilterPairs, 2, false);
        System.out.println(sql);
    }

    /**
     * When the mapping query is a distinct query, check that all the expected fields are returned.
     */
    @Test
    public void testGetPerformanceStatisticsMappingQuery_SelectDistinctFields(){

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        String sql = Utilities.getPerformanceStatisticsMappingQuery(true, null, null, serviceIdsToOps, null, 1, true,
                false);

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
        for(String s: valueMap.keySet()){
            List<ReportApi.FilterPair> fP = valueMap.get(s);
            Assert.assertTrue("List should only have 1 FilterPair", fP.size() == 1);
            Assert.assertTrue(values.get(index)+" should equal fP.get(0).getFilterValue()", values.get(index).equals(fP.get(0).getFilterValue()));
            index++;
        }

        values.clear();
        values.add("127.0.0.1");
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);
        values.add(Utilities.SQL_PLACE_HOLDER);

        boolean exception = false;
        try{
            Utilities.createDistinctKeyToFilterMap(keysToFilterPairs, values.toArray(new String[]{}), null, false);
        }catch(IllegalArgumentException iae){
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
    public void testGetAbsoluteMilliSeconds() throws Exception{
        String date = "2008/11/28 23:00";
        long parisTime = Utilities.getAbsoluteMilliSeconds(date, "Europe/Paris");
        date = "2008/11/28 14:00";
        long canadaTime = Utilities.getAbsoluteMilliSeconds(date, "Canada/Pacific");
        Assert.assertTrue("GMT equivilents of 23:00 Paris time and 14:00 Canada time should equal", parisTime == canadaTime);
    }

    /**
     * Test that the Calendar instance returned from getCalendarForTimeUnit has the correct settings
     * based on the unit of time supplied
     * @throws Exception
     */
    @Test
    public void testGetCalendarForTimeUnit() throws Exception{
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
        String timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 1 ,timeZone);
        String expected = "Sun Aug 31";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        //when 2 day interval
        d = DATE_FORMAT.parse("2008/09/02 14:12");
        endTime = d.getTime();
        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 2 ,timeZone);
        expected = "Sun Aug 31-Sep 2";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        //Test day when it goes over a year boundary - when only 1 day interval
        d = DATE_FORMAT.parse("2008/12/31 14:12");
        startTime = d.getTime();
        d = DATE_FORMAT.parse("2009/01/01 14:12");
        endTime = d.getTime();
        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 1 ,timeZone);
        expected = "Wed Dec 31 '09";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        //when 2 day interval
        d = DATE_FORMAT.parse("2008/12/31 14:12");
        startTime = d.getTime();
        d = DATE_FORMAT.parse("2009/01/02 14:12");
        endTime = d.getTime();
        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 2 ,timeZone);
        expected = "Wed Dec 31-Jan 2 '09";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));
    }
    /**
     * Test that the display string returned from getIntervalDisplayDate are correct
     * @throws Exception
     */
    @Test
    public void testGetIntervalDisplayDate() throws Exception{
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);

        String startDate = "2008/08/01 14:12";
        DATE_FORMAT.setTimeZone(tz);
        
        Date d = DATE_FORMAT.parse(startDate);
        long startTime = d.getTime();
        d = DATE_FORMAT.parse("2008/08/01 15:12");
        long endTime = d.getTime();

        String timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.HOUR, 1 ,timeZone);
        String expected = "08/01 14:12 - 15:12";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/01 16:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.HOUR, 2 ,timeZone);
        expected = "08/01 14:12 - 16:12";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/02 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 1, timeZone);
        expected = "Fri Aug 1";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/04 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 3, timeZone);
        expected = "Fri Aug 1-4";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/08 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.WEEK, 1, timeZone);
        expected = "08/01 - 08/08";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/15 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.WEEK, 2, timeZone);
        expected = "08/01 - 08/15";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));

        d = DATE_FORMAT.parse("2008/08/31 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.MONTH, 1, timeZone);
        expected = "2008 Aug";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals("2008 Aug"));

        d = DATE_FORMAT.parse("2008/09/30 14:12");
        endTime = d.getTime();

        timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.MONTH, 2, timeZone);
        expected = "2008 Aug";
        Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals("2008 Aug"));

        boolean exception = false;
        try{
            d = DATE_FORMAT.parse("2008/08/01 16:12");
            endTime = d.getTime();
            timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.HOUR, 1 ,timeZone);
            expected = "08/01 14:12 - 16:12";
            Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));
        }catch (Exception e){
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);

        exception = false;
        try{
            d = DATE_FORMAT.parse("2008/08/04 14:12");
            endTime = d.getTime();
            timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.DAY, 2, timeZone);
            expected = "Fri 08/01";
            Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals("Fri 08/01"));
        }catch (Exception e){
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);

        exception = false;
        try{
            d = DATE_FORMAT.parse("2008/08/15 14:12");
            endTime = d.getTime();
            timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.WEEK, 1, timeZone);
            expected = "08/01 - 08/15";
            Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals(expected));
        }catch (Exception e){
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);

        exception = false;
        try{
            d = DATE_FORMAT.parse("2008/09/30 14:12");
            endTime = d.getTime();
            timeDisplay = Utilities.getIntervalDisplayDate(startTime, endTime, Utilities.UNIT_OF_TIME.MONTH, 1, timeZone);
            expected = "2008 Aug";
            Assert.assertTrue("Interval display string should equal: " + expected+" it was: " + timeDisplay, timeDisplay.equals("2008 Aug"));
        }catch (Exception e){
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
    public void testGetIntervalsForTimePeriod() throws Exception{
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
    public void testGetMappingReportInfoDisplayString(){
        String val = Utilities.getMappingReportInfoDisplayString(null, true, false);
        Assert.assertTrue(val.equals(Utilities.onlyIsDetailDisplayText));

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> filters = new ArrayList<ReportApi.FilterPair>();
        ReportApi.FilterPair authFilterPair1 = new ReportApi.FilterPair("Donal");
        filters.add(authFilterPair1);
        keysToFilterPairs.put("AUTH_USER", filters);

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, true);
        String expected = Utilities.AUTHENTICATED_USER_DISPLAY+": ("+authFilterPair1.getFilterValue()+")"; 
        Assert.assertTrue("Expected: " + expected+" actual: " + val,val.equals(expected));

        ReportApi.FilterPair authFilterPair2 = new ReportApi.FilterPair("Ldap User 1");
        filters.add(authFilterPair2);
        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, true);
        Assert.assertTrue(val.equals(Utilities.AUTHENTICATED_USER_DISPLAY+": ("+authFilterPair1.getFilterValue()+", "+authFilterPair2.getFilterValue()+")"));

        //exception, when all params are null or false
        boolean exception = false;
        try{
            Utilities.getMappingReportInfoDisplayString(null, false, false);
        }catch (IllegalArgumentException e){
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

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, false);
        Assert.assertTrue(val.equals("IP_ADDRESS, CUSTOMER"));

        keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        val = Utilities.getMappingReportInfoDisplayString(keysToFilterPairs, false, false);
        expected = "IP_ADDRESS (127.0.0.1), CUSTOMER (GOLD)";
        Assert.assertTrue("Expected: " + expected+" actual: " + val,val.equals(expected));

    }

    /**
     * Tests the display string generated for each table of data for each distinct set of mapping values found.
     * Tests the string generated based on the various parameter combinations which this method accecpts
     */
    @Test
    public void testGetMappingValueDisplayString(){
        boolean exception = false;
        try{
            Utilities.getMappingValueDisplayString(null, null, null, false, null);
        }catch (NullPointerException e){
            exception = true;
        }
        Assert.assertTrue(exception);

        exception = false;
        try{
            Utilities.getMappingValueDisplayString(null, Utilities.SQL_PLACE_HOLDER, null, false, null);
        }catch (NullPointerException e){
            exception = true;
        }
        Assert.assertTrue(exception);

        String preFix = "Group values: ";
        LinkedHashMap<String, List<ReportApi.FilterPair>> emptyMap = new LinkedHashMap<String, List<ReportApi.FilterPair>>();

        String pH = Utilities.SQL_PLACE_HOLDER;
        String [] emptyStringArray = new String[]{pH, pH, pH, pH, pH};
        
        String val = Utilities.getMappingValueDisplayString(emptyMap,"Donal",emptyStringArray, true, preFix);
        Assert.assertTrue(val.equals(preFix+Utilities.AUTHENTICATED_USER_DISPLAY+": Donal"));

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        keysToFilterPairs.put("CUSTOMER", custFilters);

        val = Utilities.getMappingValueDisplayString(keysToFilterPairs, Utilities.SQL_PLACE_HOLDER, new String[]{"127.0.0.1","GOLD", pH, pH, pH}, true, preFix);
        String expected = preFix+"IP_ADDRESS: 127.0.0.1, CUSTOMER: GOLD";
        Assert.assertTrue("Expected: " + expected+" actual: " + val,val.equals(expected));

        val = Utilities.getMappingValueDisplayString(keysToFilterPairs, "Donal", new String[]{"127.0.0.1","GOLD", pH,pH,pH}, true, preFix);
        expected = preFix+Utilities.AUTHENTICATED_USER_DISPLAY+": Donal, IP_ADDRESS: 127.0.0.1, CUSTOMER: GOLD";
        Assert.assertTrue("Expected: " + expected+" actual: " + val,val.equals(expected));

        exception = false;
        try{
            Utilities.getMappingValueDisplayString(keysToFilterPairs, Utilities.SQL_PLACE_HOLDER, new String[]{"127.0.0.1",pH,pH,pH,pH}, true, preFix);
        }catch (IllegalStateException e){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testGetMilliSecondAsStringDate() throws Exception{
        String timeZone = "Canada/Pacific";
        TimeZone tz = Utilities.getTimeZone(timeZone);

        String parseDate = "2008/10/13 16:38";
        String expectedDate = "Oct 13, 2008 16:38";
        //MMM dd, yyyy HH:mm
        DATE_FORMAT.setTimeZone(tz);
        Date d = DATE_FORMAT.parse(parseDate);
        long timeMili = d.getTime();

        String milliAsDate = Utilities.getMilliSecondAsStringDate(timeMili, timeZone);
        Assert.assertTrue("milliAsDate should equal: " + expectedDate+", actual value was: " + milliAsDate,expectedDate.equals(milliAsDate));
    }

    @Test
    public void testGetMillisForEndTimePeriod(){
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
        System.out.println(sql);
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
        Calendar cal = Calendar.getInstance();
        long startTime = cal.getTimeInMillis();
        cal.add(Calendar.HOUR_OF_DAY, 6);
        long endTime = cal.getTimeInMillis();
        int resolution = Utilities.getSummaryResolutionFromTimePeriod(24, startTime, endTime);
        Assert.assertTrue(resolution == 1);

        cal.add(Calendar.DAY_OF_MONTH, -40);
        startTime = cal.getTimeInMillis();
        resolution = Utilities.getSummaryResolutionFromTimePeriod(24, startTime, endTime);
        Assert.assertTrue(resolution == 2);

        boolean exception = false;
        try{
            Utilities.getSummaryResolutionFromTimePeriod(24, endTime, startTime);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Ensure for various start and end times, that the correct resolution is used in sql queries
     */
    @Test
    public void testGetIntervalResolutionFromTimePeriod() throws ParseException {
        Calendar cal = Calendar.getInstance();
        long startTime = cal.getTimeInMillis();
        cal.add(Calendar.HOUR_OF_DAY, 6);
        long endTime = cal.getTimeInMillis();
        int resolution = Utilities.getIntervalResolutionFromTimePeriod(Utilities.UNIT_OF_TIME.HOUR, 24, startTime, endTime);
        Assert.assertTrue(resolution == 1);

        cal.add(Calendar.DAY_OF_MONTH, -40);
        startTime = cal.getTimeInMillis();

        resolution = Utilities.getIntervalResolutionFromTimePeriod(Utilities.UNIT_OF_TIME.HOUR, 24, startTime, endTime);
        Assert.assertTrue(resolution == 1);

        resolution = Utilities.getIntervalResolutionFromTimePeriod(Utilities.UNIT_OF_TIME.DAY, 24, startTime, endTime);
        Assert.assertTrue(resolution == 2);

        //test that a time period to far in the past fails for hourly interval
        boolean exception = false;
        try{

            long oldStartTime = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 2);
            long oldEndTime = cal.getTimeInMillis();

            Utilities.getIntervalResolutionFromTimePeriod(Utilities.UNIT_OF_TIME.HOUR , 24, oldStartTime, oldEndTime);
        }catch(UtilityConstraintException e){
            exception = true;
        }
        Assert.assertTrue(exception);

        exception = false;
        try{
            Utilities.getSummaryResolutionFromTimePeriod(24, endTime, startTime);
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
            RuntimeDocUtilities.getUsageRuntimeDoc(null, null);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testGetUsageRuntimeDoc_NotNull(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Assert.assertNotNull(doc);
    }

    /**
     * Don't change the number of elements, as you will break the tests which use this as convenience
     * @return
     */
    private LinkedHashMap<String, List<ReportApi.FilterPair>> getTestKeys(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        return keysToFilterPairs;
    }

    /**
     * Don't change the number of elements, as you will break the tests which use this as convenience
     * @return
     */
    private LinkedHashSet<List<String>> getTestDistinctMappingSets(){
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        for(int i = 0; i < 4; i++){
            List<String> valueList = new ArrayList<String>();
            valueList.add("Donal");
            valueList.add("127.0.0.1");
            valueList.add("Bronze"+i);//make each list unique - set is smart, not just object refs
            distinctMappingSets.add(valueList);
        }
        return distinctMappingSets;
    }

    @Test
    public void testGetUsageRuntimeDoc_FirstLevelElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
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
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();
        //3 sets of variables X 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.VARIABLES+" element should have " + (3 * distinctMappingSets.size()) + " elements",
                list.getLength() == (3 * distinctMappingSets.size()));

    }

    /**
     * Validate the variables created, Check that their name, class, calculation, resetType and resetGroup if applicable
     * are correct.
     */
    @Test
    public void testGetUsageRuntimeDoc_VariablesNames(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();
        String [] variableNames = new String[]{"COLUMN_","COLUMN_MAPPING_TOTAL_","COLUMN_SERVICE_TOTAL_"};
        String [] calculations = new String[]{"Nothing","Sum","Sum"};
        String [] resetTypes = new String[]{"Group","Group","Group"};
        String [] resetGroups = new String[]{"SERVICE_AND_OPERATION","CONSTANT","SERVICE_ID"};

        int index = 0;
        int varIndex = 1;
        for(int i = 0; i < list.getLength(); i++,varIndex++){
            if(i > 0 && (i % distinctMappingSets.size()  == 0)){
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            String expectedValue = variableNames[index % distinctMappingSets.size()]+varIndex;
            Assert.assertTrue("Name attribute should equal " + expectedValue+" actual value was: " + varName.getNodeValue()
                    ,varName.getNodeValue().equals(expectedValue));

            expectedValue = calculations[index % distinctMappingSets.size()];
            Node calcName = attributes.getNamedItem("calculation");

            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " +
                    calcName.getNodeValue() ,calcName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = resetTypes[index % distinctMappingSets.size()];
            Assert.assertTrue("resetType attribute should equal " + expectedValue+ " Actual value was: " + resetType
                    ,resetType.equals(expectedValue));

            if(resetType.equals("Group")){
                //check the actual group used to reset the variable is correct
                Node varResetGroup = attributes.getNamedItem("resetGroup");
                String resetGroup = varResetGroup.getNodeValue();

                expectedValue = resetGroups[index % distinctMappingSets.size()];
                Assert.assertTrue("resetGroup attribute should equal " + expectedValue+ " Actual value was: " + resetGroup
                        ,resetGroup.equals(expectedValue));
            }
        }
    }

    @Test
    public void testGetUsageRuntimeDoc_ConstantHeader(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_HEADER);
        list = list.item(0).getChildNodes();
        //1 text fields which are hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.CONSTANT_HEADER+" element should have " + (distinctMappingSets.size() + 1) + " elements, " +
                "instead it had " + list.getLength(),
                list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ConstantHeader_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_HEADER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);
            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            NamedNodeMap attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            String expectedValue = "java.lang.String";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.MAPPING_VALUE_FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            if(i == 0) expectedIntValue = Utilities.CONSTANT_HEADER_START_X;
            else expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            expectedValue = Utilities.USAGE_TABLE_HEADING_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));


        }
    }

    private Node findFirstChildElementByName( Node parent, String name ) {
        if ( name == null ) throw new IllegalArgumentException( "name must be non-null!" );
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE && name.equals( n.getNodeName())){
                return n;
            }
        }
        return null;
    }

    @Test
    public void testGetUsageRuntimeDoc_ServiceOperationFooter(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.SERVICE_AND_OPERATION_FOOTER+" element should have " + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ServiceOperationFooter_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //printOutDocument(doc);
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if(i < list.getLength() - 1) expectedValue = "TableCell";
            else expectedValue = Utilities.ROW_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            if(i == list.getLength() - 1){
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                        ,actualValue.equals(expectedValue));
            }

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "$V{COLUMN_"+(i+1)+"}";
            else expectedValue = "$V{SERVICE_AND_OR_OPERATION_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void testGetUsageRuntimeDoc_SerivceIdFooter(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_ID_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.SERVICE_ID_FOOTER+" element should have " + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ServiceIdFooter_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_ID_FOOTER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue = Utilities.ROW_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "$V{COLUMN_SERVICE_TOTAL_"+(i+1)+"}";
            else expectedValue = "$V{SERVICE_ONLY_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void testGetUsageRuntimeDoc_ConstantFooter(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.CONSTANT_FOOTER+" element should have " + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ConstantFooter_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_FOOTER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue = Utilities.ROW_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "$V{COLUMN_MAPPING_TOTAL_"+(i+1)+"}";
            else expectedValue = "$V{GRAND_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageRuntimeDoc_CheckWidths(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Element rootNode = doc.getDocumentElement();
        testWidths(rootNode, distinctMappingSets.size());
    }

    /**
     * Validate the variables created, Check that their name, class, calculation, resetType and resetGroup if applicable
     * are correct.
     */
    @Test
    public void getUsageIntervalMasterRuntimeDoc_Variables(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();
        String [] variableNames = new String[]{"COLUMN_SERVICE_","COLUMN_OPERATION_","COLUMN_REPORT_"};
        String [] resetTypes = new String[]{"Group","Group","Report"};
        String [] resetGroups = new String[]{"SERVICE","SERVICE_OPERATION",null};
        String [] calculations = new String[]{"Sum","Sum","Sum"};

        int index = 0;
        int varIndex = 1;
        for(int i = 0; i < list.getLength(); i++,varIndex++){
            if(i > 0 && (i % distinctMappingSets.size()  == 0)){
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = variableNames[index % distinctMappingSets.size()]+varIndex;
            Assert.assertTrue("Name attribute should equal " + expectedValue+ " Actual value was: " + varName.getNodeValue()
                    ,varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = resetTypes[index % distinctMappingSets.size()];
            Assert.assertTrue("resetType attribute should equal " + expectedValue+ " Actual value was: " + resetType
                    ,resetType.equals(expectedValue));

            if(resetType.equals("Group")){
                //check the actual group used to reset the variable is correct
                Node varResetGroup = attributes.getNamedItem("resetGroup");
                String resetGroup = varResetGroup.getNodeValue();

                expectedValue = resetGroups[index % distinctMappingSets.size()];
                Assert.assertTrue("resetGroup attribute should equal " + expectedValue+ " Actual value was: " + resetGroup
                        ,resetGroup.equals(expectedValue));
            }

            expectedValue = calculations[index % distinctMappingSets.size()];
            Node calcName = attributes.getNamedItem("calculation");

            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " +
                    calcName.getNodeValue() ,calcName.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceHeader_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_HEADER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.MAPPING_VALUE_FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i == list.getLength() - 1) expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;
            else expectedIntValue = Utilities.DATA_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            if(i == 0) expectedIntValue = Utilities.CONSTANT_HEADER_START_X;
            else expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);

            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue = Utilities.USAGE_TABLE_HEADING_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.String";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_CheckSubReport(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.RETURN_VALUE);
        list = list.item(0).getChildNodes();
        String [] variableNames = new String[]{"COLUMN_SERVICE_","COLUMN_OPERATION_","COLUMN_REPORT_"};
        String [] calculations = new String[]{"Sum","Sum","Sum"};

        int index = 0;
        int varIndex = 1;
        for(int i = 0; i < list.getLength(); i++,varIndex++){
            if(i > 0 && (i % distinctMappingSets.size()  == 0)){
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node calculation = attributes.getNamedItem("calculation");

            String expectedValue = calculations[index % distinctMappingSets.size()];
            String actualValue = calculation.getNodeValue();
            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node subreportVariable = attributes.getNamedItem("subreportVariable");
            expectedValue = "COLUMN_" + varIndex;
            actualValue = subreportVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node toVariable = attributes.getNamedItem("toVariable");
            expectedValue = variableNames[index % distinctMappingSets.size()] + varIndex;
            actualValue = toVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

        }
     }

    private void testGroupTotalRow(String elementName, String columnVariable, String totalVariable){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(elementName);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue = Utilities.ROW_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "$V{"+columnVariable+(i+1)+"}";
            else expectedValue = "$V{"+totalVariable+"}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceAndOperationFooter_CheckElements(){
        testGroupTotalRow(Utilities.SERVICE_AND_OPERATION_FOOTER,"COLUMN_OPERATION_","ROW_OPERATION_TOTAL");
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceIdFooter_CheckElements(){
        testGroupTotalRow(Utilities.SERVICE_ID_FOOTER,"COLUMN_SERVICE_","ROW_SERVICE_TOTAL");
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_summary_CheckElements(){
        testGroupTotalRow(Utilities.SUMMARY,"COLUMN_REPORT_","ROW_REPORT_TOTAL");
    }

    private void testWidths(Node rootNode, int numMappingValues){
        Node pageWidth = findFirstChildElementByName(rootNode, "pageWidth");
        int expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * numMappingValues)
                + Utilities.TOTAL_COLUMN_WIDTH + Utilities.LEFT_MARGIN_WIDTH + Utilities.RIGHT_MARGIN_WIDTH;
        int actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node columnWidth = findFirstChildElementByName(rootNode, "columnWidth");
        expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * numMappingValues)
                + Utilities.TOTAL_COLUMN_WIDTH;
        actualIntValue = Integer.valueOf(columnWidth.getTextContent());

        Assert.assertTrue("columnWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node frameWidth = findFirstChildElementByName(rootNode, "frameWidth");
        expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * numMappingValues)
                + Utilities.TOTAL_COLUMN_WIDTH;
        actualIntValue = Integer.valueOf(frameWidth.getTextContent());

        Assert.assertTrue("frameWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node leftMargin = findFirstChildElementByName(rootNode, "leftMargin");
        expectedIntValue = Utilities.LEFT_MARGIN_WIDTH;
        actualIntValue = Integer.valueOf(leftMargin.getTextContent());

        Assert.assertTrue("leftMargin element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node rightMargin = findFirstChildElementByName(rootNode, "rightMargin");
        expectedIntValue = Utilities.RIGHT_MARGIN_WIDTH;
        actualIntValue = Integer.valueOf(rightMargin.getTextContent());

        Assert.assertTrue("rightMargin element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_CheckWidths(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Element rootNode = doc.getDocumentElement();
        testWidths(rootNode, distinctMappingSets.size());
    }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_Variables(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = "COLUMN_"+(i+1);
            Assert.assertTrue("Name attribute should equal " + expectedValue+ " Actual value was: " + varName.getNodeValue()
                    ,varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = "Report";
            Assert.assertTrue("resetType attribute should equal " + expectedValue+ " Actual value was: " + resetType
                    ,resetType.equals(expectedValue));

            Node calcName = attributes.getNamedItem("calculation");
            expectedValue = "Sum";

            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " +
                    calcName.getNodeValue() ,calcName.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_CheckSubReport(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();
        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.RETURN_VALUE);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node calculation = attributes.getNamedItem("calculation");

            String expectedValue = "Sum";
            String actualValue = calculation.getNodeValue();
            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node subreportVariable = attributes.getNamedItem("subreportVariable");
            expectedValue = "COLUMN_" + (i+1);
            actualValue = subreportVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node toVariable = attributes.getNamedItem("toVariable");
            expectedValue = "COLUMN_" + (i+1);
            actualValue = toVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

        }
     }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_CheckWidths(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        Element rootNode = doc.getDocumentElement();

        Node subReportWidth = findFirstChildElementByName(rootNode, "subReportWidth");
        int expectedIntValue = (Utilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + Utilities.TOTAL_COLUMN_WIDTH;
        int actualIntValue = Integer.valueOf(subReportWidth.getTextContent());

        Assert.assertTrue("subReportWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node pageWidth = findFirstChildElementByName(rootNode, "pageWidth");
        expectedIntValue = (Utilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + Utilities.TOTAL_COLUMN_WIDTH
                + Utilities.SUB_INTERVAL_STATIC_WIDTH;
        actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

    }

    @Test
    public void getUsageSubReportRuntimeDoc_Variables(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = "COLUMN_"+(i+1);
            Assert.assertTrue("Name attribute should equal " + expectedValue+ " Actual value was: " + varName.getNodeValue()
                    ,varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = "None";
            Assert.assertTrue("resetType attribute should equal " + expectedValue+ " Actual value was: " + resetType
                    ,resetType.equals(expectedValue));

            Node calcName = attributes.getNamedItem("calculation");
            expectedValue = "Nothing";

            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " +
                    calcName.getNodeValue() ,calcName.getNodeValue().equals(expectedValue));

            //variableExpression
            Node variableExpression = findFirstChildElementByName(list.item(i), "variableExpression");

            expectedValue = "((UsageSummaryAndSubReportHelper)$P{REPORT_SCRIPTLET}).getColumnValue(\"COLUMN_"+(i+1)+"\", " +
                    "$F{AUTHENTICATED_USER},new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3}," +
                    "$F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})";
            String actualValue = variableExpression.getTextContent();
            Assert.assertTrue("variableExpression elements text should equal " + expectedValue+ " Actual value was: " +
                    actualValue ,actualValue.equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_serviceAndOperationFooter_CheckElements(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.DATA_COLUMN_WIDTH * i;
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if(i < list.getLength() - 1) expectedValue = "TableCell";
            else expectedValue = Utilities.ROW_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            if(i == list.getLength() - 1){
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                        ,actualValue.equals(expectedValue));
            }

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "$V{COLUMN_"+(i+1)+"}";
            else expectedValue = "$V{TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_noData_CheckElements(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.NO_DATA);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){
            Node staticTextField = list.item(i);

            Node reportElementNode = findFirstChildElementByName(staticTextField, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.DATA_COLUMN_WIDTH * i;
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if(i < list.getLength() - 1) expectedValue = "TableCell";
            else expectedValue = Utilities.ROW_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            if(i == list.getLength() - 1){
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                        ,actualValue.equals(expectedValue));
            }

            Node textNode = findFirstChildElementByName(staticTextField, "text");
            expectedValue = "NA";
            String actualValue = textNode.getTextContent();

            Assert.assertTrue("text element should equal " + expectedValue+ " Actual value was: " +
                    actualValue ,actualValue.equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_CheckWidths(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        Element rootNode = doc.getDocumentElement();

        Node pageWidth = findFirstChildElementByName(rootNode, "pageWidth");
        int expectedIntValue = (Utilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + Utilities.TOTAL_COLUMN_WIDTH;
        int actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

    }

    @Test
    public void testGetIntervalAsString(){
        Utilities.UNIT_OF_TIME unitOfTime = Utilities.UNIT_OF_TIME.HOUR;
        String expectedValue = "Hour";
        String actualValue = Utilities.getIntervalAsString(unitOfTime, 1);
        Assert.assertTrue("expectedValue is: " + expectedValue+" actual value was " + actualValue, expectedValue.equals(actualValue));

        expectedValue = "Hours";
        actualValue = Utilities.getIntervalAsString(unitOfTime, 2);
        Assert.assertTrue("expectedValue is: " + expectedValue+" actual value was " + actualValue, expectedValue.equals(actualValue));
    }

    private Document transform(String xslt, String src, Map<String, Object> map) throws Exception {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transfoctory.newTemplates(xsltsource).newTransformer();
        Document srcDoc = getStringAsDocument(src);
        DOMResult result = new DOMResult();
        XmlUtil.softXSLTransform(srcDoc, result, transformer, map);
        return (Document) result.getNode();
    }

    private Document getStringAsDocument(String xml) throws Exception{
        DocumentBuilderFactory builderF = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderF.newDocumentBuilder();
        builder.setEntityResolver( getEntityResolver() );
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    private String getResAsStringClasspath(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        //if this throws an exception during test running, just in crease the max length, as the file must have
        //increased in size
        byte[] resbytes = IOUtils.slurpStream(is, 150000);
        return new String(resbytes);
    }

    /**
     * Using a canned transform xml document, transform the performance summary template jrxml file, compile and
     * test the output for correctness.
     * @throws Exception
     */
    @Test
    public void transformAndCompilePSSummaryTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("PS_Summary_TransformDoc.xml");
        String transformXsl = getResAsStringClasspath("PS_SummaryTransform.xsl");
        String templateXml = getResAsStringClasspath("PS_Summary_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        //todo [Donal] logical tests on the transformed runtime doc
    }

    /**
     * Using a canned transform xml document, transform the performance summary template jrxml file, compile and
     * test the output for correctness.
     * @throws Exception
     */
    @Test
    public void transformAndCompilePSIntervalTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("PS_Interval_TransformDoc.xml");
        String transformXsl = getResAsStringClasspath("PS_IntervalMasterTransform.xsl");
        String templateXml = getResAsStringClasspath("PS_IntervalMasterReport_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        //todo [Donal] logical tests on the transformed runtime doc
    }
    
    /**
     * Validate the transformed runtime Usage Summary jrxml file. Confirms that all elements which should exist do,
     * and that the dynamic widths have been incorporated into existing template elements
     * Note: This test, tests for widths. Widths will only change in a transform if the min width set as a param
     * to the transform is exceeded. Currently this width is guaranteed to be exceeded. If the canned transform
     * documents were to change such that they min width was no longer exceeded, then these tests would fail
     *
     * @throws Exception
     */
    @Test
    public void transformUsageSummaryTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("UsageTransformDoc.xml");
        String transformXsl = getResAsStringClasspath("UsageReportTransform.xsl");
        String templateXml = getResAsStringClasspath("Usage_Summary_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        params.put("FrameMinWidth", 565);
        params.put("PageMinWidth", 595);
        int reportInfoStaticTextSize = 128;
        params.put("ReportInfoStaticTextSize", reportInfoStaticTextSize);
        int titleInnerFrameBuffer = 7;
        params.put("TitleInnerFrameBuffer", titleInnerFrameBuffer);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);
        
        XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        XPath xPath = factory.newXPath();
        //COLUMN_MAPPING_TOTAL_
        NodeList nodeList = (NodeList)xPath.evaluate("/JasperRuntimeTransformation/variables/variable[contains(@name, 'COLUMN_MAPPING_TOTAL_')]", runtimeDoc, XPathConstants.NODESET);
        int numColumns = nodeList.getLength();

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[@name='SERVICE_AND_OR_OPERATION_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 SERVICE_AND_OR_OPERATION_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[@name='GRAND_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 GRAND_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[@name='SERVICE_ONLY_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 SERVICE_ONLY_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        //COLUMN_ variables will match all COLUMN_ variables, so need to put in extra constraint to match only those varibales
        //which are actually like COLUMN_1....COLUMN_12
        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name, 'COLUMN_') and number(substring-after(@name, 'COLUMN_'))]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name, 'COLUMN_MAPPING_TOTAL_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_MAPPING_TOTAL_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name, 'COLUMN_SERVICE_TOTAL_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_SERVICE_TOTAL_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //constantHeader - 12 column headers, plus 2 totals
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='CONSTANT']/groupHeader/band/frame[2]/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2)+ " contant group header text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //serviceAndOperationFooter - 12 columns + 1 total + 1 display text field
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE_AND_OPERATION']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2)+ " SERVICE_AND_OPERATION group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //serviceIdFooter 12 columns + 1 total + 1 display text fields
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE_ID']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2)+ " SERVICE_ID group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //constantFooter - 12 columns and 1 total + 1 display text fields
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='CONSTANT']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2)+ " CONSTANT group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //page widths should match the value from the transform doc
        Double expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/pageWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        Double actualWidth = (Double) xPath.evaluate("/jasperReport/@pageWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Page width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/columnWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@columnWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Column width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        //make sure all frames are the correct width, there are various width requirements throughout the document
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        expectedWidth -= titleInnerFrameBuffer;
        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame[2]/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        //all dynamic text fields in title - two elements per frame, this is the rhs element
        //removed as these are currently not dynamic, will see if current size and stretch attributes suffices
//        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
//        expectedWidth -= reportInfoStaticTextSize;
//        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame[2]/frame/textField/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
//        for(int i = 0; i < nodeList.getLength(); i++){
//            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
//            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
//                    expectedWidth.intValue(), actualWidth.intValue()),
//                    expectedWidth.intValue() == actualWidth.intValue());
//        }

        //All group header and footer frames have the same frame width
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/group/*/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/leftMargin/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@leftMargin", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Left margin width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/rightMargin/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@rightMargin", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Right margin width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());


    }

    /**
     * Validate the transformed runtime Usage Master jrxml file. Confirms that all elements which should exist do,
     * and that the dynamic widths have been incorporated into existing template elements
     * Note: This test, tests for widths. Widths will only change in a transform if the min width set as a param
     * to the transform is exceeded. Currently this width is guaranteed to be exceeded. If the canned transform
     * documents were to change such that they min width was no longer exceeded, then these tests would fail
     *
     * @throws Exception
     */
    @Test
    public void transformUsageMasterTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("UsageMasterTransformDoc.xml");
        String transformXsl = getResAsStringClasspath("UsageReportIntervalTransform_Master.xsl");
        String templateXml = getResAsStringClasspath("Usage_IntervalMasterReport_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        params.put("FrameMinWidth", 820);
        params.put("PageMinWidth", 850);
        int reportInfoStaticTextSize = 128;
        params.put("ReportInfoStaticTextSize", reportInfoStaticTextSize);
        int titleInnerFrameBuffer = 7;
        params.put("TitleInnerFrameBuffer", titleInnerFrameBuffer);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);

        compileReport(transformedRuntimeDoc);

        XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        XPath xPath = factory.newXPath();
        //COLUMN_MAPPING_TOTAL_
        NodeList nodeList = (NodeList)xPath.evaluate("/JasperRuntimeTransformation/variables/variable[contains(@name, 'COLUMN_SERVICE_')]", runtimeDoc, XPathConstants.NODESET);
        int numColumns = nodeList.getLength();

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_SERVICE_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_SERVICE_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_OPERATION_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_OPERATION_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_REPORT_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_REPORT_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //serviceHeader = 12 columns + 1 total + 1 display text field
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE']/groupHeader/band/frame[2]/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2) + " SERVICE group header text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //subreport return variables
        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[@toVariable='ROW_OPERATION_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 ROW_OPERATION_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[@toVariable='ROW_SERVICE_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 ROW_SERVICE_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[@toVariable='ROW_REPORT_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 ROW_REPORT_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[contains(@toVariable, 'COLUMN_SERVICE_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns + " returnValue @toVariable=COLUMN_SERVICE_ , there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[contains(@toVariable, 'COLUMN_OPERATION_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns + " returnValue @toVariable=COLUMN_OPERATION_ , there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[contains(@toVariable, 'COLUMN_REPORT_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns + " returnValue @toVariable=COLUMN_REPORT_ , there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //serviceAndOperationFooter - 12 columns + 1 total (1 static text also not counted)
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE_OPERATION']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " SERVICE_OPERATION group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        //serviceIdFooter - 12 columns + 1 total(1 static text also not counted)
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " SERVICE group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        //summary - 12 columns + 1 total (1 static text also not counted)
        nodeList = (NodeList) xPath.evaluate("/jasperReport/summary/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " summary text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        Double expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/pageWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        Double actualWidth = (Double) xPath.evaluate("/jasperReport/@pageWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Page width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/columnWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@columnWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Column width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        //make sure all frames are the correct width, there are various width requirements throughout the document
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        expectedWidth -= titleInnerFrameBuffer;
        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame[2]/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        //all dynamic text fields in title - two elements per frame, this is the rhs element
//        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
//        expectedWidth -= reportInfoStaticTextSize;
//        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame[2]/frame/textField/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
//        for(int i = 0; i < nodeList.getLength(); i++){
//            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
//            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
//                    expectedWidth.intValue(), actualWidth.intValue()),
//                    expectedWidth.intValue() == actualWidth.intValue());
//        }

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        //All group header and footer frames have the same frame width
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        //todo [Donal] put in the specific reportElements, can no longer do blanket
//        nodeList = (NodeList) xPath.evaluate("/jasperReport/group/*/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
//        for(int i = 0; i < nodeList.getLength(); i++){
//            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
//            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
//                    expectedWidth.intValue(), actualWidth.intValue()),
//                    expectedWidth.intValue() == actualWidth.intValue());
//        }

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/leftMargin/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@leftMargin", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Left margin width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/rightMargin/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@rightMargin", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Right margin width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

    }


    /**
     * Validate the transformed runtime Usage Sub Interval jrxml file. Confirms that all elements which should exist do,
     * and that the dynamic widths have been incorporated into existing template elements
     * Note: This test, tests for widths. Widths will only change in a transform if the min width set as a param
     * to the transform is exceeded. Currently this width is guaranteed to be exceeded. If the canned transform
     * documents were to change such that they min width was no longer exceeded, then these tests would fail
     *
     * @throws Exception
     */
    @Test
    public void transformUsageSubIntervalTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("UsageSubIntervalTransformDoc.xml");
        String transformXsl = getResAsStringClasspath("UsageReportSubIntervalTransform_Master.xsl");
        String templateXml = getResAsStringClasspath("Usage_SubIntervalMasterReport_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        params.put("PageMinWidth", 535);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        XPath xPath = factory.newXPath();

        NodeList nodeList = (NodeList)xPath.evaluate("/JasperRuntimeTransformation/variables/variable[contains(@name, 'COLUMN_')]", runtimeDoc, XPathConstants.NODESET);
        int numColumns = nodeList.getLength();

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //sub report return values
        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/subreport/returnValue[contains(@toVariable, 'COLUMN_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns + " returnValue @toVariable=COLUMN_ , there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        Double expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/pageWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        Double actualWidth = (Double) xPath.evaluate("/jasperReport/@pageWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Page width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        //sub report width
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/subReportWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/detail/band/subreport/reportElement/@width", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Subreport width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

    }

    /**
     * Validate the transformed runtime Usage Sub Interval jrxml file. Confirms that all elements which should exist do,
     * and that the dynamic widths have been incorporated into existing template elements
     * Note: This test, tests for widths. Widths will only change in a transform if the min width set as a param
     * to the transform is exceeded. Currently this width is guaranteed to be exceeded. If the canned transform
     * documents were to change such that they min width was no longer exceeded, then these tests would fail
     * @throws Exception
     */
    @Test
    public void transformUsageSubReportTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("UsageSubReportTransformDoc.xml");
        String transformXsl = getResAsStringClasspath("Usage_SubReport.xsl");
        String templateXml = getResAsStringClasspath("Usage_SubIntervalMasterReport_subreport0_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        params.put("PageMinWidth", 535);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        XPath xPath = factory.newXPath();

        NodeList nodeList = (NodeList)xPath.evaluate("/JasperRuntimeTransformation/variables/variable[contains(@name, 'COLUMN_')]", runtimeDoc, XPathConstants.NODESET);
        int numColumns = nodeList.getLength();

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //serviceAndOperationFooter - 12 + 1 total text fields
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE_AND_OPERATION']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " SERVICE_AND_OPERATION group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        //no data - 12 + 1 for total text fields
        nodeList = (NodeList) xPath.evaluate("/jasperReport/noData/band/frame/staticText", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " no data static fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        Double expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/pageWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        Double actualWidth = (Double) xPath.evaluate("/jasperReport/@pageWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Page width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

    }

    private void compileReport(String jrxmlTemplateFile) throws Exception{
        String templateXml = getResAsStringClasspath(jrxmlTemplateFile);
        Document templateDoc = getTemplateDocument(templateXml);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(templateDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        boolean exceptionThrown = false;
        try{
            JasperReport compiledReport = JasperCompileManager.compileReport(bais);
            Assert.assertTrue("Compiled report should not be null", compiledReport != null);
        }catch(Exception ex){
            ex.printStackTrace();
            exceptionThrown = true;
        }
        Assert.assertTrue("No compile exception should have been thrown", !exceptionThrown);
    }

    private void compileReport(Document runTimeDoc) throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(runTimeDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        boolean exceptionThrown = false;
        try{
            JasperReport compiledReport = JasperCompileManager.compileReport(bais);
            Assert.assertTrue("Compiled report should not be null", compiledReport != null);
        }catch(Exception ex){
            ex.printStackTrace();
            exceptionThrown = true;
        }
        Assert.assertTrue("No compile exception should have been thrown", !exceptionThrown);
    }

    /**
     * Usage template jrxml files are required as resources so are not compiled as part of the build process.
     * This test compiles all 4 usage jrxml files
     * @exception
     */
    @Test
    public void compileUsageReports() throws Exception {

        compileReport("Usage_Summary_Template.jrxml");

        compileReport("Usage_IntervalMasterReport_Template.jrxml");

        compileReport("Usage_SubIntervalMasterReport_Template.jrxml");

        compileReport("Usage_SubIntervalMasterReport_subreport0_Template.jrxml");

    }

    /**
     * Performance statistics template jrxml files are required as resources so are not compiled as part of the build process.
     * This test compiles all ps jrxml files
     * @exception
     */
    @Test
    public void compilePerformanceStatisticsReports() throws Exception {

        compileReport("PS_Summary_Template.jrxml");

        compileReport("PS_IntervalMasterReport_Template.jrxml");

        compileReport("PS_SubIntervalMasterReport.jrxml");        

        compileReport("PS_SubIntervalMasterReport_subreport0.jrxml");

    }

    private Document getTemplateDocument(String xml) throws Exception{
        DocumentBuilderFactory builderF = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderF.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        builder.setEntityResolver( getEntityResolver() );
        Document doc = builder.parse(is);
        return doc;
    }

    private EntityResolver getEntityResolver() {
        Map<String,String> idsToResources = new HashMap<String,String>();
        idsToResources.put( "http://jasperreports.sourceforge.net/dtds/jasperreport.dtd", "com/l7tech/gateway/standardreports/jasperreport.dtd");
        return new ResourceMapEntityResolver( null, idsToResources, null );
    }

    private void printOutDocument(Document doc){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XmlUtil.nodeToFormattedOutputStream(doc, baos);
            String testXml = new String(baos.toByteArray());
            System.out.println(testXml);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}

