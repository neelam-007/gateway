/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 25, 2008
 * Time: 1:46:41 PM
 */
package com.l7tech.server.ems.standardreports;

import org.junit.Test;
import org.junit.Assert;
import org.mortbay.util.ajax.JSON;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.ems.standardreports.reporttypes.PerformanceSummary;

import java.util.*;

public class ReportRunnerTests {

    private final static String psRelativeJson = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"c32bb1a9-1538-4792-baa1-566bfd418020\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"c32bb1a9-1538-4792-baa1-566bfd418020\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"a14df4d4-0c62-4d8e-ac3a-82cd8a442a26\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"" +
            "        }" +
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
            "            \"clusterId\"         : \"c32bb1a9-1538-4792-baa1-566bfd418020\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"a14df4d4-0c62-4d8e-ac3a-82cd8a442a26\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportRanBy\" : \"Donal\"," +
            "    \"reportName\" : \"My Report\"" +
            "}";
    @Test
    public void testPerfStatRelativeReport() throws ReportApi.ReportException {
        ReportRunner reportRunner = new ReportRunner(psRelativeJson);
        Collection<ReportApi.ReportSubmission> reportSubmissions = reportRunner.getReportSubmissions();
        Assert.assertNotNull(reportSubmissions);
        Assert.assertTrue("There should be 2 reports, one for each cluster", reportSubmissions.size() == 2);

        for(ReportApi.ReportSubmission reportSub: reportSubmissions){
            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSub.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for(ReportApi.ReportSubmission.ReportParam rP: reportParams){
                paramMap.put(rP.getName(), rP);
            }
            String [] expectedParams = new String[]{PerformanceSummary.IS_RELATIVE, PerformanceSummary.RELATIVE_TIME_UNIT,
                    PerformanceSummary.RELATIVE_NUM_OF_TIME_UNITS, PerformanceSummary.REPORT_RAN_BY,
                    PerformanceSummary.SERVICE_NAMES_LIST, PerformanceSummary.SERVICE_ID_TO_OPERATIONS_MAP};

            for(String s: expectedParams){
                Assert.assertTrue("ParamMap should contain key: " + s, paramMap.containsKey(s));
            }

            //deeper tests
            ReportApi.ReportSubmission.ReportParam reportParam = paramMap.get(PerformanceSummary.SERVICE_NAMES_LIST);
            ReportApi.ReportSubmission.ReportParam serviceIdtoOp = paramMap.get(PerformanceSummary.SERVICE_ID_TO_OPERATIONS_MAP);
            Map<String, Set<String>> serviceIdToOps = (Map<String, Set<String>>) serviceIdtoOp.getValue();
            Set<String> serviceNames = (Set<String>) reportParam.getValue();
            Assert.assertNotNull(serviceNames);
            Assert.assertTrue("ServiceNames should be of size 2, it was " + serviceNames.size(), serviceNames.size() == 2);
            
        }

    }

    /**
     * Probably redundant, just checks that parse always throws an exception when it gets garbage data
     * @throws Exception
     */
    @Test
    public void testJSonDataParse() throws Exception{
        Exception ex = null;
        try{
            String badData = "asdjhfasd;fsda";
            Object o = JSON.parse(badData);
        }catch(Exception e){
            ex = e;
        }

        Assert.assertNotNull("Exception should have been thrown by JSON.parse", ex);
    }
}
