package com.l7tech.portal.metrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.*;

/**
 * This test replaces MySQL-specific syntax with HSQL equivalents.
 */
public class MySQLAggregateAndSyncUtilityTest extends AbstractPortalMetricsTestUtility {
    private static final int MINUTES = 5;
    private static final int INTERVAL_SIZE = MINUTES * MySQLAggregateAndSyncUtility.SECONDS_PER_MINUTE * MySQLAggregateAndSyncUtility.MILLISECONDS_PER_SECOND;
    private static final int FINE_INTERVAL_SIZE = 5 * MySQLAggregateAndSyncUtility.MILLISECONDS_PER_SECOND;
    private static final long SERVICE_ID = 1234L;
    private static final long MAPPING_VALUE_ID = 4L;
    private static final long MAPPING_KEY_ID = 5L;
    private static final String NODE_ID = "node";
    private static final int ATTEMPTED = 10;
    private static final int AUTHORIZED = 9;
    private static final int COMPLETED = 8;
    private static final int BACKMIN = 1;
    private static final int BACKMAX = 2;
    private static final int BACKSUM = 3;
    private static final int FRONTMIN = 4;
    private static final int FRONTMAX = 5;
    private static final int FRONTSUM = 6;
    private static final String UUID = "UUID1234";

    private MySQLAggregateAndSyncUtility utility;
    private long currentTime;

    @Before
    public void setup() throws Exception {
        setupParent();
        utility = new MySQLAggregateAndSyncUtility(new DatabaseInfo(SOURCE_DB, USERNAME, PASSWORD), new DatabaseInfo(DEST_DB, USERNAME, PASSWORD));
        replaceQueriesWithHsqlEquivalent();
        // hsql equivalent of mysql's 'IF' is 'CASEWHEN'
        MySQLAggregateAndSyncUtility.AGGREGATE_SERVICE_METRICS = MySQLAggregateAndSyncUtility.AGGREGATE_SERVICE_METRICS.replaceAll("IF", "CASEWHEN");
        MySQLAggregateAndSyncUtility.AGGREGATE_DETAILS = MySQLAggregateAndSyncUtility.AGGREGATE_DETAILS.replaceAll("IF", "CASEWHEN");
        currentTime = new GregorianCalendar().getTime().getTime();
        portalManagedServiceIds.put(SERVICE_ID,"23f9e9ec-3f0d-4d36-88ef-76d14eb711234");

    }

    @After
    public void teardown() throws Exception {
        teardownParent();
    }

