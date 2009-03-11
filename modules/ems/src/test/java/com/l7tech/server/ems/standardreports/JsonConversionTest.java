/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 25, 2008
 * Time: 1:46:41 PM
 */
package com.l7tech.server.ems.standardreports;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Ignore;
import org.mortbay.util.ajax.JSON;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.ems.standardreports.SummaryReportJsonConvertor;
import com.l7tech.gateway.standardreports.Utilities;
import com.l7tech.gateway.common.mapping.MessageContextMapping;

import java.util.*;

public class JsonConversionTest {

    private static final String cluster1 = "c32bb1a9-1538-4792-baa1-566bfd418020";
    private static final String cluster2 = "a14df4d4-0c62-4d8e-ac3a-82cd8a442a26";

    String[] psSummaryRelativeExpectedParams = new String[]{ReportApi.ReportParameters.IS_RELATIVE, ReportApi.ReportParameters.RELATIVE_TIME_UNIT,
            ReportApi.ReportParameters.RELATIVE_NUM_OF_TIME_UNITS, ReportApi.ReportParameters.REPORT_RAN_BY,
            ReportApi.ReportParameters.SERVICE_ID_TO_NAME_MAP, ReportApi.ReportParameters.SERVICE_ID_TO_OPERATIONS_MAP,
            ReportApi.ReportParameters.IS_DETAIL, ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS, ReportApi.ReportParameters.PRINT_CHART};

