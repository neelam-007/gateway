/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 29, 2008
 * Time: 1:58:00 PM
 * ReportApp is a CLI program to test jasper reports. Depends on report.properties being in the same directory
 */
package com.l7tech.standardreports;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.view.JasperViewer;


public class ReportApp
{
	private static final String TASK_FILL = "fill";
	private static final String TASK_PDF = "pdf";
	private static final String TASK_HTML = "html";
	private static final String TASK_VIEW = "view";

    //The following params must be supplied when filling the report
    private static final String REPORT_CONNECTION= "REPORT_CONNECTION";
    private static final String REPORT_TIME_ZONE = "REPORT_TIME_ZONE";
    private static final String REPORT_TYPE = "REPORT_TYPE";
    private static final String INTERVAL_TIME_UNIT = "INTERVAL_TIME_UNIT";
    private static final String INTERVAL_NUM_OF_TIME_UNITS = "INTERVAL_NUM_OF_TIME_UNITS";
    private static final String REPORT_RAN_BY = "REPORT_RAN_BY";

    private static final String IS_DETAIL = "IS_DETAIL";
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

    //db props
    private static final String CONNECTION_STRING = "CONNECTION_STRING";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASSWORD = "DB_PASSWORD";

    //Non report params, just used in ReportApp
    private static final String IS_SUMMARY = "IS_SUMMARY";
    private static final String HOURLY_MAX_RETENTION_NUM_DAYS = "HOURLY_MAX_RETENTION_NUM_DAYS";
    private static final String REPORT_FILE_NAME_NO_ENDING = "REPORT_FILE_NAME_NO_ENDING";
    private static final Properties prop = new Properties();
    private static final String MAPPING_KEYS = "MAPPING_KEYS";
    private static final String MAPPING_VALUES = "MAPPING_VALUES";
    private static final String VALUE_EQUAL_OR_LIKE = "VALUE_EQUAL_OR_LIKE";

    /**
	 *
	 */
	public static void main(String[] args) throws Exception
	{
		if(args.length == 0)
		{
			usage();
			return;
		}

		String taskName = args[0];

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

    private static void fill(String fileName, long start) throws Exception{

        //Preparing parameters
        Map parameters = new HashMap();
        //Required
        parameters.put(REPORT_CONNECTION, getConnection(prop));
        //parameters.put(REPORT_TIME_ZONE, java.util.TimeZone??);
        parameters.put(REPORT_TYPE, prop.getProperty(REPORT_TYPE));
        parameters.put(INTERVAL_TIME_UNIT, prop.getProperty(INTERVAL_TIME_UNIT));
        Integer i = Integer.parseInt(prop.getProperty(INTERVAL_NUM_OF_TIME_UNITS).toString());
        parameters.put(INTERVAL_NUM_OF_TIME_UNITS, i);
        parameters.put(REPORT_RAN_BY, prop.getProperty(REPORT_RAN_BY));
        //Optional
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

        //relative and absolute time
        Boolean b = Boolean.parseBoolean(prop.getProperty(IS_RELATIVE).toString());
        parameters.put(IS_RELATIVE, b);
        parameters.put(RELATIVE_TIME_UNIT, prop.getProperty(RELATIVE_TIME_UNIT));
        i = Integer.parseInt(prop.getProperty(RELATIVE_NUM_OF_TIME_UNITS).toString());
        parameters.put(RELATIVE_NUM_OF_TIME_UNITS, i);

        b = Boolean.parseBoolean(prop.getProperty(IS_ABSOLUTE).toString());
        parameters.put(IS_ABSOLUTE, b);
        parameters.put(ABSOLUTE_START_TIME, prop.getProperty(ABSOLUTE_START_TIME));
        parameters.put(ABSOLUTE_END_TIME, prop.getProperty(ABSOLUTE_END_TIME));

        parameters.put(HOURLY_MAX_RETENTION_NUM_DAYS, new Integer(prop.getProperty(HOURLY_MAX_RETENTION_NUM_DAYS)));

        b = Boolean.parseBoolean(prop.getProperty(IS_DETAIL).toString());
        parameters.put(IS_DETAIL, b);

        List<String > keys  = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("Customer");

        List<String> values = new ArrayList<String>();
        values.add("127.0.0.%");
        values.add("Gold");

        List<Boolean> useAnd = new ArrayList<Boolean>();
        useAnd.add(false);
        useAnd.add(true);

        parameters.put(MAPPING_KEYS, keys);
        parameters.put(MAPPING_VALUES, values);
        parameters.put(VALUE_EQUAL_OR_LIKE, useAnd);

        
        JasperFillManager.fillReportToFile(fileName+".jasper", parameters, getConnection(prop));

        System.err.println("Filling time : " + (System.currentTimeMillis() - start));
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


}
