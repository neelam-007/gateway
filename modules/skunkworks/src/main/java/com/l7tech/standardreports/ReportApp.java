/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 29, 2008
 * Time: 1:58:00 PM
 * ReportApp is a CLI program to test jasper reports. Depends on report.properties being in the same directory
 */
package com.l7tech.standardreports;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.IOUtils;
import com.l7tech.gateway.standardreports.UsageSummaryAndSubReportHelper;
import com.l7tech.gateway.standardreports.UsageReportHelper;
import com.l7tech.gateway.standardreports.Utilities;
import com.l7tech.gateway.standardreports.RuntimeDocUtilities;
import com.l7tech.server.management.api.node.ReportApi;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;


public class ReportApp
{
	private static final String TASK_RUN = "run";

    //The following params must be supplied when filling the report
    private static final String REPORT_CONNECTION= "REPORT_CONNECTION";
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

    //db props
    private static final String CONNECTION_STRING = "CONNECTION_STRING";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASSWORD = "DB_PASSWORD";

    //Non report params, just used in ReportApp
    private static final String HOURLY_MAX_RETENTION_NUM_DAYS = "HOURLY_MAX_RETENTION_NUM_DAYS";
    private static final Properties prop = new Properties();
    private static final String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
    private static final String REPORT_SCRIPTLET = "REPORT_SCRIPTLET";
    private static final String SUB_INTERVAL_SUB_REPORT = "SUB_INTERVAL_SUB_REPORT";
    private static final String SUB_REPORT = "SUB_REPORT";
    private static final String PRINT_CHART = "PRINT_CHART";
    private static final String DISPLAY_STRING_TO_MAPPING_GROUP = "DISPLAY_STRING_TO_MAPPING_GROUP";
    private static final String SUB_REPORT_HELPER = "SUB_REPORT_HELPER";
    private static final String SERVICE_ID_TO_OPERATIONS_MAP = "SERVICE_ID_TO_OPERATIONS_MAP";

    private static final String SKUNKWORK_RELATIVE_PATH = "reportOutput";
    private static final String REPORTING_RELATIVE_PATH = "../../../../../../../gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports";
    private static final String SERVICE_ID_TO_NAME_MAP = "SERVICE_ID_TO_NAME_MAP";

    private static final String SPECIFIC_TIME_ZONE = "SPECIFIC_TIME_ZONE";
    private static final String IS_USING_KEYS = "IS_USING_KEYS";

    public ReportApp() {
    }

    /**
	 *
	 */
    public static void main(String[] args) throws Exception{
        if(args.length == 0)
        {
            usage();
            return;
        }
        String taskName = args[0];
        ReportApp reportApp = new ReportApp();
        reportApp.run(taskName);

    }

