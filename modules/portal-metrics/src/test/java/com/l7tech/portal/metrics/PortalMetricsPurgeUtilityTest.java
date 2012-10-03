package com.l7tech.portal.metrics;

import org.apache.commons.lang.time.DateUtils;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static junit.framework.Assert.fail;

public class PortalMetricsPurgeUtilityTest extends AbstractPortalMetricsTestUtility {
    private static final String CONNECTION_URL = "jdbc:hsqldb:mem:mydb";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";
    private static final int NUM_DAYS = 5;
    private static final int RESOLUTION = 1;
    private Calendar calendar;
    private PortalMetricsPurgeUtility purger;
    private Connection connection;

    @Before
    public void setup() throws Exception {
        setupParent();
        final JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setDatabase(CONNECTION_URL);
        dataSource.setUser(USERNAME);
        dataSource.setPassword(PASSWORD);
        purger = new PortalMetricsPurgeUtility(dataSource);
        purger.setBatchSize(1);
        connection = DriverManager.getConnection(CONNECTION_URL);
        createTables(connection);
        // Jan 1 2000 0:0:0
        calendar = new GregorianCalendar(2000, 0, 1, 0, 0, 0);
        replaceQueriesWithHsqlEquivalent();
    }

    @After
    public void teardown() throws Exception {
        teardownParent();
        dropTables(connection);
        connection.close();
    }

    @Test
    public void purgeMultiple() throws Exception {
        final long periodStart = DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime();

        insertPublishedService(connection, 1);
        final long generatedKey = insertServiceMetric(connection, 1, periodStart);
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);

        insertPublishedService(connection, 2);
        final long generatedKey2 = insertServiceMetric(connection, 2, periodStart);
        insertMappingKey(connection, 2);
        insertMappingValue(connection, 2, 2);
        insertServiceMetricDetail(connection, generatedKey2, 2);

