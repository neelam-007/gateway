/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 25, 2008
 * Time: 2:37:55 PM
 */
package com.l7tech.server.ems.standardreports;

import java.util.*;
import java.text.ParseException;

import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.gateway.standardreports.Utilities;

public class SummaryReportJsonConvertor implements JsonReportParameterConvertor {


    @Override
    public Collection<ReportSubmissionClusterBean> getReportSubmissions(Map params, String reportRanBy) throws ReportException {
        validateParams(params);

        Map<String, Map<String, ReportApi.ReportSubmission.ReportParam>> clusterToReportParams = getReportParams(params, reportRanBy);

        String reportName = (String) params.get(JSONConstants.REPORT_NAME);
        List<ReportSubmissionClusterBean> clusterBeans = new ArrayList<ReportSubmissionClusterBean>();
        for(Map.Entry<String, Map<String, ReportApi.ReportSubmission.ReportParam>> me: clusterToReportParams.entrySet()){
            ReportApi.ReportSubmission reportSub = new ReportApi.ReportSubmission();
            reportSub.setName(reportName);
            reportSub.setType(getReportType(params));
            reportSub.setParameters(me.getValue().values());

            ReportSubmissionClusterBean clusterBean = new ReportSubmissionClusterBean(me.getKey(), reportSub);
            clusterBeans.add(clusterBean);
        }

        return clusterBeans;
    }

    protected ReportApi.ReportType getReportType(Map params) throws ReportException {

        String reportType = (String) params.get(JSONConstants.REPORT_TYPE);
        if(reportType.equals(JSONConstants.ReportType.PERFORMANCE)){
            return ReportApi.ReportType.PERFORMANCE_SUMMARY;
        }else if(reportType.equals(JSONConstants.ReportType.USAGE)){
            return ReportApi.ReportType.USAGE_SUMMARY;
        }
        throw new ReportException("Unknown report type: " + reportType);
    }

    protected Map<String, Map<String, ReportApi.ReportSubmission.ReportParam>> getReportParams(Map params, String reportRanBy) throws ReportException {

        Map<String, Map<String, ReportApi.ReportSubmission.ReportParam>> clusterToReportParams = getClusterMaps(params);

        //Time Period
        addTimeParameters(clusterToReportParams, params);

        //reportRanBy
        ReportApi.ReportSubmission.ReportParam reportRanByParam = new ReportApi.ReportSubmission.ReportParam();
        reportRanByParam.setName(ReportApi.ReportParameters.REPORT_RAN_BY);
        reportRanByParam.setValue(reportRanBy);
        addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.REPORT_RAN_BY, reportRanByParam);

        //add mapping keys
        addMappingKeysAndValues(clusterToReportParams, params);

