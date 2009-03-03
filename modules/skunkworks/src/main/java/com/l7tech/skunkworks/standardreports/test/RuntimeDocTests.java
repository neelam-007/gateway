/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 23, 2008
 * Time: 6:51:47 PM
 */
package com.l7tech.skunkworks.standardreports.test;

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
import com.l7tech.util.IOUtils;
import com.l7tech.skunkworks.standardreports.ReportApp;
import com.l7tech.server.management.api.node.ReportApi;

public class RuntimeDocTests {

    private Connection conn = null;
    private Statement stmt = null;
    private Properties prop;

    private String getResAsString(String path) throws IOException {
        InputStream is = RuntimeDocTests.class.getResourceAsStream(path);
        try {
            byte[] resbytes = IOUtils.slurpStream(is, 100000);
            return new String(resbytes);
        } finally {
            is.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        prop = new Properties();
        String propData = getResAsString("report.properties");
        StringReader sr = new StringReader(propData);
        prop.load(sr);
        conn = ReportApp.getConnection(prop);
        stmt = conn.createStatement();
    }

    @After
    public void tearDown() throws SQLException {
        if (stmt != null) {
            stmt.close();
            stmt = null;
        }
        if (conn != null) {
            conn.close();
            conn = null;
        }
    }

    @Test
    public void testRuntimeDocCreation() throws Exception {

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
        try {
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        } finally {
            fos.close();
        }
    }

    private LinkedHashSet<List<String>> getTestDistinctMappingSets() {
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        for (int i = 0; i < 4; i++) {
            List<String> valueList = new ArrayList<String>();
            valueList.add("Donal");
            valueList.add("127.0.0.1");
            valueList.add("Bronze" + i);//make each list unique - turns out set is quite smart, not just object refs
            distinctMappingSets.add(valueList);
        }
        return distinctMappingSets;
    }

    @Test
    public void testGetUsageIntervalMasterRuntimeDoc() throws Exception {
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
        try {
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        } finally {
            fos.close();
        }
    }

    @Test
    public void testGetUsageSubIntervalMasterRuntimeDoc() throws Exception {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();
        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        Assert.assertTrue(doc != null);

        XmlUtil.format(doc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageSubIntervalMasterRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        } finally {
            fos.close();
        }
    }

    @Test
    public void testGetUsageSubReportRuntimeDoc() throws Exception {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        Assert.assertTrue(doc != null);

        XmlUtil.format(doc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageSubReportRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        } finally {
            fos.close();
        }
    }

    @Test
    public void testGetPerfStatRuntimeDoc() throws Exception {

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        keysToFilters.put("IP_ADDRESS", Collections.<ReportApi.FilterPair>emptyList());
        keysToFilters.put("CUSTOMER", Collections.<ReportApi.FilterPair>emptyList());

        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> l1 = new ArrayList<String>();
        l1.add(";");
        l1.add("127.0.0.1");
        l1.add("GOLD");
        distinctMappingSets.add(l1);
        List<String> l2 = new ArrayList<String>();
        l2.add(";");
        l2.add("127.0.0.2");
        l2.add("GOLD");
        distinctMappingSets.add(l2);
        List<String> l3 = new ArrayList<String>();
        l3.add(";");
        l3.add("127.0.0.3");
        l3.add("GOLD");
        distinctMappingSets.add(l3);
        List<String> l4 = new ArrayList<String>();
        l4.add(";");
        l4.add("127.0.0.4");
        l4.add("GOLD");
        distinctMappingSets.add(l4);

        Document doc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(keysToFilters, distinctMappingSets);
        Assert.assertTrue(doc != null);

        XmlUtil.format(doc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/skunkworks/standardreports/PerfStatIntervalMastereRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        } finally {
            fos.close();
        }
    }

}
