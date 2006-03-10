/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.common.ApplicationContexts;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.service.MetricsBin;
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
        ApplicationContext springContext = ApplicationContexts.getProdApplicationContext();
        metricsManager = (ServiceMetricsManager)springContext.getBean("serviceMetricsManager");
    }

    public void testGetMetricsSummary() throws Exception {
        long hour = 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        MetricsBin bin = metricsManager.getMetricsSummary(MetricsBin.RES_FINE, now - hour, (int)hour, null, null);
        System.out.println(bin);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ServiceMetricsManagerTest.class);
        return suite;
    }

}
