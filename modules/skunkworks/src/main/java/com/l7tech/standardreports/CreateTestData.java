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

    private static int objectId = 500000;
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
    private String unitOfTime;
    private String fileName;

    //mapping_type to mapping_key
    private final LinkedHashMap<String,String> mappingKeys;
    private String mappingKeyInsert;
    private int mappingKeyObjectId;
    private final LinkedHashMap<String,List<String>> values;
    private final List<String> operations;
    private List<Map<Integer, String>> valueInserts;

    /**
     *
     * @param nodeId
     * @param mappingKeys ONLY 1 key can be 'Custom Mapping'
     * @param values the key in the Map must be a key in mappingKeys, all len
     * @param operations
     */
    public CreateTestData(String nodeId, LinkedHashMap<String, String> mappingKeys, LinkedHashMap<String, List<String>> values,
                          List<String> operations) {
        this.nodeId = nodeId;
        this.mappingKeys = mappingKeys;
        this.values = values;
        this.operations = operations;
        initMappingRecords();
        initValueRecords();
    }

    private void initMappingRecords(){
        objectId++;
        mappingKeyObjectId = objectId;
        
        StringBuilder sb = new StringBuilder("INSERT INTO message_context_mapping_keys (objectid, version, digested");
        for(int i = 0; i < mappingKeys.keySet().size(); i++){
            sb.append(",mapping"+(i+1)+"_type,");
            sb.append("mapping"+(i+1)+"_key");
        }

        sb.append(") VALUES (" + mappingKeyObjectId + ",1,\"NOT USED\"");

        for(String s: mappingKeys.keySet()){
            sb.append(",\"" + s + "\",");
            sb.append("\""+mappingKeys.get(s)+"\"");
        }
        sb.append(");");
        mappingKeyInsert = sb.toString();
//        System.out.println(mappingKeyInsert);

    }

    private void initValueRecords(){
/*
  INSERT INTO message_context_mapping_values (  objectid, digested, mapping_keys_oid, service_operation, mapping1_value,
   mapping2_value, create_time) VALUES () 
*/
        objectId++;
        StringBuilder sb = new StringBuilder("INSERT INTO message_context_mapping_values (  objectid, digested, " +
                "mapping_keys_oid, service_operation");

        for(int i = 0; i < mappingKeys.keySet().size(); i++){
            sb.append(",mapping"+(i+1)+"_value");
        }

        sb.append(",create_time) VALUES (");

        String halfInsert = sb.toString();

        List<List<String>> completeList = new ArrayList<List<String>>();
        addToList(completeList, operations);
        for(String s: mappingKeys.keySet()){
            addToList(completeList, values.get(s));
        }

        valueInserts = new ArrayList<Map<Integer, String>>();
        //Create and record the objectid to insert statement for each of the message_context_mapping_values
        for(List<String> list: completeList){
            sb = new StringBuilder(halfInsert);
            int newValueKey = objectId++;
            sb.append( newValueKey + ",\"NOT USED\", " + mappingKeyObjectId);
            for(String s: list){
                sb.append(",\"" + s+"\"");
            }
            sb.append(", " + System.currentTimeMillis()+ ");");
            Map<Integer, String> map = new HashMap<Integer, String>();
            map.put(new Integer(newValueKey), sb.toString());
            valueInserts.add(map);
        }

//        for(Map<Integer, String> map: valueInserts){
//            System.out.println(map.values().iterator().next()+" ");
//        }

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
            bw.write(mappingKeyInsert);

            for(Map<Integer, String> map: valueInserts){
                bw.write(map.values().iterator().next() + System.getProperty("line.separator"));
            }
        } finally {
            if(bw != null) {
                bw.flush();
                bw.close();
            }
        }
    }

    public void createMetricData(String unitOfTime, String serviceId) throws Exception{

        timeUnitChanges = 0;
        if(unitOfTime.equals(Utilities.HOUR)){
            maxUnitChanges = 31;
            fileName = "HourlyData";
        }else if(unitOfTime.equals(Utilities.DAY)){
            maxUnitChanges = 12;
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

        String insertStr = "INSERT INTO service_metrics_details (objectid, service_metrics_oid, mapping_values_oid, " +
                "attempted, authorized, completed, back_min, back_max, back_sum, front_min, front_max, front_sum) " +
                " VALUES (";

        for(Map<Integer, String> map: valueInserts){
            StringBuffer sb = new StringBuffer(insertStr);
            sb.append(objectId++);
            sb.append(", ").append(serviceMetricId);
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
    
    /**
     *
     * @param  serviceId
     * @return
     */
    private Object[] getValues(String serviceId){
        int resolutionToUse;
        long intervalToUse;
        if(unitOfTime.equals(Utilities.HOUR)){
            cal.add(Calendar.HOUR_OF_DAY, -1);
            if(cal.get(Calendar.DAY_OF_MONTH) != currentDay){
                currentDay = cal.get(Calendar.DAY_OF_MONTH);
                timeUnitChanges++;
                if(timeUnitChanges >= maxUnitChanges) return null;
            }
            resolutionToUse = hourlyResolution;
            intervalToUse = hourlyInterval;
        }else if(unitOfTime.equals(Utilities.DAY)){
            cal.add(Calendar.DAY_OF_MONTH, -1);
            if(cal.get(Calendar.MONTH) != currentMonth){
                currentMonth = cal.get(Calendar.MONTH);
                timeUnitChanges++;
                if(timeUnitChanges >= maxUnitChanges) return null;
            }
            resolutionToUse = dailyResolution;
            intervalToUse = dailyInterval;
        }else{
            throw new IllegalArgumentException("Unsupported unit of time supplied: " + unitOfTime);
        }

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
        int numAuthorized = (int) (numAttempted * seed);
        //int numCompleted = (numAttempted - numAuthorized > 0)?numAttempted - numAuthorized: 0;
        int numCompleted = (int) (numAuthorized * seed);
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

    private static LinkedHashMap<String, String> loadMapFromProperties(String key1, String key2, Properties prop){

        LinkedHashMap<String, String> returnMap = new LinkedHashMap<String,String>();
        String key1Name = prop.getProperty(key1+"_1");
        String key2Name = prop.getProperty(key2+"_1");
        int index = 2;

        while(key1Name != null && key2Name != null){
            returnMap.put(key1Name, key2Name);
            key1Name = prop.getProperty(key1+"_"+index);
            key2Name = prop.getProperty(key2+"_"+index);
            index++;
        }

        return returnMap;
    }

    private static List<String> loadListFromProperties(String key, Properties prop){

        List<String> returnList = new ArrayList<String>();
        String key1Name = prop.getProperty(key+"_1");
        int index = 2;

        while(key1Name != null){
            returnList.add(key1Name);
            key1Name = prop.getProperty(key+"_"+index);
            index++;
        }

        return returnList;
    }

    public static void main(String [] args) throws Exception{
        FileInputStream fileInputStream = new FileInputStream("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/report.properties");
        Properties prop = new Properties();
        prop.load(fileInputStream);

        LinkedHashMap<String, String> nameToId = loadMapFromProperties(ReportApp.SERVICE_ID_TO_NAME, ReportApp.SERVICE_ID_TO_NAME_OID, prop);
        LinkedHashMap<String, String> mappingKeys = loadMapFromProperties("MAPPING_KEY_TYPE", "MAPPING_KEY_KEY", prop);


        LinkedHashMap<String, List<String>> values = new LinkedHashMap<String,  List<String>>();

        //Loads up all the custom mapping values which is determined by the map of keys above
        for(String mappingType: mappingKeys.keySet()){
            List<String> mappingValueList = loadListFromProperties(mappingKeys.get(mappingType), prop);
            values.put(mappingType,mappingValueList);
        }

        List<String> operations = loadListFromProperties("OPERATION", prop);

        CreateTestData testData = new CreateTestData("240869cda32562eb0287696033b4acca", mappingKeys, values, operations);
        testData.createMappingData();
        for(String s: nameToId.values()){
            testData.createMetricData(Utilities.HOUR, s);
            testData.createMetricData(Utilities.DAY, s);
        }
    }


}


