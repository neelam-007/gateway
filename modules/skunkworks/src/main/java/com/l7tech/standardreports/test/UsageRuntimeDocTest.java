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
import java.io.File;
import java.io.FileOutputStream;

import com.l7tech.server.ems.standardreports.Utilities;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.standardreports.ReportApp;

public class UsageRuntimeDocTest {

    private Connection conn = null;
    private Statement stmt = null;

    @Before
    public void setUp() throws Exception {
        Properties prop = new Properties();
        prop.load(getClass().getResourceAsStream("report.properties"));
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
        Properties prop = new Properties();
        prop.load(getClass().getResourceAsStream("report.properties"));
//        System.out.println(s);

        boolean isDetail = Boolean.parseBoolean(prop.getProperty(ReportApp.IS_DETAIL).toString());

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
        File f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        }finally{
            fos.close();
        }
    }




}
