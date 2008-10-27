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
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;
import com.l7tech.server.ems.standardreports.ScripletHelper;
import com.l7tech.server.ems.standardreports.UsageReportHelper;
import com.l7tech.server.ems.standardreports.Utilities;
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
    private static final String TASK_RUN = "run";    

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
            else if (TASK_RUN.equals(taskName)){
                
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

    private void fill(String fileName, long start) throws Exception{

        //Preparing parameters
        Map parameters = new HashMap();
        //Required
        parameters.put(REPORT_CONNECTION, getConnection(prop));
        parameters.put(TEMPLATE_FILE_ABSOLUTE, "Styles.jrtx");
        parameters.put(SUBREPORT_DIRECTORY, ".");

        //parameters.put(REPORT_TIME_ZONE, java.util.TimeZone??);
        parameters.put(REPORT_TYPE, prop.getProperty(REPORT_TYPE));
        parameters.put(REPORT_RAN_BY, prop.getProperty(REPORT_RAN_BY));

        Boolean b = Boolean.parseBoolean(prop.getProperty(IS_CONTEXT_MAPPING));
        parameters.put(IS_CONTEXT_MAPPING, b);

        Boolean isDetail = Boolean.parseBoolean(prop.getProperty(IS_DETAIL).toString());
        parameters.put(IS_DETAIL, isDetail);

        //relative and absolute time
        b = Boolean.parseBoolean(prop.getProperty(IS_RELATIVE));
        parameters.put(IS_RELATIVE, b);
        parameters.put(RELATIVE_TIME_UNIT, prop.getProperty(RELATIVE_TIME_UNIT));
        Integer numRelativeTimeUnits = Integer.parseInt(prop.getProperty(RELATIVE_NUM_OF_TIME_UNITS).toString());
        parameters.put(RELATIVE_NUM_OF_TIME_UNITS, numRelativeTimeUnits);

        parameters.put(INTERVAL_TIME_UNIT, prop.getProperty(INTERVAL_TIME_UNIT));
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
        
        List<String > keys  = loadListFromProperties(MAPPING_KEY, prop);
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

        //Only required because jasper reports for some reason ignores the value of scriptletClass from the
        //jasperreport element attribute, so specifying it as a parameter explicitly fixes this issue
        String reportScriplet = prop.getProperty("REPORT_SCRIPTLET");
        Class c = Class.forName(reportScriplet);
        Object scriplet = c.newInstance();
        if(reportScriplet.endsWith("UsageReportHelper")){
            long startTimeInPast = Utilities.getRelativeMilliSecondsInPast(numRelativeTimeUnits, prop.getProperty(RELATIVE_TIME_UNIT));
            long endTimeInPast = Utilities.getMillisForEndTimePeriod(prop.getProperty(RELATIVE_TIME_UNIT));
            String sql = Utilities.getUsageDistinctMappingQuery(startTimeInPast, endTimeInPast, null, keys, values, useAnd, 2, isDetail, null, false, null);

            runUsageReport(fileName, prop, parameters, scriplet, sql, keys);
            return;
        }
        
        parameters.put("REPORT_SCRIPTLET", scriplet);

        //JasperFillManager.fillReportToFile(fileName+".jasper", parameters, getConnection(prop));
        Connection connection = getConnection(prop);
        try{
            JasperFillManager.fillReportToFile(fileName+".jasper", parameters, connection);
        }finally{
            connection.close();
        }

        System.err.println("Filling time : " + (System.currentTimeMillis() - start));
    }

    private void runUsageReport(String fileName, Properties prop, Map parameters, Object scriplet, String sql,
                                       List<String> keys)
                                                                    throws Exception{
        UsageReportHelper helper = (UsageReportHelper) scriplet;
        parameters.put("REPORT_SCRIPTLET", scriplet);
        Statement stmt = null;
        LinkedHashSet<String> mappingValues = null;
        Connection connection = getConnection(prop);
        try{
            stmt = connection.createStatement();
            mappingValues = getMappingValueSet(stmt, sql);
        }catch(Exception ex){
            if(connection != null) connection.close();
            throw(ex);
        }

        LinkedHashMap<String, String> keyToColumnName = new LinkedHashMap<String, String>();
        int count = 1;
        System.out.println("Key to column map");
        for (String s : mappingValues) {
            keyToColumnName.put(s, "COLUMN_"+count);
            System.out.println(s+" " + "COLUMN_"+count);
            count++;
        }
        helper.setKeyToColumnMap(keyToColumnName);

        //now generate the report to be compiled
        Document transformDoc = Utilities.getUsageRuntimeDoc(keys, mappingValues);

        //get xsl and xml
        String xslStr = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportTransform.xsl");
        String xmlFileName = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/Usage_Summary_XSLT_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        //Document doc = transform(xslStr, xmlStr, params);
        Document jasperDoc = transform(xslStr, xmlFileName, params);

        File f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeDoc.xml");
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

        JasperReport report = JasperCompileManager.compileReport(bais);
        System.out.println("Report compiled");
        try{
            System.out.println("Filling report");
            JasperFillManager.fillReportToFile(report, fileName+".jrprint", parameters, connection);
            System.out.println("Report filled");
        }finally{
            connection.close();
        }
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
            StringBuilder sb = new StringBuilder();
            String authUser = rs.getString(Utilities.AUTHENTICATED_USER);
            sb.append(authUser);
            for(int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++){
                sb.append(rs.getString("MAPPING_VALUE_"+(i+1)));
            }
            set.add(sb.toString());
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
		System.out.println( "\tTasks : fill | print | pdf | html" );
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
}
