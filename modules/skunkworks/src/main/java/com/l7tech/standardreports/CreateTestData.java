/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 26, 2008
 * Time: 9:32:18 AM
 * Class to create service_metric data. Output is a .sql file which contains all the insert commands to be loaded
 * into an SSG > 4.7 database
 * If the unit of time supplied is DAY, then 12 months of daily metric bins are created. There is an output file
 * per service
 */
package com.l7tech.standardreports;

import com.l7tech.server.ems.standardreports.Utilities;

import java.io.*;
import java.util.*;

public class CreateTestData {

    private static int objectId = 700000;
    private Calendar cal;
    private int currentDay;
    private int timeUnitChanges;
    private int maxUnitChanges;
    private String nodeId;
    private final int hourlyResolution = 1;
    private final int dailyResolution = 2;
    private final long hourlyInterval = 3600000;
    private final long dailyInterval = 86400000;
    private final int attempted = 0;
    private final int authorized = 1;
    private final int completed = 2;
    private final int back_min = 0;
    private final int back_max = 1;
    private final int back_sum = 2;
    private final int front_min = 3;
    private final int front_max = 4;
    private final int front_sum = 5;
    private final String insert = "INSERT INTO service_metrics (objectid, nodeid, published_service_oid, resolution, " +
            "period_start, start_time, interval_size, end_time, attempted, authorized, " +
            "completed, back_min, back_max, back_sum, front_min, front_max, front_sum)" +
            " VALUES ( ";
    private int currentMonth;
    private Utilities.UNIT_OF_TIME unitOfTime;
    private String fileName;

    //mapping_type to mapping_key
    private final List<LinkedHashMap<String,String>> mappingKeys;
    private List<String> mappingKeyInsertList = new ArrayList<String>();
    private List<Integer> mappingKeyObjectIdList =  new ArrayList<Integer>();
    private final LinkedHashMap<String,List<String>> values;
    private final List<String> operations;
    private final List<String> authenticatedUser;
    private List<List<Map<Integer, String>>> valueInsertsList = new ArrayList<List<Map<Integer, String>>>();

    private static final String NODE_ID = "NODE_ID";
    /**
     *
     * @param nodeId
     * @param mappingKeys ONLY 1 key can be 'Custom Mapping'
     * @param values the key in the Map must be a key in mappingKeys, all len
     * @param operations
     */
    public CreateTestData(String nodeId, List<LinkedHashMap<String, String>> mappingKeys, LinkedHashMap<String, List<String>> values,
                          List<String> operations, List<String> authenticatedUser) {
        this.nodeId = nodeId;
        this.mappingKeys = mappingKeys;
        this.values = values;
        this.operations = operations;
        this.authenticatedUser = authenticatedUser;
        initMappingRecords();
        initValueRecords();
    }

    private void initMappingRecords(){
        for(LinkedHashMap<String, String> currentKeyMap: mappingKeys){
            objectId++;
            mappingKeyObjectIdList.add(objectId);

            StringBuilder sb = new StringBuilder("INSERT INTO message_context_mapping_keys (objectid, version, digested");
            for(int i = 0; i < currentKeyMap.keySet().size(); i++){
                sb.append(",mapping"+(i+1)+"_type,");
                sb.append("mapping"+(i+1)+"_key");
            }

            sb.append(") VALUES (" + objectId + ",1,\"NOT USED\"");

            for(String s: currentKeyMap.keySet()){
                sb.append(",\"" + s + "\",");
                sb.append("\""+currentKeyMap.get(s)+"\"");
            }
            sb.append(");");
            mappingKeyInsertList.add(sb.toString());
        }

    }

