/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 20, 2009
 * Time: 1:13:11 PM
 */
package com.l7tech.gateway.standardreports;

import org.junit.Test;
import org.junit.Assert;

import java.util.List;
import java.util.ArrayList;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRException;

class TestCountsImpl implements TestCounts {
    public void addIntCount() {
        intCount++;
    }

    public void addLongCount() {
        longCount++;
    }

    public void addStringCount() {
        stringCount++;
    }

    public int getIntCounts() {
        return intCount;
    }

    public int getLongCounts() {
        return longCount;
    }

    public int getStringCounts() {
        return stringCount;
    }

    private int intCount, longCount, stringCount;
}

public class TestPreparedStatementDataSource {

    @Test
    public void testConfigure() throws Exception {
        TestCounts tc = new TestCountsImpl();
        PreparedStatementDataSource psds = new PreparedStatementDataSource(new MockConnection(tc, 1, false));
        String sql = "? ? ?";
        List<Object> params = new ArrayList<Object>();
        params.add(1);
        params.add(24233L);
        params.add("String");

        psds.configure(sql, params);

        Assert.assertEquals(1, tc.getIntCounts());
        Assert.assertEquals(1, tc.getLongCounts());
        Assert.assertEquals(1, tc.getStringCounts());

        sql = "? ? ? ? ? ? ? ? ? ? ? ?";
        params.clear();
        params.add(1);
        params.add(1);
        params.add(1);
        params.add(24233L);
        params.add(24233L);
        params.add(24233L);
        params.add(24233L);
        params.add("String");
        params.add("String");
        params.add("String");
        params.add("String");
        params.add("String");

        tc = new TestCountsImpl();
        psds = new PreparedStatementDataSource(new MockConnection(tc, 1, false));
        psds.configure(sql, params);

        Assert.assertEquals(3, tc.getIntCounts());
        Assert.assertEquals(4, tc.getLongCounts());
        Assert.assertEquals(5, tc.getStringCounts());
    }

    @Test
    public void testConfigureExceptions() throws Exception {
        TestCounts tc = new TestCountsImpl();
        PreparedStatementDataSource psds = new PreparedStatementDataSource(new MockConnection(tc, 1, false));
        String sql = "? ? ? ?";
        List<Object> params = new ArrayList<Object>();
        params.add(1);
        params.add(24233L);
        params.add("String");

        boolean exception = false;
        try {
            psds.configure(sql, params);
        } catch (Exception e) {
            exception = true;
        }

        Assert.assertTrue("?s and param size do not match, so exception should be thrown", exception);

        //Don't support double
        params.add(new Double(0));
        //now the number of params match
        exception = false;
        try {
            psds.configure(sql, params);
        } catch (Exception e) {
            exception = true;
        }
        Assert.assertTrue("java.lang.Double is not supported as a param, so exception should be thrown", exception);
    }


    @Test
    public void testGetFieldValue() throws Exception {
        TestCounts tc = new TestCountsImpl();
        PreparedStatementDataSource psds =
                new PreparedStatementDataSource(
                        new MockConnection(tc, PreparedStatementDataSource.ColumnName.values().length, false));
        String sql = "? ? ?";
        List<Object> params = new ArrayList<Object>();
        params.add(1);
        params.add(24233L);
        params.add("String");

        psds.configure(sql, params);
        psds.next();

        PreparedStatementDataSource.ColumnName[] names = PreparedStatementDataSource.ColumnName.values();
        for (final PreparedStatementDataSource.ColumnName cn : names) {
            JRField testField = new JRFieldAdapter() {
                public String getName() {
                    return cn.getColumnName();
                }
            };

            Object o = psds.getFieldValue(testField);

            String expected = cn.getType().getName();
            String actual = o.getClass().getName();

            Assert.assertEquals("Expected class: " + expected + " should equal actual: " + actual, actual, expected);
        }
    }

    /**
     * next() in PreparedStatementDataSource should always close resources when it runs.
     * The normal case is when next() is exhausted. This test case ensures close() is called.
     * The exception case is when calling rs.next() causes an underlying SQLException. When this happens close()
     * should also always be called
     *
     * @throws Exception
     */
    @Test
    public void testNextClosesResources() throws Exception {

        TestCounts tc = new TestCountsImpl();
        int numRowsResultSet = 10;
        PreparedStatementDataSource psds = new PreparedStatementDataSource(new MockConnection(tc, numRowsResultSet, false));
        String sql = "? ? ?";
        List<Object> params = new ArrayList<Object>();
        params.add(1);
        params.add(24233L);
        params.add("String");

        psds.configure(sql, params);
        for (int i = 0; i < numRowsResultSet; i++) {
            psds.next();
        }

        Assert.assertTrue("close() should have been called", psds.isClosed());

        psds = new PreparedStatementDataSource(new MockConnection(tc, numRowsResultSet, true));
        psds.configure(sql, params);

        boolean exception = false;
        try {
            psds.next();
        } catch (JRException ex) {
            exception = true;
        }

        Assert.assertTrue("Exception should have been thrown", exception);

        Assert.assertTrue("close() should have been called due to exception", psds.isClosed());
    }

    /**
     * Tests that sub report data sources are clossed correctly
     */
    @Test
    public void testSubReportsAreClosed() throws Exception {
        TestCounts tc = new TestCountsImpl();
        int numRowsResultSet = 2;
        PreparedStatementDataSource psds = new PreparedStatementDataSource(new MockConnection(tc, numRowsResultSet, false));
        String sql = "? ? ?";
        List<Object> params = new ArrayList<Object>();
        params.add(1);
        params.add(24233L);
        params.add("String");

        psds.configure(sql, params);

        List<PreparedStatementDataSource> subDataSources = new ArrayList<PreparedStatementDataSource>();
        for (int i = 0; i < 10; i++) {
            PreparedStatementDataSource subReportDS = psds.getSubReportInstance();
            subDataSources.add(subReportDS);
            subReportDS.configure(sql, params);

            while (subReportDS.next()) ;
        }

        for (PreparedStatementDataSource subDS : subDataSources) {
            Assert.assertTrue("Sub report data source should have been closed", subDS.isClosed());
        }

    }
}
