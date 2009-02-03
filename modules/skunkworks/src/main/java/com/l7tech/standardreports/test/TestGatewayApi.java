/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 27, 2008
 * Time: 11:51:32 AM
 */
package com.l7tech.standardreports.test;

import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.standardreports.*;
import com.l7tech.server.management.api.node.ReportApi;
import org.junit.*;
import org.mortbay.util.ajax.JSON;

import javax.activation.DataHandler;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

public class TestGatewayApi {

    private ReportApi reportApi;
    @Before
    public void setUp(){
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        GatewayContext gatewayContext = new GatewayContext(null, "irishman.l7tech.com", 8443, "admin", "password");
        reportApi = gatewayContext.getReportApi();
    }


    @Test
    public void testSubmitReport_PSSummary() throws ReportException, ReportApi.ReportException, IOException {
        Object o = JSON.parse(psRelativeJsonSummaryTest);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        for(ReportSubmissionClusterBean clusterBean: reportClusterBeans){

            List<ReportApi.ReportOutputType> outputTypes = new ArrayList<ReportApi.ReportOutputType>();
            outputTypes.add(ReportApi.ReportOutputType.PDF);
            ReportApi.ReportSubmission submission = clusterBean.getReportSubmission();
            String reportId = reportApi.submitReport(submission, outputTypes);
            Assert.assertNotNull(reportId);

            List<String> reportids = new ArrayList<String>();
            reportids.add(reportId);
            Collection<ReportApi.ReportStatus> status = reportApi.getReportStatus(reportids);
            System.out.println(status);
            for(ReportApi.ReportStatus rs: status){
                System.out.println("Status for id: " + reportId+" is: "+ rs.getStatus());                
            }

            ReportApi.ReportResult reportResult = reportApi.getReportResult(reportId, ReportApi.ReportOutputType.PDF);
            DataHandler dataHandler = reportResult.getData();
            dataHandler.writeTo(new FileOutputStream(new File("ReportOutput_"+submission.getName()+".pdf")));
            Assert.assertNotNull(dataHandler);
        }

    }

    @Test
    public void testSubmitReport_PSInterval() throws ReportException, ReportApi.ReportException, IOException {
        Object o = JSON.parse(psRelativeJsonIntervalTest);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        for(ReportSubmissionClusterBean clusterBean: reportClusterBeans){

            List<ReportApi.ReportOutputType> outputTypes = new ArrayList<ReportApi.ReportOutputType>();
            outputTypes.add(ReportApi.ReportOutputType.PDF);
            ReportApi.ReportSubmission submission = clusterBean.getReportSubmission();
            String reportId = reportApi.submitReport(submission, outputTypes);
            Assert.assertNotNull(reportId);

            List<String> reportids = new ArrayList<String>();
            reportids.add(reportId);
            Collection<ReportApi.ReportStatus> status = reportApi.getReportStatus(reportids);
            System.out.println(status);
            for(ReportApi.ReportStatus rs: status){
                System.out.println("Status for id: " + reportId+" is: "+ rs.getStatus());
            }

            ReportApi.ReportResult reportResult = reportApi.getReportResult(reportId, ReportApi.ReportOutputType.PDF);
            DataHandler dataHandler = reportResult.getData();
            dataHandler.writeTo(new FileOutputStream(new File("ReportOutput_"+submission.getName()+".pdf")));
            Assert.assertNotNull(dataHandler);
        }
    }

    private final static String clusterId = "f7f6bf457e1346e5a842efc7f5aff75d";
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

}
