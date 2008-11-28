/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 27, 2008
 * Time: 6:22:50 PM
 */
package com.l7tech.standardreports.test;

import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.ems.standardreports.JsonReportParameterConvertor;
import com.l7tech.server.ems.standardreports.JsonReportParameterConvertorFactory;
import com.l7tech.server.ems.standardreports.ReportSubmissionClusterBean;
import com.l7tech.server.ems.standardreports.ReportException;
import com.l7tech.server.cluster.ReportApiImpl;
import com.l7tech.standardreports.ReportApp;
import com.l7tech.gateway.standardreports.ReportGenerator;

import java.util.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;
import org.mortbay.util.ajax.JSON;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

public class TestGatewayReportCompileAndFill {

    private Map<String,Object> buildReportParameters( final Collection<ReportApi.ReportSubmission.ReportParam> parameters ) {
        Map<String,Object> params = new HashMap<String,Object>();

        for ( ReportApi.ReportSubmission.ReportParam param : parameters ) {
            Object value = param.getValue();
            params.put( param.getName(), value );
        }

        return Collections.unmodifiableMap( params );
    }

    private Properties prop;

    private Connection conn = null;
    private Statement stmt = null;

    @Before
    public void setUp() throws Exception {
        prop = new Properties();
        InputStream is = new FileInputStream(new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/report.properties"));
        prop.load(is);
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
        //System.out.println("DB conn closed");
    }


    @Test
    public void testReport_PSSummary_Groupings() throws Exception {

        Object o = JSON.parse(psRelativeJsonSummaryTest);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");

        ReportSubmissionClusterBean clusterBean = reportClusterBeans.iterator().next();
        ReportApi.ReportSubmission submission = clusterBean.getReportSubmission();
        Map<String, Object> reportParams = buildReportParameters(submission.getParameters());

        ReportGenerator reportGenerator = new ReportGenerator();
        ReportGenerator.ReportHandle reportHandle = reportGenerator.compileReport(ReportApi.ReportType.PERFORMANCE_SUMMARY, reportParams, ReportApp.getConnection(prop));

        ReportGenerator.ReportHandle fillReport = reportGenerator.fillReport( reportHandle, ReportApp.getConnection(prop));

        Map<ReportApi.ReportOutputType,byte[]> artifacts = new HashMap<ReportApi.ReportOutputType,byte[]>();
        artifacts.put( ReportApi.ReportOutputType.PDF, reportGenerator.generateReportOutput( fillReport, ReportApi.ReportOutputType.PDF.toString()) );

        byte[] reportData = artifacts.get(ReportApi.ReportOutputType.PDF);

        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource( reportData, "application/octet-stream" ));
        dataHandler.writeTo(new FileOutputStream(new File("ReportOutput_"+submission.getName()+".pdf")));
    }

