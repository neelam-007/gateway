/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 14, 2008
 * Time: 12:37:43 PM
 */
package com.l7tech.skunkworks.standardreports.test;

import org.junit.*;

import java.io.InputStream;
import java.util.*;
import java.sql.*;

import com.l7tech.gateway.standardreports.Utilities;
import com.l7tech.skunkworks.standardreports.ReportApp;
import com.l7tech.server.management.api.node.ReportApi;

public class CheckTestData {

    private Properties prop;

    private static String sqlNumDistinctValueRows = "select count(*) as num_rows from " +
            "(select distinct service_operation, mapping1_value, mapping2_value from " +
            "message_context_mapping_values) a";

    private static String sqlNumDailyMetricRows = "select count(*) as total_rows from service_metrics sm, " +
            "service_metrics_details smd where sm.objectid = smd.service_metrics_oid AND " +
            "sm.resolution = 2";

    private Connection conn = null;
    private Statement stmt = null;
    private final int numServices = 5;

    @Before
    public void setUp() throws Exception {
        prop = new Properties();
        InputStream is = CheckTestData.class.getResourceAsStream("testdata.properties");
        prop.load(is);
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
        //System.out.println("DB conn closed");
    }

    private int getNumValueRows() throws Exception {
        ResultSet rs = stmt.executeQuery(sqlNumDistinctValueRows);
        Assert.assertTrue(rs.first());
        return rs.getInt("num_rows");
    }

    @Test
    public void testCreatedData_NumValueRows() throws Exception {
        int numRows = getNumValueRows();
        Assert.assertTrue("numRows should equal 24, it was: " + numRows, numRows == 24);
    }

    private int getNumDailyRows() throws Exception {
        ResultSet rs = stmt.executeQuery(sqlNumDailyMetricRows);
        Assert.assertTrue(rs.first());
        return rs.getInt("total_rows");
    }

    @Test
    public void testCreatedData_NumDailyMetricRows() throws Exception {
        int numRows = getNumDailyRows();
        Assert.assertTrue("numRows should equal 43680, it was: " + numRows, numRows == 43680);
    }

    private int getMetricRowsPerDistinctValue() throws Exception {

        int numMetricRows = getNumDailyRows();
        int numValues = getNumValueRows();

        return numMetricRows / (numServices * numValues);
    }

    @Test
    public void testCreatedData_NumDailyMetricsPerDistinctValues() throws Exception {
        int numPerDistinctValues = getMetricRowsPerDistinctValue();
        Assert.assertTrue(numPerDistinctValues == 364);
    }

    private int getNumDistinctMetricDetailRows() throws Exception {
        //LinkedHashMap<String, List<FilterPair>> keysToFilterPairs = ReportApp.getFilterPairMap(prop);

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        List<ReportApi.FilterPair> authFilters = new ArrayList<ReportApi.FilterPair>();
        authFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("AUTH_USER", authFilters);

        // Assert.assertTrue("2 mapping keys should be specified in report.properties", keysToFilterPairs.keySet().size() == 2);

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        //is usage is false, as keys must be specified for this test
        String s = Utilities.getPerformanceStatisticsMappingQuery(true, null, null, serviceIdsToOps, keysToFilterPairs, 2, true, false);

        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) as total from (");
        sql.append(s);
        sql.append(") a");

        ResultSet rs = stmt.executeQuery(sql.toString());
        Assert.assertTrue(rs.first());
        System.out.println("Value sql: " + sql.toString());
        return rs.getInt("total");
    }

    /**
     * If we group by every mapping key we have in our test database, as well as operation,
     * and auth_user_id, we get a total number of groups. This group should equal the number of
     * value rows x the number of services we have. This test confirm that, it's sql comes from
     * Utilities.getPerformanceStatisticsMappingQuery(true... This query is used to drive the sub interval performance
     * statistics reports when context mapping's are used.
     *
     * @throws Exception
     */
    @Test
    public void testMasterMappingQuery_Master() throws Exception {
        int totalMasterRowsFound = getNumDistinctMetricDetailRows();
        int numValueRows = getNumValueRows();

//        System.out.println("total master rows: "+totalMasterRowsFound);
//        System.out.println("numValueRows: "+numValueRows);
//        System.out.println("numServices: "+numServices);

        Assert.assertTrue("Expected: " + totalMasterRowsFound + " rows, actual number found: " + (numValueRows * numServices), totalMasterRowsFound == (numValueRows * numServices));
    }

