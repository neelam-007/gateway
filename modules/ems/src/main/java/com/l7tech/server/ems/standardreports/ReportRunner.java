/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 25, 2008
 * Time: 1:45:52 PM
 */
package com.l7tech.server.ems.standardreports;

import org.mortbay.util.ajax.JSON;

import java.util.Map;
import java.util.Collection;

import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.standardreports.reporttypes.PerformanceSummary;

public class ReportRunner {

    private String jsonData;
    private Map jsonMap;


    public ReportRunner(String jsonData) {
        this.jsonData = jsonData;
        Object o = JSON.parse(this.jsonData);
        System.out.println(o.getClass().getName());

        jsonMap = (Map) o;
        for(Object o1: jsonMap.entrySet()){
            Map.Entry me = (Map.Entry) o1;
            System.out.println(me.getKey()+" " + me.getValue());            
        }
    }


    public Collection<ReportApi.ReportSubmission> getReportSubmissions() throws ReportApi.ReportException {

        String reportType = (String) jsonMap.get(JSONConstants.REPORT_TYPE);
        Boolean summaryReport = (Boolean) jsonMap.get(JSONConstants.SUMMARY_REPORT);
        
        if(reportType.equals(JSONConstants.ReportType.PERFORMANCE)
                && summaryReport){
            PerformanceSummary ps = new PerformanceSummary();
            return ps.getReportSubmissions(jsonMap);
            
        }

        return null;
    }
}
