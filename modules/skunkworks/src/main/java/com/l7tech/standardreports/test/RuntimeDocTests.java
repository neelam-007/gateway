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
import java.sql.ResultSet;
import java.io.*;

import com.l7tech.server.ems.standardreports.Utilities;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.IOUtils;
import com.l7tech.standardreports.ReportApp;

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

        List<String > keys  = ReportApp.loadListFromProperties(ReportApp.MAPPING_KEY, prop);
        List<String> values = ReportApp.loadListFromProperties(ReportApp.MAPPING_VALUE, prop);

        List<String> useAnd = ReportApp.loadListFromProperties(ReportApp.VALUE_EQUAL_OR_LIKE, prop);

        String sql = Utilities.getUsageDistinctMappingQuery(null, null, null, keys, values, useAnd, 2, isDetail, null, false, null);

        LinkedHashSet<String> set = new LinkedHashSet<String>();
        ResultSet rs = stmt.executeQuery(sql);

        while(rs.next()){
            StringBuilder sb = new StringBuilder();
            String authUser = rs.getString(Utilities.AUTHENTICATED_USER);
            sb.append(authUser);
            for(int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++){
                sb.append(rs.getString("MAPPING_VALUE_"+(i+1)));
            }
            set.add(sb.toString());
        }

        Document doc = Utilities.getUsageRuntimeDoc(false, keys, set);
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

    @Test
    public void testGetUsageIntervalMasterRuntimeDoc() throws Exception{
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();
        mappingValues.add("127.0.0.1Bronze");
        mappingValues.add("127.0.0.1Gold");
        mappingValues.add("127.0.0.1Silver");
        
        Document doc = Utilities.getUsageIntervalMasterRuntimeDoc(false, keys, mappingValues);
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
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();
        mappingValues.add("127.0.0.1Bronze");
        mappingValues.add("127.0.0.1Gold");
        mappingValues.add("127.0.0.1Silver");
        mappingValues.add("127.0.0.2Bronze");
        mappingValues.add("127.0.0.2Gold");
        mappingValues.add("127.0.0.2Silver");

        Document doc = Utilities.getUsageSubIntervalMasterRuntimeDoc(false, keys, mappingValues);
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
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();
        mappingValues.add("127.0.0.1Bronze");
        mappingValues.add("127.0.0.1Gold");
        mappingValues.add("127.0.0.1Silver");
        mappingValues.add("127.0.0.2Bronze");
        mappingValues.add("127.0.0.2Gold");
        mappingValues.add("127.0.0.2Silver");

        Document doc = Utilities.getUsageSubReportRuntimeDoc(false, keys, mappingValues);
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

        Document doc = Utilities.getPerfStatAnyRuntimeDoc(true, linkedHashMap);
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
