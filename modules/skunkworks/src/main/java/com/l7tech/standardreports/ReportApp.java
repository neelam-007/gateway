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
import com.l7tech.server.ems.standardreports.UsageReportHelper;
import com.l7tech.server.ems.standardreports.Utilities;
import com.l7tech.server.ems.standardreports.SubIntervalScriptletHelper;
import com.l7tech.server.ems.standardreports.ScriptletHelper;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.IOUtils;
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
	private static final String TASK_FILL = "fill";
	private static final String TASK_PDF = "pdf";
	private static final String TASK_HTML = "html";
	private static final String TASK_VIEW = "view";
    private static final String TASK_CANNED_USAGE = "usage";
    private static final String TASK_CANNED_INTERVAL_USAGE = "u_interval";

    //The following params must be supplied when filling the report
    private static final String REPORT_CONNECTION= "REPORT_CONNECTION";
    private static final String REPORT_TIME_ZONE = "REPORT_TIME_ZONE";
    private static final String REPORT_TYPE = "REPORT_TYPE";
    private static final String INTERVAL_TIME_UNIT = "INTERVAL_TIME_UNIT";
    private static final String INTERVAL_NUM_OF_TIME_UNITS = "INTERVAL_NUM_OF_TIME_UNITS";
    private static final String REPORT_RAN_BY = "REPORT_RAN_BY";

    private static final String TEMPLATE_FILE_ABSOLUTE = "TEMPLATE_FILE_ABSOLUTE";
    private static final String SUBREPORT_DIRECTORY = "SUBREPORT_DIRECTORY";

    private static final String IS_CONTEXT_MAPPING = "IS_CONTEXT_MAPPING";
    public static final String IS_DETAIL = "IS_DETAIL";
    //Optional
    private static final String SERVICE_NAME_TO_ID_MAP = "SERVICE_NAME_TO_ID_MAP";
    public static final String SERVICE_ID_TO_NAME = "SERVICE_ID_TO_NAME";
    public static final String SERVICE_ID_TO_NAME_OID = "SERVICE_ID_TO_NAME_OID";

    //one of each set must be supplied - relative or absolute time
    private static final String IS_RELATIVE = "IS_RELATIVE";
    private static final String RELATIVE_NUM_OF_TIME_UNITS = "RELATIVE_NUM_OF_TIME_UNITS";
    private static final String RELATIVE_TIME_UNIT = "RELATIVE_TIME_UNIT";

    private static final String IS_ABSOLUTE = "IS_ABSOLUTE";
    private static final String ABSOLUTE_START_TIME = "ABSOLUTE_START_TIME";
    private static final String ABSOLUTE_END_TIME = "ABSOLUTE_END_TIME";

    public static final String MAPPING_KEYS = "MAPPING_KEYS";
    public static final String MAPPING_VALUES = "MAPPING_VALUES";
    public static final String VALUE_EQUAL_OR_LIKE = "VALUE_EQUAL_OR_LIKE";
    public static final String MAPPING_KEY = "MAPPING_KEY";
    public static final String MAPPING_VALUE = "MAPPING_VALUE";
    public static final String OPERATIONS = "OPERATIONS";

    private static final String USE_USER = "USE_USER";
    public static final String AUTHENTICATED_USERS = "AUTHENTICATED_USERS";

    //db props
    private static final String CONNECTION_STRING = "CONNECTION_STRING";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASSWORD = "DB_PASSWORD";

    //Non report params, just used in ReportApp
    private static final String IS_SUMMARY = "IS_SUMMARY";
    private static final String HOURLY_MAX_RETENTION_NUM_DAYS = "HOURLY_MAX_RETENTION_NUM_DAYS";
    private static final String REPORT_FILE_NAME_NO_ENDING = "REPORT_FILE_NAME_NO_ENDING";
    private static final Properties prop = new Properties();
    private static final String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
    private static final String REPORT_SCRIPTLET = "REPORT_SCRIPTLET";
    private static final String SUB_INTERVAL_SUB_REPORT = "SUB_INTERVAL_SUB_REPORT";
    private static final String SUB_REPORT = "SUB_REPORT";
    private static final String SUBINTERVAL_REPORT_SCRIPTLET = "SUBINTERVAL_REPORT_SCRIPTLET";
    private static final String PRINT_CHART = "PRINT_CHART";

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
        FileInputStream fileInputStream = new FileInputStream("report.properties");
        prop.load(fileInputStream);
        String fileName = prop.getProperty(REPORT_FILE_NAME_NO_ENDING);

        try
		{
			long start = System.currentTimeMillis();
            if (TASK_FILL.equals(taskName))
			{
                fill(fileName, start);
			}
			else if (TASK_PDF.equals(taskName))
			{
                JasperExportManager.exportReportToPdfFile(fileName+".jrprint");
				System.err.println("PDF creation time : " + (System.currentTimeMillis() - start));
			}
			else if (TASK_HTML.equals(taskName))
			{
                JasperExportManager.exportReportToHtmlFile(fileName+".jrprint");
				System.err.println("HTML creation time : " + (System.currentTimeMillis() - start));
			}
            else if (TASK_VIEW.equals(taskName))
            {
                JasperViewer.viewReport(fileName+".jrprint", false);
                System.err.println("View time : " + (System.currentTimeMillis() - start));
            }
            else if (TASK_CANNED_USAGE.equals(taskName)){
                //to run this canned report, only ip address and customers should be supplied and values should be empty
                LinkedHashMap<String, String> keyToColumnName = new LinkedHashMap<String, String>();
                keyToColumnName.put("127.0.0.1<br>Bronze<br>", "COLUMN_1");
                keyToColumnName.put("127.0.0.1<br>Gold<br>", "COLUMN_2");
                keyToColumnName.put("127.0.0.1<br>Silver<br>", "COLUMN_3");
                keyToColumnName.put("127.0.0.2<br>Bronze<br>", "COLUMN_4");
                keyToColumnName.put("127.0.0.2<br>Gold<br>", "COLUMN_5");
                keyToColumnName.put("127.0.0.2<br>Silver<br>", "COLUMN_6");

                Map<String, Object> parameters = getParameters();
                Object scriplet = parameters.get(REPORT_SCRIPTLET);
                UsageReportHelper helper = (UsageReportHelper) scriplet;
                helper.setKeyToColumnMap(keyToColumnName);

                Connection connection = getConnection(prop);
                JasperPrint jrPrint = null;
                try{
                    jrPrint = JasperFillManager.fillReport("Canned_Usage_Report.jasper", parameters, connection);
                }finally{
                    connection.close();
                }

                System.out.println("Viewing...");
                JasperViewer.viewReport(jrPrint, false);
            }
            else if(TASK_CANNED_INTERVAL_USAGE.equals(taskName)){
                LinkedHashMap<String, String> keyToColumnName = new LinkedHashMap<String, String>();
                keyToColumnName.put("127.0.0.1<br>Bronze<br>", "COLUMN_1");
                keyToColumnName.put("127.0.0.1<br>Gold<br>", "COLUMN_2");
                keyToColumnName.put("127.0.0.1<br>Silver<br>", "COLUMN_3");
                keyToColumnName.put("127.0.0.2<br>Bronze<br>", "COLUMN_4");
                keyToColumnName.put("127.0.0.2<br>Gold<br>", "COLUMN_5");
                keyToColumnName.put("127.0.0.2<br>Silver<br>", "COLUMN_6");

                Map<String, Object> parameters = getParameters();
                Object scriplet = parameters.get(REPORT_SCRIPTLET);
                UsageReportHelper helper = (UsageReportHelper) scriplet;
                helper.setKeyToColumnMap(keyToColumnName);

                try {
                    File reportfile = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/Usage_SubIntervalMasterReport.jasper");                    
                    ObjectInputStream in = new ObjectInputStream(new FileInputStream( reportfile ));
                    Object object = in.readObject();
                    JasperReport subIntervalReport = (JasperReport) object;
                    parameters.put(SUB_INTERVAL_SUB_REPORT, subIntervalReport);

                    reportfile = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/Usage_SubIntervalMasterReport_subreport0.jasper");
                    in = new ObjectInputStream(new FileInputStream( reportfile ));
                    object = in.readObject();
                    JasperReport subReport = (JasperReport) object;
                    parameters.put(SUB_REPORT, subReport);
                    System.out.println("All subreports loaded");
                }catch(Exception ex){
                    System.out.println("Could not load all subreports: " + ex.getMessage());
                    ex.printStackTrace();
                }

                Connection connection = getConnection(prop);
                JasperPrint jrPrint = null;
                try{
                    jrPrint = JasperFillManager.fillReport("Usage_IntervalMasterReport.jasper", parameters, connection);
                }finally{
                    connection.close();
                }

                System.out.println("Viewing...");
                JasperViewer.viewReport(jrPrint, false);
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

    private void fill(String fileName, long start) throws Exception{
        Map<String, Object> parameters = getParameters();
        String type = parameters.get(REPORT_TYPE).toString();

        if(!type.equals("Usage") && !type.equals("Usage_Interval")){
            //JasperFillManager.fillReportToFile(fileName+".jasper", parameters, getConnection(prop));
            Connection connection = getConnection(prop);
            try{
                JasperFillManager.fillReportToFile(fileName+".jasper", parameters, connection);
            }finally{
                connection.close();
            }
        }else{
            int numRelativeTimeUnits = Integer.valueOf(parameters.get(RELATIVE_NUM_OF_TIME_UNITS).toString());
            Utilities.UNIT_OF_TIME relUnitOfTime = Utilities.getUnitFromString(prop.getProperty(RELATIVE_TIME_UNIT));
            long startTimeInPast = Utilities.getRelativeMilliSecondsInPast(numRelativeTimeUnits, relUnitOfTime );
            long endTimeInPast = Utilities.getMillisForEndTimePeriod(relUnitOfTime);

            List<String> keys = (List<String>) parameters.get(MAPPING_KEYS);
            List<String> values = (List<String>) parameters.get(MAPPING_VALUES);
            List<String> useAnd = (List<String>) parameters.get(VALUE_EQUAL_OR_LIKE);
            Collection<String> serviceIds = (Collection<String>) ((Map<String, String>) parameters.get(SERVICE_NAME_TO_ID_MAP)).values();
            List<String> operations = (List<String>) parameters.get(OPERATIONS);

            boolean useUser = Boolean.valueOf(parameters.get(USE_USER).toString());
            List<String> authUsers = (List<String>) parameters.get(AUTHENTICATED_USERS);


            boolean isDetail = Boolean.valueOf(parameters.get(IS_DETAIL).toString());
            Object scriplet = parameters.get(REPORT_SCRIPTLET);

            int resolution = Utilities.getResolutionFromTimePeriod(30, startTimeInPast, endTimeInPast);
            String sql = Utilities.getUsageDistinctMappingQuery(startTimeInPast, endTimeInPast, serviceIds, keys, values, useAnd, resolution, isDetail, operations, useUser, authUsers);
            System.out.println("Distinct sql: " + sql);

            if(type.equals("Usage")){
                runUsageReport(fileName, prop, parameters, scriplet, sql, keys, useUser);
            }else if(type.equals("Usage_Interval")){
                runUsageIntervalReport(fileName, prop, parameters, scriplet, sql, keys, useUser);
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

    private LinkedHashSet<String> getMappingValues(Connection connection, String sql) throws Exception{
        LinkedHashSet<String> mappingValues;
        try{
            Statement stmt = connection.createStatement();
            mappingValues = getMappingValueSet(stmt, sql);
        }catch(Exception ex){
            if(connection != null) connection.close();
            throw(ex);
        }

        return mappingValues;
    }

    private LinkedHashMap<String, String> getKeyToColumnValues(LinkedHashSet<String> mappingValues) {
        LinkedHashMap<String, String> keyToColumnName = new LinkedHashMap<String, String>();
        int count = 1;
        System.out.println("Key to column map");
        for (String s : mappingValues) {
            keyToColumnName.put(s, "COLUMN_"+count);
            System.out.println(s+" " + "COLUMN_"+count);
            count++;
        }
        return keyToColumnName;
    }

    private void runUsageIntervalReport(String fileName, Properties prop, Map parameters, Object scriplet, String sql,
                                       List<String> keys, boolean useUser)
                                                                    throws Exception{
        UsageReportHelper helper = (UsageReportHelper) scriplet;
        Connection connection = getConnection(prop);
        LinkedHashSet<String> mappingValues = getMappingValues(connection, sql);
        LinkedHashMap<String, String> keyToColumnName = getKeyToColumnValues(mappingValues);
        helper.setKeyToColumnMap(keyToColumnName);

        //Master report first
        Document transformDoc = Utilities.getUsageIntervalMasterRuntimeDoc(useUser, keys, mappingValues);
        File f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageMasterTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        String xslStr = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportIntervalTransform_Master.xsl");
        String xmlFileName = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/Usage_IntervalMasterReport_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 535);
        params.put("PageMinWidth", 595);
        params.put("ReportInfoStaticTextSize", 128);
        params.put("TitleInnerFrameBuffer", 7);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperMasterDoc = transform(xslStr, xmlFileName, params);

        f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/MasterJasperRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperMasterDoc, fos);
        }finally{
            fos.close();
        }

        //MasterSubInterval report
        transformDoc = Utilities.getUsageSubIntervalMasterRuntimeDoc(useUser, keys, mappingValues);
        f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageSubIntervalTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        xslStr = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportSubIntervalTransform_Master.xsl");
        xmlFileName = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/Usage_SubIntervalMasterReport_Template.jrxml");
        params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 535);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperSubIntervalDoc = transform(xslStr, xmlFileName, params);

        f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/SubIntervalJasperRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperSubIntervalDoc, fos);
        }finally{
            fos.close();
        }

        //subreport report
        transformDoc = Utilities.getUsageSubReportRuntimeDoc(useUser, keys, mappingValues);
        f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageSubReportTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        xslStr = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/Usage_SubReport.xsl");
        xmlFileName = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/Usage_SubIntervalMasterReport_subreport0_Template.jrxml");
        params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 535);

        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperSubReportDoc = transform(xslStr, xmlFileName, params);

        f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageSubReportJasperRuntimeDoc.xml");
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

    private void runUsageReport(String fileName, Properties prop, Map parameters, Object scriplet, String sql,
                                       List<String> keys, boolean useUser)
                                                                    throws Exception{
        UsageReportHelper helper = (UsageReportHelper) scriplet;
        Connection connection = getConnection(prop);
        LinkedHashSet<String> mappingValues = getMappingValues(connection, sql);
        LinkedHashMap<String, String> keyToColumnName = getKeyToColumnValues(mappingValues);
        helper.setKeyToColumnMap(keyToColumnName);

        //now generate the report to be compiled
        //todo [Donal] note - if there is no data this throws an exception. Need to create a canned report showing report info to return to user
        Document transformDoc = Utilities.getUsageRuntimeDoc(useUser, keys, mappingValues);
        File f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        //get xsl and xml
        String xslStr = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportTransform.xsl");
        String xmlFileName = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/"+fileName+".jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 565);
        params.put("PageMinWidth", 595);
        params.put("ReportInfoStaticTextSize", 128);
        params.put("TitleInnerFrameBuffer", 7);


        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperDoc = transform(xslStr, xmlFileName, params);

        f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageRuntimeDoc.xml");
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
            byte[] resbytes = IOUtils.slurpStream(is, 100000);
            return new String(resbytes);
        }finally{
            is.close();
        }
    }
    
    private LinkedHashSet<String> getMappingValueSet(Statement stmt, String sql) throws Exception{

        LinkedHashSet<String> set = new LinkedHashSet<String>();
        ResultSet rs = stmt.executeQuery(sql);

        while(rs.next()){
            List<String> mappingStrings = new ArrayList<String>();
            String authUser = rs.getString(Utilities.AUTHENTICATED_USER);
            for(int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++){
                mappingStrings.add(rs.getString("MAPPING_VALUE_"+(i+1)));
            }
            String mappingValue = Utilities.getMappingValueString(authUser, mappingStrings.toArray(new String[]{}));
            set.add(mappingValue);
        }
        return set;
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


        Map<String, String> nameToId = new HashMap<String,String>();
        String serviceName = prop.getProperty(SERVICE_ID_TO_NAME+"_1");
        String serviceOid = prop.getProperty(SERVICE_ID_TO_NAME_OID+"_1");
        int index = 2;
        while(serviceName != null && serviceOid != null){
            nameToId.put(serviceName, serviceOid);
            serviceName = prop.getProperty(SERVICE_ID_TO_NAME+"_"+index);
            serviceOid = prop.getProperty(SERVICE_ID_TO_NAME_OID+"_"+index);
            index++;
        }
        if(!nameToId.isEmpty()) parameters.put(SERVICE_NAME_TO_ID_MAP, nameToId);

        List<String> keys  = loadListFromProperties(MAPPING_KEY, prop);
        List<String> values = loadListFromProperties(MAPPING_VALUE, prop);
        List<String> useAnd = loadListFromProperties(VALUE_EQUAL_OR_LIKE, prop);

        parameters.put(MAPPING_KEYS, keys);
        parameters.put(MAPPING_VALUES, values);
        parameters.put(VALUE_EQUAL_OR_LIKE, useAnd);

        List<String> operations = loadListFromProperties(OPERATIONS, prop);
        parameters.put(OPERATIONS, operations);

        b = Boolean.parseBoolean(prop.getProperty(USE_USER).toString());
        parameters.put(USE_USER, b);
        List<String> authUser = loadListFromProperties(AUTHENTICATED_USERS, prop);
        parameters.put(AUTHENTICATED_USERS, authUser);

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
        SubIntervalScriptletHelper sH = new SubIntervalScriptletHelper();
        sH.setScriptletHelper((ScriptletHelper)scriplet);
        parameters.put(SUBINTERVAL_REPORT_SCRIPTLET, sH);

        return parameters;
    }
    
}