    @Test
    public void testNumClustersFound() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummary);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);
        Assert.assertTrue("There should be 2 reports, one for each cluster", reportClusterBeans.size() == 2);
    }

    @Test
    public void testReportType() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummary);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();
            Assert.assertTrue("Report should be performance summary",
                    reportSubmission.getType() == ReportApi.ReportType.PERFORMANCE_SUMMARY);
        }

        o = JSON.parse(psRelativeIntervalJson);
        jsonMap = (Map) o;
        convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();
            Assert.assertTrue("Report should be performance interval",
                    reportSubmission.getType() == ReportApi.ReportType.PERFORMANCE_INTERVAL);
        }

        o = JSON.parse(usageRelativeJson);
        jsonMap = (Map) o;
        convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();
            Assert.assertTrue("Report should be usage summary",
                    reportSubmission.getType() == ReportApi.ReportType.USAGE_SUMMARY);
        }

        o = JSON.parse(usageIntervalRelativeJson);
        jsonMap = (Map) o;
        convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();
            Assert.assertTrue("Report should be usage interval",
                    reportSubmission.getType() == ReportApi.ReportType.USAGE_INTERVAL);
        }

    }

    @Test
    public void testPerfStatSummaryIsRelative() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummary);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam isRelativeParam = paramMap.get(ReportApi.ReportParameters.IS_RELATIVE);
            Boolean isRelative = (Boolean) isRelativeParam.getValue();
            Assert.assertTrue("isRelative should be true", isRelative);
        }
    }

    @Test
    public void testPerfStatSummaryTimeUnit() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummary);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam timeUnitParam = paramMap.get(ReportApi.ReportParameters.RELATIVE_TIME_UNIT);
            String timeUnit = (String) timeUnitParam.getValue();
            Utilities.UNIT_OF_TIME unitOfTime = Utilities.getUnitFromString(timeUnit);
            Assert.assertTrue("Relative time unit should be ", unitOfTime == Utilities.UNIT_OF_TIME.DAY);
        }
    }

    @Test
    public void testConvertorFactory() throws Exception {
        Object o = JSON.parse(psRelativeJsonSummary);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Assert.assertTrue(convertor.getClass().getName() == SummaryReportJsonConvertor.class.getName());

        o = JSON.parse(psRelativeIntervalJson);
        jsonMap = (Map) o;
        convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Assert.assertTrue(convertor.getClass().getName() == IntervalReportJsonConvertor.class.getName());
    }

    @Test
    public void testPerfStatInterval_IntervalTime() throws ReportException {
        Object o = JSON.parse(psRelativeIntervalJson);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam intervalTimeUnitParam = paramMap.get(ReportApi.ReportParameters.INTERVAL_TIME_UNIT);
            String intervalTimeUnit = (String) intervalTimeUnitParam.getValue();
            Utilities.UNIT_OF_TIME unitOfTime = Utilities.getUnitFromString(intervalTimeUnit);
            Assert.assertTrue("Interval time unit should be ", unitOfTime == Utilities.UNIT_OF_TIME.HOUR);
        }
    }

    @Test
    public void testTimeZone() throws ReportException {
        Object o = JSON.parse(invalidTimeZone);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        boolean exception = false;
        try {
            convertor.getReportSubmissions(jsonMap, "Donal");
        } catch (ReportException e) {
            exception = true;
        }

        Assert.assertTrue("Timezone should not have been found", exception);
    }

    @Test
    public void testKeysAndValuesSize() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummaryTest);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam
                    mappingKeyParam = paramMap.get(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);

            LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = (LinkedHashMap<String, List<ReportApi.FilterPair>>) mappingKeyParam.getValue();
            Assert.assertNotNull("keysToFilterPairs should not be null", keysToFilterPairs);

            Assert.assertTrue("Size of keys should be 2, it was: " + keysToFilterPairs.size(),
                    keysToFilterPairs.size() == 2);
        }

    }

    @Test
    public void testWhenOnly1ClusterWithKeys_KeyToListParamIsSet() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummaryOnly1ClusterWithKeys);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam
                    keysToListParam = paramMap.get(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);

            Assert.assertNotNull(keysToListParam);

            Assert.assertNotNull(keysToListParam.getValue());

            String clusterId = clusterBean.getClusterId();
            if (clusterId.equals(cluster2)) {
                LinkedHashMap<String, List<ReportApi.FilterPair>>
                        keysToFilters = (LinkedHashMap<String, List<ReportApi.FilterPair>>) keysToListParam.getValue();

                Assert.assertNotNull(keysToFilters);

                Assert.assertTrue(keysToFilters.isEmpty());
            }
        }
    }

    @Test
    public void testPerfStatSummary_MappingKeysAndValues() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummary);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam
                    keysToListParam = paramMap.get(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);

            LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = (LinkedHashMap<String, List<ReportApi.FilterPair>>) keysToListParam.getValue();
            Assert.assertNotNull("keysToFilterPairs should not be null", keysToFilterPairs);

            Assert.assertTrue("Size of keys should be 2, it was: " + keysToFilterPairs.size(),
                    keysToFilterPairs.size() == 2);

            for (String s : keysToFilterPairs.keySet()) {
                Assert.assertTrue("Should be 1 values per key, there was: " + keysToFilterPairs.get(s).size(), keysToFilterPairs.get(s).size() == 1);
            }

            String clusterId = clusterBean.getClusterId();
            if (clusterId.equals(cluster1)) {
                Assert.assertTrue("cluster 1 should have 2 mapping keys", keysToFilterPairs.size() == 2);
                String[] expectedKeys = new String[]{"IP_ADDRESS", "CUSTOMER"};
                for (String s : expectedKeys) {
                    Assert.assertTrue("Key " + s + " should be in the mapping keys", keysToFilterPairs.containsKey(s));
                }
            } else if (clusterId.equals(cluster2)) {

                Assert.assertTrue("cluster 2 should have 2 mapping keys", keysToFilterPairs.size() == 2);
                String[] expectedKeys = new String[]{"IP_ADDRESS", "CUSTOMER"};
                for (String s : expectedKeys) {
                    Assert.assertTrue("Key " + s + " should be in the mapping keys", keysToFilterPairs.containsKey(s));
                }

                List<ReportApi.FilterPair> fps = keysToFilterPairs.get("IP_ADDRESS");
                Assert.assertTrue("IP_ADDRESS key should have 1 mapping values", fps.size() == 1);

                Assert.assertTrue("IP_ADDRESS constraint should equal 127.%.%.1, it was: " + fps.get(0).getFilterValue(),
                        fps.get(0).getFilterValue().equals("127.%.%.1"));

                Assert.assertTrue("IP_ADDRESS constraint should be a like constraint", fps.get(0).isQueryUsingWildCard());


                fps = keysToFilterPairs.get("CUSTOMER");
                Assert.assertTrue("CUSTOMER key should have 1 mapping values", fps.size() == 1);

                Assert.assertTrue("CUSTOMER constraint should equal GOLD, it was: " + fps.get(0).getFilterValue(),
                        fps.get(0).getFilterValue().equals("GOLD"));

                Assert.assertTrue("CUSTOMER  constraint should be a and constraint", !fps.get(0).isQueryUsingWildCard());
            }
        }
    }

    /**
     * Check that multiple mapping values per key are parsed correctly
     *
     * @throws ReportException
     */
    @Test
    public void testPerfStatSummary_MappingKeysAndMultipleValues() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummaryMultipleKeyValues);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam
                    keysToListParam = paramMap.get(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);

            LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = (LinkedHashMap<String, List<ReportApi.FilterPair>>) keysToListParam.getValue();
            Assert.assertNotNull("keysToFilterPairs should not be null", keysToFilterPairs);

            Assert.assertTrue("Size of keys should be 2, it was: " + keysToFilterPairs.size(),
                    keysToFilterPairs.size() == 2);

            String clusterId = clusterBean.getClusterId();
            if (clusterId.equals(cluster1)) {
                Assert.assertTrue("cluster 1 should have 2 mapping keys", keysToFilterPairs.size() == 2);
                String[] expectedKeys = new String[]{"IP_ADDRESS", "CUSTOMER"};
                for (String s : expectedKeys) {
                    Assert.assertTrue("Key " + s + " should be in the mapping keys", keysToFilterPairs.containsKey(s));
                }

                List<ReportApi.FilterPair> fps = keysToFilterPairs.get("IP_ADDRESS");
                Assert.assertTrue("Should be 1 IP_ADDRESS values, there were: " + fps.size(), fps.size() == 1);
                fps = keysToFilterPairs.get("CUSTOMER");
                Assert.assertTrue("Should be 2 customer values, there were: " + fps.size(), fps.size() == 2);
                Assert.assertTrue("First customer value should equal GOLD, it was: " + fps.get(0).getFilterValue(),
                        fps.get(0).getFilterValue().equals("GOLD"));
                Assert.assertTrue("Second customer value should equal SILVER, it was: " + fps.get(1).getFilterValue(),
                        fps.get(1).getFilterValue().equals("SILVER"));

            } else if (clusterId.equals(cluster2)) {

                Assert.assertTrue("cluster 2 should have 2 mapping keys", keysToFilterPairs.size() == 2);
                String[] expectedKeys = new String[]{"IP_ADDRESS", "CUSTOMER"};
                for (String s : expectedKeys) {
                    Assert.assertTrue("Key " + s + " should be in the mapping keys", keysToFilterPairs.containsKey(s));
                }

                List<ReportApi.FilterPair> fps = keysToFilterPairs.get("IP_ADDRESS");
                Assert.assertTrue("Should be 2 IP_ADDRESS values, there were: " + fps.size(), fps.size() == 2);
                Assert.assertTrue("IP_ADDRESS key should have 2 mapping values", fps.size() == 2);

                Assert.assertTrue("IP_ADDRESS constraint should equal 127.%.%.1, it was: " + fps.get(0).getFilterValue(),
                        fps.get(0).getFilterValue().equals("127.%.%.1"));

                Assert.assertTrue("IP_ADDRESS second FilterPair should be empty", fps.get(1).isEmpty());

                Assert.assertTrue("IP_ADDRESS constraint should be a like constraint", fps.get(0).isQueryUsingWildCard());


                fps = keysToFilterPairs.get("CUSTOMER");
                Assert.assertTrue("CUSTOMER key should have 1 mapping values", fps.size() == 1);

                Assert.assertTrue("CUSTOMER constraint should equal GOLD, it was: " + fps.get(0).getFilterValue(),
                        fps.get(0).getFilterValue().equals("GOLD"));

                Assert.assertTrue("CUSTOMER  constraint should be a and constraint", !fps.get(0).isQueryUsingWildCard());
            }
        }
    }

    @Test
    public void testPerfStatExpectedParameters() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummary);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            for (String s : psSummaryRelativeExpectedParams) {
                Assert.assertTrue("ParamMap should contain key: " + s, paramMap.containsKey(s));
            }
        }

    }

    @Test
    public void testPerfStatRelativeReport_WithAuthenticatedUser() throws ReportException {
        Object o = JSON.parse(psRelativeJsonWithAuthUser);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            String clusterId = clusterBean.getClusterId();
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam
                    reportParam = paramMap.get(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);
            LinkedHashMap<String, List<ReportApi.FilterPair>>
                    keysToFilterPairs = (LinkedHashMap<String, List<ReportApi.FilterPair>>) reportParam.getValue();


            if (clusterId.equals(cluster1)) {
                List<ReportApi.FilterPair> authFilters = keysToFilterPairs.get(MessageContextMapping.MappingType.AUTH_USER.toString());
                Assert.assertNull("authUsers should be null", authFilters);
            } else if (clusterId.equals(cluster2)) {
                List<ReportApi.FilterPair> authFilters = keysToFilterPairs.get(MessageContextMapping.MappingType.AUTH_USER.toString());
                Assert.assertNotNull("authUsers should not be null", authFilters);
                Assert.assertFalse("authUsers should not be empty", authFilters.isEmpty());
                Assert.assertTrue("Authusers should be 2, actual size was " + authFilters.size(), authFilters.size() == 2);
                for (ReportApi.FilterPair fp : authFilters) {
                    Assert.assertTrue("FilterPair for auth user should be empty", fp.isEmpty());
                }
            }
        }
    }


    @Test
    public void testPerfStatRelativeReport_ServiceAndOperations() throws ReportException {
        Object o = JSON.parse(psRelativeJsonSummary);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        Collection<ReportSubmissionClusterBean> reportClusterBeans = convertor.getReportSubmissions(jsonMap, "Donal");
        Assert.assertNotNull(reportClusterBeans);

        for (ReportSubmissionClusterBean clusterBean : reportClusterBeans) {

            String clusterId = clusterBean.getClusterId();
            ReportApi.ReportSubmission reportSubmission = clusterBean.getReportSubmission();

            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = reportSubmission.getParameters();
            Assert.assertNotNull(reportParams);

            Map<String, ReportApi.ReportSubmission.ReportParam> paramMap =
                    new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
            for (ReportApi.ReportSubmission.ReportParam rP : reportParams) {
                paramMap.put(rP.getName(), rP);
            }

            ReportApi.ReportSubmission.ReportParam reportParam = paramMap.get(ReportApi.ReportParameters.SERVICE_ID_TO_NAME_MAP);
            Map<String, String> serviceIdToNameMap = (Map<String, String>) reportParam.getValue();
            Assert.assertNotNull(serviceIdToNameMap);

            if (clusterId.equals(cluster1)) {
                Assert.assertTrue("ServiceNames should be of size 2, it was " + serviceIdToNameMap.values().size(), serviceIdToNameMap.values().size() == 2);
            } else if (clusterId.equals(cluster2)) {
                Assert.assertTrue("ServiceNames should be of size 1, it was " + serviceIdToNameMap.values().size(), serviceIdToNameMap.values().size() == 1);
            } else {
                throw new IllegalStateException("Unexpected cluster id found :" + clusterId);
            }

            ReportApi.ReportSubmission.ReportParam serviceIdtoOp = paramMap.get(ReportApi.ReportParameters.SERVICE_ID_TO_OPERATIONS_MAP);
            Map<String, Set<String>> serviceIdToOps = (Map<String, Set<String>>) serviceIdtoOp.getValue();
            for (Map.Entry<String, Set<String>> me : serviceIdToOps.entrySet()) {

                if (clusterId.equals(cluster1)) {
                    if (me.getKey().equals("229376")) {
                        Assert.assertTrue("229376 should have 2 operations", me.getValue().size() == 2);
                    } else if (me.getKey().equals("229377")) {
                        Assert.assertTrue("229377 should have 1 operation", me.getValue().size() == 1);
                    } else {
                        throw new IllegalStateException("Unexpected key found in map: " + me.getKey());
                    }
                } else if (clusterId.equals(cluster2)) {
                    if (me.getKey().equals("229378")) {
                        Assert.assertTrue("229378 should have 1 operation", me.getValue().size() == 1);
                    } else {
                        throw new IllegalStateException("Unexpected key found in map: " + me.getKey());
                    }
                }
            }
        }
    }

    /**
     * Probably redundant, just checks that parse always throws an exception when it gets garbage data
     *
     * @throws Exception
     */
    @Test
    public void testJSonDataParse() throws Exception {
        Exception ex = null;
        try {
            String badData = "asdjhfasd;fsda";
            JSON.parse(badData);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertNotNull("Exception should have been thrown by JSON.parse", ex);
    }

    @Test
    public void testGroupingEmptyKey() throws Exception {
        Object o = JSON.parse(usageRelativeJsonSummaryTest_MissingKey);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        boolean exception = false;
        try {
            convertor.getReportSubmissions(jsonMap, "Donal");
        } catch (ReportException e) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown as key is empty string", exception);
    }

    /**
     * Test checks that if a report submission for a cluster for a usage report contains no groupings, that an
     * exception is thrown. A usage report cannot run unless a grouping has been specified
     *
     * @throws Exception
     */
    @Test
    public void testUsage_ClusterMissingGrouping() throws Exception {
        Object o = JSON.parse(usageRelativeJsonSummaryTest_ClusterMissingKey);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        boolean exception = false;
        try {
            convertor.getReportSubmissions(jsonMap, "Donal");
        } catch (ReportException e) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown as cluster2 has no groupings", exception);
    }

    @Test
    public void testUsage_OnlyMappingAuthUser() throws Exception {
        Object o = JSON.parse(usageOnlyMappingIsAuthUser);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        convertor.getReportSubmissions(jsonMap, "Donal");
    }

    @Test
    public void testInvalidAbsoluteDateFormat() throws Exception {
        Object o = JSON.parse(absoluteInvalidDateFormat);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        boolean exception = false;
        try {
            convertor.getReportSubmissions(jsonMap, "Donal");
        } catch (ReportException e) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown as absolute date is invalid", exception);
    }

    @Test
    public void testInvalidAbsoluteDatePeriod() throws Exception {
        Object o = JSON.parse(absoluteInvalidDatePeriod);
        Map jsonMap = (Map) o;
        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonMap);
        boolean exception = false;
        try {
            convertor.getReportSubmissions(jsonMap, "Donal");
        } catch (ReportException e) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown as absolute date end is before start", exception);
    }

    private final static String psRelativeJsonSummaryTest = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String psRelativeJsonSummary = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.*.*.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String psRelativeJsonSummaryMultipleKeyValues = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"SILVER\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.*.*.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String usageRelativeJson = "{\"reportType\":\"usage\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String usageIntervalRelativeJson = "{\"reportType\":\"usage\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : false," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String psRelativeJsonWithAuthUser = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"AUTH_USER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"AUTH_USER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String psRelativeIntervalJson = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.*.*.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : false," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String usageRelativeJsonSummaryTest_MissingKey = "{\"reportType\":\"usage\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.0.0.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"Usage_Summary_Report\"" +
            "}";

    private final static String usageRelativeJsonSummaryTest_ClusterMissingKey = "{\"reportType\":\"usage\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360448\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 1 [w1]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360449\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 2 [w2]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"360450\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"360451\"," +
            "            \"publishedServiceName\" : \"Warehouse Service 3 [w3]\"," +
            "            \"operation\"          : \"\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.0.0.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"Usage_Summary_Report\"" +
            "}";

    private final static String invalidTimeZone = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"invalidtimezone\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"127.*.*.1\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"GOLD\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : false," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String usageOnlyMappingIsAuthUser = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Europe/Paris\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"AUTH_USER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"AUTH_USER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : false," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String absoluteInvalidDateFormat = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"absolute\"," +
            "        \"start\"    : \"2008-07-31 13:00:00\"," +
            "        \"end\"    : \"2008-07-31 15:00:00\"," +
            "        \"timeZone\" : \"Europe/Paris\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"AUTH_USER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"AUTH_USER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : false," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String absoluteInvalidDatePeriod = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"absolute\"," +
            "        \"start\"    : \"2008/07/31 13:00:00\"," +
            "        \"end\"    : \"2008/07/30 15:00:00\"," +
            "        \"timeZone\" : \"Europe/Paris\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"AUTH_USER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster2 + "\"," +
            "            \"messageContextKey\" : \"AUTH_USER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : false," +
            "    \"reportName\" : \"My Report\"" +
            "}";

    private final static String psRelativeJsonSummaryOnly1ClusterWithKeys = "{\"reportType\":\"performance\",    " +
            "    \"entityType\" : \"publishedService\"," +
            "    \"entities\" : [" +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listProducts\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229376\"," +
            "            \"publishedServiceName\" : \"Warehouse [w1]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster1 + "\"," +
            "            \"publishedServiceId\" : \"229377\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }," +
            "        {" +
            "            \"clusterId\"          : \"" + cluster2 + "\"," +
            "            \"publishedServiceId\" : \"229378\"," +
            "            \"publishedServiceName\" : \"Warehouse [w2]\"," +
            "            \"operation\"          : \"listOrders\"," +
            "            \"relatedId\"          : null" +
            "        }" +
            "    ]," +
            "    \"timePeriod\" : {" +
            "        \"type\"     : \"relative\"," +
            "        \"numberOfTimeUnits\"    : \"1\"," +
            "        \"unitOfTime\"     : \"DAY\"," +
            "        \"timeZone\" : \"Canada/Pacific\"" +
            "    }," +
            "    \"timeInterval\" : {" +
            "        \"value\" : \"1\"," +
            "        \"unit\"  : \"HOUR\"" +
            "    }," +
            "    \"groupings\" : [" +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"IP_ADDRESS\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "        {" +
            "            \"clusterId\"         : \"" + cluster1 + "\"," +
            "            \"messageContextKey\" : \"CUSTOMER\"," +
            "            \"constraint\"        : \"\"" +
            "        }," +
            "    ]," +
            "    \"summaryChart\" : true," +
            "    \"summaryReport\" : true," +
            "    \"reportName\" : \"My Report\"" +
            "}";

}
