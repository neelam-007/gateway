package com.l7tech.server.service;

/**
 * Created with IntelliJ IDEA.
 * User: rseminoff
 * Date: 5/15/12
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class MockMetricsCollector extends ServiceMetrics.MetricsCollector {
    MockMetricsCollector(long startTime) {
        super(startTime);
        System.out.println("*** CALL *** MockMetricsCollector: constructor(long)");
    }
}