        assertNumRows(connection, 2, 2, 2);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);

        assertNumRows(connection, 0, 0, 0);
    }

    @Test
    public void purgePeriodStartOnEndLimit() throws Exception {
        insertPublishedService(connection, 1);
        final long periodStart = DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime();
        final long generatedKey = insertServiceMetric(connection, 1, periodStart);
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);
        assertNumRows(connection, 1, 1, 1);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);

        assertNumRows(connection, 0, 0, 0);
    }

    @Test
    public void purgePeriodStartBeforeEndLimit() throws Exception {
        insertPublishedService(connection, 1);
        final long periodStart = DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime() - 1;
        final long generatedKey = insertServiceMetric(connection, 1, periodStart);
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);
        assertNumRows(connection, 1, 1, 1);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);

        assertNumRows(connection, 0, 0, 0);
    }

    @Test
    public void purgePeriodStartAfterEndLimit() throws Exception {
        insertPublishedService(connection, 1);
        final long periodStart = DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime() + 1;
        final long generatedKey = insertServiceMetric(connection, 1, periodStart);
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);
        assertNumRows(connection, 1, 1, 1);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);

        assertNumRows(connection, 1, 1, 1);
    }

    @Test
    public void purgeNoRows() throws Exception {
        assertNumRows(connection, 0, 0, 0);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);

        assertNumRows(connection, 0, 0, 0);
    }

    @Test
    public void purgeDoesNotDeletePublishedServicesThatAreStillReferenced() throws Exception {
        insertPublishedService(connection, 1);

        // should be purged
        final long generatedKey = insertServiceMetric(connection,1, DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime());
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);

        // should not be purged
        final long generatedKey2 = insertServiceMetric(connection, 1, DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime() + 1);
        insertMappingKey(connection, 2);
        insertMappingValue(connection, 2, 2);
        insertServiceMetricDetail(connection, generatedKey2, 2);

        assertNumRows(connection, 2, 2, 1);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);

        assertNumRows(connection, 1, 1, 1);
    }

    @Test
    public void purgeDoesNotDeleteKeysThatAreStillReferenced() throws Exception {
        insertMappingKey(connection, 1);

        // should be purged
        insertPublishedService(connection, 1);
        final long generatedKey = insertServiceMetric(connection, 1, DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime());
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);

        // should not be purged
        insertPublishedService(connection, 2);
        final long generatedKey2 = insertServiceMetric(connection, 2, DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime() + 1);
        insertMappingValue(connection, 2, 1);
        insertServiceMetricDetail(connection, generatedKey2, 2);

        assertNumRows(connection, 2, 1, 2, 2);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);

        assertNumRows(connection, 1, 1, 1, 1);
    }

    @Test
    public void purgeDoesNotDeleteKeysOrValuesThatAreStillReferenced() throws Exception {
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);

        // should be purged
        insertPublishedService(connection, 1);
        final long generatedKey = insertServiceMetric(connection, 1, DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime());
        insertServiceMetricDetail(connection, generatedKey, 1);

        // should not be purged
        insertPublishedService(connection, 2);
        final long generatedKey2 = insertServiceMetric(connection, 2, DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime() + 1);
        insertServiceMetricDetail(connection, generatedKey2, 1);

        assertNumRows(connection, 2, 1, 2);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);

        assertNumRows(connection, 1, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void purgeDaysBeforeLessThanZero() throws Exception {
        insertPublishedService(connection, 1);
        final long generatedKey = insertServiceMetric(connection, 1, calendar.getTime().getTime());
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);
        assertNumRows(connection, 1, 1, 1);

        try {
            purger.purge(-1, calendar.getTime(), RESOLUTION);
            fail("expected Illegal Argument Exception");
        } catch (final IllegalArgumentException e) {
            assertNumRows(connection, 1, 1, 1);
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void purgeDaysBeforeEqualToZero() throws Exception {
        insertPublishedService(connection, 1);
        final long generatedKey = insertServiceMetric(connection, 1, calendar.getTime().getTime());
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);
        assertNumRows(connection, 1, 1, 1);

        try {
            purger.purge(0, calendar.getTime(), RESOLUTION);
            fail("expected Illegal Argument Exception");
        } catch (final IllegalArgumentException e) {
            assertNumRows(connection, 1, 1, 1);
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void purgeNullDate() throws Exception {
        insertPublishedService(connection, 1);
        final long generatedKey = insertServiceMetric(connection, 1, calendar.getTime().getTime());
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);
        assertNumRows(connection, 1, 1, 1);

        try {
            purger.purge(NUM_DAYS, null, RESOLUTION);
            fail("expected Illegal Argument Exception");
        } catch (final IllegalArgumentException e) {
            assertNumRows(connection, 1, 1, 1);
            throw e;
        }
    }

    @Test(expected = PortalMetricsPurgeUtility.PurgeException.class)
    public void purgeCannotConnectToDatabase() throws Exception {
        final JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setDatabase("invalid");
        purger = new PortalMetricsPurgeUtility(dataSource);

        purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);
    }

    @Test(expected = PortalMetricsPurgeUtility.PurgeException.class)
    public void purgeSQLExceptionRollsBackTransaction() throws Exception {
        insertPublishedService(connection, 1);
        final long periodStart = DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime();
        final long generatedKey = insertServiceMetric(connection, 1, periodStart);
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);
        assertNumRows(connection, 1, 1, 1);

        // change col name so SQL Exception will be thrown
        connection.createStatement().execute("ALTER TABLE MESSAGE_CONTEXT_MAPPING_KEYS ALTER COLUMN OBJECTID RENAME TO INVALID");

        try {
            purger.purge(NUM_DAYS, calendar.getTime(), RESOLUTION);
            fail("Expected Purge Exception");
        } catch (final PortalMetricsPurgeUtility.PurgeException e) {
            assertNumRows(connection, 1, 1, 1);
            throw e;
        }
    }

    @Test
    public void purgeWrongResolution() throws Exception {
        final long periodStart = DateUtils.addDays(calendar.getTime(), NUM_DAYS * -1).getTime();

        insertPublishedService(connection, 1);
        final long generatedKey = insertServiceMetric(connection, 1, periodStart);
        insertMappingKey(connection, 1);
        insertMappingValue(connection, 1, 1);
        insertServiceMetricDetail(connection, generatedKey, 1);

        assertNumRows(connection, 1, 1, 1);

        purger.purge(NUM_DAYS, calendar.getTime(), 3);

        // nothing should have been purged
        assertNumRows(connection, 1, 1, 1);
    }
}
