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

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Calendar;

public class CreateTestData {

    private final Calendar cal;
    private int currentDay;
    private int timeUnitChanges = 0;
    private int maxUnitChanges = 31;
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
    private final String insert = "INSERT INTO service_metrics (nodeid, published_service_oid, resolution, " +
            "period_start, start_time, interval_size, end_time, attempted, authorized, " +
            "completed, back_min, back_max, back_sum, front_min, front_max, front_sum)" +
            " VALUES ( ";
    private int currentMonth;
    private String unitOfTime;
    private final String fileName;

    public CreateTestData(String nodeId, String unitOfTime) {
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
        this.nodeId = nodeId;
    }

    public void createData(String serviceId) throws Exception{

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
                values = getValues(serviceId);
            }
        } finally {
            if(bw != null) {
                bw.flush();
                bw.close();
            }
        }
    }

    /**
     *
     * @param  serviceId
     * @return
     */
    public Object[] getValues(String serviceId){
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
        Object [] values = new Object[]{"'"+nodeId+"'", serviceId, resolutionToUse, cal.getTimeInMillis(), cal.getTimeInMillis(),
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
        int numCompleted = (numAttempted - numAuthorized > 0)?numAttempted - numAuthorized: 0;
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

    public static void main(String [] args) throws Exception{
//        String [] services = new String[]{"294912", "1933312"};

        for(String s: args){
            new CreateTestData("240869cda32562eb0287696033b4acca", Utilities.HOUR).createData(s);
            new CreateTestData("240869cda32562eb0287696033b4acca", Utilities.DAY).createData(s);
        }
    }
}

