/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 29, 2008
 * Time: 11:28:20 AM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRException;

import java.util.List;
import java.util.ArrayList;

public class TimePeriodDataSource implements JRDataSource{

    public final static String INTERVAL_START = "INTERVAL_START";
    public final static String INTERVAL_END = "INTERVAL_END";

    private final List<Long> intervals;
    private int intervalIndex = 0;
    private boolean first = true;

    public TimePeriodDataSource(long timePeriodStart, long timePeriodEnd, int intervalNumberOfUnits, Utilities.UNIT_OF_TIME intervalUnitOfTime ) {
        intervals = Utilities.getIntervalsForTimePeriod(timePeriodStart, timePeriodEnd, intervalNumberOfUnits, intervalUnitOfTime );
    }

    public TimePeriodDataSource(){
        intervals = new ArrayList<Long>();
    }
    public Object getFieldValue(JRField jrField) throws JRException {
        String fieldName = jrField.getName();
        if(fieldName.equals(INTERVAL_START)){
            return intervals.get(intervalIndex);
        }else if(fieldName.equals(INTERVAL_END)){
            return intervals.get(intervalIndex + 1);
        }else{
            throw new JRException("Unknown field: " + fieldName);
        }
    }

    /**
     * The first call to next does nothing. It can be though of as moving the cursor onto the first row however
     * it's not required. This behaviour was implemented so a loop of while(dataSource.next()) can be written without
     * worrying about the first record, and at the same time if getFieldValue is called before next is called the
     * data source will behave correctly.
     * next() moves the cursor forward on the second call
     * @return
     * @throws JRException
     */
    public boolean next() throws JRException {
        if(first){
            first = false;
            return intervals.size() > 0;
        }
        //-2 because the last interval is intervals.get(size() -2) - intervals.get(size() - 1)
        if(intervalIndex < intervals.size()-2){
            intervalIndex++;
            return true;
        }
        return false;
    }
}

