/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Category;
import com.l7tech.server.util.UptimeMonitor;

/**
 * Test the UptimeMonitor and the parsing in UptimeMetrics.
 *
 * User: mike
 * Date: Sep 16, 2003
 * Time: 5:56:19 PM
 */
public class UptimeMonitorTest extends TestCase {
    private static Category log = Category.getInstance(UptimeMonitorTest.class.getName());

    public UptimeMonitorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(UptimeMonitorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static void logUptimeMetrics(UptimeMetrics um) {
        log.info("Raw    : " + um);
        log.info("Parsed : " +
                 "uptime: " + um.getDays() + " days, " + um.getHours() + " hours, " + um.getMinutes() + " minutes.  " +
                 "load: " + um.getLoad1() + ", " + um.getLoad2() + ", " + um.getLoad3());
    }

    public void testUptimeMetrics() {
        long time = System.currentTimeMillis();
        UptimeMetrics um = new UptimeMetrics(" 11:22:20  up 28 days, 18:57,  1 user,  load average: 1.43, 8.33, 0.12\n", time );
        //logUptimeMetrics(um);
        assertTrue(um.getDays() == 28);
        assertTrue(um.getHours() == 18);
        assertTrue(um.getMinutes() == 57);
        assertTrue(um.getLoad1() > 1.42 && um.getLoad1() < 1.44);
        assertTrue(um.getLoad2() > 8.32 && um.getLoad2() < 8.34);
        assertTrue(um.getLoad3() > 0.11 && um.getLoad3() < 0.13);

        um = new UptimeMetrics("11:36AM  up 10 days, 14 hrs, 2 users, load averages: 102934.55, 8.09, 0.13\n", time );
        //logUptimeMetrics(um);
        assertTrue(um.getDays() == 10);
        assertTrue(um.getHours() == 14);
        assertTrue(um.getMinutes() == 0);
        assertTrue(um.getLoad1() > 102934.54 && um.getLoad1() < 102934.56);
        assertTrue(um.getLoad2() > 8.08 && um.getLoad2() < 8.10);
        assertTrue(um.getLoad3() > 0.12 && um.getLoad3() < 0.14);

        um = new UptimeMetrics("10:47am  up 27 day(s), 50 mins,  1 user,	load average: 0.18, 0.26, 0.20\n", time );
        //logUptimeMetrics(um);
        assertTrue(um.getDays() == 27);
        assertTrue(um.getHours() == 0);
        assertTrue(um.getMinutes() == 50);
        assertTrue(um.getLoad1() > 0.17 && um.getLoad1() < 0.19);
        assertTrue(um.getLoad2() > 0.25 && um.getLoad2() < 0.27);
        assertTrue(um.getLoad3() > 0.19 && um.getLoad3() < 0.21);

        um = new UptimeMetrics(" 11:47:39 up 5 days, 18:40,  0 users,  load average: 0.02, 0.40, 5.00\n", time );
        //logUptimeMetrics(um);
        assertTrue(um.getDays() == 5);
        assertTrue(um.getHours() == 18);
        assertTrue(um.getMinutes() == 40);
        assertTrue(um.getLoad1() > 0.01 && um.getLoad1() < 0.03);
        assertTrue(um.getLoad2() > 0.39 && um.getLoad2() < 0.41);
        assertTrue(um.getLoad3() > 4.99 && um.getLoad3() < 5.01);

        um = new UptimeMetrics(" 12:28:55  up  3:24,  4 users,  load average: 0.20, 0.35, 0.43\n", time );
        //logUptimeMetrics(um);
        assertTrue(um.getDays() == 0);
        assertTrue(um.getHours() == 3);
        assertTrue(um.getMinutes() == 24);
        assertTrue(um.getLoad1() > 0.19 && um.getLoad1() < 0.21);
        assertTrue(um.getLoad2() > 0.34 && um.getLoad2() < 0.36);
        assertTrue(um.getLoad3() > 0.42 && um.getLoad3() < 0.44);
    }

    public void testUptime() throws Exception {
        assertTrue(UptimeMonitor.isUptimeMetricsAvailable());
        log.info("Current uptime: " + UptimeMonitor.getLastUptime());
        Thread.sleep(500);
        UptimeMetrics um = UptimeMonitor.getLastUptime();
        assertTrue(um != null);
        logUptimeMetrics(um);
        UptimeMonitor.shutdownMonitorThread();
    }
}
