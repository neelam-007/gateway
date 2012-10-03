package com.l7tech.portal.metrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static junit.framework.Assert.*;

/**
 * This test replaces MySQL-specific syntax with HSQL equivalents.
 */
public class MySQLSyncUtilityTest extends AbstractPortalMetricsTestUtility {
    private MySQLSyncUtility syncher;

    @Before
    public void setup() throws Exception {
        setupParent();
        syncher = new MySQLSyncUtility(new DatabaseInfo(SOURCE_DB, USERNAME, PASSWORD), new DatabaseInfo(DEST_DB, USERNAME, PASSWORD));
        syncher.setBatchSize(1);
        replaceQueriesWithHsqlEquivalent();
    }

    @After
    public void teardown() throws Exception {
        teardownParent();
    }

    @Test
    public void syncSinglePortalManagedService() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedId = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedId, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
    }

    @Test
    public void syncMultiplePortalManagedServices() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        portalManagedServiceIds.put(2L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f2");

        insertPublishedService(sourceConnection, 1);
        final long generatedId1 = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedId1, 1);

        insertPublishedService(sourceConnection, 2);
        final long generatedId2 = insertServiceMetric(sourceConnection, 2, 1);
        insertMappingKey(sourceConnection, 2);
        insertMappingValue(sourceConnection, 2, 2);
        insertServiceMetricDetail(sourceConnection, generatedId2, 2);

        assertNumRowsInSourceAndDestination(2, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(2, 2);
    }

    @Test
    public void syncMultipleRowsPerPortalManagedService() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        portalManagedServiceIds.put(2L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f2");

        insertPublishedService(sourceConnection, 1);
        final long generatedId1 = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedId1, 1);
        final long generatedId2 = insertServiceMetric(sourceConnection, 1, 2);
        insertServiceMetricDetail(sourceConnection, generatedId2, 1);

        insertPublishedService(sourceConnection, 2);
        final long generatedId3 = insertServiceMetric(sourceConnection, 2, 1);
        insertMappingKey(sourceConnection, 2);
        insertMappingValue(sourceConnection, 2, 2);
        insertServiceMetricDetail(sourceConnection, generatedId3, 2);
        final long generatedId4 = insertServiceMetric(sourceConnection, 2, 2);
        insertServiceMetricDetail(sourceConnection, generatedId4, 2);

        assertNumRows(sourceConnection, 4, 2, 2, 2);
        assertNumRows(destConnection, 0, 0, 0, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRows(sourceConnection, 4, 2, 2, 2);
        assertNumRows(destConnection, 4, 2, 2, 2);
    }

    @Test
    public void syncNoPortalManagedServices() throws Exception {
        portalManagedServiceIds.clear();

        // non portal managed service exists
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);

        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 0);
    }

    @Test
    public void syncIgnoresNonPortalManagedServices() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 2);
        final long generatedKey = insertServiceMetric(sourceConnection, 2, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 0);
    }

    @Test
    public void syncNonZeroPeriodStart() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        portalManagedServiceIds.put(2L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f2");

        // existing row
        insertPublishedService(destConnection, 1);
        final long generatedKey1 = insertServiceMetric(destConnection, 1, 1);
        insertMappingKey(destConnection, 1);
        insertMappingValue(destConnection, 1, 1);
        insertServiceMetricDetail(destConnection, generatedKey1, 1);

        // existing row
        insertPublishedService(sourceConnection, 1);
        final long generatedKey2 = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey2, 1);

        // new row
        insertPublishedService(sourceConnection, 2);
        final long generatedKey3 = insertServiceMetric(sourceConnection, 2, 2);
        insertMappingKey(sourceConnection, 2);
        insertMappingValue(sourceConnection, 2, 2);
        insertServiceMetricDetail(sourceConnection, generatedKey3, 2);

        assertNumRows(sourceConnection, 2, 2, 2);
        assertNumRows(destConnection, 1, 1, 1);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRows(sourceConnection, 2, 2, 2);
        assertNumRows(destConnection, 2, 2, 2);
    }

    @Test(expected = SyncException.class)
    public void syncCannotConnectToSource() throws Exception {
        syncher = new MySQLSyncUtility(new DatabaseInfo("invalid", USERNAME, PASSWORD), new DatabaseInfo(DEST_DB, USERNAME, PASSWORD));
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");

        syncher.syncHourlyData(portalManagedServiceIds);
    }

    @Test(expected = SyncException.class)
    public void syncCannotConnectToDestination() throws Exception {
        syncher = new MySQLSyncUtility(new DatabaseInfo(SOURCE_DB, USERNAME, PASSWORD), new DatabaseInfo( "invalid", USERNAME, PASSWORD));
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");

        syncher.syncHourlyData(portalManagedServiceIds);
    }

    @Test(expected = SyncException.class)
    public void syncSQLExceptionRollsBackTransaction() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);

        assertNumRows(sourceConnection, 1, 1, 1);
        assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.SERVICE_METRICS));
        assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.SERVICE_METRICS_DETAILS));
        assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.PUBLISHED_SERVICE));
        assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.MAPPING_VALUES));
        assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.MAPPING_KEYS));

        //change table name so SQL exception is thrown
        destConnection.createStatement().execute("ALTER TABLE SERVICE_METRICS RENAME TO BLAH");

        try {
            syncher.syncHourlyData(portalManagedServiceIds);
            fail("Expected SyncException.");
        } catch (final SyncException e) {
            // destination should not have changed
            destConnection.createStatement().execute("ALTER TABLE BLAH RENAME TO SERVICE_METRICS");
            assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.SERVICE_METRICS));
            assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.SERVICE_METRICS_DETAILS));
            assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.PUBLISHED_SERVICE));
            assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.MAPPING_VALUES));
            assertEquals(0, getNumRows(destConnection, MySQLSyncUtility.MAPPING_KEYS));
            throw (e);
        }
    }

    @Test
    public void syncNullPublishedServicePolicyOid() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1, null, 1L, false,"UUID1234");
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.PUBLISHED_SERVICE, "POLICY_OID");
    }

    @Test
    public void syncNullPublishedServiceFolderOid() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1, 1L, null, false,"UUID1234");
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.PUBLISHED_SERVICE, "FOLDER_OID");
    }

    @Test
    public void syncNullMappingKeyCreateTime() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1, null);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.MAPPING_KEYS, "CREATE_TIME");
    }

    @Test
    public void syncNullMappingValueCreateTime() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1, null);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.MAPPING_VALUES, "CREATE_TIME");
    }

    @Test
    public void syncNullServiceMetricsBackMin() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1, null, 1, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.SERVICE_METRICS, "BACK_MIN");
    }

    @Test
    public void syncNullServiceMetricsBackMax() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1, 1, null, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.SERVICE_METRICS, "BACK_MAX");
    }

    @Test
    public void syncNullServiceMetricsFrontMin() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1, 1, 1, null, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.SERVICE_METRICS, "FRONT_MIN");
    }

    @Test
    public void syncNullServiceMetricsFrontMax() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1, 1, 1, 1, null);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.SERVICE_METRICS, "FRONT_MAX");
    }

    @Test
    public void syncNullServiceMetricsDetailsBackMin() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1, null, 1, 1, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.SERVICE_METRICS_DETAILS, "BACK_MIN");
    }

    @Test
    public void syncNullServiceMetricsDetailsBackMax() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1, 1, null, 1, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.SERVICE_METRICS_DETAILS, "BACK_MAX");
    }

    @Test
    public void syncNullServiceMetricsDetailsFrontMin() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1, 1, 1, null, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.SERVICE_METRICS_DETAILS, "FRONT_MIN");
    }

    @Test
    public void syncNullServiceMetricsDetailsFrontMax() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedKey = insertServiceMetric(sourceConnection, 1, 1);
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedKey, 1, 1, 1, 1, null);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 1);
        assertColumnValueNull(destConnection, MySQLSyncUtility.SERVICE_METRICS_DETAILS, "FRONT_MAX");
    }

    @Test
    public void syncIgnoresNonHourlyMetrics() throws Exception {
        portalManagedServiceIds.put(1L,"23f9e9ec-3f0d-4d36-88ef-76d14eb715f1");
        insertPublishedService(sourceConnection, 1);
        final long generatedId = insertServiceMetric(sourceConnection, 1, "node", 1, 1, 1, MySQLSyncUtility.CUSTOM_RESOLUTION, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,"UUID1234");
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedId, 1);
        assertNumRowsInSourceAndDestination(1, 0);

        syncher.syncHourlyData(portalManagedServiceIds);

        assertNumRowsInSourceAndDestination(1, 0);
    }

    @Test
    public void assertCopyUUID() throws Exception{
        portalManagedServiceIds.put(1L,"UUID1234");
        insertPublishedService(sourceConnection, 1,"UUID1234");
        final long generatedId = insertServiceMetric(sourceConnection, 1, "node", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,"UUID1234");
        insertMappingKey(sourceConnection, 1);
        insertMappingValue(sourceConnection, 1, 1);
        insertServiceMetricDetail(sourceConnection, generatedId, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        syncher.syncHourlyData(portalManagedServiceIds);
//        assertColumnValueEq(destConnection,AbstractPortalMetricsUtility.SERVICE_METRICS_DETAILS,"uuid","UUID1234");
        assertColumnValueEq(destConnection,AbstractPortalMetricsUtility.SERVICE_METRICS,"uuid","UUID1234");
    }



    private void assertColumnValueNull(final Connection connection, final String tableName, final String columnName) throws Exception {
        final ResultSet resultSet = connection.createStatement().executeQuery("SELECT " + columnName + " FROM " + tableName);
        if (resultSet.next()) {
            assertNull(resultSet.getObject(columnName));
        } else {
            fail("Expected non-empty result set.");
        }


    }

    private void assertNumRowsInSourceAndDestination(final int expectedNumRowsInSource, final int expectedNumRowsInDestination) throws Exception {
        assertNumRows(sourceConnection, expectedNumRowsInSource, expectedNumRowsInSource, expectedNumRowsInSource);
        assertNumRows(destConnection, expectedNumRowsInDestination, expectedNumRowsInDestination, expectedNumRowsInDestination);
    }


    private void assertColumnValueEq(final Connection connection, final String tableName, final String columnName,final String value) throws Exception {
        final ResultSet resultSet = connection.createStatement().executeQuery("SELECT " + columnName + " FROM " + tableName);
        if (resultSet.next()) {
            assertEquals(value,resultSet.getString(columnName));
        } else {
            fail("Expected non-empty result set.");
        }
    }

}
