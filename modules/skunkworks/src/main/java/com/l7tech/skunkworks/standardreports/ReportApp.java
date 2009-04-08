/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 29, 2008
 * Time: 1:58:00 PM
 * ReportApp is a CLI program to test jasper reports. Depends on report.properties being in the same directory
 */
package com.l7tech.skunkworks.standardreports;

import java.io.*;
import java.sql.*;
import java.util.*;

import net.sf.jasperreports.engine.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.gateway.standardreports.*;
import com.l7tech.server.management.api.node.ReportApi;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;


public class ReportApp {
    private static final String TASK_RUN = "run";

    //The following params must be supplied when filling the report
    private static final String REPORT_CONNECTION = "REPORT_CONNECTION";
    private static final String REPORT_TYPE = "REPORT_TYPE";
    private static final String INTERVAL_TIME_UNIT = "INTERVAL_TIME_UNIT";
    private static final String INTERVAL_NUM_OF_TIME_UNITS = "INTERVAL_NUM_OF_TIME_UNITS";
    private static final String REPORT_RAN_BY = "REPORT_RAN_BY";

    private static final String TEMPLATE_FILE_ABSOLUTE = "TEMPLATE_FILE_ABSOLUTE";
    private static final String SUBREPORT_DIRECTORY = "SUBREPORT_DIRECTORY";

    private static final String IS_CONTEXT_MAPPING = "IS_CONTEXT_MAPPING";
    public static final String IS_DETAIL = "IS_DETAIL";
    //Optional
    public static final String SERVICE_ID_TO_NAME = "SERVICE_ID_TO_NAME";
    public static final String SERVICE_ID_TO_NAME_OID = "SERVICE_ID_TO_NAME_OID";

    //one of each set must be supplied - relative or absolute time
    private static final String IS_RELATIVE = "IS_RELATIVE";
    private static final String RELATIVE_NUM_OF_TIME_UNITS = "RELATIVE_NUM_OF_TIME_UNITS";
    private static final String RELATIVE_TIME_UNIT = "RELATIVE_TIME_UNIT";

    private static final String IS_ABSOLUTE = "IS_ABSOLUTE";
    private static final String ABSOLUTE_START_TIME = "ABSOLUTE_START_TIME";
    private static final String ABSOLUTE_END_TIME = "ABSOLUTE_END_TIME";

    public static final String KEYS_TO_LIST_FILTER_PAIRS = "KEYS_TO_LIST_FILTER_PAIRS";

    public static final String MAPPING_KEY_ = "MAPPING_KEY_";
    public static final String VALUE_ = "_VALUE_";

    public static final String OPERATIONS = "OPERATIONS";

    private static final String SPECIFIC_TIME_ZONE = "SPECIFIC_TIME_ZONE";
    private static final String IS_USING_KEYS = "IS_USING_KEYS";
    private static final String IS_IGNORE_PAGINATION = "IS_IGNORE_PAGINATION";

    //db props
    private static final String CONNECTION_STRING = "CONNECTION_STRING";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASSWORD = "DB_PASSWORD";

    //Non report params, just used in ReportApp
    private static final String HOURLY_MAX_RETENTION_NUM_DAYS = "HOURLY_MAX_RETENTION_NUM_DAYS";
    private static final String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
    private static final String REPORT_SCRIPTLET = "REPORT_SCRIPTLET";
    private static final String SUB_INTERVAL_SUB_REPORT = "SUB_INTERVAL_SUB_REPORT";
    private static final String SUB_REPORT = "SUB_REPORT";
    private static final String PRINT_CHART = "PRINT_CHART";
    private static final String DISPLAY_STRING_TO_MAPPING_GROUP = "DISPLAY_STRING_TO_MAPPING_GROUP";
    private static final String SUB_REPORT_HELPER = "SUB_REPORT_HELPER";
    private static final String SERVICE_ID_TO_OPERATIONS_MAP = "SERVICE_ID_TO_OPERATIONS_MAP";

    private static final String SKUNKWORK_RELATIVE_PATH = "reportOutput";
    private static final String REPORTING_RELATIVE_PATH = "../../../../../../../../gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports";
    private static final String SERVICE_ID_TO_NAME_MAP = "SERVICE_ID_TO_NAME_MAP";

    public static final Properties prop = new Properties();
    private static final String REPORT_DATA_SOURCE = "REPORT_DATA_SOURCE";
    private static final String SQL_PARAMS = "sqlParams";

    public ReportApp() {
    }

