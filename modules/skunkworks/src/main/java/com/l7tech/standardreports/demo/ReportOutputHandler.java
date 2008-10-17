/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 16, 2008
 * Time: 11:22:08 AM
 */
package com.l7tech.standardreports.demo;

import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.HttpConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ReportOutputHandler extends AbstractHandler {
    private final String reportOutputDir;
    private File outputDir;


    public ReportOutputHandler(String reportOutputDir) {
        this.reportOutputDir = reportOutputDir;
        outputDir = new File(reportOutputDir);
        if(!outputDir.isDirectory()) throw new IllegalArgumentException("reportOutputDir must be a directory: "+ reportOutputDir);
    }

    public void handle(String s, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, int i) throws IOException, ServletException {
        Request base_request = (httpServletRequest instanceof Request) ? (Request) httpServletRequest : HttpConnection.getCurrentConnection().getRequest();
        if(!s.equals("/getReportOutput")){
            return;
        }
        base_request.setHandled(true);

        //Get all files from the directory and print them out

        StringBuilder sb = new StringBuilder();

        String [] allPdfFiles = outputDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if(name.endsWith(".pdf")) return true;
                return false;  
            }
        });

        
        Arrays.sort(allPdfFiles);

        sb.append("<table class=\"indent spaced\">");
        sb.append("<th class=\"name\">Report Name</th>");
        sb.append("<th class=\"name\">PDF Download</th>");
        sb.append("<th class=\"name\">HTML Download</th>");
        for(String f: allPdfFiles){
            String reportName = f.substring(0, f.indexOf("."));
            sb.append("<tr>");
            sb.append("<td class=\"details\">"+reportName+"</td>");
            sb.append("<td class=\"details\"><a href=\""+f+"\">pdf</a></td>");
            sb.append("<td class=\"details\"><a href=\""+reportName+".html\">html</a></td>");

            sb.append("</tr>");
        }
        sb.append("</table>");

        //System.out.println(sb.toString());
        httpServletResponse.setContentType("text/html");
        httpServletResponse.getWriter().println(sb.toString());
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        
    }
}
