/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 25, 2008
 * Time: 2:37:55 PM
 */
package com.l7tech.server.ems.standardreports.reporttypes;

import java.util.*;

import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.gateway.standardreports.Utilities;

public class PerformanceSummary {

    //common
    public final static String IS_RELATIVE = "IS_RELATIVE";
    public final static String RELATIVE_NUM_OF_TIME_UNITS = "RELATIVE_NUM_OF_TIME_UNITS";
    public final static String RELATIVE_TIME_UNIT = "RELATIVE_TIME_UNIT";
    public final static String IS_ABSOLUTE = "IS_ABSOLUTE";
    public final static String ABSOLUTE_START_TIME = "ABSOLUTE_START_TIME";
    public final static String ABSOLUTE_END_TIME = "ABSOLUTE_END_TIME";
    public final static String REPORT_RAN_BY = "REPORT_RAN_BY";
    public final static String SERVICE_NAMES_LIST = "SERVICE_NAMES_LIST";
    public final static String SERVICE_ID_TO_OPERATIONS_MAP = "SERVICE_ID_TO_OPERATIONS_MAP";
    public final static String HOURLY_MAX_RETENTION_NUM_DAYS = "HOURLY_MAX_RETENTION_NUM_DAYS";
    public final static String MAPPING_KEYS = "MAPPING_KEYS";
    public final static String MAPPING_VALUES = "MAPPING_VALUES";
    public final static String VALUE_EQUAL_OR_LIKE = "VALUE_EQUAL_OR_LIKE";
    public final static String IS_DETAIL = "IS_DETAIL";
    public final static String USE_USER = "USE_USER";
    public final static String AUTHENTICATED_USERS = "AUTHENTICATED_USERS";
    public final static String PRINT_CHART = "PRINT_CHART";

    //ps only
    public final static String IS_CONTEXT_MAPPING = "IS_CONTEXT_MAPPING";

//    public final String [] requiredParams = new String[]{IS_RELATIVE, RELATIVE_NUM_OF_TIME_UNITS, RELATIVE_TIME_UNIT,
//    IS_ABSOLUTE, ABSOLUTE_START_TIME, ABSOLUTE_END_TIME, REPORT_RAN_BY, SERVICE_NAMES_LIST, SERVICE_ID_TO_OPERATIONS_MAP,
//    HOURLY_MAX_RETENTION_NUM_DAYS, MAPPING_KEYS, MAPPING_VALUES, VALUE_EQUAL_OR_LIKE, IS_DETAIL, USE_USER,
//    AUTHENTICATED_USERS, PRINT_CHART};

    //need to be filled in on the SSG
    public final static String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
    public final static String DISPLAY_STRING_TO_MAPPING_GROUP = "DISPLAY_STRING_TO_MAPPING_GROUP";



    public Collection<ReportApi.ReportSubmission> getReportSubmissions(Map params) throws ReportApi.ReportException {
        validateParams(params);
        ReportApi.ReportSubmission reportSubmission = new ReportApi.ReportSubmission();
        reportSubmission.setType(ReportApi.ReportType.PERFORMANCE);
        //reportSubmission.setParameters(getReportParams(params));

        Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> clusterToReportParams = getReportParams(params);
        List<ReportApi.ReportSubmission> repSubs = new ArrayList<ReportApi.ReportSubmission>();
        for(String s: clusterToReportParams.keySet()){
            ReportApi.ReportSubmission reportSub = new ReportApi.ReportSubmission();
            reportSub.setParameters(clusterToReportParams.get(s));
            repSubs.add(reportSub);
        }
        return repSubs;
    }



