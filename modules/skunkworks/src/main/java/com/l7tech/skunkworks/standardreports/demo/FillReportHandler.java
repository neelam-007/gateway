/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 15, 2008
 * Time: 4:21:10 PM
 */
package com.l7tech.skunkworks.standardreports.demo;

import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.HttpConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JRException;
import com.l7tech.gateway.standardreports.ScriptletHelper;
import com.l7tech.gateway.standardreports.Utilities;

public class FillReportHandler extends AbstractHandler {
    //db props
    private static final String CONNECTION_STRING = "CONNECTION_STRING";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASSWORD = "DB_PASSWORD";

    private static final String REPORT_CONNECTION= "REPORT_CONNECTION";
    private static final String IS_RELATIVE = "IS_RELATIVE";
    private final static String IS_SUMMARY = "IS_SUMMARY";
    private final static String RELATIVE_TIME_UNIT = "RELATIVE_TIME_UNIT";
    private final static String RELATIVE_NUM_OF_TIME_UNITS = "RELATIVE_NUM_OF_TIME_UNITS";
    private final static String INTERVAL_TIME_UNIT = "INTERVAL_TIME_UNIT";
    private final static String INTERVAL_NUM_OF_TIME_UNITS = "INTERVAL_NUM_OF_TIME_UNITS";
    private final static String SELECTED_MAPPING_KEYS = "SELECTED_MAPPING_KEYS";
    private static final String REPORT_RAN_BY = "REPORT_RAN_BY";
    private static final String IS_CONTEXT_MAPPING = "IS_CONTEXT_MAPPING";
    private static final String MAPPING_KEYS = "MAPPING_KEYS";
    private static final String IS_DETAIL = "IS_DETAIL";
    private static final String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
    private static final String REPORT_SCRIPTLET = "REPORT_SCRIPTLET";
    private static final String REPORT_NAME = "REPORT_NAME";
    private static final String TEMPLATE_FILE_ABSOLUTE = "TEMPLATE_FILE_ABSOLUTE";
    private static final String SUBREPORT_DIRECTORY = "SUBREPORT_DIRECTORY";
    private static final String SERVICE_NAME_TO_ID_MAP = "SERVICE_NAME_TO_ID_MAP";

    private static final String jasperFileDir = "/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports";
    private static final String templateFile = "/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/resources/com/l7tech/server/ems/standardreports";
    private static final String demoOutput = "/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/build/demo_output";

    Properties dbProps = new Properties();

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    public FillReportHandler() throws Exception{
        FileInputStream fileInputStream = new FileInputStream("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/demo/dbprops.properties");
        dbProps.load(fileInputStream);
    }

    private List<String> getListParameter(String param, HttpServletRequest httpServletRequest){
        List<String> paramList = new ArrayList<String>();
        String mappingKey =  httpServletRequest.getParameter(param+"_"+1);
        int counter = 2;
        while(mappingKey != null){
            if(mappingKey.equals("")){
                paramList.add(null);                
            }else{
                paramList.add(mappingKey);
            }
            mappingKey =  httpServletRequest.getParameter(param+"_"+counter);
            counter++;
        }
        return paramList;
    }