    @Test
    public void nullServiceIds() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM,UUID);

        utility.aggregateAndSync(null, MINUTES);

        assertTrue(getAllServiceMetrics(destConnection).isEmpty());
    }

    @Test
    public void emptyServiceIds() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM,UUID);

        Map<Long,String> empty = new HashMap<Long,String>();
        utility.aggregateAndSync(empty, MINUTES);

        assertTrue(getAllServiceMetrics(destConnection).isEmpty());
    }

    /**
     * 1 metric + 1 detail
     */
    @Test
    public void singleRow() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        final long generatedKey = insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM,UUID);
        insertMappingKey(sourceConnection, MAPPING_KEY_ID);
        insertMappingValue(sourceConnection, MAPPING_VALUE_ID, MAPPING_KEY_ID);
        insertServiceMetricDetail(sourceConnection, generatedKey, MAPPING_VALUE_ID, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(start, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED, metric.getAttempted());
        assertEquals(AUTHORIZED, metric.getAuthorized());
        assertEquals(COMPLETED, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
        assertEquals(BACKSUM, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
        assertEquals(FRONTSUM, metric.getFrontSum());

        final List<ServiceMetricDetail> details = getAllDetails(destConnection);
        assertEquals(1, details.size());
        final ServiceMetricDetail detail = details.get(0);
        assertEquals(metric.getId(), detail.getServiceMetricsId());
        assertEquals(MAPPING_VALUE_ID, detail.getMappingValuesId());
        assertEquals(ATTEMPTED, detail.getAttempted());
        assertEquals(AUTHORIZED, detail.getAuthorized());
        assertEquals(COMPLETED, detail.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN), detail.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX), detail.getBackMax());
        assertEquals(BACKSUM, detail.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN), detail.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX), detail.getFrontMax());
        assertEquals(FRONTSUM, detail.getFrontSum());

        final List<PublishedService> publishedServices = getAllPublishedServices(destConnection);
        assertEquals(1, publishedServices.size());
        assertEquals(SERVICE_ID, publishedServices.get(0).getId());

        final List<MappingKey> mappingKeys = getAllMappingKeys(destConnection);
        assertEquals(1, mappingKeys.size());
        assertEquals(MAPPING_KEY_ID, mappingKeys.get(0).getId());

        final List<MappingValue> mappingValues = getAllMappingValues(destConnection);
        assertEquals(1, mappingValues.size());
        assertEquals(MAPPING_VALUE_ID, mappingValues.get(0).getId());

    }

    @Test
    public void singleRowDisabledService() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, true);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM,UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.DISABLED, metric.getServiceState());
        assertEquals(start, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED, metric.getAttempted());
        assertEquals(AUTHORIZED, metric.getAuthorized());
        assertEquals(COMPLETED, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
        assertEquals(BACKSUM, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
        assertEquals(FRONTSUM, metric.getFrontSum());
    }

    /**
     * 2 metrics + 4 details (2 details per metric) should be aggregated into 1 metric + 2 details.
     */
    @Test
    public void multipleRows() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertMappingKey(sourceConnection, MAPPING_KEY_ID);
        insertMappingValue(sourceConnection, MAPPING_VALUE_ID, MAPPING_KEY_ID);
        insertMappingValue(sourceConnection, MAPPING_VALUE_ID + 1, MAPPING_KEY_ID);
        final long generatedKey1 = insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED*2, AUTHORIZED*2, COMPLETED*2, BACKMIN - 1, BACKMAX + 1, BACKSUM, FRONTMIN - 1, FRONTMAX + 1, FRONTSUM,UUID);
        insertServiceMetricDetail(sourceConnection, generatedKey1, MAPPING_VALUE_ID, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM);
        insertServiceMetricDetail(sourceConnection, generatedKey1, MAPPING_VALUE_ID + 1, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM);
        final long generatedKey2 = insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED*2, AUTHORIZED*2, COMPLETED*2, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM,UUID);
        insertServiceMetricDetail(sourceConnection, generatedKey2, MAPPING_VALUE_ID, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM);
        insertServiceMetricDetail(sourceConnection, generatedKey2, MAPPING_VALUE_ID + 1, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 4, metric.getAttempted());
        assertEquals(AUTHORIZED * 4, metric.getAuthorized());
        assertEquals(COMPLETED * 4, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());

        final List<ServiceMetricDetail> details = getAllDetails(destConnection);
        assertEquals(2, details.size());
        final List<Long> foundMappingValueIds = new ArrayList<Long>();
        for (final ServiceMetricDetail detail : details) {
            assertEquals(metric.getId(), detail.getServiceMetricsId());
            foundMappingValueIds.add(detail.getMappingValuesId());
            assertEquals(ATTEMPTED * 2, detail.getAttempted());
            assertEquals(AUTHORIZED * 2, detail.getAuthorized());
            assertEquals(COMPLETED * 2, detail.getCompleted());
            assertEquals(Integer.valueOf(BACKMIN), detail.getBackMin());
            assertEquals(Integer.valueOf(BACKMAX), detail.getBackMax());
            assertEquals(BACKSUM * 2, detail.getBackSum());
            assertEquals(Integer.valueOf(FRONTMIN), detail.getFrontMin());
            assertEquals(Integer.valueOf(FRONTMAX), detail.getFrontMax());
            assertEquals(FRONTSUM * 2, detail.getFrontSum());
        }

        final List<PublishedService> publishedServices = getAllPublishedServices(destConnection);
        assertEquals(1, publishedServices.size());
        assertEquals(SERVICE_ID, publishedServices.get(0).getId());

        final List<MappingKey> mappingKeys = getAllMappingKeys(destConnection);
        assertEquals(1, mappingKeys.size());
        assertEquals(MAPPING_KEY_ID, mappingKeys.get(0).getId());

        final List<MappingValue> mappingValues = getAllMappingValues(destConnection);
        assertEquals(2, mappingValues.size());
        assertEquals(2, foundMappingValueIds.size());
        assertTrue(foundMappingValueIds.contains(MAPPING_VALUE_ID));
        assertTrue(foundMappingValueIds.contains(MAPPING_VALUE_ID + 1));
    }

    @Test
    public void singleRowNullBackMin() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, null, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM,UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(start, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED, metric.getAttempted());
        assertEquals(AUTHORIZED, metric.getAuthorized());
        assertEquals(COMPLETED, metric.getCompleted());
        assertNull(metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
        assertEquals(BACKSUM, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
        assertEquals(FRONTSUM, metric.getFrontSum());
    }

    @Test
    public void singleRowNullBackMax() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, null, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM,UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(start, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED, metric.getAttempted());
        assertEquals(AUTHORIZED, metric.getAuthorized());
        assertEquals(COMPLETED, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
        assertNull(metric.getBackMax());
        assertEquals(BACKSUM, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
        assertEquals(FRONTSUM, metric.getFrontSum());
    }

    @Test
    public void singleRowNullFrontMin() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, null, FRONTMAX, FRONTSUM,UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(start, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED, metric.getAttempted());
        assertEquals(AUTHORIZED, metric.getAuthorized());
        assertEquals(COMPLETED, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
        assertEquals(BACKSUM, metric.getBackSum());
        assertNull(metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
        assertEquals(FRONTSUM, metric.getFrontSum());
    }

    @Test
    public void singleRowNullFrontMax() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, null, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(start, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED, metric.getAttempted());
        assertEquals(AUTHORIZED, metric.getAuthorized());
        assertEquals(COMPLETED, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
        assertEquals(BACKSUM, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
        assertNull(metric.getFrontMax());
        assertEquals(FRONTSUM, metric.getFrontSum());
    }

    @Test
    public void singleRowUnableToDetermineServiceState() throws Exception {
        //purposely not inserting row into published_servivce table

        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertNull(metric.getServiceState());
        assertEquals(start, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED, metric.getAttempted());
        assertEquals(AUTHORIZED, metric.getAuthorized());
        assertEquals(COMPLETED, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
        assertEquals(BACKSUM, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
        assertEquals(FRONTSUM, metric.getFrontSum());
    }

    @Test
    public void multipleRowsBackSumOverMaxInteger() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN - 1, BACKMAX + 1, Integer.MAX_VALUE, FRONTMIN - 1, FRONTMAX + 1, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, 1, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 2, metric.getAttempted());
        assertEquals(AUTHORIZED * 2, metric.getAuthorized());
        assertEquals(COMPLETED * 2, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(Integer.MAX_VALUE, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());
    }

    @Test
    public void multipleRowsFrontSumOverMaxInteger() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN - 1, BACKMAX + 1, BACKSUM, FRONTMIN - 1, FRONTMAX + 1, Integer.MAX_VALUE, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, 1, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 2, metric.getAttempted());
        assertEquals(AUTHORIZED * 2, metric.getAuthorized());
        assertEquals(COMPLETED * 2, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(Integer.MAX_VALUE, metric.getFrontSum());
    }

    @Test
    public void multipleRowsAttemptedOverMaxInteger() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, Integer.MAX_VALUE, AUTHORIZED, COMPLETED, BACKMIN - 1, BACKMAX + 1, BACKSUM, FRONTMIN - 1, FRONTMAX + 1, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, 1, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(Integer.MAX_VALUE, metric.getAttempted());
        assertEquals(AUTHORIZED * 2, metric.getAuthorized());
        assertEquals(COMPLETED * 2, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());
    }

    @Test
    public void multipleRowsAuthorizedOverMaxInteger() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, Integer.MAX_VALUE, COMPLETED, BACKMIN - 1, BACKMAX + 1, BACKSUM, FRONTMIN - 1, FRONTMAX + 1, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, 1, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 2, metric.getAttempted());
        assertEquals(Integer.MAX_VALUE, metric.getAuthorized());
        assertEquals(COMPLETED * 2, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());
    }

    @Test
    public void multipleRowsCompletedOverMaxInteger() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, Integer.MAX_VALUE, BACKMIN - 1, BACKMAX + 1, BACKSUM, FRONTMIN - 1, FRONTMAX + 1, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, 1, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 2, metric.getAttempted());
        assertEquals(AUTHORIZED * 2, metric.getAuthorized());
        assertEquals(Integer.MAX_VALUE, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());
    }

    @Test
    public void multipleRowsNullBackMin() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX + 1, BACKSUM, FRONTMIN - 1, FRONTMAX + 1, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, null, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 2, metric.getAttempted());
        assertEquals(AUTHORIZED * 2, metric.getAuthorized());
        assertEquals(COMPLETED * 2, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());
    }

    @Test
    public void multipleRowsNullBackMax() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN - 1, BACKMAX, BACKSUM, FRONTMIN - 1, FRONTMAX + 1, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, null, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 2, metric.getAttempted());
        assertEquals(AUTHORIZED * 2, metric.getAuthorized());
        assertEquals(COMPLETED * 2, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());
    }

    @Test
    public void multipleRowsNullFrontMin() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN - 1, BACKMAX + 1, BACKSUM, FRONTMIN, FRONTMAX + 1, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, null, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 2, metric.getAttempted());
        assertEquals(AUTHORIZED * 2, metric.getAuthorized());
        assertEquals(COMPLETED * 2, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX + 1), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());
    }

    @Test
    public void multipleRowsNullFrontMax() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 2, currentTime - 2, currentTime - 1, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN - 1, BACKMAX + 1, BACKSUM, FRONTMIN - 1, FRONTMAX, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, currentTime - 1, currentTime - 1, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, null, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(1, metrics.size());
        final ServiceMetric metric = metrics.get(0);
        assertEquals(NODE_ID, metric.getNodeId());
        assertEquals(SERVICE_ID, metric.getServiceId());
        assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
        assertTrue(metric.getPeriodStart() > 0);
        assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
        assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
        assertEquals(currentTime - 2, metric.getStartTime());
        assertEquals(currentTime, metric.getEndTime());
        assertEquals(ATTEMPTED * 2, metric.getAttempted());
        assertEquals(AUTHORIZED * 2, metric.getAuthorized());
        assertEquals(COMPLETED * 2, metric.getCompleted());
        assertEquals(Integer.valueOf(BACKMIN - 1), metric.getBackMin());
        assertEquals(Integer.valueOf(BACKMAX + 1), metric.getBackMax());
        assertEquals(BACKSUM * 2, metric.getBackSum());
        assertEquals(Integer.valueOf(FRONTMIN - 1), metric.getFrontMin());
        assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
        assertEquals(FRONTSUM * 2, metric.getFrontSum());
    }

    @Test
    public void skipsRowsWithWrongServiceId() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID + 1, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        assertTrue(getAllServiceMetrics(destConnection).isEmpty());
    }

    @Test
    public void skipsRowsWithPeriodStartTooEarly() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - ((MINUTES + 1) * MySQLAggregateAndSyncUtility.SECONDS_PER_MINUTE * MySQLAggregateAndSyncUtility.MILLISECONDS_PER_SECOND);
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        assertTrue(getAllServiceMetrics(destConnection).isEmpty());
    }

    @Test
    public void skipsRowsWithWrongResolution() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        assertTrue(getAllServiceMetrics(destConnection).isEmpty());
    }

    @Test
    public void multiplePortalManagedServices() throws Exception {
        portalManagedServiceIds.put(SERVICE_ID + 1,"23f9e9ec-3f0d-4d36-88ef-76d14eb7"+SERVICE_ID+1);
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        insertPublishedService(sourceConnection, SERVICE_ID + 1, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);
        insertServiceMetric(sourceConnection, SERVICE_ID + 1, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(2, metrics.size());
        final List<Long> foundServiceIds = new ArrayList<Long>();
        for (final ServiceMetric metric : metrics) {
            foundServiceIds.add(metric.getServiceId());
            assertEquals(NODE_ID, metric.getNodeId());
            assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
            assertTrue(metric.getPeriodStart() > 0);
            assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
            assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
            assertEquals(start, metric.getStartTime());
            assertEquals(currentTime, metric.getEndTime());
            assertEquals(ATTEMPTED, metric.getAttempted());
            assertEquals(AUTHORIZED, metric.getAuthorized());
            assertEquals(COMPLETED, metric.getCompleted());
            assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
            assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
            assertEquals(BACKSUM, metric.getBackSum());
            assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
            assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
            assertEquals(FRONTSUM, metric.getFrontSum());
        }
        assertTrue(foundServiceIds.contains(SERVICE_ID));
        assertTrue(foundServiceIds.contains(SERVICE_ID + 1));
    }

    @Test
    public void multipleNodes() throws Exception {
        final String nodeId1 = NODE_ID + "a";
        final String nodeId2 = NODE_ID + "b";
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        final long generatedId1 = insertServiceMetric(sourceConnection, SERVICE_ID, nodeId1, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);
        final long generatedId2 = insertServiceMetric(sourceConnection, SERVICE_ID, nodeId2, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);
        insertMappingKey(sourceConnection, MAPPING_KEY_ID);
        insertMappingValue(sourceConnection, MAPPING_VALUE_ID, MAPPING_KEY_ID);
        insertServiceMetricDetail(sourceConnection, generatedId1, MAPPING_VALUE_ID, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM);
        insertServiceMetricDetail(sourceConnection, generatedId2, MAPPING_VALUE_ID, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM);
        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);

        // there should be one detail per node
        final List<ServiceMetric> metrics = getAllServiceMetrics(destConnection);
        assertEquals(2, metrics.size());
        final List<String> foundNodeIds = new ArrayList<String>();
        for (final ServiceMetric metric : metrics) {
            foundNodeIds.add(metric.getNodeId());
            assertEquals(SERVICE_ID, metric.getServiceId());
            assertEquals(MySQLAggregateAndSyncUtility.CUSTOM_RESOLUTION, metric.getResolution());
            assertTrue(metric.getPeriodStart() > 0);
            assertEquals(INTERVAL_SIZE, metric.getIntervalSize());
            assertEquals(MySQLAggregateAndSyncUtility.ENABLED, metric.getServiceState());
            assertEquals(start, metric.getStartTime());
            assertEquals(currentTime, metric.getEndTime());
            assertEquals(ATTEMPTED, metric.getAttempted());
            assertEquals(AUTHORIZED, metric.getAuthorized());
            assertEquals(COMPLETED, metric.getCompleted());
            assertEquals(Integer.valueOf(BACKMIN), metric.getBackMin());
            assertEquals(Integer.valueOf(BACKMAX), metric.getBackMax());
            assertEquals(BACKSUM, metric.getBackSum());
            assertEquals(Integer.valueOf(FRONTMIN), metric.getFrontMin());
            assertEquals(Integer.valueOf(FRONTMAX), metric.getFrontMax());
            assertEquals(FRONTSUM, metric.getFrontSum());
        }
        assertTrue(foundNodeIds.contains(nodeId1));
        assertTrue(foundNodeIds.contains(nodeId2));

        // there should be one detail per service_metric row
        final List<ServiceMetricDetail> details = getAllDetails(destConnection);
        assertEquals(2, details.size());
        final List<Long> foundMetricIds = new ArrayList<Long>();
        for (final ServiceMetricDetail detail : details) {
            foundMetricIds.add(detail.getServiceMetricsId());
            assertEquals(MAPPING_VALUE_ID, detail.getMappingValuesId());
            assertEquals(ATTEMPTED, detail.getAttempted());
            assertEquals(AUTHORIZED, detail.getAuthorized());
            assertEquals(COMPLETED, detail.getCompleted());
            assertEquals(Integer.valueOf(BACKMIN), detail.getBackMin());
            assertEquals(Integer.valueOf(BACKMAX), detail.getBackMax());
            assertEquals(BACKSUM, detail.getBackSum());
            assertEquals(Integer.valueOf(FRONTMIN), detail.getFrontMin());
            assertEquals(Integer.valueOf(FRONTMAX), detail.getFrontMax());
            assertEquals(FRONTSUM, detail.getFrontSum());
        }
        assertTrue(foundMetricIds.contains(generatedId1));
        assertTrue(foundMetricIds.contains(generatedId2));
    }

    @Test(expected = SyncException.class)
    public void cannotConnectToSource() throws Exception {
        utility = new MySQLAggregateAndSyncUtility(new DatabaseInfo("foo", USERNAME, PASSWORD), new DatabaseInfo(DEST_DB, USERNAME, PASSWORD));

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);
    }

    @Test(expected = SyncException.class)
    public void cannotConnectToDestination() throws Exception {
        utility = new MySQLAggregateAndSyncUtility(new DatabaseInfo(SOURCE_DB, USERNAME, PASSWORD), new DatabaseInfo("foo", USERNAME, PASSWORD));

        utility.aggregateAndSync(portalManagedServiceIds, MINUTES);
    }

    @Test(expected = SyncException.class)
    public void SQLError() throws Exception {
        insertPublishedService(sourceConnection, SERVICE_ID, false);
        final long start = currentTime - 1;
        insertServiceMetric(sourceConnection, SERVICE_ID, NODE_ID, start, start, currentTime, MySQLAggregateAndSyncUtility.FINE_RESOLUTION, FINE_INTERVAL_SIZE, ATTEMPTED, AUTHORIZED, COMPLETED, BACKMIN, BACKMAX, BACKSUM, FRONTMIN, FRONTMAX, FRONTSUM, UUID);

        // rename so insert fails
        destConnection.createStatement().execute("ALTER TABLE service_metrics RENAME TO foo");

        try {
            utility.aggregateAndSync(portalManagedServiceIds, MINUTES);
            fail("expected SyncException");
        } catch (final SyncException e) {
            // rename back so teardown succeeds
            destConnection.createStatement().execute("ALTER TABLE foo RENAME TO service_metrics");
            throw e;
        }
    }


}
