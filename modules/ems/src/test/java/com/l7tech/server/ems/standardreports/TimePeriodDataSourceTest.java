/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 14, 2008
 * Time: 9:21:52 AM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;

import java.util.Date;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;
import org.junit.Test;

public class TimePeriodDataSourceTest extends TestCase {

    private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Utilities.DATE_STRING);
    /**
     * Checks that the data source contains the correct number of rows for the inputs supplied.
     * Checks the number of rows over 24 hours for an interval of 1, 2, 3 and 5 hours
     * @throws ParseException
     * @throws JRException
     */
    @Test
    public void testTimeSource_Hour() throws ParseException, JRException {
        String startDate = "2008/10/12 00:00";
        String endDate = "2008/10/13 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        TimePeriodDataSource dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.HOUR);

        int counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 24);

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.HOUR);
        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 12);

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.HOUR);
        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 8);

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                5, Utilities.HOUR);
        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 5);
    }

    /**
     * Checks that the data source contains the correct number of rows for the inputs supplied.
     * Checks the number of rows over the month of september for an interval of 1, 2, 3 and 7 days
     * @throws ParseException
     * @throws JRException
     */
    @Test
    public void testTimeSource_Day() throws ParseException, JRException {
        String startDate = "2008/09/01 00:00";
        String endDate = "2008/10/01 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        TimePeriodDataSource dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.DAY);

        int counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 30);

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.DAY);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 15);

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.DAY);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 10);

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                7, Utilities.DAY);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 5);
    }

    /**
     * Checks that the data source contains the correct number of rows for the inputs supplied.
     * Checks the number of rows over the months of August and September for an interval of 1, 2, 3 and 5 weeks
     * @throws ParseException
     * @throws JRException
     */
    @Test
    public void testTimeSource_Week() throws ParseException, JRException {
        String startDate = "2008/08/01 00:00";
        String endDate = "2008/10/01 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        TimePeriodDataSource dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.WEEK);

        int counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 9);


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.WEEK);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 5);


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.WEEK);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 3);

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                5, Utilities.WEEK);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 2);
    }

    /**
     * Checks that the data source contains the correct number of rows for the inputs supplied.
     * Checks the number of rows over the lats 12 months for an interval of 1, 2, 3 and 5 months
     * @throws ParseException
     * @throws JRException
     */
    @Test
    public void testTimeSource_Month() throws ParseException, JRException {
        String startDate = "2007/10/01 00:00";
        String endDate = "2008/10/01 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        TimePeriodDataSource dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.MONTH);

        int counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 12);


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.MONTH);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 6);


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.MONTH);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 4);
        

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                5, Utilities.MONTH);

        counter = 0;
        while(dataSource.next()){
            counter++;
        }

        assertTrue(counter == 3);
    }

    /**
     * Tests that the values returned for each row of the data source are correct for intervals
     * of 1,2,3 and 5 hours, over a 24 hour period
     * @throws ParseException
     * @throws JRException
     */
    @Test
    public void testTimeSourceFields_Hour() throws ParseException, JRException {
        String startDate = "2008/10/12 00:00";
        String endDate = "2008/10/13 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        TimePeriodDataSource dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.HOUR);

        long hourInMilli = 3600000L;
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + hourInMilli;
            assertTrue(intervalEndtime == end);
        }

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.HOUR);

        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (hourInMilli * 2);
            assertTrue(intervalEndtime == end);
        }

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.HOUR);

        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (hourInMilli * 3);
            assertTrue(intervalEndtime == end);
        }

        //5 doesn't go into 24, so the last interval is a remainder
        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                5, Utilities.HOUR);
        int counter = 0;
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (hourInMilli * 5);

            //the last interval has only 4 days 24/5 = 4 and 4 days over.
            if(counter < 4) assertTrue(intervalEndtime == end);
            else assertTrue((start + (hourInMilli * 4)) == end);
            counter++;
        }
    }

    /**
     * Tests that the values returned for each row of the data source are correct for intervals
     * of 1,2,3 and 7 days, over the month of September
     * @throws ParseException
     * @throws JRException
     */
    @Test
    public void testTimeSourceFields_Day() throws ParseException, JRException {
        String startDate = "2008/09/01 00:00";
        String endDate = "2008/10/01 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        TimePeriodDataSource dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.DAY);

        long dayInMilli = 86400000L; 
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + dayInMilli;
            assertTrue(intervalEndtime == end);
        }


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.DAY);

        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (dayInMilli * 2);
            assertTrue(intervalEndtime == end);
        }

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.DAY);

        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (dayInMilli * 3);
            assertTrue(intervalEndtime == end);
        }

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                7, Utilities.DAY);

        int counter = 0;
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (dayInMilli * 7);

            //the last interval has only 2 30/7=4 and 2 days over
            if(counter < 4) assertTrue(intervalEndtime == end);
            else assertTrue((start + (dayInMilli * 2)) == end);

            counter++;
        }
    }

    /**
     * Tests that the values returned for each row of the data source are correct for intervals
     * of 1,2,3 and 5 weeks, over the month of August September ,and 2 days in October, to make the time
     * period dividible by 7 days = 63 days
     * @throws ParseException
     * @throws JRException
     */
    @Test
    public void testTimeSourceFields_Week() throws ParseException, JRException {
        String startDate = "2008/08/01 00:00";
        String endDate = "2008/10/03 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        TimePeriodDataSource dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.WEEK);

        long weekInMilli = 604800000L;
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + weekInMilli;
            assertTrue(intervalEndtime == end);
        }


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.WEEK);

        int counter = 0;
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (weekInMilli * 2);
            if(counter < 4) assertTrue(intervalEndtime == end);
            else assertTrue((start + weekInMilli) == end); 
            counter++;
        }
        

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.WEEK);

        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (weekInMilli * 3);
            assertTrue(intervalEndtime == end);
        }


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                5, Utilities.WEEK);

        counter = 0;
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            long intervalEndtime = start + (weekInMilli * 5);

            //the first interval has 35 days and the last has 28

            if(counter < 1) assertTrue(intervalEndtime == end);
            else{
                long dayInMilli = 86400000L;
                assertTrue((start + (dayInMilli * 28)) == end);
            }
            counter++;
        }
    }
    
    /**
     * Tests that the values returned for each row of the data source are correct for intervals
     * of 1,2,3 and 5 months, over the last 12 months
     * @throws ParseException
     * @throws JRException
     */
    @Test
    public void testTimeSourceFields_Month() throws ParseException, JRException {
        String startDate = "2007/10/01 00:00";
        String endDate = "2008/10/01 00:00";
        Date d = DATE_FORMAT.parse(startDate);
        long timePeriodStartInclusive = d.getTime();
        d = DATE_FORMAT.parse(endDate);
        long timePeriodEndExclusive = d.getTime();

        TimePeriodDataSource dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                1, Utilities.MONTH);

        Calendar cal = Calendar.getInstance();
        
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            cal.setTimeInMillis(start);
            cal.add(Calendar.MONTH,1);

            assertTrue(cal.getTimeInMillis() == end);
        }


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                2, Utilities.MONTH);

        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            cal.setTimeInMillis(start);
            cal.add(Calendar.MONTH,2);

            assertTrue(cal.getTimeInMillis() == end);
        }


        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                3, Utilities.MONTH);

        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            cal.setTimeInMillis(start);
            cal.add(Calendar.MONTH,3);

            assertTrue(cal.getTimeInMillis() == end);
        }

        dataSource = new TimePeriodDataSource(timePeriodStartInclusive, timePeriodEndExclusive,
                5, Utilities.MONTH);

        int counter = 0;
        while(dataSource.next()){
            Long start = (Long) dataSource.getFieldValue(fieldStart);
            Long end = (Long) dataSource.getFieldValue(fieldEnd);

            assertTrue(start < end);

            cal.setTimeInMillis(start);


            if(counter < 2){
                cal.add(Calendar.MONTH,5);
                assertTrue(cal.getTimeInMillis() == end);
            }
            else{
                cal.add(Calendar.MONTH,2);
                assertTrue(cal.getTimeInMillis() == end);
            }
            counter++;
        }
    }
    
    private JRField fieldStart = new JRFieldAdapter(){
        public String getName() {
            return TimePeriodDataSource.INTERVAL_START;
        }
    };

    private JRField fieldEnd = new JRFieldAdapter(){
        public String getName() {
            return TimePeriodDataSource.INTERVAL_END;
        }
    };

    //class just to implement JRField so we can adapt it above
    private class JRFieldAdapter implements JRField{
        public String getDescription() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getName() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Class getValueClass() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getValueClassName() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setDescription(String s) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public JRPropertiesHolder getParentProperties() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public JRPropertiesMap getPropertiesMap() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean hasProperties() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object clone() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