    /**
     *
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        String taskName = args[0];
        ReportApp reportApp = new ReportApp();
        reportApp.run(taskName);

    }

    public void run(String taskName) throws Exception {

        File f = new File(SKUNKWORK_RELATIVE_PATH);
        if (!f.exists()) {
            if (!f.mkdir()) {
                throw new RuntimeException("Cannot create folder: " + SKUNKWORK_RELATIVE_PATH);
            } else {
                System.out.println("Creating outupt directory where transformed jrxml and xml documents will be outputted to");
            }
        }

        InputStream in = ReportApp.class.getResourceAsStream("/com/l7tech/skunkworks/standardreports/test/report.properties");
        prop.load(in);
        try {
            long start = System.currentTimeMillis();
            if (TASK_RUN.equals(taskName)) {
                fill(start);
            } else if ("ps".equals(taskName)) {
                testPS();
            } else {
                usage();
            }
        }
        catch (JRException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fill a report using a prepared statement
     */
    private void testPS() throws Exception {
        Map<String, Object> parameters = getParameters();

        String sql = "" +
                "SELECT distinct p.objectid as SERVICE_ID, p.name as SERVICE_NAME, p.routing_uri as ROUTING_URI ,'1' as " +
                "CONSTANT_GROUP, ';' AS AUTHENTICATED_USER, ';' AS SERVICE_OPERATION_VALUE, ';' AS MAPPING_VALUE_1, ';' " +
                "AS MAPPING_VALUE_2, ';' AS MAPPING_VALUE_3, ';' AS MAPPING_VALUE_4, ';' AS MAPPING_VALUE_5 " +
                "FROM service_metrics sm, published_service p WHERE p.objectid = sm.published_service_oid  " +
                "AND sm.resolution = ?  AND sm.period_start >= ? " +
                "AND sm.period_start < ? AND p.objectid IN (?, ?, ?, ?, ?) ORDER BY p.objectid ";

        sql = "SELECT DISTINCT  ';' AS AUTHENTICATED_USER, CASE  WHEN mcmk.mapping1_key = ? THEN mcmv.mapping1_value " +
                "WHEN mcmk.mapping2_key = ? THEN mcmv.mapping2_value WHEN mcmk.mapping3_key = ? THEN mcmv.mapping3_value " +
                "WHEN mcmk.mapping4_key = ? THEN mcmv.mapping4_value WHEN mcmk.mapping5_key = ? THEN mcmv.mapping5_value " +
                "END AS MAPPING_VALUE_1, ';' AS MAPPING_VALUE_2, ';' AS MAPPING_VALUE_3, ';' AS MAPPING_VALUE_4, ';' " +
                "AS MAPPING_VALUE_5 FROM service_metrics sm, published_service p, service_metrics_details smd, " +
                "message_context_mapping_values mcmv, message_context_mapping_keys mcmk WHERE " +
                "p.objectid = sm.published_service_oid AND sm.objectid = smd.service_metrics_oid " +
                "AND smd.mapping_values_oid = mcmv.objectid AND mcmv.mapping_keys_oid = mcmk.objectid  " +
                "AND sm.resolution = ?  AND sm.period_start >= ? AND sm.period_start < ? " +
                "AND p.objectid IN (?, ?, ?, ?, ?) AND (( mcmk.mapping1_key = ?  )  OR ( mcmk.mapping2_key = ?  )  " +
                "OR ( mcmk.mapping3_key = ?  )  OR ( mcmk.mapping4_key = ?  )  OR ( mcmk.mapping5_key = ?  )  )  " +
                "ORDER BY AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5";

        Connection conn = getConnection(prop);
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, "IP_ADDRESS");
        ps.setString(2, "IP_ADDRESS");
        ps.setString(3, "IP_ADDRESS");
        ps.setString(4, "IP_ADDRESS");
        ps.setString(5, "IP_ADDRESS");
        ps.setInt(6, 1);
        ps.setLong(7, 1237489200000L);
        ps.setLong(8, 1237525200000L);
        ps.setLong(9, 688129L);
        ps.setLong(10, 688128L);
        ps.setLong(11, 360451L);
        ps.setLong(12, 688130L);
        ps.setLong(13, 688131L);
        ps.setString(14, "IP_ADDRESS");
        ps.setString(15, "IP_ADDRESS");
        ps.setString(16, "IP_ADDRESS");
        ps.setString(17, "IP_ADDRESS");
        ps.setString(18, "IP_ADDRESS");

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            System.out.println(rs.getString("MAPPING_VALUE_1"));
        }


    }

    private void fill(long start) throws Exception {
        Map<String, Object> parameters = getParameters();
        String type = parameters.get(REPORT_TYPE).toString();

        int numRelativeTimeUnits = Integer.valueOf(parameters.get(RELATIVE_NUM_OF_TIME_UNITS).toString());
        Utilities.UNIT_OF_TIME relUnitOfTime = Utilities.getUnitFromString(prop.getProperty(RELATIVE_TIME_UNIT));

        String timeZone = prop.getProperty(SPECIFIC_TIME_ZONE);
        boolean isAbsolute = Boolean.parseBoolean(prop.getProperty(IS_ABSOLUTE).toString());

        long startTimeInPast;
        long endTimeInPast;

        if (!isAbsolute) {
            startTimeInPast = Utilities.getRelativeMilliSecondsInPast(numRelativeTimeUnits, relUnitOfTime, timeZone);
            endTimeInPast = Utilities.getMillisForEndTimePeriod(relUnitOfTime, timeZone);
        } else {
            String abStartTime = prop.getProperty(ABSOLUTE_START_TIME);
            String abEndTime = prop.getProperty(ABSOLUTE_END_TIME);

            startTimeInPast = Utilities.getAbsoluteMilliSeconds(abStartTime, timeZone);
            endTimeInPast = Utilities.getAbsoluteMilliSeconds(abEndTime, timeZone);
        }

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getFilterPairMap(prop);
        parameters.put(KEYS_TO_LIST_FILTER_PAIRS, keysToFilterPairs);
        Map<String, Set<String>> serivceIdsToOp = (Map<String, Set<String>>) parameters.get(SERVICE_ID_TO_OPERATIONS_MAP);


        boolean isDetail = Boolean.valueOf(parameters.get(IS_DETAIL).toString());
        Object scriplet = parameters.get(REPORT_SCRIPTLET);

        int resolution = Utilities.getSummaryResolutionFromTimePeriod(30, startTimeInPast, endTimeInPast);

        boolean isContextMapping = Boolean.valueOf(parameters.get(IS_CONTEXT_MAPPING).toString());

        boolean isUsingKeys = !keysToFilterPairs.isEmpty();

        //
//        Connection conn1 = getConnection(prop);
//        Statement stmt = conn1.createStatement();
//        ResultSet rs = stmt.executeQuery("SELECT distinct p.objectid as SERVICE_ID, p.name as SERVICE_NAME, p.routing_uri as ROUTING_URI ,'1' as CONSTANT_GROUP, ';' AS AUTHENTICATED_USER,  mcmv.service_operation AS SERVICE_OPERATION_VALUE, CASE  WHEN mcmk.mapping1_key = 'CustomHeader' THEN mcmv.mapping1_value WHEN mcmk.mapping2_key = 'CustomHeader' THEN mcmv.mapping2_value WHEN mcmk.mapping3_key = 'CustomHeader' THEN mcmv.mapping3_value WHEN mcmk.mapping4_key = 'CustomHeader' THEN mcmv.mapping4_value WHEN mcmk.mapping5_key = 'CustomHeader' THEN mcmv.mapping5_value END AS MAPPING_VALUE_1, ';' AS MAPPING_VALUE_2, ';' AS MAPPING_VALUE_3, ';' AS MAPPING_VALUE_4, ';' AS MAPPING_VALUE_5 FROM service_metrics sm, published_service p, service_metrics_details smd, message_context_mapping_values mcmv, message_context_mapping_keys mcmk WHERE p.objectid = sm.published_service_oid AND sm.objectid = smd.service_metrics_oid AND smd.mapping_values_oid = mcmv.objectid AND mcmv.mapping_keys_oid = mcmk.objectid  AND sm.period_start >=1237399200000 AND sm.period_start <1237410000000 AND p.objectid IN (688129, 688128, 360451, 688130, 688131) AND (( mcmk.mapping1_key = 'CustomHeader'  )  OR ( mcmk.mapping2_key = 'CustomHeader'  )  OR ( mcmk.mapping3_key = 'CustomHeader'  )  OR ( mcmk.mapping4_key = 'CustomHeader'  )  OR ( mcmk.mapping5_key = 'CustomHeader'  )  )  ORDER BY AUTHENTICATED_USER, MAPPING_VALUE_1, MAPPING_VALUE_2, MAPPING_VALUE_3, MAPPING_VALUE_4, MAPPING_VALUE_5 ,p.objectid, SERVICE_OPERATION_VALUE");
//        while(rs.next()){
//            String colVal = rs.getString("MAPPING_VALUE_1");
//            System.out.println("ColVal is : '" + colVal+"'");
//            if(colVal == null){
//                System.out.println("Col val is actually java null");
//            }
//        }
//        System.out.println("After query");
//        if(true) System.exit(1);
        //

        Pair<String, List<Object>> sqlAndParamsPair;
        Boolean isUsage = Boolean.valueOf(prop.getProperty("IS_USAGE"));
        if (isContextMapping && isUsingKeys) {
            sqlAndParamsPair = Utilities.getDistinctMappingQuery(startTimeInPast, endTimeInPast, serivceIdsToOp, keysToFilterPairs, resolution, isDetail, isUsage);
        } else if (isContextMapping) {
            sqlAndParamsPair = Utilities.getPerformanceStatisticsMappingQuery(true, startTimeInPast, endTimeInPast, serivceIdsToOp, keysToFilterPairs, resolution, isDetail, isUsage);
        } else {
            sqlAndParamsPair = Utilities.getNoMappingQuery(true, startTimeInPast, endTimeInPast, serivceIdsToOp.keySet(), resolution);
        }
        //System.out.println("Distinct sql: " + sql);

        if (!type.equals("Usage") && !type.equals("Usage_Interval")) {
            if (type.equals("Performance_Interval")) {
                runPerfStatIntervalReport(isContextMapping, isUsingKeys, prop, parameters, sqlAndParamsPair, keysToFilterPairs);
            } else {
                runPerfStatSummaryReport(isContextMapping, isUsingKeys, prop, parameters, sqlAndParamsPair, keysToFilterPairs);
            }
        } else {
            if (type.equals("Usage")) {
                runUsageReport(prop, parameters, scriplet, sqlAndParamsPair, keysToFilterPairs);
            } else if (type.equals("Usage_Interval")) {
                runUsageIntervalReport(prop, parameters, scriplet, sqlAndParamsPair, keysToFilterPairs);
            }
        }

        System.err.println("Filling time : " + (System.currentTimeMillis() - start));
    }

    public static LinkedHashMap<String, String> loadMapFromProperties(String key1, String key2, Properties prop) {

        LinkedHashMap<String, String> returnMap = new LinkedHashMap<String, String>();
        String key1Name = prop.getProperty(key1 + "_1");
        String key2Name = prop.getProperty(key2 + "_1");
        int index = 2;

        while (key1Name != null && key2Name != null) {
            returnMap.put(key1Name, key2Name);
            key1Name = prop.getProperty(key1 + "_" + index);
            key2Name = prop.getProperty(key2 + "_" + index);
            index++;
        }

        return returnMap;
    }


    public static LinkedHashMap<String, List<ReportApi.FilterPair>> getFilterPairMap(Properties prop) {
        LinkedHashMap<String, List<ReportApi.FilterPair>> returnMap = new LinkedHashMap<String, List<ReportApi.FilterPair>>();

        int index = 1;
        String keyName = prop.getProperty(MAPPING_KEY_ + index);
        while (keyName != null) {
            int valueIndex = 1;
            ReportApi.FilterPair defaultFilter = new ReportApi.FilterPair();
            List<ReportApi.FilterPair> filterList = new ArrayList<ReportApi.FilterPair>();

            //any values for this key?

            String keyValue = prop.getProperty(MAPPING_KEY_ + index + VALUE_ + valueIndex);
            while (keyValue != null) {
                //todo [Donal] replace wildcard with %
                ReportApi.FilterPair filter = new ReportApi.FilterPair(keyValue);
                filterList.add(filter);
                valueIndex++;
                keyValue = prop.getProperty(MAPPING_KEY_ + index + VALUE_ + valueIndex);
            }

            if (filterList.isEmpty()) filterList.add(defaultFilter);
            returnMap.put(keyName, filterList);
            index++;
            keyName = prop.getProperty(MAPPING_KEY_ + index);
        }

        return returnMap;
    }

    public static List<String> loadListFromProperties(String key, Properties prop) {

        List<String> returnList = new ArrayList<String>();
        String key1Name = prop.getProperty(key + "_1");
        int index = 2;

        while (key1Name != null) {
            returnList.add(key1Name);
            key1Name = prop.getProperty(key + "_" + index);
            index++;
        }

        return returnList;
    }

    /**
     * Get the ordered set of distinct mapping sets for the keys and values in the sql string from the db
     *
     * @param connection
     * @param sqlAndParamsPair
     * @return
     * @throws Exception
     */
    public static LinkedHashSet<List<String>> getDistinctMappingSets(Connection connection,
                                                                     Pair<String, List<Object>> sqlAndParamsPair) throws Exception {
        LinkedHashSet<List<String>> returnSet = new LinkedHashSet<List<String>>();

        try {
            PreparedStatementDataSource psds = new PreparedStatementDataSource(connection);
            psds.configure(sqlAndParamsPair.getKey(), sqlAndParamsPair.getValue());

            while (psds.next()) {
                List<String> mappingStrings = new ArrayList<String>();
                String authUser = (String) psds.getFieldValue(new JRFieldAdapter() {
                    public String getName() {
                        return PreparedStatementDataSource.ColumnName.AUTHENTICATED_USER.getColumnName();
                    }
                });
                mappingStrings.add(authUser);
                for (int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++) {
                    final int index = i;
                    String aMapStr = (String) psds.getFieldValue(new JRFieldAdapter() {
                        public String getName() {
                            PreparedStatementDataSource.ColumnName columnName =
                                    PreparedStatementDataSource.ColumnName.getColumnName("MAPPING_VALUE_" + (index + 1));
                            return columnName.getColumnName();
                        }
                    });

                    mappingStrings.add(aMapStr);
                }
                returnSet.add(mappingStrings);
            }
        } catch (Exception ex) {
            if (connection != null) connection.close();
            throw (ex);
        }
        return returnSet;
    }

    private LinkedHashSet<Pair<String, String>> getServiceDisplayStrings(Connection connection,
                                                                         Pair<String, List<Object>> sqlAndParamsPair) throws Exception {
        try {
            PreparedStatementDataSource psds = new PreparedStatementDataSource(connection);
            psds.configure(sqlAndParamsPair.getKey(), sqlAndParamsPair.getValue());

            LinkedHashSet<Pair<String, String>> set = new LinkedHashSet<Pair<String, String>>();
            while (psds.next()) {
                String serviceName = (String) psds.getFieldValue(new JRFieldAdapter() {
                    public String getName() {
                        return PreparedStatementDataSource.ColumnName.SERVICE_NAME.getColumnName();
                    }
                });

                String routingUri = (String) psds.getFieldValue(new JRFieldAdapter() {
                    public String getName() {
                        return PreparedStatementDataSource.ColumnName.ROUTING_URI.getColumnName();
                    }
                });

                set.add(new Pair<String, String>(serviceName, routingUri));
            }
            return set;

        } catch (Exception ex) {
            if (connection != null) connection.close();
            throw (ex);
        }
    }

    private void runUsageIntervalReport(Properties prop, Map parameters, Object scriplet,
                                        Pair<String, List<Object>> sqlAndParamsPair,
                                        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs)
            throws Exception {
        UsageReportHelper helper = (UsageReportHelper) scriplet;
        Connection connection = getConnection(prop);
        LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sqlAndParamsPair);
        LinkedHashMap<String, String> keyToColumnName = RuntimeDocUtilities.getKeyToColumnValues(distinctMappingSets);
        helper.setKeyToColumnMap(keyToColumnName);
        UsageSummaryAndSubReportHelper summaryAndSubReportHelper = new UsageSummaryAndSubReportHelper();
        summaryAndSubReportHelper.setKeyToColumnMap(keyToColumnName);
        parameters.put(SUB_REPORT_HELPER, summaryAndSubReportHelper);

        LinkedHashSet<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets, false, null, null);
        LinkedHashMap<Integer, String> groupIndexToGroup = Utilities.getGroupIndexToGroupString(mappingValuesLegend.size());
        helper.setIndexToGroupMap(groupIndexToGroup);

        //Master report first
        Document transformDoc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();

        File f = new File(SKUNKWORK_RELATIVE_PATH + "/UsageMasterTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

        String xslStr = getResAsString(REPORTING_RELATIVE_PATH + "/UsageReportIntervalTransform_Master.xsl");
        String xmlFileName = getResAsString(REPORTING_RELATIVE_PATH + "/Usage_IntervalMasterReport_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 820);
        params.put("PageMinWidth", 850);
        params.put("TitleInnerFrameBuffer", 7);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperMasterDoc = transform(xslStr, xmlFileName, params);

        f = new File(SKUNKWORK_RELATIVE_PATH + "/UsageMasterJasperRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperMasterDoc, fos);
        } finally {
            fos.close();
        }

        //MasterSubInterval report
        transformDoc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets).getDocument();
        f = new File(SKUNKWORK_RELATIVE_PATH + "/UsageSubIntervalTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

        xslStr = getResAsString(REPORTING_RELATIVE_PATH + "/UsageReportSubIntervalTransform_Master.xsl");
        xmlFileName = getResAsString(REPORTING_RELATIVE_PATH + "/Usage_SubIntervalMasterReport_Template.jrxml");
        params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 820);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperSubIntervalDoc = transform(xslStr, xmlFileName, params);

        f = new File(SKUNKWORK_RELATIVE_PATH + "/SubIntervalJasperRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperSubIntervalDoc, fos);
        } finally {
            fos.close();
        }

        //subreport report
        transformDoc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets).getDocument();
        f = new File(SKUNKWORK_RELATIVE_PATH + "/UsageSubReportTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

        xslStr = getResAsString(REPORTING_RELATIVE_PATH + "/Usage_SubReport.xsl");
        xmlFileName = getResAsString(REPORTING_RELATIVE_PATH + "/Usage_SubIntervalMasterReport_subreport0_Template.jrxml");
        params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 707);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperSubReportDoc = transform(xslStr, xmlFileName, params);

        f = new File(SKUNKWORK_RELATIVE_PATH + "/UsageSubReportJasperRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperSubReportDoc, fos);
        } finally {
            fos.close();
        }

        //Compile all 3 reports
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperMasterDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        JasperReport masterReport = JasperCompileManager.compileReport(bais);

        baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperSubIntervalDoc, baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        JasperReport subIntervalReport = JasperCompileManager.compileReport(bais);

        baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperSubReportDoc, baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        JasperReport subReport = JasperCompileManager.compileReport(bais);

        parameters.put(SUB_INTERVAL_SUB_REPORT, subIntervalReport);
        parameters.put(SUB_REPORT, subReport);

        System.out.println("Reports compiled");

        //add in the data source
        PreparedStatementDataSource psds = new PreparedStatementDataSource(connection);
        parameters.put(REPORT_DATA_SOURCE, psds);

        JasperPrint jp = null;
        try {
            System.out.println("Filling report");
            jp = JasperFillManager.fillReport(masterReport, parameters, psds);
            System.out.println("Report filled");
        } finally {
            connection.close();
        }