    public void handle(String s, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, int i) throws IOException, ServletException {
        Request base_request = (httpServletRequest instanceof Request) ? (Request) httpServletRequest : HttpConnection.getCurrentConnection().getRequest();
        if(!s.equals("/fillReport")){
            return;
        }
        base_request.setHandled(true);

        String summary = httpServletRequest.getParameter(IS_SUMMARY);
//        System.out.println("Summary: " + summary);

        final boolean isSummary = (summary.equalsIgnoreCase("true"))? true: false;
        String relativeTimeUnit = httpServletRequest.getParameter(RELATIVE_TIME_UNIT);
        String numRelativeTimeUnit = httpServletRequest.getParameter(RELATIVE_NUM_OF_TIME_UNITS);

        String intervalTimeUnit = httpServletRequest.getParameter(INTERVAL_TIME_UNIT);
        String numIntervalTimeUnit = httpServletRequest.getParameter(INTERVAL_NUM_OF_TIME_UNITS);

        //SERVICES
        Map<String, String> serviceIdMap = new HashMap<String, String>();
        String serviceKey =  httpServletRequest.getParameter("SERVICES_MAP_KEY_"+1);
        String serviceValue =  httpServletRequest.getParameter("SERVICES_MAP_VALUE_"+1);
        int counter = 2;
        while(serviceKey != null && serviceValue != null){
            serviceIdMap.put(serviceKey, serviceValue);
            serviceKey =  httpServletRequest.getParameter("SERVICES_MAP_KEY_"+counter);
            serviceValue =  httpServletRequest.getParameter("SERVICES_MAP_VALUE_"+counter);
            counter++;
        }

        //MAPPING_KEY
        List<String> keys = getListParameter("MAPPING_KEY", httpServletRequest);
        boolean isContextMapping = (keys.isEmpty())?false: true;

        String reportRanBy = httpServletRequest.getParameter(REPORT_RAN_BY);
        String rName = httpServletRequest.getParameter(REPORT_NAME);
        Calendar cal = Calendar.getInstance();
        Date d = cal.getTime();
        String date = DATE_FORMAT.format(d);
        final String reportName = date+"_"+rName;

        final Map reportProps = new HashMap<>();
        reportProps.put(TEMPLATE_FILE_ABSOLUTE, templateFile+"/Styles.jrtx");
        reportProps.put(SUBREPORT_DIRECTORY, jasperFileDir);

        reportProps.put("REPORT_TYPE", "Performance Statistics");
        reportProps.put(REPORT_RAN_BY, reportRanBy);
        

        String isDetailVal = httpServletRequest.getParameter(IS_DETAIL);
        boolean isDetail = (isDetailVal != null && isDetailVal.equalsIgnoreCase("true"));
        reportProps.put(IS_DETAIL, isDetail);

        //if isDetail is true, then so must context mapping be.
        //if isDetail is false, context mapping can still be true on account of keys above
        if(isDetail) isContextMapping = true;


        reportProps.put(IS_RELATIVE, true);
        Utilities.UNIT_OF_TIME unitOfTime = Utilities.getUnitFromString(relativeTimeUnit);
        reportProps.put(RELATIVE_TIME_UNIT, unitOfTime);
        Integer num = Integer.parseInt(numRelativeTimeUnit);
        reportProps.put(RELATIVE_NUM_OF_TIME_UNITS, num);

        if(!isSummary){
            Utilities.UNIT_OF_TIME intervalUnitOfTime = Utilities.getUnitFromString(intervalTimeUnit);
            reportProps.put(INTERVAL_TIME_UNIT, intervalUnitOfTime);
            num = Integer.parseInt(numIntervalTimeUnit);
            reportProps.put(INTERVAL_NUM_OF_TIME_UNITS, num);
        }
        reportProps.put("IS_ABSOLUTE", false);

        reportProps.put(SERVICE_NAME_TO_ID_MAP, serviceIdMap);

        reportProps.put(MAPPING_KEYS, keys);
        //
        List<String> values = getListParameter("MAPPING_VALUE", httpServletRequest);
        reportProps.put("MAPPING_VALUES", values);
        List<String> useAnd = new ArrayList<String>();
        reportProps.put("VALUE_EQUAL_OR_LIKE", useAnd);
        if(isDetail){
            List<String> operations = getListParameter("OPERATION", httpServletRequest);
            reportProps.put("OPERATIONS", operations);
        }

        List<String> authUsers = getListParameter("AUTHENTICATED_USER", httpServletRequest);
        boolean useUser = !authUsers.isEmpty();
        
        //use is a mapping, so if true, use it
        if(useUser) isContextMapping = true;
        reportProps.put(IS_CONTEXT_MAPPING, isContextMapping);
        
        reportProps.put("USE_USER", useUser);
        reportProps.put("AUTHENTICATED_USERS", authUsers);

        JasperPrint jp = null;
        try {
            jp = JasperFillManager.fillReport(jasperFileDir+"/StyleGenerator.jasper", reportProps);
        } catch (JRException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Map sMap = jp.getStylesMap();
        if(sMap == null) throw new NullPointerException("sMap is null");

        reportProps.put(STYLES_FROM_TEMPLATE, sMap);

        ScriptletHelper sh = new ScriptletHelper();
        reportProps.put(REPORT_SCRIPTLET, sh);

        final Connection conn =getConnection(dbProps);
        Runnable r = new Runnable(){
            public void run() {
                ReportRunner runner = new ReportRunner(jasperFileDir, demoOutput, reportProps);

                try {
                    runner.runReport(isSummary, conn, reportName);
                } catch (ReportException e) {
                    e.printStackTrace();
                } finally{
                    try {
                        if(conn != null && !conn.isClosed()){
                            conn.close();
                            System.out.println("Connection closed");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }


                }
            }
        };
        new Thread(r).start();

        httpServletResponse.setContentType("text/html");
        httpServletResponse.getWriter().println("<p>Report has been submitted</p>");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);

    }

    public static Connection getConnection(Properties prop) throws ServletException{

        String connectString = prop.getProperty(CONNECTION_STRING);
        String driver = "com.mysql.jdbc.Driver";
        String user = prop.getProperty(DB_USER);
        String password = prop.getProperty(DB_PASSWORD);

        try {
            Class.forName(driver);
            return DriverManager.getConnection(connectString, user, password);
        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        } catch (SQLException e) {
            throw new ServletException("Cannot connect to db: " + e.getMessage());
        }
    }
    
}
