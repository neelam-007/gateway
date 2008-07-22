/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

/**
 * @author alex
 */
public class ServiceMetricsManagerTest extends TestCase {
    private ServiceMetricsManager metricsManager;

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
        System.out.println("Test complete: " + ServiceMetricsManagerTest.class);
    }

    protected void setUp() throws Exception {
        ApplicationContext springContext = null;//ApplicationContexts.getProdApplicationContext();
        metricsManager = (ServiceMetricsManager)springContext.getBean("serviceMetricsManager");
    }

    public void testSummarizeLatest() throws Exception {
        int hour = 60 * 60 * 1000;
        MetricsSummaryBin bin = metricsManager.summarizeLatest(null, null, MetricsBin.RES_FINE, hour, false);
        System.out.println(bin);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ServiceMetricsManagerTest.class);
        return suite;
    }

}
