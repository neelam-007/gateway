/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 26, 2008
 * Time: 4:35:07 PM
 */
package com.l7tech.server.ems.standardreports;

import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.standardreports.PerformanceSummaryJsonConvertor;

import java.util.Map;

public class JsonReportParameterConvertorFactory {

    public static JsonReportParameterConvertor getConvertor(Map jsonMap){
        String reportType = (String) jsonMap.get(JSONConstants.REPORT_TYPE);
        Boolean summaryReport = (Boolean) jsonMap.get(JSONConstants.SUMMARY_REPORT);

        if(reportType.equals(JSONConstants.ReportType.PERFORMANCE)
                && summaryReport){
            PerformanceSummaryJsonConvertor ps = new PerformanceSummaryJsonConvertor();
            return ps;
        }

        throw new RuntimeException("No implementation yet for supplied report type");
    }
}