//        System.out.println("Viewing...");
//        try{
//            JasperViewer.viewReport(jp, false);
//        }catch(Exception ex){
//            System.out.println("Exception: " + ex.getMessage());
//            ex.printStackTrace();
//        }

        JasperExportManager.exportReportToPdfFile(jp, "UsageInterval.pdf");
        JasperExportManager.exportReportToHtmlFile(jp, "UsageInterval.html");

    }

    private void runPerfStatSummaryReport(boolean isContextMapping, boolean isUsingKeys, Properties prop, Map parameters,
                                          Pair<String, List<Object>> sqlAndParamsPair, LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs) throws Exception {
        Connection connection = getConnection(prop);

        LinkedHashMap<String, String> displayStringToGroup = new LinkedHashMap<String, String>();

        Document transformDoc = null;
        if (isContextMapping && isUsingKeys) {
            LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sqlAndParamsPair);
            LinkedHashSet<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets, false, null, null);
            //We need to look up the mappingValues from both the group value and also the display string value

            int index = 1;
            for (String s : mappingValuesLegend) {
                String group = "Group " + index;
                //System.out.println("Group: " + group+" s: " + s);
//                groupToDisplayString.put(group, s);
                displayStringToGroup.put(s, group);
                index++;
            }

            transformDoc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        } else {
            LinkedHashSet<Pair<String, String>> serviceValues = getServiceDisplayStrings(connection, sqlAndParamsPair);
            LinkedHashMap<String, Pair<String, String>> groupToDisplayString = new LinkedHashMap<String, Pair<String, String>>();

            //We need to look up the mappingValues from both the group value and also the display string value
            int index = 1;
            for (Pair<String, String> pair : serviceValues) {
                String shortServiceName = "Service " + index;
                //System.out.println("Service: " + shortServiceName+" s: " + pair);
//                groupToDisplayString.put(shortServiceName, TextUtils.truncStringMiddleExact(pair, 150));
                //groupToDisplayString needs truncated values

                String serviceTrunc = Utilities.getServiceStringTruncatedNoEscape(pair.getKey(), Utilities.SERVICE_DISPLAY_NAME_LENGTH);
                String routingTrunc = Utilities.getRoutingUriStringTruncatedNoEscape(pair.getValue(), Utilities.ROUTING_URI_LENGTH);
                groupToDisplayString.put(shortServiceName, new Pair<String, String>(serviceTrunc, routingTrunc));
                //displayStringToGroup must not have truncated values
                String displayName = Utilities.getServiceDisplayStringNotTruncatedNoEscape(pair.getKey(), pair.getValue());
                displayStringToGroup.put(displayName, shortServiceName);
                index++;
            }
            transformDoc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(groupToDisplayString).getDocument();
        }

        //Set the parameter IS_USING_KEYS to let chart know if operation is being used alone, to make some display changes

        parameters.put(IS_USING_KEYS, isUsingKeys);

        parameters.put(DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);

        String xslStr = getResAsString(REPORTING_RELATIVE_PATH + "/PS_SummaryTransform.xsl");
        String xmlSrc = getResAsString(REPORTING_RELATIVE_PATH + "/PS_Summary_Template.jrxml");
        //String xmlSrc = getResAsString(SKUNKWORK_RELATIVE_PATH+"/PS_Summary_Template_New_Title.jrxml");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        File f = new File(SKUNKWORK_RELATIVE_PATH + "/PS_SummaryRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport summaryReport = JasperCompileManager.compileReport(bais);
        System.out.println("Report compiled");

        //add in the data source
        PreparedStatementDataSource psds = new PreparedStatementDataSource(connection);
        parameters.put(REPORT_DATA_SOURCE, psds);

        JasperPrint jp = null;
        try {
            System.out.println("Filling summaryReport");
            jp = JasperFillManager.fillReport(summaryReport, parameters, psds);
            System.out.println("Report filled");
        } finally {
//            psds.close();
            System.out.println("IN FINALLY AFTER FILLING REPORT");
            connection.close();
        }