        ReportApi.ReportSubmission.ReportParam printChartParam = new ReportApi.ReportSubmission.ReportParam();
        printChartParam.setName(ReportApi.ReportParameters.PRINT_CHART);
        printChartParam.setValue(params.get(JSONConstants.SUMMARY_CHART));
        addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.PRINT_CHART, printChartParam);
        
        return clusterToReportParams;
    }

    protected void addParamToAllClusters(
            Map<String, Map<String, ReportApi.ReportSubmission.ReportParam>> clusterToReportParams,
            String paramName,
            ReportApi.ReportSubmission.ReportParam paramToAdd){

        for(String s: clusterToReportParams.keySet()){
            Map<String, ReportApi.ReportSubmission.ReportParam> reportParams = clusterToReportParams.get(s);
            reportParams.put(paramName, paramToAdd);
        }
    }
    /**
     * Get a map per cluster from the request json data.
     * @param params
     * @return
     * @throws ReportApi.ReportException
     */
    private Map<String, Map<String, ReportApi.ReportSubmission.ReportParam>> getClusterMaps(Map params) throws ReportException {

        Map<String, Map<String, ReportApi.ReportSubmission.ReportParam>> returnMap
                = new HashMap<String, Map<String, ReportApi.ReportSubmission.ReportParam>>();

        Map<String, Map<String, Set<String>>> clusterIdToServicesAndOperations
                = new HashMap<String, Map<String, Set<String>>>();

        Map<String,Map<String, String>> clusterIdToServiceIdsToNameMap = new HashMap<String, Map<String, String>>();

        Object [] entities = (Object[]) params.get(JSONConstants.REPORT_ENTITIES);

        for(Object o: entities){
            Map currentEntity = (Map) o;
            validateSubMap(currentEntity, JSONConstants.ReportEntities.ALL_KEYS);
            String clusterId = (String) currentEntity.get(JSONConstants.ReportEntities.CLUSTER_ID);
            if(!returnMap.containsKey(clusterId)){
                Map<String, ReportApi.ReportSubmission.ReportParam>
                        clusterParams = new HashMap<String, ReportApi.ReportSubmission.ReportParam>();
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
            if(opObj != null && !opObj.equals("")){
                Set<String> operations = serviceIdToOps.get(serviceId);
                operations.add(opObj.toString());
            }

            if(!clusterIdToServiceIdsToNameMap.containsKey(clusterId)){
                Map<String, String> serviceIdToName = new HashMap<String, String>();
                clusterIdToServiceIdsToNameMap.put(clusterId, serviceIdToName);
            }

            Map<String, String> serviceIdToName = clusterIdToServiceIdsToNameMap.get(clusterId);
            serviceIdToName.put(serviceId,(String) currentEntity.get(JSONConstants.ReportEntities.PUBLISHED_SERVICE_NAME));
        }

        for(String clusterId: returnMap.keySet()){
            Map<String, Set<String>> serviceIdToOps = clusterIdToServicesAndOperations.get(clusterId);
            ReportApi.ReportSubmission.ReportParam serviceIdToOperationParam = new ReportApi.ReportSubmission.ReportParam();
            serviceIdToOperationParam.setName(ReportApi.ReportParameters.SERVICE_ID_TO_OPERATIONS_MAP);
            serviceIdToOperationParam.setValue(serviceIdToOps);
            Map<String, ReportApi.ReportSubmission.ReportParam> clusterParams = returnMap.get(clusterId);
            clusterParams.put(ReportApi.ReportParameters.SERVICE_ID_TO_OPERATIONS_MAP, serviceIdToOperationParam);

            //check if any operations are selected, if they are, then we have a detail query
            //todo [Donal] the Utility functions can determine this itself, create a utility function determine this
            //the report still needs the isDetail parameter, so it knows when to display certain bands
            boolean isDetail = false;
            for(Map.Entry<String, Set<String>> me: serviceIdToOps.entrySet()){
                if(!me.getValue().isEmpty()) isDetail = true;
            }

            ReportApi.ReportSubmission.ReportParam isDetailParam = new ReportApi.ReportSubmission.ReportParam();
            isDetailParam.setName(ReportApi.ReportParameters.IS_DETAIL);
            isDetailParam.setValue(isDetail);
            clusterParams.put(ReportApi.ReportParameters.IS_DETAIL, isDetailParam);

            //add the is ctx mapping param. At this point it should not exist, however testing for it in case
            //program gets reordered
            if(!clusterParams.containsKey(ReportApi.ReportParameters.IS_CONTEXT_MAPPING)){
                ReportApi.ReportSubmission.ReportParam isCtxMappingParam = new ReportApi.ReportSubmission.ReportParam();
                isCtxMappingParam.setName(ReportApi.ReportParameters.IS_CONTEXT_MAPPING);
                isCtxMappingParam.setValue(isDetail);
                clusterParams.put(ReportApi.ReportParameters.IS_CONTEXT_MAPPING, isCtxMappingParam);
            }else{
                ReportApi.ReportSubmission.ReportParam
                        isCtxMappingParam = clusterParams.get(ReportApi.ReportParameters.IS_CONTEXT_MAPPING);
                isCtxMappingParam.setValue(isDetail || (Boolean)isCtxMappingParam.getValue());
            }

            Map<String, String> serviceIdToName = clusterIdToServiceIdsToNameMap.get(clusterId);
            ReportApi.ReportSubmission.ReportParam serviceNameParam = new ReportApi.ReportSubmission.ReportParam();
            serviceNameParam.setName(ReportApi.ReportParameters.SERVICE_ID_TO_NAME_MAP);
            serviceNameParam.setValue(serviceIdToName);
            clusterParams.put(ReportApi.ReportParameters.SERVICE_ID_TO_NAME_MAP, serviceNameParam);
        }

        return returnMap;
    }


    private void validateParams(Map params){
    String [] requiredParams = new String[]{JSONConstants.REPORT_TYPE, JSONConstants.ENTITY_TYPE, JSONConstants.REPORT_ENTITIES,
            JSONConstants.TimePeriodTypeKeys.TIME_PERIOD_MAIN,JSONConstants.GROUPINGS, JSONConstants.SUMMARY_CHART,
    JSONConstants.SUMMARY_REPORT, JSONConstants.REPORT_NAME};

        for(String s: requiredParams){
            if(!params.containsKey(s)) throw new IllegalArgumentException("Required param '"+s+"' is missing");
        }
    }

    /**
     * also determines if IS_CONTEXT_MAPPING param is true or not. This isn't needed for usage reports
     * @param clusterToReportParams
     * @param params
     * @throws ReportApi.ReportException
     */
    private void addMappingKeysAndValues(
            Map<String, Map<String, ReportApi.ReportSubmission.ReportParam>> clusterToReportParams,
            Map params) throws ReportException {
        //MAPPING_KEYS
        Object [] groupings = (Object[]) params.get(JSONConstants.GROUPINGS);
        if(groupings.length == 0){

            if(getReportType(params) == ReportApi.ReportType.USAGE_SUMMARY){
                throw new ReportException("Usage reports must have at least one grouping specified, per cluster");
            }
            
            for(Map.Entry<String, Map<String, ReportApi.ReportSubmission.ReportParam>> me: clusterToReportParams.entrySet()){
                //add default values
                ReportApi.ReportSubmission.ReportParam keysToFilterParam = new ReportApi.ReportSubmission.ReportParam();
                keysToFilterParam.setName(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);
                keysToFilterParam.setValue(new LinkedHashMap<String, List<ReportApi.FilterPair>>());
                me.getValue().put(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS, keysToFilterParam);

                //It's possible that even though there are no groupings that is context mapping should be true, as
                //operations may have been specified by the user. In this case only set the param, if it's not already set
                if(!me.getValue().containsKey(ReportApi.ReportParameters.IS_CONTEXT_MAPPING)){
                    ReportApi.ReportSubmission.ReportParam isCtxMappingParam = new ReportApi.ReportSubmission.ReportParam();
                    isCtxMappingParam.setName(ReportApi.ReportParameters.IS_CONTEXT_MAPPING);
                    isCtxMappingParam.setValue(false);
                    me.getValue().put(ReportApi.ReportParameters.IS_CONTEXT_MAPPING, isCtxMappingParam);
                }
            }
            return;
        }

        Map<String, LinkedHashMap<String, List<ReportApi.FilterPair>>> clusterToKeysToListFilterPairs
                = new HashMap<String, LinkedHashMap<String, List<ReportApi.FilterPair>>>();

        for(Object o: groupings){
            Map currentGrouping = (Map) o;
            validateSubMap(currentGrouping, JSONConstants.ReportMappings.ALL_KEYS);

            String clusterFound = (String) currentGrouping.get(JSONConstants.ReportMappings.CLUSTER_ID);
            if(!clusterToKeysToListFilterPairs.containsKey(clusterFound)){
                LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
                clusterToKeysToListFilterPairs.put(clusterFound, keysToFilterPairs);
            }

            String key = (String) currentGrouping.get(JSONConstants.ReportMappings.MESSAGE_CONTEXT_KEY);
            if(key.equals("")){
                throw new ReportException("Key value cannot be empty");
            }
            String value = (String) currentGrouping.get(JSONConstants.ReportMappings.CONSTRAINT);

            LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = clusterToKeysToListFilterPairs.get(clusterFound);
            if(!keysToFilterPairs.containsKey(key)){
                List<ReportApi.FilterPair> lFp = new ArrayList<ReportApi.FilterPair>();
                keysToFilterPairs.put(key, lFp);
                if(keysToFilterPairs.keySet().size() > MAX_KEYS_PER_CLUSTER){
                    throw new ReportException("The max number of mapping keys per cluster is 5");
                }
            }

            List<ReportApi.FilterPair> lFp = keysToFilterPairs.get(key);
            if(value.equals("")){
                lFp.add(new ReportApi.FilterPair());
            }else{
                lFp.add(new ReportApi.FilterPair(value));
            }
        }


        //make sure all clusters found match a cluster from the entities section
        for(String mappingClusterId: clusterToKeysToListFilterPairs.keySet()){
            if(!clusterToReportParams.containsKey(mappingClusterId)){
                throw new ReportException("Cluster id: " + mappingClusterId+" found in groupings section," +
                        " but does not exist in entities section");
            }
        }

        //iterate through based on the clusters we know about
        //this is important as otherwise we can not set the keys to filter pairs parameter on a cluster which has
        //had no mapping keys added for it
        for(String clusterId: clusterToReportParams.keySet()){

            Map<String, ReportApi.ReportSubmission.ReportParam> clusterParams = clusterToReportParams.get(clusterId);
            ReportApi.ReportSubmission.ReportParam keysToFilterParam = new ReportApi.ReportSubmission.ReportParam();
            keysToFilterParam.setName(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);

            if(!clusterToKeysToListFilterPairs.containsKey(clusterId)){
                //no keys for this cluster
                keysToFilterParam.setValue(new LinkedHashMap<String, List<ReportApi.FilterPair>>());
                clusterParams.put(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS, keysToFilterParam);
                continue;
            }

            keysToFilterParam.setValue(clusterToKeysToListFilterPairs.get(clusterId));
            clusterParams.put(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS, keysToFilterParam);

            boolean isContextMapping = clusterToKeysToListFilterPairs.get(clusterId).size() > 0;

            //does the ctx mapping param already exist?
            if(!clusterParams.containsKey(ReportApi.ReportParameters.IS_CONTEXT_MAPPING)){
                ReportApi.ReportSubmission.ReportParam isCtxMappingParam = new ReportApi.ReportSubmission.ReportParam();
                isCtxMappingParam.setName(ReportApi.ReportParameters.IS_CONTEXT_MAPPING);
                isCtxMappingParam.setValue(isContextMapping);
                clusterParams.put(ReportApi.ReportParameters.IS_CONTEXT_MAPPING, isCtxMappingParam);
            }else{
                ReportApi.ReportSubmission.ReportParam
                        isCtxMappingParam = clusterParams.get(ReportApi.ReportParameters.IS_CONTEXT_MAPPING);
                isCtxMappingParam.setValue(isContextMapping || (Boolean)isCtxMappingParam.getValue());
            }

        }

        //Usage reports MUST have at least one grouping
        if(getReportType(params) == ReportApi.ReportType.USAGE_SUMMARY
                || getReportType(params) == ReportApi.ReportType.USAGE_INTERVAL){

            for(Map.Entry<String, Map<String, ReportApi.ReportSubmission.ReportParam>> me:
                    clusterToReportParams.entrySet()){

                //check mapping_keys and use_user
                ReportApi.ReportSubmission.ReportParam keyToListParam =
                        me.getValue().get(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);

                if(keyToListParam == null){
                    throw new ReportException("Cluster: " + me.getKey()
                            +" must have at least one mapping key specified in the JSON data");
                }
                LinkedHashMap<String, List<ReportApi.FilterPair>>
                        keyToListMap = (LinkedHashMap<String, List<ReportApi.FilterPair>>) keyToListParam.getValue();
                if(keyToListMap.isEmpty()){
                    throw new ReportException("Cluster: " + me.getKey()
                            +" must have at least one mapping key specified in the JSON data");
                }
            }
        }

    }

    private void addTimeParameters(
            Map<String, Map<String, ReportApi.ReportSubmission.ReportParam>> clusterToReportParams, Map params)
            throws ReportException {

        Object o = params.get(JSONConstants.TimePeriodTypeKeys.TIME_PERIOD_MAIN);
        Map timePeriodMap = (Map) o;
        String type = (String) timePeriodMap.get(JSONConstants.TimePeriodTypeKeys.TYPE);
        ReportApi.ReportSubmission.ReportParam isRelative = new ReportApi.ReportSubmission.ReportParam();
        isRelative.setName(ReportApi.ReportParameters.IS_RELATIVE);
        ReportApi.ReportSubmission.ReportParam isAbsolute = new ReportApi.ReportSubmission.ReportParam();
        isAbsolute.setName(ReportApi.ReportParameters.IS_ABSOLUTE);

        String timeZone = (String) timePeriodMap.get(JSONConstants.TimePeriodAbsoluteKeys.TIME_ZONE);
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        if(!tz.getID().equals(timeZone)){
            throw new ReportException("Timezone: " + timeZone+" is not valid");
        }

        ReportApi.ReportSubmission.ReportParam timeZoneParam = new ReportApi.ReportSubmission.ReportParam();
        timeZoneParam.setName(ReportApi.ReportParameters.SPECIFIC_TIME_ZONE);
        timeZoneParam.setValue(timeZone);
        addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.SPECIFIC_TIME_ZONE, timeZoneParam);


        if(type.equals(JSONConstants.TimePeriodTypeValues.RELATIVE)){
            isRelative.setValue(true);
            isAbsolute.setValue(false);
            addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.IS_RELATIVE, isRelative);
            addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.IS_ABSOLUTE, isAbsolute);

            validateSubMap(timePeriodMap, JSONConstants.TimePeriodRelativeKeys.ALL_KEYS);

            String unitType = (String) timePeriodMap.get(JSONConstants.TimePeriodRelativeKeys.UNIT_OF_TIME);
            try{
                Utilities.getUnitFromString(unitType);
            }catch (IllegalArgumentException e){
                throw new ReportException(e.getMessage());
            }
            ReportApi.ReportSubmission.ReportParam relativeUnit = new ReportApi.ReportSubmission.ReportParam();
            relativeUnit.setName(ReportApi.ReportParameters.RELATIVE_TIME_UNIT);
            relativeUnit.setValue(unitType);
            addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.RELATIVE_TIME_UNIT, relativeUnit);

            String numberOfTimeUnits = (String) timePeriodMap.get(JSONConstants.TimePeriodRelativeKeys.NUMBER_OF_TIME_UNITS);
            ReportApi.ReportSubmission.ReportParam numberRelativeTimeUnits = new ReportApi.ReportSubmission.ReportParam();
            numberRelativeTimeUnits.setName(ReportApi.ReportParameters.RELATIVE_NUM_OF_TIME_UNITS);
            numberRelativeTimeUnits.setValue(Integer.valueOf(numberOfTimeUnits));
            addParamToAllClusters(clusterToReportParams,
                    ReportApi.ReportParameters.RELATIVE_NUM_OF_TIME_UNITS, numberRelativeTimeUnits);

        }else if(type.equals(JSONConstants.TimePeriodTypeValues.ABSOLUTE)){
            isRelative.setValue(false);
            isAbsolute.setValue(true);
            addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.IS_RELATIVE, isRelative);
            addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.IS_ABSOLUTE, isAbsolute);

            validateSubMap(timePeriodMap, JSONConstants.TimePeriodAbsoluteKeys.ALL_KEYS);

            String startTime = (String) timePeriodMap.get(JSONConstants.TimePeriodAbsoluteKeys.START);
            Long startTimeMilli = null;
            try{
                startTimeMilli = Utilities.getAbsoluteMilliSeconds(startTime, timeZone);
            }catch (ParseException ex){
                throw new ReportException
                        ("Cannot parse startTime: " + startTime+" must be in the format: " + Utilities.DATE_STRING);                
            }
            
            ReportApi.ReportSubmission.ReportParam absoluteStartTimeParam = new ReportApi.ReportSubmission.ReportParam();
            absoluteStartTimeParam.setName(ReportApi.ReportParameters.ABSOLUTE_START_TIME);
            absoluteStartTimeParam.setValue(startTime);
            addParamToAllClusters(clusterToReportParams,ReportApi.ReportParameters.ABSOLUTE_START_TIME,  absoluteStartTimeParam);

            String endTime = (String) timePeriodMap.get(JSONConstants.TimePeriodAbsoluteKeys.END);
            Long endTimeMilli = null;
            try{
                endTimeMilli = Utilities.getAbsoluteMilliSeconds(endTime, timeZone);
            }catch (ParseException ex){
                throw new ReportException
                        ("Cannot parse startTime: " + startTime+" must be in the format: " + Utilities.DATE_STRING);
            }

            if(startTimeMilli >= endTimeMilli){
                throw new ReportException("start time cannot be the same as or after the end time");
            }
            
            ReportApi.ReportSubmission.ReportParam absoluteEndTimeParam = new ReportApi.ReportSubmission.ReportParam();
            absoluteEndTimeParam.setName(ReportApi.ReportParameters.ABSOLUTE_END_TIME);
            absoluteEndTimeParam.setValue(endTime);
            addParamToAllClusters(clusterToReportParams, ReportApi.ReportParameters.ABSOLUTE_END_TIME, absoluteEndTimeParam);
        }else{
            throw new ReportException("Invalid json value for key:" + JSONConstants.TimePeriodTypeKeys.TYPE);
        }
    }

    protected void validateSubMap(Map subMap, String [] requiredKeys){
        for(String s: requiredKeys){
            if(!subMap.containsKey(s)) throw new IllegalArgumentException("Required param '"+s+"' is missing");                
        }
    }
}