    private void initValueRecords(){
/*
  INSERT INTO message_context_mapping_values (  objectid, digested, mapping_keys_oid, service_operation, mapping1_value,
   mapping2_value, create_time) VALUES () 
*/

        for (int i1 = 0; i1 < mappingKeys.size(); i1++) {
            LinkedHashMap<String, String> currentKeyMap = mappingKeys.get(i1);
            List<Map<Integer, String>> currentValueListMap = new ArrayList<Map<Integer, String>>();
            objectId++;
            StringBuilder sb = new StringBuilder("INSERT INTO message_context_mapping_values (  objectid, digested, " +
                    "mapping_keys_oid, auth_user_id, service_operation");

            for (int i = 0; i < currentKeyMap.keySet().size(); i++) {
                sb.append(",mapping" + (i + 1) + "_value");
            }

            sb.append(",create_time) VALUES (");

            String halfInsert = sb.toString();

            List<List<String>> completeList = new ArrayList<List<String>>();
            addToList(completeList, operations);
            for (String s : currentKeyMap.keySet()) {
                addToList(completeList, values.get(s));
            }

            //Create and record the objectid to insert statement for each of the message_context_mapping_values
            for (List<String> list : completeList) {
                sb = new StringBuilder(halfInsert);
                int newValueKey = objectId++;
                sb.append(newValueKey + ",\"NOT USED\", " + mappingKeyObjectIdList.get(i1));
                if(objectId % 2 == 0){
                    sb.append(", '" + authenticatedUser.get(0) +"'");
                }else{
                    sb.append(", '" + authenticatedUser.get(1) +"'");
                }
                for (String s : list) {
                    sb.append(",\"" + s + "\"");
                }
                sb.append(", " + System.currentTimeMillis() + ");");
                Map<Integer, String> map = new HashMap<Integer, String>();
                map.put(new Integer(newValueKey), sb.toString());
                currentValueListMap.add(map);
            }

            valueInsertsList.add(currentValueListMap);

            //        for(Map<Integer, String> map: valueInsertsList){
            //            System.out.println(map.values().iterator().next()+" ");
            //        }
        }

    }

    private void addToList(List<List<String>> completeList, List<String> listToExpandWith){

        if(completeList.isEmpty()){
            for(String s: listToExpandWith){
                List<String> newList = new ArrayList<String>();
                newList.add(s);
                completeList.add(newList);
            }
            return;
        }

        int initialSize = 0;
        for(int i = 0; i < listToExpandWith.size(); i++){
            if(i == 0){
                initialSize = completeList.size();
                //udpate existing entries with first value
                for(List<String> list: completeList){
                    list.add(listToExpandWith.get(i));
                }
                continue;
            }
            //now add any new entries that need to be created, remembering all values already in lists
            //now add values to completeList

            List<List<String>> newLists = new ArrayList<List<String>>();
            for(int z = 0; z < completeList.size() && z < initialSize; z++){
                List<String> list = completeList.get(z);
                List<String> newList = new ArrayList<String>();
                for(int j = 0; j < list.size() - 1; j++){
                    newList.add(list.get(j));
                }
                newList.add(listToExpandWith.get(i));
                newLists.add(newList);
            }
//            initialSize += newLists.size();
            completeList.addAll(newLists);            
        }
    }

    public void createMappingData() throws Exception{
        BufferedWriter bw = null;

        try{
            File f = new File("MappingRecords.sql");
            if(!f.exists()) f.createNewFile();
            else{
                f.delete();
                f.createNewFile();
            }

            bw = new BufferedWriter(new FileWriter(f));
            for(String s: mappingKeyInsertList){
                bw.write(s + System.getProperty("line.separator"));    
            }

            for(List<Map<Integer, String>> listMap: valueInsertsList){
                for(Map<Integer, String> map: listMap){
                    bw.write(map.values().iterator().next() + System.getProperty("line.separator"));
                }
            }
            
        } finally {
            if(bw != null) {
                bw.flush();
                bw.close();
            }
        }
    }