    public void run(String taskName) throws Exception
	{

        File f = new File(SKUNKWORK_RELATIVE_PATH);
        if(!f.exists()){
            if(!f.mkdir()){
                throw new RuntimeException("Cannot create folder: " + SKUNKWORK_RELATIVE_PATH);
            }else{
                System.out.println("Creating outupt directory where transformed jrxml and xml documents will be outputted to");                
            }
        }

        FileInputStream fileInputStream = new FileInputStream("report.properties");
        prop.load(fileInputStream);
        try
		{
			long start = System.currentTimeMillis();
            if (TASK_RUN.equals(taskName))
			{
                fill(start);
			}
            else
			{
				usage();
			}
		}
		catch (JRException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

    private void fill(long start) throws Exception{
        Map<String, Object> parameters = getParameters();
        String type = parameters.get(REPORT_TYPE).toString();

        int numRelativeTimeUnits = Integer.valueOf(parameters.get(RELATIVE_NUM_OF_TIME_UNITS).toString());
        Utilities.UNIT_OF_TIME relUnitOfTime = Utilities.getUnitFromString(prop.getProperty(RELATIVE_TIME_UNIT));
        String timeZone = "Canada/Pacific";
        long startTimeInPast = Utilities.getRelativeMilliSecondsInPast(numRelativeTimeUnits, relUnitOfTime, timeZone);
        long endTimeInPast = Utilities.getMillisForEndTimePeriod(relUnitOfTime, timeZone);

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getFilterPairMap(prop);
        parameters.put(KEYS_TO_LIST_FILTER_PAIRS, keysToFilterPairs);
        Map<String, Set<String>> serivceIdsToOp = (Map<String, Set<String>>) parameters.get(SERVICE_ID_TO_OPERATIONS_MAP);


        boolean isDetail = Boolean.valueOf(parameters.get(IS_DETAIL).toString());
        Object scriplet = parameters.get(REPORT_SCRIPTLET);

        int resolution = Utilities.getSummaryResolutionFromTimePeriod(30, startTimeInPast, endTimeInPast);

        boolean isContextMapping = Boolean.valueOf(parameters.get(IS_CONTEXT_MAPPING).toString());

        boolean isUsingKeys = !keysToFilterPairs.isEmpty();

        String sql;
        Boolean isUsage = Boolean.valueOf(prop.getProperty("IS_USAGE"));
        if(isContextMapping && isUsingKeys){
            sql = Utilities.getUsageDistinctMappingQuery(startTimeInPast, endTimeInPast, serivceIdsToOp, keysToFilterPairs, resolution, isDetail, isUsage);
        }else if(isContextMapping){
            sql = Utilities.getPerformanceStatisticsMappingQuery(true, startTimeInPast, endTimeInPast, serivceIdsToOp, keysToFilterPairs, resolution, isDetail, isUsage);
        }else{
            sql = Utilities.getNoMappingQuery(true, startTimeInPast, endTimeInPast, serivceIdsToOp.keySet(), resolution);
        }
        //System.out.println("Distinct sql: " + sql);
        
        if(!type.equals("Usage") && !type.equals("Usage_Interval")){
            if(type.equals("Performance_Interval")){
                runPerfStatIntervalReport(isContextMapping, isUsingKeys, prop, parameters, sql, keysToFilterPairs);
            }else{
                runPerfStatSummaryReport(isContextMapping, isUsingKeys, prop, parameters, sql, keysToFilterPairs);
            }
        }else{
            if(type.equals("Usage")){
                runUsageReport(prop, parameters, scriplet, sql, keysToFilterPairs);
            }else if(type.equals("Usage_Interval")){
                runUsageIntervalReport(prop, parameters, scriplet, sql, keysToFilterPairs);
            }
        }

        System.err.println("Filling time : " + (System.currentTimeMillis() - start));
    }

    public static LinkedHashMap<String, String> loadMapFromProperties(String key1, String key2, Properties prop){

        LinkedHashMap<String, String> returnMap = new LinkedHashMap<String,String>();
        String key1Name = prop.getProperty(key1+"_1");
        String key2Name = prop.getProperty(key2+"_1");
        int index = 2;

        while(key1Name != null && key2Name != null){
            returnMap.put(key1Name, key2Name);
            key1Name = prop.getProperty(key1+"_"+index);
            key2Name = prop.getProperty(key2+"_"+index);
            index++;
        }

        return returnMap;
    }


    public static LinkedHashMap<String, List<ReportApi.FilterPair>> getFilterPairMap(Properties prop){
        LinkedHashMap<String, List<ReportApi.FilterPair>> returnMap = new LinkedHashMap<String, List<ReportApi.FilterPair>>();

        int index = 1;
        String keyName = prop.getProperty(MAPPING_KEY_+index);
        while(keyName != null){
            int valueIndex = 1;
            ReportApi.FilterPair defaultFilter = new ReportApi.FilterPair();
            List<ReportApi.FilterPair> filterList = new ArrayList<ReportApi.FilterPair>();

            //any values for this key?

            String keyValue = prop.getProperty(MAPPING_KEY_+index+VALUE_+valueIndex);
            while(keyValue != null){
                //todo [Donal] replace wildcard with %
                ReportApi.FilterPair filter = new ReportApi.FilterPair(keyValue);
                filterList.add(filter);
                valueIndex++;
                keyValue = prop.getProperty(MAPPING_KEY_+index+VALUE_+valueIndex);
            }
            
            if(filterList.isEmpty()) filterList.add(defaultFilter);
            returnMap.put(keyName, filterList);
            index++;
            keyName = prop.getProperty(MAPPING_KEY_+index);            
        }

        return returnMap;
    }

    public static List<String> loadListFromProperties(String key, Properties prop){

        List<String> returnList = new ArrayList<String>();
        String key1Name = prop.getProperty(key+"_1");
        int index = 2;

        while(key1Name != null){
            returnList.add(key1Name);
            key1Name = prop.getProperty(key+"_"+index);
            index++;
        }

        return returnList;
    }

    /**
     * Get the ordered set of distinct mapping sets for the keys and values in the sql string from the db
     * @param connection
     * @param sql
     * @return
     * @throws Exception
     */
    public static LinkedHashSet<List<String>> getDistinctMappingSets(Connection connection, String sql) throws Exception{
        LinkedHashSet<List<String>> returnSet = new LinkedHashSet<List<String>>();

        try{
            Statement stmt = connection.createStatement();
            LinkedHashSet<String> set = new LinkedHashSet<String>();
            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next()){
                List<String> mappingStrings = new ArrayList<String>();
                String authUser = rs.getString(Utilities.AUTHENTICATED_USER);
                mappingStrings.add(authUser);
                for(int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++){
                    mappingStrings.add(rs.getString("MAPPING_VALUE_"+(i+1)));
                }
                returnSet.add(mappingStrings);
            }
        }catch(Exception ex){
            if(connection != null) connection.close();
            throw(ex);
        }
        return returnSet;
    }

//    private LinkedHashSet<String> getMappingDisplayStrings(Connection connection, String sql, List<String> keys) throws Exception{
//        try{
//            Statement stmt = connection.createStatement();
//            LinkedHashSet<String> set = new LinkedHashSet<String>();
//            ResultSet rs = stmt.executeQuery(sql);
//
//            while(rs.next()){
//                List<String> mappingStrings = new ArrayList<String>();
//                String authUser = rs.getString(Utilities.AUTHENTICATED_USER);
//                for(int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++){
//                    mappingStrings.add(rs.getString("MAPPING_VALUE_"+(i+1)));
//                }
//                String mappingValue = Utilities.getMappingValueDisplayString(keys, authUser, mappingStrings.toArray(new String[]{}), false, null);
//                set.add(mappingValue);
//            }
//            return set;
//
//        }catch(Exception ex){
//            if(connection != null) connection.close();
//            throw(ex);
//        }
//    }

    private LinkedHashSet<String> getServiceDisplayStrings(Connection connection, String sql) throws Exception{
        try{
            Statement stmt = connection.createStatement();
            LinkedHashSet<String> set = new LinkedHashSet<String>();
            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next()){
                String serviceName = rs.getString(Utilities.SERVICE_NAME);
                String routingUri = rs.getString(Utilities.ROUTING_URI);
                String service = Utilities.getServiceDisplayStringNotTruncated(serviceName, routingUri);
                set.add(service);
            }
            return set;

        }catch(Exception ex){
            if(connection != null) connection.close();
            throw(ex);
        }
    }

