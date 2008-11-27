/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 26, 2008
 * Time: 6:36:34 PM
 */
package com.l7tech.server.ems.standardreports;

import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.gateway.standardreports.Utilities;

import java.util.Collection;
import java.util.Map;

public class IntervalReportJsonConvertor extends SummaryReportJsonConvertor {

    /**
     * Override super's implementation just to validate the interval parameter
     * @param params
     * @param reportRanBy
     * @return
     * @throws ReportApi.ReportException
     */
    @Override
    public Collection<ReportSubmissionClusterBean> getReportSubmissions(Map params, String reportRanBy) throws ReportException {
        validateParams(params);
        return super.getReportSubmissions(params, reportRanBy);
    }

    /**
     * Overridden to add the interval parameter
     * @param params
     * @param reportRanBy
     * @return
     * @throws ReportApi.ReportException
     */
    @Override
    protected Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> getReportParams(Map params, String reportRanBy) throws ReportException {
        Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> clusterToReportParams = super.getReportParams(params, reportRanBy);
        addIntervalTimeParameters(clusterToReportParams, params);

        return clusterToReportParams;
    }

    protected ReportApi.ReportType getReportType(Map params) throws ReportException {

        String reportType = (String) params.get(JSONConstants.REPORT_TYPE);
        if(reportType.equals(JSONConstants.ReportType.PERFORMANCE)){
            return ReportApi.ReportType.PERFORMANCE_INTERVAL;
        }else if(reportType.equals(JSONConstants.ReportType.USAGE)){
            return ReportApi.ReportType.USAGE_INTERVAL;
        }

        throw new ReportException("Unknown report type: " + reportType);
    }

    private void addIntervalTimeParameters(
            Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> clusterToReportParams, Map params)
            throws ReportException {

        Object o = params.get(JSONConstants.TimePeriodTypeKeys.TIME_INTERVAL);
        Map timeIntervalMap = (Map) o;

        validateSubMap(timeIntervalMap, JSONConstants.TimePeriodIntervalKeys.ALL_KEYS);

        String unitType = (String) timeIntervalMap.get(JSONConstants.TimePeriodIntervalKeys.INTERVAL_UNIT_OF_TIME);
        try{
            Utilities.getUnitFromString(unitType);
        }catch (IllegalArgumentException e){
            throw new ReportException(e.getMessage());
        }
        ReportApi.ReportSubmission.ReportParam intervalTimeUnitParam = new ReportApi.ReportSubmission.ReportParam();
        intervalTimeUnitParam.setName(INTERVAL_TIME_UNIT);
        intervalTimeUnitParam.setValue(unitType);
        addParamToAllClusters(clusterToReportParams, intervalTimeUnitParam);

        String numberOfIntervalUnits = (String) timeIntervalMap.get(JSONConstants.TimePeriodIntervalKeys.NUMBER_OF_INTERVAL_TIME_UNITS);
        ReportApi.ReportSubmission.ReportParam numberIntervalTimeUnitsParam = new ReportApi.ReportSubmission.ReportParam();
        numberIntervalTimeUnitsParam.setName(INTERVAL_NUM_OF_TIME_UNITS);
        numberIntervalTimeUnitsParam.setValue(Integer.valueOf(numberOfIntervalUnits));
        addParamToAllClusters(clusterToReportParams, numberIntervalTimeUnitsParam);

    }

    private void validateParams(Map params){
    String [] requiredParams = new String[]{JSONConstants.TimePeriodTypeKeys.TIME_INTERVAL};

        for(String s: requiredParams){
            if(!params.containsKey(s)) throw new IllegalArgumentException("Required param '"+s+"' is missing");
        }
    }

}