    public void createMetricData(Utilities.UNIT_OF_TIME unitOfTime, String serviceId) throws Exception{

        timeUnitChanges = 0;
        if(unitOfTime == Utilities.UNIT_OF_TIME.HOUR){
            maxUnitChanges = 744;
            fileName = "HourlyData";
        }else if(unitOfTime == Utilities.UNIT_OF_TIME.DAY){
            maxUnitChanges = 365;
            fileName = "DailyData";
        }else{
            throw new IllegalArgumentException("Unsupported unit of time supplied: " + unitOfTime);
        }
        this.unitOfTime = unitOfTime;
        cal = Utilities.getCalendarForTimeUnit(unitOfTime);
        currentDay = cal.get(Calendar.DAY_OF_MONTH);
        currentMonth = cal.get(Calendar.MONTH);

        BufferedWriter bw = null;

        try{
            File f = new File(fileName + "_" + serviceId + ".sql");
            if(!f.exists()) f.createNewFile();
            else{
                f.delete();
                f.createNewFile();
            }

            bw = new BufferedWriter(new FileWriter(f));
            Object [] values = getValues(serviceId);
            while(values != null){
                StringBuilder sb = new StringBuilder(insert);
                for(int i = 0; i < values.length; i++){
                    if(i != 0) sb.append(", ");
                    sb.append(values[i]);
                }
                sb.append(");");
                bw.write(sb.toString() + System.getProperty("line.separator"));
                writeSubBins(Integer.parseInt(values[0].toString()), bw);
                bw.flush();
                values = getValues(serviceId);
            }
        } finally {
            if(bw != null) {
                bw.flush();
                bw.close();
            }
        }
    }

    private void writeSubBins(int serviceMetricId, BufferedWriter bw) throws IOException {
/*
        INSERT INTO service_metrics_details (objectid, service_metrics_oid, mapping_values_oid, attempted, authorized, " +
            "completed, back_min, back_max, back_sum, front_min, front_max, front_sum)" +
            " VALUES (  
*/

        String insertStr = "INSERT INTO service_metrics_details (service_metrics_oid, mapping_values_oid, " +
                "attempted, authorized, completed, back_min, back_max, back_sum, front_min, front_max, front_sum) " +
                " VALUES (";

        for(List<Map<Integer, String>> currentValueListMap: valueInsertsList){
            for(Map<Integer, String> map: currentValueListMap){
                StringBuffer sb = new StringBuffer(insertStr);
                sb.append(serviceMetricId);
                sb.append(", ").append(map.keySet().iterator().next());
                int [] tpStats = getThroughPutStats();
                for(int i: tpStats){
                    sb.append(", ").append(i);
                }
                int [] responseTimes = getResponseTimes();
                for(int i: responseTimes){
                    sb.append(", ").append(i);
                }
                sb.append(");");
                bw.write(sb.toString() + System.getProperty("line.separator"));
            }
        }
    }
    
    /**
     *
     * @param  serviceId
     * @return
     */
    private Object[] getValues(String serviceId){
        int resolutionToUse;
        long intervalToUse;
        if(unitOfTime == Utilities.UNIT_OF_TIME.HOUR){
            cal.add(Calendar.HOUR_OF_DAY, -1);
            resolutionToUse = hourlyResolution;
            intervalToUse = hourlyInterval;
        }else if(unitOfTime == Utilities.UNIT_OF_TIME.DAY){
            cal.add(Calendar.DAY_OF_MONTH, -1);
            resolutionToUse = dailyResolution;
            intervalToUse = dailyInterval;
        }else{
            throw new IllegalArgumentException("Unsupported unit of time supplied: " + unitOfTime);
        }

        timeUnitChanges++;
        if(timeUnitChanges >= maxUnitChanges) return null;

        int [] tpStats = getThroughPutStats();
        int [] responseTimes = getResponseTimes();
        Object [] values = new Object[]{objectId++,"'"+nodeId+"'", serviceId, resolutionToUse, cal.getTimeInMillis(), cal.getTimeInMillis(),
                intervalToUse, cal.getTimeInMillis() + intervalToUse , tpStats[attempted], tpStats[authorized],
                tpStats[completed], responseTimes[back_min], responseTimes[back_max], responseTimes[back_sum],
                responseTimes[front_min], responseTimes[front_max], responseTimes[front_sum]};

        return values;
    }

