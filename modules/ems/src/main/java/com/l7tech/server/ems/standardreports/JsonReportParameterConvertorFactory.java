/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 26, 2008
 * Time: 4:35:07 PM
 */
package com.l7tech.server.ems.standardreports;

import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.management.api.node.ReportApi;

import java.util.Map;

public class JsonReportParameterConvertorFactory {

    public static JsonReportParameterConvertor getConvertor(Map jsonMap) throws ReportApi.ReportException {
        if(!jsonMap.containsKey(JSONConstants.SUMMARY_REPORT)){
            throw new ReportApi.ReportException("Key: "+JSONConstants.SUMMARY_REPORT+" is missing from JSON data");
        }
        Boolean summaryReport = (Boolean) jsonMap.get(JSONConstants.SUMMARY_REPORT);

        if(summaryReport){
            return new SummaryReportJsonConvertor();
        }else {
            return new IntervalReportJsonConvertor();
        }
    }
}
