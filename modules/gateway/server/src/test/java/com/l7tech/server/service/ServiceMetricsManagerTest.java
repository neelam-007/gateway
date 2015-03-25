package com.l7tech.server.service;

import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerTest;
import com.l7tech.util.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class ServiceMetricsManagerTest extends EntityManagerTest {
    private ServiceMetricsManager serviceMetricsManager;
    private Goid service1Goid = new Goid(123, 1);
    private String node1 = UUID.randomUUID().toString();

    @Before
    public void setUp() throws Exception {
        serviceMetricsManager = applicationContext.getBean("serviceMetricsManager", ServiceMetricsManager.class);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCreate() {
        MetricsBin metricsBin = new MetricsBin();
        metricsBin.setServiceGoid(service1Goid);
        metricsBin.setClusterNodeId(node1);
        metricsBin.setResolution(1);
        metricsBin.setPeriodStart(1L);
        metricsBin.setStartTime(1L);
        metricsBin.setInterval(1);
        metricsBin.setEndTime(1L);
        metricsBin.setNumAttemptedRequest(1);
        metricsBin.setNumAuthorizedRequest(1);
        metricsBin.setNumCompletedRequest(1);
        metricsBin.setMaxBackendResponseTime(1);
        metricsBin.setMinBackendResponseTime(1);
        metricsBin.setSumBackendResponseTime(1);
        metricsBin.setMaxFrontendResponseTime(1);
        metricsBin.setMinFrontendResponseTime(1);
        metricsBin.setSumFrontendResponseTime(1);
        metricsBin.setServiceState(ServiceState.ENABLED);

        ServiceMetrics.MetricsCollector metricsCollector = new ServiceMetrics.MetricsCollector(1);
        ServiceMetrics.MetricsCollectorSet metricsCollectorSet = new ServiceMetrics.MetricsCollectorSet(
                service1Goid,
                ServiceState.ENABLED,
                metricsCollector,
                CollectionUtils.MapBuilder.<ServiceMetrics.MetricsDetailKey, ServiceMetrics.MetricsCollector>builder()
                        .put(new ServiceMetrics.MetricsDetailKey("test", null, null), metricsCollector).map());

        serviceMetricsManager.doFlush(metricsCollectorSet, metricsBin);

        session.flush();
    }
}