    private Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> getReportParams(Map params) throws ReportApi.ReportException {

        Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> clusterToReportParams = getClusterMaps(params);
        //Time Period
        addTimeParameters(clusterToReportParams, params);

        //reportRanBy
        String reportRanBy = (String) params.get(JSONConstants.REPORT_RAN_BY);
        ReportApi.ReportSubmission.ReportParam reportRanByParam = new ReportApi.ReportSubmission.ReportParam();
        reportRanByParam.setName(REPORT_RAN_BY);
        reportRanByParam.setValue(reportRanBy);

        addParamToAllClusters(clusterToReportParams, reportRanByParam);
        return clusterToReportParams;
    }

    private void addParamToAllClusters(
            Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> clusterToReportParams,
            ReportApi.ReportSubmission.ReportParam paramToAdd){

        for(String s: clusterToReportParams.keySet()){
            Collection<ReportApi.ReportSubmission.ReportParam> reportParams = clusterToReportParams.get(s);
            reportParams.add(paramToAdd);
        }
    }
    /**
     * Get a map per cluster from the request json data.
     * @param params
     * @return
     * @throws ReportApi.ReportException
     */
    private Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> getClusterMaps(Map params) throws ReportApi.ReportException {

        Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> returnMap
                = new HashMap<String, Collection<ReportApi.ReportSubmission.ReportParam>>();

        Map<String, Map<String, Set<String>>> clusterIdToServicesAndOperations
                = new HashMap<String, Map<String, Set<String>>>();

        Map<String, Set<String>> clusterIdToServiceNames = new HashMap<String, Set<String>>();

        Object [] entities = (Object[]) params.get(JSONConstants.REPORT_ENTITIES);

        for(Object o: entities){
            Map currentEntity = (Map) o;
            validateSubMap(currentEntity, JSONConstants.ReportEntities.ALL_KEYS);
            String clusterId = (String) currentEntity.get(JSONConstants.ReportEntities.CLUSTER_ID);
            if(!returnMap.containsKey(clusterId)){
                List<ReportApi.ReportSubmission.ReportParam> clusterParams =
                        new ArrayList<ReportApi.ReportSubmission.ReportParam>();

                returnMap.put(clusterId, clusterParams);
            }

            if(!clusterIdToServicesAndOperations.containsKey(clusterId)){
                Map<String, Set<String>> serviceIdToOperation = new HashMap<String, Set<String>>();
                clusterIdToServicesAndOperations.put(clusterId, serviceIdToOperation);
            }

            String serviceId = (String) currentEntity.get(JSONConstants.ReportEntities.PUBLISHED_SERVICE_ID);
            Map<String, Set<String>> serviceIdToOps = clusterIdToServicesAndOperations.get(clusterId);

            if(!serviceIdToOps.containsKey(serviceId)){
                Set<String> operations = new HashSet<String>();
                serviceIdToOps.put(serviceId, operations);
            }
            Object opObj = currentEntity.get(JSONConstants.ReportEntities.OPERATION);
            if(opObj == null || opObj.equals("")) continue;

            Set<String> operations = serviceIdToOps.get(serviceId);
            operations.add(opObj.toString());

            if(!clusterIdToServiceNames.containsKey(clusterId)){
                Set<String> serviceNames = new HashSet<String>();
                clusterIdToServiceNames.put(clusterId, serviceNames);
            }

            Set<String> serviceNames = clusterIdToServiceNames.get(clusterId);
            serviceNames.add((String) currentEntity.get(JSONConstants.ReportEntities.PUBLISHED_SERVICE_NAME));
        }

        for(String clusterId: returnMap.keySet()){
            Map<String, Set<String>> serviceIdToOps = clusterIdToServicesAndOperations.get(clusterId);
            ReportApi.ReportSubmission.ReportParam serviceIdToOperationParam = new ReportApi.ReportSubmission.ReportParam();
            serviceIdToOperationParam.setName(SERVICE_ID_TO_OPERATIONS_MAP);
            serviceIdToOperationParam.setValue(serviceIdToOps);
            Collection<ReportApi.ReportSubmission.ReportParam> clusterParams = returnMap.get(clusterId);
            clusterParams.add(serviceIdToOperationParam);

            Set<String> serviceNames = clusterIdToServiceNames.get(clusterId);
            ReportApi.ReportSubmission.ReportParam serviceNameParam = new ReportApi.ReportSubmission.ReportParam();
            serviceNameParam.setName(SERVICE_NAMES_LIST);
            serviceNameParam.setValue(serviceNames);
            clusterParams.add(serviceNameParam);
        }

        return returnMap;
    }


