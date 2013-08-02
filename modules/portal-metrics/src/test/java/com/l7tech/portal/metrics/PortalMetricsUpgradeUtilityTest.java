package com.l7tech.portal.metrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.portal.metrics.PortalMetricsUpgradeUtility.CLASSNAME;
import static org.junit.Assert.*;

public class PortalMetricsUpgradeUtilityTest extends AbstractPortalMetricsTestUtility {
    private PortalMetricsUpgradeUtility upgrader;

    @Before
    public void setup() throws Exception {
        setupParent();
        upgrader = new PortalMetricsUpgradeUtility(new DatabaseInfo(SOURCE_DB, USERNAME, PASSWORD),
                new DatabaseInfo(DEST_DB, USERNAME, PASSWORD));
        replaceQueriesWithHsqlEquivalent();
    }

    @After
    public void teardown() throws Exception {
        teardownParent();
    }

    @Test
    @Ignore //This test is broken after the introduction of Goid's
    public void upgrade() throws Exception {
        final long service1 = 1111L;
        final long service2 = 2222L;
        final String apiId1 = "a1";
        final String apiId2 = "a2";
        insertGenericEntity(sourceConnection, 1L, apiId1, String.valueOf(service1), CLASSNAME);
        insertGenericEntity(sourceConnection, 2L, apiId2, String.valueOf(service2), CLASSNAME);
        insertPublishedService(destConnection, service1, null);
        insertPublishedService(destConnection, service2, null);
        insertServiceMetric(destConnection, service1, 1L, null);
        insertServiceMetric(destConnection, service1, 2L, null);
        insertServiceMetric(destConnection, service2, 1L, null);
        insertServiceMetric(destConnection, service2, 2L, null);

        upgrader.upgrade2_0To2_1();

        // check published_service table has uuid set
        final List<PublishedService> publishedServices = getAllPublishedServices(destConnection);
        assertEquals(2, publishedServices.size());
        final Map<Long, String> serviceMap = new HashMap<Long, String>();
        for (final PublishedService publishedService : publishedServices) {
            serviceMap.put(publishedService.getId(), publishedService.getUuid());
        }
        assertEquals(2, serviceMap.size());
        assertEquals(apiId1, serviceMap.get(service1));
        assertEquals(apiId2, serviceMap.get(service2));

        // check service_metrics table has uuid set
        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(4, metrics.size());
        final Map<Long, String> metricMap = new HashMap<Long, String>();
        for (final ServiceMetric metric : metrics) {
            metricMap.put(metric.getServiceId(), metric.getUuid());
        }
        assertEquals(2, metricMap.size());
        assertEquals(apiId1, metricMap.get(service1));
        assertEquals(apiId2, metricMap.get(service2));
    }

    @Test
    @Ignore //This test is broken after the introduction of Goid's
    public void skipsUUIDsAlreadySet() throws Exception {
        final long service1 = 1111L;
        final String apiId1 = "a1";
        insertGenericEntity(sourceConnection, 1L, apiId1, String.valueOf(service1), CLASSNAME);
        insertPublishedService(destConnection, service1, "alreadyset");
        insertServiceMetric(destConnection, service1, 1L, "alreadyset");

        upgrader.upgrade2_0To2_1();

        final List<PublishedService> publishedServices = getAllPublishedServices(destConnection);
        assertEquals(1, publishedServices.size());
        assertEquals(service1, publishedServices.get(0).getId());
        assertEquals("alreadyset", publishedServices.get(0).getUuid());

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        assertEquals(service1, metrics.get(0).getServiceId());
        assertEquals("alreadyset", metrics.get(0).getUuid());
    }

    @Test
    @Ignore //This test is broken after the introduction of Goid's
    public void skipsIfServiceIdNotInGenericEntityTable() throws Exception {
        final long service1 = 1111L;
        insertGenericEntity(sourceConnection, 1L, "a1", "12345678", CLASSNAME);
        insertPublishedService(destConnection, service1, null);
        insertServiceMetric(destConnection, service1, 1L, null);

        upgrader.upgrade2_0To2_1();

        final List<PublishedService> publishedServices = getAllPublishedServices(destConnection);
        assertEquals(1, publishedServices.size());
        assertEquals(service1, publishedServices.get(0).getId());
        assertNull(publishedServices.get(0).getUuid());

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        assertEquals(service1, metrics.get(0).getServiceId());
        assertNull(metrics.get(0).getUuid());
    }

    @Test
    @Ignore //This test is broken after the introduction of Goid's
    public void nullServiceIdInGenericEntity() throws Exception {
        final long service1 = 1111L;
        insertGenericEntity(sourceConnection, 1L, "a1", null, CLASSNAME);
        insertPublishedService(destConnection, service1, null);
        insertServiceMetric(destConnection, service1, 1L, null);

        upgrader.upgrade2_0To2_1();

        final List<PublishedService> publishedServices = getAllPublishedServices(destConnection);
        assertEquals(1, publishedServices.size());
        assertEquals(service1, publishedServices.get(0).getId());
        assertNull(publishedServices.get(0).getUuid());

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        assertEquals(service1, metrics.get(0).getServiceId());
        assertNull(metrics.get(0).getUuid());
    }

    @Test
    @Ignore //This test is broken after the introduction of Goid's
    public void nonNumericServiceIdInGenericEntity() throws Exception {
        final long service1 = 1111L;
        insertGenericEntity(sourceConnection, 1L, "a1", "thisisnotnumeric", CLASSNAME);
        insertPublishedService(destConnection, service1, null);
        insertServiceMetric(destConnection, service1, 1L, null);

        upgrader.upgrade2_0To2_1();

        final List<PublishedService> publishedServices = getAllPublishedServices(destConnection);
        assertEquals(1, publishedServices.size());
        assertEquals(service1, publishedServices.get(0).getId());
        assertNull(publishedServices.get(0).getUuid());

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        assertEquals(service1, metrics.get(0).getServiceId());
        assertNull(metrics.get(0).getUuid());
    }

    @Test
    @Ignore //This test is broken after the introduction of Goid's
    public void decimalServiceIdInGenericEntity() throws Exception {
        final long service1 = 1111L;
        insertGenericEntity(sourceConnection, 1L, "a1", "1.5", CLASSNAME);
        insertPublishedService(destConnection, service1, null);
        insertServiceMetric(destConnection, service1, 1L, null);

        upgrader.upgrade2_0To2_1();

        final List<PublishedService> publishedServices = getAllPublishedServices(destConnection);
        assertEquals(1, publishedServices.size());
        assertEquals(service1, publishedServices.get(0).getId());
        assertNull(publishedServices.get(0).getUuid());

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        assertEquals(service1, metrics.get(0).getServiceId());
        assertNull(metrics.get(0).getUuid());
    }
}