    private int getSumOfAllDailyValueGroups() throws SQLException {
        //LinkedHashMap<String, List<FilterPair>> keysToFilterPairs = ReportApp.getFilterPairMap(prop);
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        List<ReportApi.FilterPair> authFilters = new ArrayList<ReportApi.FilterPair>();
        authFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("AUTH_USER", authFilters);

        //Assert.assertTrue("2 mapping keys should be specified in report.properties", keysToFilterPairs.keySet().size() == 2);

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        String s = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, keysToFilterPairs, 2, true, false);

        StringBuilder sql = new StringBuilder();
        sql.append("select sum(total) as overall_total from (");
        sql.append(s.substring(0, s.indexOf("FROM")));
        sql.append(", count(*) as total ");
        sql.append(s.substring(s.indexOf("FROM"), s.length()));
        sql.append(") a");

        //System.out.println(sql.toString());

        ResultSet rs = stmt.executeQuery(sql.toString());
        Assert.assertTrue(rs.first());
        return rs.getInt("overall_total");
    }

    /**
     * Tests the query returned from Utilities.getPerformanceStatisticsMappingQuery(false...
     * If we group by every value group, operation, auth_user_id and add up the
     * sum of all the rows in each group, it should equal the total number of
     * daily metric bins in service_metric_details
     *
     * @throws Exception
     */
    @Test
    public void testMappingQuery_SummaryDaily() throws Exception {
        int sumOfAllValueGroups = getSumOfAllDailyValueGroups();
        int totalDailyRows = getNumDailyRows();

        Assert.assertTrue("Expected: totalDailyRows == sumOfAllValueGroups: " + totalDailyRows + " != " + sumOfAllValueGroups, totalDailyRows == sumOfAllValueGroups);
    }

    /**
     * Run a mapping query from Utilities.getPerformanceStatisticsMappingQuery(false... with keys and value specified. Wrap the query
     * to determine the sum of all rows found for each group that match the key and value criteria
     *
     * @return
     * @throws SQLException
     */
    private int getMappingQueryOverallTotalSpecificValues_Daily(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs) throws SQLException {
        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        String s = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdsToOps, keysToFilterPairs, 2, true, false);

        StringBuilder sql = new StringBuilder();
        sql.append("select sum(total) as overall_total from (");
        sql.append(s.substring(0, s.indexOf("FROM")));
        sql.append(", count(*) as total ");
        sql.append(s.substring(s.indexOf("FROM"), s.length()));
        sql.append(") a");

        System.out.println(sql.toString());

        ResultSet rs = stmt.executeQuery(sql.toString());
        Assert.assertTrue(rs.first());
        return rs.getInt("overall_total");
    }

    private int getMappingQueryOverallTotalSpecificValues_DailyNew(Map<String, Set<String>> serviceIdToOp,
                                                                   LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs
    ) throws SQLException {
        String s = Utilities.getPerformanceStatisticsMappingQuery(false, null, null, serviceIdToOp, keysToFilterPairs, 2, true, false);

        StringBuilder sql = new StringBuilder();
        sql.append("select sum(total) as overall_total from (");
        sql.append(s.substring(0, s.indexOf("FROM")));
        sql.append(", count(*) as total ");
        sql.append(s.substring(s.indexOf("FROM"), s.length()));
        sql.append(") a");

        System.out.println(sql.toString());

        ResultSet rs = stmt.executeQuery(sql.toString());
        Assert.assertTrue(rs.first());
        return rs.getInt("overall_total");
    }

    private int getDistinctValuesForKey(String key) throws Exception {
        PreparedStatement ps = null;
        try {
            String distinctValues = "SELECT count(*) as count from (select distinct CASE WHEN mcmk.mapping1_key = ? " +
                    "THEN mcmv.mapping1_value WHEN mcmk.mapping2_key = ? THEN mcmv.mapping2_value " +
                    "WHEN mcmk.mapping3_key = ? THEN mcmv.mapping3_value WHEN " +
                    "mcmk.mapping4_key = ? THEN mcmv.mapping4_value WHEN mcmk.mapping5_key = ? " +
                    "THEN mcmv.mapping5_value END AS MAPPING_VALUE_1 FROM  message_context_mapping_values mcmv, " +
                    "message_context_mapping_keys mcmk WHERE mcmv.mapping_keys_oid = mcmk.objectid ) a";

            ps = conn.prepareStatement(distinctValues);

            for (int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++) {
                ps.setString(i + 1, key);
            }

            ResultSet rs1 = ps.executeQuery();
            Assert.assertTrue(rs1.first());
            int totalNumValue = rs1.getInt("count");
            return totalNumValue;
        } finally {
            if (ps != null) ps.close();
            ps = null;
        }
    }

    /**
     * Checks that the mapping query generated by Utilities.getPerformanceStatisticsMappingQuery(false... for a specific set of values
     * for keys, yields the correct number of rows based on the filter criteria created.
     * The test data created by CreateTestData.java currently has the following properties, it doesn't matter if the
     * values in report.properties changes, the logic is the same, we have 3 values of Customer, 2 values of IP Address,
     * 2 operations and 2 users = 3x2x2x2 = 24.
     * This means that if you look at the distinct set of values in message_context_mapping_values, normalized for
     * key index location, you get 24 rows.
     * When a value is supplied for a key, the set of matching rows from message_context_mapping_values is
     * constrained. This test checks that the constraint has been implemented correctly by doing the following:-
     * 1) Execute the query with the value constraints supplied
     * 2) Determines for each key how many distinct rows it contains
     * 3) Adds up the number of distinct rows for each key
     * 4) Gets the total number of normalized distinct value rows
     * 5) Determines the applicable number of value rows by dividing the result from 4) by the sum from 3)
     * 6) Gets the number of rows expected for an individual value row
     * 7) Multiples 5) x 6) which must equal the value from 1)
     *
     * @throws Exception
     */
    @Test
    public void testMappingQuerySpecificValues_SummaryDaily() throws Exception {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = ReportApp.getFilterPairMap(prop);
        validateKeyFilterMap(keysToFilterPairs);

        int total = getMappingQueryOverallTotalSpecificValues_Daily(keysToFilterPairs);
        //364
        int rowsPerDistinctValue = getMetricRowsPerDistinctValue();
        //24
        int numValueRows = getNumValueRows();

        int numApplicableValueRows = 0;
        for (String s1 : keysToFilterPairs.keySet()) {
            numApplicableValueRows += getDistinctValuesForKey(s1);
        }

        int totalValueFactor = numValueRows / numApplicableValueRows;
        //System.out.println("totalValueFactor: " + totalValueFactor);
        int queryExpectedTotal = rowsPerDistinctValue * totalValueFactor * numServices;
        //System.out.println("queryExpectedTotal: " + queryExpectedTotal);
        //System.out.println("total: " + total);
        Assert.assertTrue("Expected: " + queryExpectedTotal + " rows, found: " + total, queryExpectedTotal == total);
    }

    private void validateKeyFilterMap(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs) {
        if (keysToFilterPairs.keySet().size() != 2) {
            throw new IllegalArgumentException("This test designed to work with two keys");
        }
        for (Map.Entry<String, List<ReportApi.FilterPair>> me : keysToFilterPairs.entrySet()) {
            if (me.getValue().size() != 1) {
                throw new IllegalArgumentException("Each key must have 1 FilterPair");
            }
            if (me.getValue().get(0).isConstraintNotRequired()) {
                throw new IllegalArgumentException("Test is designed to work with actual key constraint values");
            }
        }
    }

    @Test
    public void testMappingQuerySpecificValues_SummaryDailyNew() throws Exception {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = ReportApp.getFilterPairMap(prop);
        validateKeyFilterMap(keysToFilterPairs);

        Map<String, Set<String>> serviceIdToOp = new HashMap<String, Set<String>>();

        List<String> ops = ReportApp.loadListFromProperties(ReportApp.OPERATIONS, prop);

        String serviceOid = prop.getProperty(ReportApp.SERVICE_ID_TO_NAME_OID + "_1");
        int index = 2;
        while (serviceOid != null) {
            serviceIdToOp.put(serviceOid, new HashSet<String>(ops));
            serviceOid = prop.getProperty(ReportApp.SERVICE_ID_TO_NAME_OID + "_" + index);
            index++;
        }

        int total = getMappingQueryOverallTotalSpecificValues_DailyNew(serviceIdToOp, keysToFilterPairs);
        //364
        int rowsPerDistinctValue = getMetricRowsPerDistinctValue();
        //24
        int numValueRows = getNumValueRows();

        int numApplicableValueRows = 0;
        for (String s1 : keysToFilterPairs.keySet()) {
            numApplicableValueRows += getDistinctValuesForKey(s1);
        }

        int totalValueFactor = numValueRows / numApplicableValueRows;
        //System.out.println("totalValueFactor: " + totalValueFactor);
        int queryExpectedTotal = rowsPerDistinctValue * totalValueFactor * numServices;
        //System.out.println("queryExpectedTotal: " + queryExpectedTotal);
        //System.out.println("total: " + total);
        Assert.assertTrue("Expected total was: " + queryExpectedTotal + " actual total was: " + total + ", " +
                "check that the operations match those in the db", queryExpectedTotal == total);
    }
}