    private void validateParams(Map params){
    String [] requiredParams = new String[]{JSONConstants.TimePeriodTypeKeys.TIME_PERIOD_MAIN,
            JSONConstants.REPORT_RAN_BY, JSONConstants.REPORT_ENTITIES};

        for(String s: requiredParams){
            if(!params.containsKey(s)) throw new IllegalArgumentException("Required param '"+s+"' is missing");
        }
    }
    
    private void addTimeParameters(Map<String, Collection<ReportApi.ReportSubmission.ReportParam>> clusterToReportParams, Map params) throws ReportApi.ReportException {

        Object o = params.get(JSONConstants.TimePeriodTypeKeys.TIME_PERIOD_MAIN);
        Map timePeriodMap = (Map) o;
        String type = (String) timePeriodMap.get(JSONConstants.TimePeriodTypeKeys.TYPE);
        ReportApi.ReportSubmission.ReportParam isRelative = new ReportApi.ReportSubmission.ReportParam();
        isRelative.setName(IS_RELATIVE);
        ReportApi.ReportSubmission.ReportParam isAbsolute = new ReportApi.ReportSubmission.ReportParam();
        isAbsolute.setName(IS_ABSOLUTE);

        if(type.equals(JSONConstants.TimePeriodTypeValues.RELATIVE)){
            isRelative.setValue(true);
            isAbsolute.setValue(false);
            addParamToAllClusters(clusterToReportParams, isRelative);

            validateSubMap(timePeriodMap, JSONConstants.TimePeriodRelativeKeys.ALL_KEYS);

            String unitType = (String) timePeriodMap.get(JSONConstants.TimePeriodRelativeKeys.UNIT_OF_TIME);
            try{
                Utilities.getUnitFromString(unitType);
            }catch (IllegalArgumentException e){
                throw new ReportApi.ReportException(e.getMessage());
            }
            ReportApi.ReportSubmission.ReportParam relativeUnit = new ReportApi.ReportSubmission.ReportParam();
            relativeUnit.setName(RELATIVE_TIME_UNIT);
            relativeUnit.setValue(unitType);
            addParamToAllClusters(clusterToReportParams, relativeUnit);

            String numberOfTimeUnits = (String) timePeriodMap.get(JSONConstants.TimePeriodRelativeKeys.NUMBER_OF_TIME_UNITS);
            ReportApi.ReportSubmission.ReportParam numberRelativeTimeUnits = new ReportApi.ReportSubmission.ReportParam();
            numberRelativeTimeUnits.setName(RELATIVE_NUM_OF_TIME_UNITS);
            numberRelativeTimeUnits.setValue(numberOfTimeUnits);
            addParamToAllClusters(clusterToReportParams, numberRelativeTimeUnits);

        }else if(type.equals(JSONConstants.TimePeriodTypeValues.ABSOLUTE)){
            isRelative.setValue(false);
            isAbsolute.setValue(true);
            addParamToAllClusters(clusterToReportParams, isAbsolute);
            throw new UnsupportedOperationException("Absolute time period not yet implemented on ESM");
        }else{
            throw new ReportApi.ReportException("Invalid json value for key:" + JSONConstants.TimePeriodTypeKeys.TYPE);
        }
    }

    private void validateSubMap(Map subMap, String [] requiredKeys){
        for(String s: requiredKeys){
            if(!subMap.containsKey(s)) throw new IllegalArgumentException("Required param '"+s+"' is missing");                
        }
    }
}