    private void runUsageIntervalReport(Properties prop, Map parameters, Object scriplet, String sql,
                                       LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs)
                                                                    throws Exception{
        UsageReportHelper helper = (UsageReportHelper) scriplet;
        Connection connection = getConnection(prop);
        LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sql);
        LinkedHashMap<String, String> keyToColumnName = RuntimeDocUtilities.getKeyToColumnValues(distinctMappingSets);
        helper.setKeyToColumnMap(keyToColumnName);
        UsageSummaryAndSubReportHelper summaryAndSubReportHelper = new UsageSummaryAndSubReportHelper();
        summaryAndSubReportHelper.setKeyToColumnMap(keyToColumnName);
        parameters.put(SUB_REPORT_HELPER, summaryAndSubReportHelper);

        LinkedHashSet<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets);
        LinkedHashMap<Integer, String> groupIndexToGroup = Utilities.getGroupIndexToGroupString(mappingValuesLegend);
        helper.setIndexToGroupMap(groupIndexToGroup);
        
        //Master report first
        Document transformDoc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);

        File f = new File(SKUNKWORK_RELATIVE_PATH +"/UsageMasterTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        String xslStr = getResAsString(REPORTING_RELATIVE_PATH +"/UsageReportIntervalTransform_Master.xsl");
        String xmlFileName = getResAsString(REPORTING_RELATIVE_PATH +"/Usage_IntervalMasterReport_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 820);
        params.put("PageMinWidth", 850);
        params.put("ReportInfoStaticTextSize", 128);
        params.put("TitleInnerFrameBuffer", 7);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperMasterDoc = transform(xslStr, xmlFileName, params);

        f = new File(SKUNKWORK_RELATIVE_PATH +"/UsageMasterJasperRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperMasterDoc, fos);
        }finally{
            fos.close();
        }

        //MasterSubInterval report
        transformDoc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        f = new File(SKUNKWORK_RELATIVE_PATH +"/UsageSubIntervalTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        xslStr = getResAsString(REPORTING_RELATIVE_PATH +"/UsageReportSubIntervalTransform_Master.xsl");
        xmlFileName = getResAsString(REPORTING_RELATIVE_PATH +"/Usage_SubIntervalMasterReport_Template.jrxml");
        params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 820);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperSubIntervalDoc = transform(xslStr, xmlFileName, params);

        f = new File(SKUNKWORK_RELATIVE_PATH +"/SubIntervalJasperRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperSubIntervalDoc, fos);
        }finally{
            fos.close();
        }

        //subreport report
        transformDoc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        f = new File(SKUNKWORK_RELATIVE_PATH +"/UsageSubReportTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        xslStr = getResAsString(REPORTING_RELATIVE_PATH+"/Usage_SubReport.xsl");
        xmlFileName = getResAsString(REPORTING_RELATIVE_PATH+"/Usage_SubIntervalMasterReport_subreport0_Template.jrxml");
        params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 820);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperSubReportDoc = transform(xslStr, xmlFileName, params);

        f = new File(SKUNKWORK_RELATIVE_PATH+"UsageSubReportJasperRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperSubReportDoc, fos);
        }finally{
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

        JasperPrint jp = null;
        try{
            System.out.println("Filling report");
            jp = JasperFillManager.fillReport(masterReport, parameters, connection);
            System.out.println("Report filled");
        }finally{
            connection.close();
        }

        System.out.println("Viewing...");
        try{
            JasperViewer.viewReport(jp, false);
        }catch(Exception ex){
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        JasperExportManager.exportReportToPdfFile(jp,"UsageInterval.pdf");

    }

    private void runPerfStatSummaryReport(boolean isContextMapping, boolean isUsingKeys, Properties prop, Map parameters,
                                           String sql, LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs) throws Exception{
        Connection connection = getConnection(prop);

        LinkedHashMap<String, String> groupToDisplayString = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> displayStringToGroup = new LinkedHashMap<String, String>();

        if(isContextMapping && isUsingKeys){
            LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sql);
            LinkedHashSet<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets);
            //We need to look up the mappingValues from both the group value and also the display string value

            int index = 1;
            for(String s: mappingValuesLegend){
                String group = "Group "+index;
                //System.out.println("Group: " + group+" s: " + s);
                groupToDisplayString.put(group, s);
                displayStringToGroup.put(s, group);
                index++;
            }

        }else{
            LinkedHashSet<String> serviceValues = getServiceDisplayStrings(connection, sql);
            //We need to look up the mappingValues from both the group value and also the display string value
            int index = 1;
            for(String s: serviceValues){
                String service = "Service "+index;
                //System.out.println("Service: " + service+" s: " + s);
                groupToDisplayString.put(service, s);
                displayStringToGroup.put(s, service);
                index++;
            }
        }

        //Set the parameter IS_USING_KEYS to let chart know if operation is being used alone, to make some display changes

        parameters.put(IS_USING_KEYS, isUsingKeys );

        parameters.put(DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);


        Document transformDoc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(isContextMapping, isUsingKeys, groupToDisplayString);

        String xslStr = getResAsString(REPORTING_RELATIVE_PATH+"/PS_SummaryTransform.xsl");
        String xmlSrc = getResAsString(REPORTING_RELATIVE_PATH+"/PS_Summary_Template.jrxml");
        //String xmlSrc = getResAsString(SKUNKWORK_RELATIVE_PATH+"/PS_Summary_Template_New_Title.jrxml");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        File f = new File(SKUNKWORK_RELATIVE_PATH+"/PS_SummaryRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport summaryReport = JasperCompileManager.compileReport(bais);
        System.out.println("Report compiled");

        JasperPrint jp = null;
        try{
            System.out.println("Filling summaryReport");
            jp = JasperFillManager.fillReport(summaryReport, parameters, connection);
            System.out.println("Report filled");
        }finally{
            connection.close();
        }

        System.out.println("Viewing...");
        try{
            JasperViewer.viewReport(jp, false);
        }catch(Exception ex){
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        JasperExportManager.exportReportToPdfFile(jp,"PS_Summary.pdf");

    }


    private void runPerfStatIntervalReport(boolean isContextMapping, boolean isUsingKeys, Properties prop, Map parameters,
                                           String sql, LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs) throws Exception{

        //Compile both subreports and add to parameters
        String subIntervalReport = getResAsString(REPORTING_RELATIVE_PATH+"/PS_SubIntervalMasterReport.jrxml");
        ByteArrayInputStream bais = new ByteArrayInputStream(subIntervalReport.getBytes("UTF-8"));
        JasperReport subIntervalCompiledReport = JasperCompileManager.compileReport(bais);

        String subReport = getResAsString(REPORTING_RELATIVE_PATH+"/PS_SubIntervalMasterReport_subreport0.jrxml");
        bais = new ByteArrayInputStream(subReport.getBytes("UTF-8"));
        JasperReport subCompiledReport = JasperCompileManager.compileReport(bais);

        parameters.put(SUB_INTERVAL_SUB_REPORT, subIntervalCompiledReport);
        parameters.put(SUB_REPORT, subCompiledReport);

        Connection connection = getConnection(prop);
        Document transformDoc = null;
        LinkedHashMap<String, String> groupToDisplayString = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> displayStringToGroup = new LinkedHashMap<String, String>();
        
        if(isContextMapping && isUsingKeys){
            LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sql);
            LinkedHashSet<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets);
            //We need to look up the mappingValues from both the group value and also the display string value

            int index = 1;
            for(String s: mappingValuesLegend){
                String group = "Group "+index;
                //System.out.println("Group: " + group+" s: " + s);
                groupToDisplayString.put(group, s);
                displayStringToGroup.put(s, group);
                index++;
            }

        }else{
            LinkedHashSet<String> serviceValues = getServiceDisplayStrings(connection, sql);
            //We need to look up the mappingValues from both the group value and also the display string value
            int index = 1;
            for(String s: serviceValues){
                String service = "Service "+index;
                //System.out.println("Service: " + service+" s: " + s);
                groupToDisplayString.put(service, s);
                displayStringToGroup.put(s, service);
                index++;
            }
        }

        parameters.put(IS_USING_KEYS, isUsingKeys );
        
        parameters.put(DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);
        transformDoc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(isContextMapping, isUsingKeys, groupToDisplayString);

        File f = new File(SKUNKWORK_RELATIVE_PATH+"/PS_IntervalTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }


        String xslStr = getResAsString(REPORTING_RELATIVE_PATH+"/PS_IntervalMasterTransform.xsl");
        String xmlSrc = getResAsString(REPORTING_RELATIVE_PATH+"/PS_IntervalMasterReport_Template.jrxml");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        f = new File(SKUNKWORK_RELATIVE_PATH+"/PS_IntervalRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport intervalMasterReport = JasperCompileManager.compileReport(bais);

        System.out.println("Report compiled");

        JasperPrint jp = null;
        try{
            System.out.println("Filling intervalMasterReport");
            jp = JasperFillManager.fillReport(intervalMasterReport, parameters, connection);
            System.out.println("Report filled");
        }finally{
            connection.close();
        }

        System.out.println("Viewing...");
        try{
            JasperViewer.viewReport(jp, false);
        }catch(Exception ex){
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        JasperExportManager.exportReportToPdfFile(jp,"PS_Interval.pdf");
    }

    private void runUsageReport(Properties prop, Map parameters, Object scriplet, String sql,
                                       LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs)
                                                                    throws Exception{
        UsageSummaryAndSubReportHelper helper = (UsageSummaryAndSubReportHelper) scriplet;
        Connection connection = getConnection(prop);
        LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sql);
        LinkedHashMap<String, String> keyToColumnName = RuntimeDocUtilities.getKeyToColumnValues(distinctMappingSets);
        helper.setKeyToColumnMap(keyToColumnName);
        LinkedHashSet<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets);
        LinkedHashMap<String, String> displayStringToGroup = Utilities.getLegendDisplayStringToGroupMap(mappingValuesLegend);
        parameters.put(DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);

        Document transformDoc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        File f = new File(SKUNKWORK_RELATIVE_PATH+"/UsageTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        //get xsl and xml
        String xslStr = getResAsString(REPORTING_RELATIVE_PATH+"/UsageReportTransform.xsl");
        String xmlFileName = getResAsString(REPORTING_RELATIVE_PATH+"/Usage_Summary_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 820);
        params.put("PageMinWidth", 850);
        params.put("ReportInfoStaticTextSize", 128);
        params.put("TitleInnerFrameBuffer", 7);


        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperDoc = transform(xslStr, xmlFileName, params);

        f = new File(SKUNKWORK_RELATIVE_PATH+"/UsageRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport report = JasperCompileManager.compileReport(bais);

        System.out.println("Report compiled");
        
        JasperPrint jp = null;
        try{
            System.out.println("Filling report");
            jp = JasperFillManager.fillReport(report, parameters, connection);
            System.out.println("Report filled");
        }finally{
            connection.close();
        }

        System.out.println("Viewing...");
        try{
            JasperViewer.viewReport(jp, false);
        }catch(Exception ex){
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        JasperExportManager.exportReportToPdfFile(jp,"UsageSummary.pdf");


    }

    private String getResAsString(String path) throws IOException {
        File f = new File(path);
        InputStream is = new FileInputStream(f);
        try{
            byte[] resbytes = IOUtils.slurpStream(is, 200000);
            return new String(resbytes);
        }finally{
            is.close();
        }
    }

    /**
	 *
	 */
	private static void usage()
	{
		System.out.println( "ReportApp usage:" );
		System.out.println( "\tjava SubreportApp task file" );
		System.out.println( "\tTasks : fill | print | pdf | html | usage" );
	}


	/**
	 *
	 */
    public static Connection getConnection(Properties prop) throws Exception{

        String connectString = prop.getProperty(CONNECTION_STRING);
        String driver = "com.mysql.jdbc.Driver";
        String user = prop.getProperty(DB_USER);
        String password = prop.getProperty(DB_PASSWORD);

        Class.forName(driver);
        Connection conn = DriverManager.getConnection(connectString, user, password);

        return conn;
    }

    private Document transform(String xslt, String xmlSrc, Map<String, Object> map ) throws Exception {
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

    private Map<String, Object> getParameters() throws Exception{
        //Preparing parameters
        Map<String, Object> parameters = new HashMap<String, Object>();
        //Required
        parameters.put(REPORT_CONNECTION, getConnection(prop));
        parameters.put(TEMPLATE_FILE_ABSOLUTE, "Styles.jrtx");
        parameters.put(SUBREPORT_DIRECTORY, ".");

        //parameters.put(REPORT_TIME_ZONE, java.util.TimeZone??);
        parameters.put(REPORT_TYPE, prop.getProperty(REPORT_TYPE));
        parameters.put(REPORT_RAN_BY, prop.getProperty(REPORT_RAN_BY));

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

        String serviceName = prop.getProperty(SERVICE_ID_TO_NAME+"_1");
        String serviceOid = prop.getProperty(SERVICE_ID_TO_NAME_OID+"_1");
        int index = 2;
        while(serviceName != null && serviceOid != null){
            serviceIdsToOps.put(serviceOid, new HashSet<String>(operations));
            serviceIdsToName.put(serviceOid, serviceName);

            serviceName = prop.getProperty(SERVICE_ID_TO_NAME+"_"+index);
            serviceOid = prop.getProperty(SERVICE_ID_TO_NAME_OID+"_"+index);
            index++;
        }

        parameters.put(SERVICE_ID_TO_OPERATIONS_MAP, serviceIdsToOps);
        parameters.put(SERVICE_ID_TO_NAME_MAP, serviceIdsToName);


        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getFilterPairMap(prop);
        parameters.put(KEYS_TO_LIST_FILTER_PAIRS, keysToFilterPairs);

        JasperPrint jp = JasperFillManager.fillReport("StyleGenerator.jasper", parameters);
        Map sMap = jp.getStylesMap();
        if(sMap == null) throw new NullPointerException("sMap is null");

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
