/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 15, 2008
 * Time: 3:52:31 PM
 */
package com.l7tech.skunkworks.standardreports.demo;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperExportManager;

import java.util.Map;
import java.sql.Connection;

/**
 * Runs a report and converts it to pdf and html for demo purposes.
 */
public class ReportRunner {
    private final Map prop;
    private final String outputDirectory;
    private final String compiledReportDirectory;

    public ReportRunner(String compiledReportDirectory, String outputDirectory, Map prop){
        this.compiledReportDirectory = compiledReportDirectory;
        this.outputDirectory = outputDirectory;
        this.prop = prop;
    }

    public void runReport(boolean isSummary, Connection conn, String reportName) throws ReportException{
        try {
            String fileName;
            if(isSummary){
                fileName = "PS_Summary";
            }else{
                fileName = "PS_IntervalMasterReport";
            }
            JasperPrint jP = JasperFillManager.fillReport(compiledReportDirectory+"/"+fileName+".jasper", prop, conn);
            JasperExportManager.exportReportToPdfFile(jP, outputDirectory+"/"+reportName+".pdf");
            JasperExportManager.exportReportToHtmlFile(jP, outputDirectory+"/"+reportName+".html");
            
        } catch (JRException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.out.println("runReport");
    }
}
