/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 23, 2008
 * Time: 6:51:47 PM
 */
package com.l7tech.standardreports.test;

import org.junit.*;
import org.w3c.dom.Document;

import java.util.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.*;

import com.l7tech.gateway.standardreports.Utilities;
import com.l7tech.gateway.standardreports.RuntimeDocUtilities;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.IOUtils;
import com.l7tech.standardreports.ReportApp;
import com.l7tech.server.management.api.node.ReportApi;

public class RuntimeDocTests {

    private Connection conn = null;
    private Statement stmt = null;
    private Properties prop;

    private String getResAsString(String path) throws IOException {
        File f = new File(path);
        InputStream is = new FileInputStream(f);
        try{
            byte[] resbytes = IOUtils.slurpStream(is, 100000);
            return new String(resbytes);
        }finally{
            is.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        prop = new Properties();
        String propData = getResAsString("modules/skunkworks/src/main/java/com/l7tech/standardreports/report.properties");
        StringReader sr = new StringReader(propData);
        prop.load(sr);
        conn = ReportApp.getConnection(prop);
        stmt = conn.createStatement();
    }

    @After
    public void tearDown() throws SQLException {
        if(stmt != null) {
            stmt.close();
            stmt = null;
        }
        if(conn != null){
            conn.close();
            conn = null;
        }
    }

    @Test
    public void testRuntimeDocCreation() throws Exception{

        boolean isDetail = Boolean.parseBoolean(prop.getProperty(ReportApp.IS_DETAIL));

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = ReportApp.getFilterPairMap(prop);

        String sql = Utilities.getUsageDistinctMappingQuery(null, null, null, keysToFilterPairs, 2, isDetail, true);

        LinkedHashSet<List<String>> distinctMappingSets = ReportApp.getDistinctMappingSets(conn, sql);
        
        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Assert.assertTrue(doc != null);
        XmlUtil.format(doc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        }finally{
            fos.close();
        }
    }

    private LinkedHashSet<List<String>> getTestDistinctMappingSets(){
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        for(int i = 0; i < 4; i++){
            List<String> valueList = new ArrayList<String>();
            valueList.add("Donal");
            valueList.add("127.0.0.1");
            valueList.add("Bronze"+i);//make each list unique - turns out set is quite smart, not just object refs
            distinctMappingSets.add(valueList);
        }
        return distinctMappingSets;
    }

    @Test
    public void testGetUsageIntervalMasterRuntimeDoc() throws Exception{
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Assert.assertTrue(doc != null);

        XmlUtil.format(doc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageIntervalMasterRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        }finally{
            fos.close();
        }
    }

    @Test
    public void testGetUsageSubIntervalMasterRuntimeDoc() throws Exception{
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();
        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        Assert.assertTrue(doc != null);

        XmlUtil.format(doc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageSubIntervalMasterRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        }finally{
            fos.close();
        }
    }

    @Test
    public void testGetUsageSubReportRuntimeDoc() throws Exception{
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        Assert.assertTrue(doc != null);

        XmlUtil.format(doc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageSubReportRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        }finally{
            fos.close();
        }
    }

    @Test
    public void testGetPerfStatRuntimeDoc() throws Exception{

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("Group 1", "IP_ADDRESS: 127.0.0.1, CUSTOMER: GOLD");
        linkedHashMap.put("Group 2", "IP_ADDRESS: 127.0.0.2, CUSTOMER: GOLD");
        linkedHashMap.put("Group 3", "IP_ADDRESS: 127.0.0.3, CUSTOMER: GOLD");
        linkedHashMap.put("Group 4", "IP_ADDRESS: 127.0.0.4, CUSTOMER: GOLD");

        Document doc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(true, true, linkedHashMap);
        Assert.assertTrue(doc != null);

        XmlUtil.format(doc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PerfStatIntervalMastereRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        }finally{
            fos.close();
        }
    }
    
}