    /**
     * Get values for "attempted", "authorized" and "completed"
     * @return
     */
    private int[] getThroughPutStats(){
        int [] tpStats = new int[3];

        double seed = Math.random();
        int numAttempted = (int) (seed * 100);
        int numCompleted = (int) (numAttempted * seed);
        int totalFailures = numAttempted - numCompleted;
        int numAuthorized = (int) (totalFailures * seed);
        numAuthorized = numAuthorized + numCompleted;
//        int numAttempted = (int) (seed * 100);
//        int numAuthorized = (int) (numAttempted * seed);
//        ////int numCompleted = (numAttempted - numAuthorized > 0)?numAttempted - numAuthorized: 0;
//        int numCompleted = (int) (numAuthorized * seed);
        tpStats[attempted] = numAttempted;
        tpStats[authorized] = numAuthorized;
        tpStats[completed] = numCompleted;
//        System.out.println(seed + " " + numAttempted + " " + numAuthorized+ " " + numCompleted);
        return tpStats;
    }

    /**
     * Get values for "back_min", "back_max", "back_sum", "front_min", "front_max" and "front_sum"
     * @return
     */
    private int [] getResponseTimes(){
        int [] responseTimes = new int[6];
        double seed = Math.random();
        int backMin = (int) (seed * 10);
        backMin = (backMin == 0)? 1: backMin;
        int backMax = (int) (backMin + (seed * 10));
        int backSum = backMax * 10;

        int frontMin = backMin + (int)(seed * 10);
        int frontMax = (int) (frontMin + (seed * 10));
        int frontSum = frontMax * 10;

        responseTimes[back_min] = backMin;
        responseTimes[back_max] = backMax;
        responseTimes[back_sum] = backSum;
        responseTimes[front_min] = frontMin;
        responseTimes[front_max] = frontMax;
        responseTimes[front_sum] = frontSum;
        return responseTimes;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String [] args) throws Exception{
        System.out.println("Starting");
        FileInputStream fileInputStream = new FileInputStream("report.properties");
        Properties prop = new Properties();
        prop.load(fileInputStream);

        LinkedHashMap<String, String> nameToId = ReportApp.loadMapFromProperties(ReportApp.SERVICE_ID_TO_NAME, ReportApp.SERVICE_ID_TO_NAME_OID, prop);

        List<LinkedHashMap<String, String>> mappingKeyList = new ArrayList<LinkedHashMap<String, String>>();
        LinkedHashMap<String, String> mappingKeys;
        boolean empty = false;
        int index = 1;
        while(!empty){
            mappingKeys = ReportApp.loadMapFromProperties(index+"_MAPPING_KEY_TYPE", index+"_MAPPING_KEY_KEY", prop);
            empty = mappingKeys.isEmpty();
            if(!empty) mappingKeyList.add(mappingKeys);
            index++;
        }

        if(mappingKeyList.isEmpty()) throw new IllegalArgumentException("At least one key mapping row must be defined");
        
        LinkedHashMap<String, List<String>> values = new LinkedHashMap<String,  List<String>>();

        //Loads up all the custom mapping values which is determined by the map of keys above
        //Use the first map in mappingkeyList for key names
        LinkedHashMap<String, String> mappingKeyFirstMap = mappingKeyList.iterator().next();

        for(String mappingType: mappingKeyFirstMap.keySet()){
            List<String> mappingValueList = ReportApp.loadListFromProperties(mappingKeyFirstMap.get(mappingType), prop);
            values.put(mappingType,mappingValueList);
        }

        List<String> operations = ReportApp.loadListFromProperties("OPERATION", prop);
        List<String> authenticatedUser = ReportApp.loadListFromProperties("AUTH_USER_ID", prop);
        if(authenticatedUser.size() != 2) throw new IllegalArgumentException("Must be two and only two authenticated users");

        String nodeId = prop.getProperty(NODE_ID);
        CreateTestData testData = new CreateTestData(nodeId, mappingKeyList, values, operations, authenticatedUser);
        testData.createMappingData();
        for(String s: nameToId.values()){
            testData.createMetricData(Utilities.UNIT_OF_TIME.HOUR, s);
            testData.createMetricData(Utilities.UNIT_OF_TIME.DAY, s);
        }
        System.out.println("Finishing");
    }


}


