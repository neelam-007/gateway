package com.l7tech.server.service;

import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.util.SyspropUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * @author alex
 */
@Ignore("Developer test")
public class ServiceMetricsManagerTest {
    private ServiceMetricsManager metricsManager;

    @Before
    public void setUp() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.server.home", "build/deploy/Gateway/node/default" );
        SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );
        ApplicationContext springContext = ApplicationContexts.getProdApplicationContext();
        metricsManager = springContext.getBean("serviceMetricsManager", ServiceMetricsManager.class);
    }

    @Test
    public void testSummarizeLatest() throws Exception {
        int hour = 60 * 60 * 1000;
        MetricsSummaryBin bin = metricsManager.summarizeLatest(null, null, MetricsBin.RES_FINE, hour, false);
        System.out.println(bin);
    }
}
