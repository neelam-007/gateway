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

    private static final String cluster1 = "c32bb1a9-1538-4792-baa1-566bfd418020";
    private static final String cluster2 = "a14df4d4-0c62-4d8e-ac3a-82cd8a442a26";
    
    private final static String psRelativeJson = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \""+cluster1+"\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+cluster1+"\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+cluster1+"\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+cluster2+"\"," +
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
            "            \"clusterId\"         : \""+cluster1+"\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+cluster1+"\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+cluster2+"\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String psRelativeJsonWithAuthUser = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \""+cluster1+"\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+cluster1+"\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+cluster1+"\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \""+cluster2+"\"," +
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
            "            \"clusterId\"         : \""+cluster1+"\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+cluster1+"\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+cluster2+"\"," +
            "            \"messageContextKey\" : \"auth_user_id\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \""+cluster2+"\"," +
            "            \"messageContextKey\" : \"auth_user_id\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    String [] psSummaryRelativeExpectedParams = new String[]{PerformanceSummary.IS_RELATIVE, PerformanceSummary.RELATIVE_TIME_UNIT,
            PerformanceSummary.RELATIVE_NUM_OF_TIME_UNITS, PerformanceSummary.REPORT_RAN_BY,
            PerformanceSummary.SERVICE_NAMES_LIST, PerformanceSummary.SERVICE_ID_TO_OPERATIONS_MAP,
            PerformanceSummary.IS_DETAIL, PerformanceSummary.MAPPING_KEYS, PerformanceSummary.MAPPING_VALUES,
            PerformanceSummary.USE_USER, PerformanceSummary.AUTHENTICATED_USERS, PerformanceSummary.PRINT_CHART};

    @Test
    public void testNumClustersFound() throws ReportApi.ReportException {
        ReportRunner reportRunner = new ReportRunner(psRelativeJson, "Donal");
        Collection<ReportSubmissionClusterBean> reportClusterBeans = reportRunner.getReportSubmissions();
        Assert.assertNotNull(reportClusterBeans);
        Assert.assertTrue("There should be 2 reports, one for each cluster", reportClusterBeans.size() == 2);
    }

    @Test
    public void testPerfStatExpectedParameters() throws ReportApi.ReportException {
        ReportRunner reportRunner = new ReportRunner(psRelativeJson, "Donal");
        Collection<ReportSubmissionClusterBean> reportClusterBeans = reportRunner.getReportSubmissions();
        Assert.assertNotNull(reportClusterBeans);

        for(ReportSubmissionClusterBean clusterBean: reportClusterBeans){

            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for(ReportApi.ReportSubmission.ReportParam rP: reportParams){
                paramMap.put(rP.getName(), rP);
            }

            for(String s: psSummaryRelativeExpectedParams){
                Assert.assertTrue("ParamMap should contain key: " + s, paramMap.containsKey(s));
            }
        }

    }

    @Test
    public void testPerfStatRelativeReport_NoAuthenticatedUser() throws ReportApi.ReportException {
        ReportRunner reportRunner = new ReportRunner(psRelativeJson, "Donal");
        Collection<ReportSubmissionClusterBean> reportClusterBeans = reportRunner.getReportSubmissions();
        Assert.assertNotNull(reportClusterBeans);

        for(ReportSubmissionClusterBean clusterBean: reportClusterBeans){

            String clusterId = clusterBean.getClusterId();
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for(ReportApi.ReportSubmission.ReportParam rP: reportParams){
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam reportParam = paramMap.get(PerformanceSummary.AUTHENTICATED_USERS);
            List<String> authUsers = (List<String>) reportParam.getValue();
            Assert.assertNotNull("authUsers should not be null", authUsers);
            Assert.assertTrue("Authusers should be empty", authUsers.size() == 0);

            ReportApi.ReportSubmission.ReportParam useUserParam = paramMap.get(PerformanceSummary.USE_USER);
            Boolean useUser = (Boolean) useUserParam.getValue();
            Assert.assertFalse("Use user should be false", useUser);

        }
    }

    @Test
    public void testPerfStatRelativeReport_WithAuthenticatedUser() throws ReportApi.ReportException {
        ReportRunner reportRunner = new ReportRunner(psRelativeJsonWithAuthUser, "Donal");
        Collection<ReportSubmissionClusterBean> reportClusterBeans = reportRunner.getReportSubmissions();
        Assert.assertNotNull(reportClusterBeans);

        for(ReportSubmissionClusterBean clusterBean: reportClusterBeans){

            String clusterId = clusterBean.getClusterId();
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for(ReportApi.ReportSubmission.ReportParam rP: reportParams){
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam reportParam = paramMap.get(PerformanceSummary.AUTHENTICATED_USERS);
            List<String> authUsers = (List<String>) reportParam.getValue();
            Assert.assertNotNull("authUsers should not be null", authUsers);
            ReportApi.ReportSubmission.ReportParam useUserParam = paramMap.get(PerformanceSummary.USE_USER);
            Boolean useUser = (Boolean) useUserParam.getValue();

            if(clusterId.equals(cluster1)){
                Assert.assertTrue("Authusers should be empty, actual size was " + authUsers.size(), authUsers.size() == 0);
                Assert.assertFalse("Use user should be false", useUser);
            }else if(clusterId.equals(cluster2)){
                Assert.assertTrue("Authusers should be empty, actual size was " + authUsers.size(), authUsers.size() == 0);
                Assert.assertTrue("Use user should be true", useUser);
            }
        }
    }


    @Test
    public void testPerfStatRelativeReport_ServiceAndOperations() throws ReportApi.ReportException {
        ReportRunner reportRunner = new ReportRunner(psRelativeJson, "Donal");
        Collection<ReportSubmissionClusterBean> reportClusterBeans = reportRunner.getReportSubmissions();
        Assert.assertNotNull(reportClusterBeans);

        for(ReportSubmissionClusterBean clusterBean: reportClusterBeans){

            String clusterId = clusterBean.getClusterId();
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();
            
            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for(ReportApi.ReportSubmission.ReportParam rP: reportParams){
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam reportParam = paramMap.get(PerformanceSummary.SERVICE_NAMES_LIST);
            Set<String> serviceNames = (Set<String>) reportParam.getValue();
            Assert.assertNotNull(serviceNames);

            if(clusterId.equals(cluster1)){
                Assert.assertTrue("ServiceNames should be of size 2, it was " + serviceNames.size(), serviceNames.size() == 2);
            }else if(clusterId.equals(cluster2)){
                Assert.assertTrue("ServiceNames should be of size 1, it was " + serviceNames.size(), serviceNames.size() == 1);
            }else{
                throw new IllegalStateException("Unexpected cluster id found :" + clusterId);
            }

            ReportApi.ReportSubmission.ReportParam serviceIdtoOp = paramMap.get(PerformanceSummary.SERVICE_ID_TO_OPERATIONS_MAP);
            Map<String, Set<String>> serviceIdToOps = (Map<String, Set<String>>) serviceIdtoOp.getValue();
            for(Map.Entry<String, Set<String>> me: serviceIdToOps.entrySet()){

                if(clusterId.equals(cluster1)){
                    if(me.getKey().equals("229376")){
                        Assert.assertTrue("229376 should have 2 operations", me.getValue().size() == 2);
                    }else if(me.getKey().equals("229377")){
                        Assert.assertTrue("229377 should have 1 operation", me.getValue().size() == 1);
                    }else{
                        throw new IllegalStateException("Unexpected key found in map: " + me.getKey());
                    }
                }else if(clusterId.equals(cluster2)){
                    if(me.getKey().equals("229378")){
                        Assert.assertTrue("229378 should have 1 operation", me.getValue().size() == 1);
                    }else{
                        throw new IllegalStateException("Unexpected key found in map: " + me.getKey());
                    }
                }
            }
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