    @Test
    public void testReport_PSSummary_NoGroupings() throws Exception {

        Object o = JSON.parse(psRelativeJsonSummaryNoGroupingsTest);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");

        ReportSubmissionClusterBean clusterBean = reportClusterBeans.iterator().next();
        ReportApi.ReportSubmission submission = clusterBean.getReportSubmission();
        Map<String, Object> reportParams = buildReportParameters(submission.getParameters());

        ReportGenerator reportGenerator = new ReportGenerator();
        ReportGenerator.ReportHandle reportHandle = reportGenerator.compileReport(ReportApi.ReportType.PERFORMANCE_SUMMARY, reportParams, ReportApp.getConnection(prop));

        ReportGenerator.ReportHandle fillReport = reportGenerator.fillReport( reportHandle, ReportApp.getConnection(prop));

        Map<ReportApi.ReportOutputType,byte[]> artifacts = new HashMap<ReportApi.ReportOutputType,byte[]>();
        artifacts.put( ReportApi.ReportOutputType.PDF, reportGenerator.generateReportOutput( fillReport, ReportApi.ReportOutputType.PDF.toString()) );

        byte[] reportData = artifacts.get(ReportApi.ReportOutputType.PDF);

        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource( reportData, "application/octet-stream" ));
        dataHandler.writeTo(new FileOutputStream(new File("ReportOutput_"+submission.getName()+".pdf")));
    }

    @Test
    public void testReport_PSInterval() throws Exception {

        Object o = JSON.parse(psRelativeJsonIntervalTest);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");

        ReportSubmissionClusterBean clusterBean = reportClusterBeans.iterator().next();
        ReportApi.ReportSubmission submission = clusterBean.getReportSubmission();
        Map<String, Object> reportParams = buildReportParameters(submission.getParameters());

        ReportGenerator reportGenerator = new ReportGenerator();
        ReportGenerator.ReportHandle reportHandle = reportGenerator.compileReport(ReportApi.ReportType.PERFORMANCE_INTERVAL, reportParams, ReportApp.getConnection(prop));

        ReportGenerator.ReportHandle fillReport = reportGenerator.fillReport( reportHandle, ReportApp.getConnection(prop));

        Map<ReportApi.ReportOutputType,byte[]> artifacts = new HashMap<ReportApi.ReportOutputType,byte[]>();
        artifacts.put( ReportApi.ReportOutputType.PDF, reportGenerator.generateReportOutput( fillReport, ReportApi.ReportOutputType.PDF.toString()) );

        byte[] reportData = artifacts.get(ReportApi.ReportOutputType.PDF);

        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource( reportData, "application/octet-stream" ));
        dataHandler.writeTo(new FileOutputStream(new File("ReportOutput_"+submission.getName()+".pdf")));
    }

    @Test
    public void testReport_UsageSummary() throws Exception {

        Object o = JSON.parse(usageRelativeJsonSummaryTest);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");

        ReportSubmissionClusterBean clusterBean = reportClusterBeans.iterator().next();
        ReportApi.ReportSubmission submission = clusterBean.getReportSubmission();
        Map<String, Object> reportParams = buildReportParameters(submission.getParameters());

        ReportGenerator reportGenerator = new ReportGenerator();
        ReportGenerator.ReportHandle reportHandle = reportGenerator.compileReport(ReportApi.ReportType.USAGE_SUMMARY, reportParams, ReportApp.getConnection(prop));

        ReportGenerator.ReportHandle fillReport = reportGenerator.fillReport( reportHandle, ReportApp.getConnection(prop));

        Map<ReportApi.ReportOutputType,byte[]> artifacts = new HashMap<ReportApi.ReportOutputType,byte[]>();
        artifacts.put( ReportApi.ReportOutputType.PDF, reportGenerator.generateReportOutput( fillReport, ReportApi.ReportOutputType.PDF.toString()) );

        byte[] reportData = artifacts.get(ReportApi.ReportOutputType.PDF);

        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource( reportData, "application/octet-stream" ));
        dataHandler.writeTo(new FileOutputStream(new File("ReportOutput_"+submission.getName()+".pdf")));
    }

    @Test
    public void testReport_UsageSummary_JustKeys() throws Exception {

        Object o = JSON.parse(usageRelativeJsonSummaryTest_JustKeys);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");

        ReportSubmissionClusterBean clusterBean = reportClusterBeans.iterator().next();
        ReportApi.ReportSubmission submission = clusterBean.getReportSubmission();
        Map<String, Object> reportParams = buildReportParameters(submission.getParameters());

        ReportGenerator reportGenerator = new ReportGenerator();
        ReportGenerator.ReportHandle reportHandle = reportGenerator.compileReport(ReportApi.ReportType.USAGE_SUMMARY, reportParams, ReportApp.getConnection(prop));

        ReportGenerator.ReportHandle fillReport = reportGenerator.fillReport( reportHandle, ReportApp.getConnection(prop));

        Map<ReportApi.ReportOutputType,byte[]> artifacts = new HashMap<ReportApi.ReportOutputType,byte[]>();
        artifacts.put( ReportApi.ReportOutputType.PDF, reportGenerator.generateReportOutput( fillReport, ReportApi.ReportOutputType.PDF.toString()) );

        byte[] reportData = artifacts.get(ReportApi.ReportOutputType.PDF);

        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource( reportData, "application/octet-stream" ));
        dataHandler.writeTo(new FileOutputStream(new File("ReportOutput_"+submission.getName()+".pdf")));
    }

    @Test
    public void testReport_UsageInterval_JustKeys() throws Exception {

        Object o = JSON.parse(usageRelativeJsonIntervalTest_JustKeys);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");

        ReportSubmissionClusterBean clusterBean = reportClusterBeans.iterator().next();
        ReportApi.ReportSubmission submission = clusterBean.getReportSubmission();
        Map<String, Object> reportParams = buildReportParameters(submission.getParameters());

        Assert.assertTrue("Type should be usage interval", submission.getType() == ReportApi.ReportType.USAGE_INTERVAL);
        ReportGenerator reportGenerator = new ReportGenerator();
        ReportGenerator.ReportHandle reportHandle = reportGenerator.compileReport(submission.getType(), reportParams, ReportApp.getConnection(prop));

        ReportGenerator.ReportHandle fillReport = reportGenerator.fillReport( reportHandle, ReportApp.getConnection(prop));

        Map<ReportApi.ReportOutputType,byte[]> artifacts = new HashMap<ReportApi.ReportOutputType,byte[]>();
        artifacts.put( ReportApi.ReportOutputType.PDF, reportGenerator.generateReportOutput( fillReport, ReportApi.ReportOutputType.PDF.toString()) );

        byte[] reportData = artifacts.get(ReportApi.ReportOutputType.PDF);

        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource( reportData, "application/octet-stream" ));
        dataHandler.writeTo(new FileOutputStream(new File("ReportOutput_"+submission.getName()+".pdf")));
    }

    private final static String clusterId = "f7f6bf457e1346e5a842efc7f5aff75d";
    
    private final static String psRelativeJsonIntervalTest = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"start\"    : \"2008-07-31 13:00:00\"," +
            "        \"end\"      : \"2008-07-31 13:00:00\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.0.0.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : false," +
            "    \"reportName\" : \"PS_Interval_Report\"" +
            "}";

    private final static String psRelativeJsonSummaryTest = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"start\"    : \"2008-07-31 13:00:00\"," +
            "        \"end\"      : \"2008-07-31 13:00:00\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.0.0.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"PS_Summary_Report\"" +
            "}";

    private final static String psRelativeJsonSummaryNoGroupingsTest = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"start\"    : \"2008-07-31 13:00:00\"," +
            "        \"end\"      : \"2008-07-31 13:00:00\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"PS_Summary_NO_GROUPINGS_Report\"" +
            "}";

    private final static String usageRelativeJsonSummaryTest = "{\"reportType\":\"usage\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"start\"    : \"2008-07-31 13:00:00\"," +
            "        \"end\"      : \"2008-07-31 13:00:00\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.0.0.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"Usage_Summary_Report\"" +
            "}";

    private final static String usageRelativeJsonSummaryTest_JustKeys = "{\"reportType\":\"usage\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"start\"    : \"2008-07-31 13:00:00\"," +
            "        \"end\"      : \"2008-07-31 13:00:00\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"Usage_Summary_Report\"" +
            "}";

    private final static String usageRelativeJsonIntervalTest_JustKeys = "{\"reportType\":\"usage\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+clusterId+"\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"start\"    : \"2008-07-31 13:00:00\"," +
            "        \"end\"      : \"2008-07-31 13:00:00\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+clusterId+"\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : false," +
            "    \"reportName\" : \"Usage_Interval_Report\"" +
            "}";

}