//        System.out.println("Viewing...");
//        try{
//            JasperViewer.viewReport(jp, false);
//        }catch(Exception ex){
//            System.out.println("Exception: " + ex.getMessage());
//            ex.printStackTrace();
//        }

        JasperExportManager.exportReportToPdfFile(jp, "PS_Summary.pdf");

        JasperExportManager.exportReportToHtmlFile(jp, "PS_Summary.html");

    }


    private void runPerfStatIntervalReport(boolean isContextMapping, boolean isUsingKeys, Properties prop, Map parameters,
                                           Pair<String, List<Object>> sqlAndParamsPair,
                                           LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs)
            throws Exception {

        //Compile both subreports and add to parameters
        String subIntervalReport = getResAsString(REPORTING_RELATIVE_PATH + "/PS_SubIntervalMasterReport.jrxml");
        ByteArrayInputStream bais = new ByteArrayInputStream(subIntervalReport.getBytes("UTF-8"));
        JasperReport subIntervalCompiledReport = JasperCompileManager.compileReport(bais);

        String subReport = getResAsString(REPORTING_RELATIVE_PATH + "/PS_SubIntervalMasterReport_subreport0.jrxml");
        bais = new ByteArrayInputStream(subReport.getBytes("UTF-8"));
        JasperReport subCompiledReport = JasperCompileManager.compileReport(bais);

        parameters.put(SUB_INTERVAL_SUB_REPORT, subIntervalCompiledReport);
        parameters.put(SUB_REPORT, subCompiledReport);

        Connection connection = getConnection(prop);
        Document transformDoc = null;
        LinkedHashMap<String, String> displayStringToGroup = new LinkedHashMap<String, String>();

        if (isContextMapping && isUsingKeys) {
            LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sqlAndParamsPair);
            LinkedHashSet<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets, false, null, null);
            //We need to look up the mappingValues from both the group value and also the display string value

            int index = 1;
            for (String s : mappingValuesLegend) {
                String group = "Group " + index;
                //System.out.println("Group: " + group+" s: " + s);
                displayStringToGroup.put(s, group);
                index++;
            }

            transformDoc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        } else {
            LinkedHashSet<Pair<String, String>> serviceValues = getServiceDisplayStrings(connection, sqlAndParamsPair);
            LinkedHashMap<String, Pair<String, String>> groupToDisplayString = new LinkedHashMap<String, Pair<String, String>>();

            //We need to look up the mappingValues from both the group value and also the display string value
            int index = 1;
            for (Pair<String, String> pair : serviceValues) {
                String shortServiceName = "Service " + index;
                //System.out.println("Service: " + shortServiceName+" s: " + pair);
//                groupToDisplayString.put(shortServiceName, TextUtils.truncStringMiddleExact(pair, 150));
                //groupToDisplayString needs truncated values

                String serviceTrunc = Utilities.getServiceStringTruncatedNoEscape(pair.getKey(), Utilities.SERVICE_DISPLAY_NAME_LENGTH);
                String routingTrunc = Utilities.getRoutingUriStringTruncatedNoEscape(pair.getValue(), Utilities.ROUTING_URI_LENGTH);
                groupToDisplayString.put(shortServiceName, new Pair<String, String>(serviceTrunc, routingTrunc));
                //displayStringToGroup must not have truncated values
                String displayName = Utilities.getServiceDisplayStringNotTruncatedNoEscape(pair.getKey(), pair.getValue());
                displayStringToGroup.put(displayName, shortServiceName);
                index++;
            }
            transformDoc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(groupToDisplayString).getDocument();
        }

        parameters.put(IS_USING_KEYS, isUsingKeys);

        parameters.put(DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);

        File f = new File(SKUNKWORK_RELATIVE_PATH + "/PS_IntervalTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }


        String xslStr = getResAsString(REPORTING_RELATIVE_PATH + "/PS_IntervalMasterTransform.xsl");
        String xmlSrc = getResAsString(REPORTING_RELATIVE_PATH + "/PS_IntervalMasterReport_Template.jrxml");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        f = new File(SKUNKWORK_RELATIVE_PATH + "/PS_IntervalRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport intervalMasterReport = JasperCompileManager.compileReport(bais);

        System.out.println("Report compiled");

        //add in the data source
        //REPORT_DATA_SOURCE
        PreparedStatementDataSource psds = new PreparedStatementDataSource(connection);
        parameters.put(REPORT_DATA_SOURCE, psds);

        JasperPrint jp = null;
        try {
            System.out.println("Filling intervalMasterReport");
            jp = JasperFillManager.fillReport(intervalMasterReport, parameters, psds);
            System.out.println("Report filled");
        } finally {
            connection.close();
        }

//        System.out.println("Viewing...");
//        try{
//            JasperViewer.viewReport(jp, false);
//        }catch(Exception ex){
//            System.out.println("Exception: " + ex.getMessage());
//            ex.printStackTrace();
//        }

        JasperExportManager.exportReportToPdfFile(jp, "PS_Interval.pdf");

        JasperExportManager.exportReportToHtmlFile(jp, "PS_Interval.html");
    }

    private void runUsageReport(Properties prop, Map parameters, Object scriplet,
                                Pair<String, List<Object>> sqlAndParamsPair,
                                LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs)
            throws Exception {
        UsageSummaryAndSubReportHelper helper = (UsageSummaryAndSubReportHelper) scriplet;
        Connection connection = getConnection(prop);
        LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sqlAndParamsPair);
        LinkedHashMap<String, String> keyToColumnName = RuntimeDocUtilities.getKeyToColumnValues(distinctMappingSets);
        helper.setKeyToColumnMap(keyToColumnName);
        LinkedHashSet<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets, false, null, null);
        LinkedHashMap<Integer, String> groupIndexToGroup = Utilities.getGroupIndexToGroupString(mappingValuesLegend.size());
        helper.setIndexToGroupMap(groupIndexToGroup);

        LinkedHashMap<String, String> displayStringToGroup = Utilities.getLegendDisplayStringToGroupMap(mappingValuesLegend);
        parameters.put(DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);

        Document transformDoc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        File f = new File(SKUNKWORK_RELATIVE_PATH + "/UsageTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

        //get xsl and xml
        String xslStr = getResAsString(REPORTING_RELATIVE_PATH + "/UsageReportTransform.xsl");
        String xmlFileName = getResAsString(REPORTING_RELATIVE_PATH + "/Usage_Summary_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 820);
        params.put("PageMinWidth", 850);
        params.put("TitleInnerFrameBuffer", 7);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperDoc = transform(xslStr, xmlFileName, params);

        f = new File(SKUNKWORK_RELATIVE_PATH + "/UsageRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport report = JasperCompileManager.compileReport(bais);

        System.out.println("Report compiled");

        //add in the data source
        PreparedStatementDataSource psds = new PreparedStatementDataSource(connection);
        parameters.put(REPORT_DATA_SOURCE, psds);

        JasperPrint jp = null;
        try {
            System.out.println("Filling report");
            jp = JasperFillManager.fillReport(report, parameters, psds);
            System.out.println("Report filled");
        } finally {
            connection.close();
        }

//        System.out.println("Viewing...");
//        try{
//            JasperViewer.viewReport(jp, false);
//        }catch(Exception ex){
//            System.out.println("Exception: " + ex.getMessage());
//            ex.printStackTrace();
//        }

        JasperExportManager.exportReportToPdfFile(jp, "UsageSummary.pdf");
//        JRHtmlExporter exporter = new JRHtmlExporter();
//        exporter.setParameter( JRHtmlExporterParameter.JASPER_PRINT, jp);
//        exporter.setParameter(JRHtmlExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
//        exporter.setParameter( JRHtmlExporterParameter.IS_OUTPUT_IMAGES_TO_DIR, Boolean.FALSE );
//        FileOutputStream fout = new FileOutputStream(new File("test.html"));
//        exporter.setParameter( JRHtmlExporterParameter.OUTPUT_STREAM, fout );
//
//        exporter.exportReport();


        JasperExportManager.exportReportToHtmlFile(jp, "UsageSummary.html");
    }

    private String getResAsString(String path) throws IOException {
        File f = new File(path);
        InputStream is = new FileInputStream(f);
        try {
            byte[] resbytes = IOUtils.slurpStream(is, 200000);
            return new String(resbytes);
        } finally {
            is.close();
        }
    }

    /**
     *
     */
    private static void usage() {
        System.out.println("ReportApp usage:");
        System.out.println("\tjava SubreportApp task file");
        System.out.println("\tTasks : fill | print | pdf | html | usage");
    }


    /**
     *
     */
    public static Connection getConnection(Properties prop) throws Exception {

        String connectString = prop.getProperty(CONNECTION_STRING);
        String driver = "com.mysql.jdbc.Driver";
        String user = prop.getProperty(DB_USER);
        String password = prop.getProperty(DB_PASSWORD);

        Class.forName(driver);
        Connection conn = DriverManager.getConnection(connectString, user, password);

        return conn;
    }

    private Document transform(String xslt, String xmlSrc, Map<String, Object> map) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transformerFactory.newTemplates(xsltsource).newTransformer();

        DocumentBuilderFactory builderF = DocumentBuilderFactory.newInstance();
        //all jasper reports must have a dtd, were not going to handle it, just ignore
        //builderF.setValidating(false);
        DocumentBuilder builder = builderF.newDocumentBuilder();

        InputSource is = new InputSource(new StringReader(xmlSrc));
        Document doc = builder.parse(is);

        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        XmlUtil.softXSLTransform(doc, result, transformer, map);
//        System.out.println(sw.toString());
        StringReader reader = new StringReader(sw.toString());
        Document returnDoc = builder.parse(new InputSource(reader));
        return returnDoc;
    }

    private Map<String, Object> getParameters() throws Exception {
        //Preparing parameters
        Map<String, Object> parameters = new HashMap<String, Object>();
        //Required
        parameters.put(REPORT_CONNECTION, getConnection(prop));
        parameters.put(TEMPLATE_FILE_ABSOLUTE, "Styles.jrtx");
        parameters.put(SUBREPORT_DIRECTORY, ".");

        //parameters.put(REPORT_TIME_ZONE, java.util.TimeZone??);
        parameters.put(REPORT_TYPE, prop.getProperty(REPORT_TYPE));
        parameters.put(REPORT_RAN_BY, prop.getProperty(REPORT_RAN_BY));

        Boolean ignorePageBreaks = Boolean.valueOf(prop.getProperty(IS_IGNORE_PAGINATION));
        parameters.put(IS_IGNORE_PAGINATION, ignorePageBreaks);

        Boolean b = Boolean.parseBoolean(prop.getProperty(IS_CONTEXT_MAPPING));
        parameters.put(IS_CONTEXT_MAPPING, b);

        Boolean printChart = Boolean.parseBoolean(prop.getProperty(PRINT_CHART));
        parameters.put(PRINT_CHART, printChart);


        Boolean isDetail = Boolean.parseBoolean(prop.getProperty(IS_DETAIL).toString());
        parameters.put(IS_DETAIL, isDetail);

        String timeZone = prop.getProperty(SPECIFIC_TIME_ZONE);
        parameters.put(SPECIFIC_TIME_ZONE, timeZone);
        //relative and absolute time
        b = Boolean.parseBoolean(prop.getProperty(IS_RELATIVE));
        parameters.put(IS_RELATIVE, b);
        Utilities.UNIT_OF_TIME unitOfTime = Utilities.getUnitFromString(prop.getProperty(RELATIVE_TIME_UNIT));
        parameters.put(RELATIVE_TIME_UNIT, unitOfTime);
        Integer numRelativeTimeUnits = Integer.parseInt(prop.getProperty(RELATIVE_NUM_OF_TIME_UNITS).toString());
        parameters.put(RELATIVE_NUM_OF_TIME_UNITS, numRelativeTimeUnits);

        Utilities.UNIT_OF_TIME intervalUnitOfTime = Utilities.getUnitFromString(prop.getProperty(INTERVAL_TIME_UNIT));
        parameters.put(INTERVAL_TIME_UNIT, intervalUnitOfTime);
        Integer i = Integer.parseInt(prop.getProperty(INTERVAL_NUM_OF_TIME_UNITS).toString());
        parameters.put(INTERVAL_NUM_OF_TIME_UNITS, i);

        b = Boolean.parseBoolean(prop.getProperty(IS_ABSOLUTE).toString());
        parameters.put(IS_ABSOLUTE, b);
        parameters.put(ABSOLUTE_START_TIME, prop.getProperty(ABSOLUTE_START_TIME));
        parameters.put(ABSOLUTE_END_TIME, prop.getProperty(ABSOLUTE_END_TIME));

        parameters.put(HOURLY_MAX_RETENTION_NUM_DAYS, new Integer(prop.getProperty(HOURLY_MAX_RETENTION_NUM_DAYS)));

        List<String> operations = loadListFromProperties(OPERATIONS, prop);

        Map<String, Set<String>> serviceIdsToOps = new HashMap<String, Set<String>>();
        Map<String, String> serviceIdsToName = new HashMap<String, String>();

        String serviceName = prop.getProperty(SERVICE_ID_TO_NAME + "_1");
        String serviceOid = prop.getProperty(SERVICE_ID_TO_NAME_OID + "_1");
        int index = 2;
        while (serviceName != null && serviceOid != null) {
            serviceIdsToOps.put(serviceOid, new HashSet<String>(operations));
            serviceIdsToName.put(serviceOid, serviceName);

            serviceName = prop.getProperty(SERVICE_ID_TO_NAME + "_" + index);
            serviceOid = prop.getProperty(SERVICE_ID_TO_NAME_OID + "_" + index);
            index++;
        }

        parameters.put(SERVICE_ID_TO_OPERATIONS_MAP, serviceIdsToOps);
        parameters.put(SERVICE_ID_TO_NAME_MAP, serviceIdsToName);


        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getFilterPairMap(prop);
        parameters.put(KEYS_TO_LIST_FILTER_PAIRS, keysToFilterPairs);

        JasperPrint jp = JasperFillManager.fillReport("StyleGenerator.jasper", parameters);
        Map sMap = jp.getStylesMap();
        if (sMap == null) throw new NullPointerException("sMap is null");

        parameters.put(STYLES_FROM_TEMPLATE, sMap);

        String reportScriplet = prop.getProperty(REPORT_SCRIPTLET);
        Class c = Class.forName(reportScriplet);
        Object scriplet = c.newInstance();
        //Only required because jasper reports for some reason ignores the value of scriptletClass from the
        //jasperreport element attribute, so specifying it as a parameter explicitly fixes this issue
        parameters.put(REPORT_SCRIPTLET, scriplet);

        return parameters;
    }


}
