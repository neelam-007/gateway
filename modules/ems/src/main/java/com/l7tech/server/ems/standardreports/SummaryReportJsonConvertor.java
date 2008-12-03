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
                ReportApi.ReportSubmission.ReportParam mappingKeyParam = new ReportApi.ReportSubmission.ReportParam();
                mappingKeyParam.setName(ReportApi.ReportParameters.MAPPING_KEYS);
                mappingKeyParam.setValue(new ArrayList<String>());
                me.getValue().put(ReportApi.ReportParameters.MAPPING_KEYS, mappingKeyParam);

                ReportApi.ReportSubmission.ReportParam mappingValueParam = new ReportApi.ReportSubmission.ReportParam();
                mappingValueParam.setName(ReportApi.ReportParameters.MAPPING_VALUES);
                mappingValueParam.setValue(new ArrayList<String>());
                me.getValue().put(ReportApi.ReportParameters.MAPPING_VALUES, mappingValueParam);

                ReportApi.ReportSubmission.ReportParam mappingValueEqualOrLikeParam = new ReportApi.ReportSubmission.ReportParam();
                mappingValueEqualOrLikeParam.setName(ReportApi.ReportParameters.VALUE_EQUAL_OR_LIKE);
                mappingValueEqualOrLikeParam.setValue(new ArrayList<String>());
                me.getValue().put(ReportApi.ReportParameters.VALUE_EQUAL_OR_LIKE, mappingValueEqualOrLikeParam);

                ReportApi.ReportSubmission.ReportParam useUserParam = new ReportApi.ReportSubmission.ReportParam();
                useUserParam.setName(ReportApi.ReportParameters.USE_USER);
                useUserParam.setValue(false);
                me.getValue().put(ReportApi.ReportParameters.USE_USER, useUserParam);

                ReportApi.ReportSubmission.ReportParam authenticatedUsersParam = new ReportApi.ReportSubmission.ReportParam();
                authenticatedUsersParam.setName(ReportApi.ReportParameters.AUTHENTICATED_USERS);
                authenticatedUsersParam.setValue(new ArrayList<String>());
                me.getValue().put(ReportApi.ReportParameters.AUTHENTICATED_USERS, authenticatedUsersParam);

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
        
        Map<String, List<String>> clusterToKeys = new HashMap<String, List<String>>();
        Map<String, List<String>> clusterToValues = new HashMap<String, List<String>>();
        Map<String, List<String>> clusterToValuesEqualOrLike = new HashMap<String, List<String>>();
        Map<String, Set<String>> clusterToAuthUsers = new HashMap<String, Set<String>>();
        //For each cluster, track if auth user key has been set
        //used to determine whether to set is context mapping param to true or not
        Set<String> clusterAuthUserSet = new HashSet<String>();
        
        for(Object o: groupings){
            Map currentGrouping = (Map) o;
            validateSubMap(currentGrouping, JSONConstants.ReportMappings.ALL_KEYS);

            String clusterFound = (String) currentGrouping.get(JSONConstants.ReportMappings.CLUSTER_ID);
            if(!clusterToKeys.containsKey(clusterFound)){
                List<String> keys = new ArrayList<String>();
                clusterToKeys.put(clusterFound, keys);
                List<String> values = new ArrayList<String>();
                clusterToValues.put(clusterFound, values);
                List<String> valuesEqualOrLike = new ArrayList<String>();
                clusterToValuesEqualOrLike.put(clusterFound, valuesEqualOrLike);
                Set<String> authUsers = new HashSet<String>();
                clusterToAuthUsers.put(clusterFound, authUsers);
            }

            String key = (String) currentGrouping.get(JSONConstants.ReportMappings.MESSAGE_CONTEXT_KEY);
            if(key.equals("")){
                throw new ReportException("Key value cannot be empty");
            }
            String value = (String) currentGrouping.get(JSONConstants.ReportMappings.CONSTRAINT);

            if(key.equals(JSONConstants.AUTH_USER)){
                clusterAuthUserSet.add(clusterFound);
                Set<String> authUsers = clusterToAuthUsers.get(clusterFound);
                if(!value.equals("") ) authUsers.add(value);
            }else{
                List<String> clusterKeys = clusterToKeys.get(clusterFound);
                clusterKeys.add(key);
                List<String> clusterValuesEqualOrLike = clusterToValuesEqualOrLike.get(clusterFound);
                if(value.equals("")){
                    clusterValuesEqualOrLike.add(null);
                }else{
                    if(value.indexOf("*") != -1){
                        value = value.replaceAll("\\*", "%");
                        clusterValuesEqualOrLike.add("LIKE");
                    }else{
                        clusterValuesEqualOrLike.add("AND");                        
                    }
                }
                List<String> clusterValues = clusterToValues.get(clusterFound);
                //note value may be udpated above, with *'s replaced with %'s
                if(value.equals("")){
                    clusterValues.add(null);
                }else{
                    clusterValues.add(value);
                }
            }
        }

        for(Map.Entry<String, List<String>> me: clusterToKeys.entrySet()){

            if(!clusterToReportParams.containsKey(me.getKey())){
                throw new ReportException("Unknown cluster: " + me.getKey() +" found in grouping JSON");
            }
            Map<String, ReportApi.ReportSubmission.ReportParam> clusterParams = clusterToReportParams.get(me.getKey());

            Set<String> testKeySet = new HashSet<String>(me.getValue());
            if(testKeySet.size() != me.getValue().size()){
                throw new ReportException("Groups cannot contain duplicate keys on a per cluster basis");
            }
            ReportApi.ReportSubmission.ReportParam mappingKeyParam = new ReportApi.ReportSubmission.ReportParam();
            mappingKeyParam.setName(ReportApi.ReportParameters.MAPPING_KEYS);
            mappingKeyParam.setValue(me.getValue());
            clusterParams.put(ReportApi.ReportParameters.MAPPING_KEYS, mappingKeyParam);

            List<String> mappingValues = clusterToValues.get(me.getKey());
            ReportApi.ReportSubmission.ReportParam mappingValueParam = new ReportApi.ReportSubmission.ReportParam();
            mappingValueParam.setName(ReportApi.ReportParameters.MAPPING_VALUES);
            mappingValueParam.setValue(mappingValues);
            clusterParams.put(ReportApi.ReportParameters.MAPPING_VALUES, mappingValueParam);

            List<String> mappingValuesEqualOrLike = clusterToValuesEqualOrLike.get(me.getKey());
            ReportApi.ReportSubmission.ReportParam mappingValueEqualOrLikeParam = new ReportApi.ReportSubmission.ReportParam();
            mappingValueEqualOrLikeParam.setName(ReportApi.ReportParameters.VALUE_EQUAL_OR_LIKE);
            mappingValueEqualOrLikeParam.setValue(mappingValuesEqualOrLike);
            clusterParams.put(ReportApi.ReportParameters.VALUE_EQUAL_OR_LIKE, mappingValueEqualOrLikeParam);

            boolean useUser = clusterAuthUserSet.contains(me.getKey());
            ReportApi.ReportSubmission.ReportParam useUserParam = new ReportApi.ReportSubmission.ReportParam();
            useUserParam.setName(ReportApi.ReportParameters.USE_USER);
            useUserParam.setValue(useUser);
            clusterParams.put(ReportApi.ReportParameters.USE_USER, useUserParam);

            //AUTHENTICATED_USERS
            List<String> authUsers = new ArrayList<String>(clusterToAuthUsers.get(me.getKey()));
            ReportApi.ReportSubmission.ReportParam authenticatedUsersParam = new ReportApi.ReportSubmission.ReportParam();
            authenticatedUsersParam.setName(ReportApi.ReportParameters.AUTHENTICATED_USERS);
            authenticatedUsersParam.setValue(authUsers);
            clusterParams.put(ReportApi.ReportParameters.AUTHENTICATED_USERS, authenticatedUsersParam);


            boolean isContextMapping = (useUser || me.getValue().size() > 0);

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
                ReportApi.ReportSubmission.ReportParam mappingKeysParam = me.getValue().get(ReportApi.ReportParameters.MAPPING_KEYS);
                boolean mappingKeysOk = false;
                if(mappingKeysParam != null){
                    List<String> mappingKeys = (List<String>) mappingKeysParam.getValue();
                    if(!mappingKeys.isEmpty()){
                        mappingKeysOk = true;    
                    }

                }

                ReportApi.ReportSubmission.ReportParam useUserParam = me.getValue().get(ReportApi.ReportParameters.USE_USER);

                Boolean useUser = false;
                if(useUserParam != null){
                    useUser = (Boolean) useUserParam.getValue();    
                }

                if(!mappingKeysOk && !useUser){
                    throw new ReportException("Cluster: " + me.getKey() +" must have at least one mapping key specified in the JSON data");                    
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


