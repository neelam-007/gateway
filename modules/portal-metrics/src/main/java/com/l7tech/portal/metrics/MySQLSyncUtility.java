package com.l7tech.portal.metrics;

import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * MySQL implementation of PortalMetricsSyncUtility.
 * <p/>
 * Relevant tables: published_service, service_metrics, service_metrics_details, message_context_mapping_keys, message_context_mapping_values.
 *
 * @author alee
 */
public class MySQLSyncUtility extends AbstractPortalMetricsUtility implements PortalMetricsSyncUtility {
    private static final Logger LOGGER = Logger.getLogger(MySQLSyncUtility.class);
    private static final int DEFAULT_BATCH_SIZE = 100;

    // select queries
    private static final String LATEST = "LATEST";
    private static final int HOURLY_RESOLUTION = 1;
    private static final String LATEST_PERIOD_START_QUERY = "SELECT MAX(PERIOD_START) AS " + LATEST + " FROM " + SERVICE_METRICS + " WHERE RESOLUTION = ? AND PUBLISHED_SERVICE_OID = ?";
    private static final String WHERE_CLAUSE = "WHERE RESOLUTION = ? AND PERIOD_START > ? AND PUBLISHED_SERVICE_OID = ? ";
    private static final String LIMIT = "LIMIT ? ";
    private static final String ORDER_BY = "ORDER BY PERIOD_START ASC ";
    private static final String SERVICE_METRICS_QUERY = "SELECT * FROM " + SERVICE_METRICS + " ";
    private static final String COUNT_SERVICE_METRICS_QUERY = "SELECT COUNT(*) AS C FROM " + SERVICE_METRICS + " ";
    private static final String SERVICE_METRICS_DETAILS_QUERY = "SELECT * FROM " + SERVICE_METRICS_DETAILS + " WHERE SERVICE_METRICS_OID IN ";
    private static final String MESSAGE_CONTEXT_VALUES_OBJECTIDS_QUERY = "SELECT DISTINCT MAPPING_VALUES_OID FROM " + SERVICE_METRICS_DETAILS + " WHERE SERVICE_METRICS_OID IN ";

    protected int batchSize = DEFAULT_BATCH_SIZE;

    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    public MySQLSyncUtility(final DatabaseInfo sourceDatabaseInfo, final DatabaseInfo destDatabaseInfo) {
        super(sourceDatabaseInfo, destDatabaseInfo);
    }

    /**
     * Copies hourly service metrics for the given service ids from source to destination databases in batches.
     * <p/>
     * If an error occurs, the batch in which the error occurred will be rolled back and no more batches will be attempted.
     *
     * @param portalManagedServiceIds the portal managed service ids for which to copy data.
     * @throws SQLException
     */
    @Override
    public void syncHourlyData(final Map<Long, String> portalManagedServiceIds) throws SQLException {
        if (portalManagedServiceIds != null && !portalManagedServiceIds.isEmpty()) {
            LOGGER.info("Copying data in batches of " + batchSize + " for portal managed services: " + portalManagedServiceIds);
            Connection sourceConnection = null;
            Connection destConnection = null;
            try {
                // source connection is read only
                sourceConnection = DriverManager.getConnection(sourceDatabaseInfo.getUrl(), sourceDatabaseInfo.getUsername(), sourceDatabaseInfo.getPassword());
                destConnection = DriverManager.getConnection(destDatabaseInfo.getUrl(), destDatabaseInfo.getUsername(), destDatabaseInfo.getPassword());
                // want destination queries to be transactional because any partial failure will put the destination database
                // out of sync with the source database
                destConnection.setAutoCommit(false);
            } catch (final Exception e) {
                closeConnections(sourceConnection, destConnection);
                throw new SyncException("Error establishing database connections.", e);
            }



            try {
                // process service ids one at a time
                for (final Long serviceId : portalManagedServiceIds.keySet()) {
                    String uuid = portalManagedServiceIds.get(serviceId);
                    LOGGER.info("Copying data for service id: " + serviceId +" uuid: "+uuid);
                    // order is necessary for foreign key relationships
                    copyPublishedService(sourceConnection, destConnection, serviceId, uuid );
                    long periodStart = getLatestPeriodStart(destConnection, serviceId);
                    int numMetricsToSync = countNumberOfMetricsToSync(sourceConnection, periodStart, serviceId);
                    while (numMetricsToSync > 0) {
                        // batch
                        final Map<Long, Long> serviceMetricsKeys = copyServiceMetrics(sourceConnection, destConnection, periodStart, serviceId,uuid);
                        copyMappingValues(sourceConnection, destConnection, serviceMetricsKeys.keySet());
                        copyServiceMetricsDetails(sourceConnection, destConnection, serviceMetricsKeys,uuid);
                        destConnection.commit();
                        periodStart = getLatestPeriodStart(destConnection, serviceId);
                        numMetricsToSync = countNumberOfMetricsToSync(sourceConnection, periodStart, serviceId);
                    }
                }
            } catch (final Exception e) {
                destConnection.rollback();
                throw new SyncException("Transaction rolled back.", e);
            } finally {
                closeConnections(sourceConnection, destConnection);
            }
        } else {
            LOGGER.info("Copy data skipped because there are no portal managed services.");
        }
    }



    private int countNumberOfMetricsToSync(final Connection sourceConnection, final Long latestPeriodStart, final Long serviceId) throws SQLException {
        final PreparedStatement statement = sourceConnection.prepareStatement(COUNT_SERVICE_METRICS_QUERY + WHERE_CLAUSE);
        setSelectParams(statement, latestPeriodStart, serviceId);
        final ResultSet resultSet = statement.executeQuery();
        int numRows = 0;
        if (resultSet.next()) {
            numRows = resultSet.getInt("C");
        }
        statement.close();
        LOGGER.info("Number of metrics that require sync for service " + serviceId + ": " + numRows);
        return numRows;
    }

    /**
     * Only copies mapping values (and keys due to foreign key) that are referenced by new hourly metrics data.
     */
    private void copyMappingValues(final Connection sourceConnection, final Connection destConnection, final Set<Long> serviceMetricsIds) throws SQLException {
        final Statement statement = sourceConnection.createStatement();
        final ResultSet resultSet = statement.executeQuery(createMappingValuesSelectQuery(serviceMetricsIds));
        int total = 0;
        while (resultSet.next()) {
            total = total + copyMappingValue(sourceConnection, destConnection, resultSet.getLong(1));
        }
        statement.close();
    }

    /**
     * Only copies metric details that are connected to new hourly metrics data.
     */
    private void copyServiceMetricsDetails(final Connection sourceConnection, final Connection destConnection, final Map<Long, Long> serviceMetricKeys, final String uuid) throws SQLException {
        final Statement statement = sourceConnection.createStatement();
        final ResultSet resultSet = statement.executeQuery(createServiceMetricsDetailsQuery(serviceMetricKeys.keySet()));
        final PreparedStatement insertStatement = destConnection.prepareStatement(INSERT_SERVICE_METRICS_DETAILS_QUERY, Statement.RETURN_GENERATED_KEYS);
        int total = 0;
        while (resultSet.next()) {
            final long oldServiceMetricsOid = resultSet.getLong("SERVICE_METRICS_OID");
            insertStatement.setLong(1, serviceMetricKeys.get(oldServiceMetricsOid));
            insertStatement.setLong(2, resultSet.getLong("MAPPING_VALUES_OID"));
            int index = setBaseMetricFields(resultSet, insertStatement, 3);
//            insertStatement.setString(++index,uuid);
            total = total + insertStatement.executeUpdate();
        }
        LOGGER.info(total + " row(s) copied to " + SERVICE_METRICS_DETAILS + " table.");
        insertStatement.close();
        statement.close();
    }

    /**
     * Only copies any new hourly metrics data.
     *
     * @return a map where key = old primary key from source, value = new primary key from destination
     */
    private Map<Long, Long> copyServiceMetrics(final Connection sourceConnection, final Connection destConnection, final Long latestPeriodStart, final Long serviceId, final String uuid) throws SQLException {
        // order by clause required to ensure oldest entries are copied first
        final PreparedStatement statement = sourceConnection.prepareStatement(SERVICE_METRICS_QUERY + WHERE_CLAUSE + ORDER_BY + LIMIT);
        setSelectParams(statement, latestPeriodStart, serviceId, batchSize);
        final ResultSet resultSet = statement.executeQuery();
        final PreparedStatement insertStatement = destConnection.prepareStatement(INSERT_SERVICE_METRICS_QUERY, Statement.RETURN_GENERATED_KEYS);
        final Map<Long, Long> serviceMetricsKeys = new HashMap<Long, Long>();
        int total = 0;
        while (resultSet.next()) {
            final long oldObjectId = resultSet.getLong("OBJECTID");
            insertStatement.setString(1, resultSet.getString("NODEID"));
            insertStatement.setLong(2, resultSet.getLong("PUBLISHED_SERVICE_OID"));
            insertStatement.setInt(3, resultSet.getInt("RESOLUTION"));
            insertStatement.setLong(4, resultSet.getLong("PERIOD_START"));
            insertStatement.setLong(5, resultSet.getLong("START_TIME"));
            insertStatement.setInt(6, resultSet.getInt("INTERVAL_SIZE"));
            insertStatement.setLong(7, resultSet.getLong("END_TIME"));
            insertStatement.setString(8, resultSet.getString("SERVICE_STATE"));
            int index = setBaseMetricFields(resultSet, insertStatement, 9);
            insertStatement.setString(++index,uuid);
            total = total + insertStatement.executeUpdate();
            final ResultSet generatedKeys = insertStatement.getGeneratedKeys();
            generatedKeys.next();
            final long newObjectId = generatedKeys.getLong(1);
            serviceMetricsKeys.put(oldObjectId, newObjectId);
        }
        LOGGER.info(total + " row(s) copied to " + SERVICE_METRICS + " table.");
        insertStatement.close();
        statement.close();
        return serviceMetricsKeys;
    }

    private int setBaseMetricFields(final ResultSet resultSet, final PreparedStatement insertStatement, final int startingIndex) throws SQLException {
        int index = startingIndex;
        insertStatement.setInt(index, resultSet.getInt("ATTEMPTED"));
        insertStatement.setInt(++index, resultSet.getInt("AUTHORIZED"));
        insertStatement.setInt(++index, resultSet.getInt("COMPLETED"));
        insertStatement.setObject(++index, getNullableInt(resultSet, "BACK_MIN"), Types.INTEGER);
        insertStatement.setObject(++index, getNullableInt(resultSet, "BACK_MAX"), Types.INTEGER);
        insertStatement.setInt(++index, resultSet.getInt("BACK_SUM"));
        insertStatement.setObject(++index, getNullableInt(resultSet, "FRONT_MIN"), Types.INTEGER);
        insertStatement.setObject(++index, getNullableInt(resultSet, "FRONT_MAX"), Types.INTEGER);
        insertStatement.setInt(++index, resultSet.getInt("FRONT_SUM"));
        return index;
    }

    /**
     * Retrieves last period start for hourly metrics that exist in the db.
     */
    private long getLatestPeriodStart(final Connection connection, final Long serviceId) throws SQLException {
        final PreparedStatement statement = connection.prepareStatement(LATEST_PERIOD_START_QUERY);
        int i = 0;
        statement.setLong(++i, HOURLY_RESOLUTION);
        statement.setLong(++i, serviceId);
        final ResultSet resultSet = statement.executeQuery();
        long periodStart = 0L;
        if (resultSet.next()) {
            periodStart = resultSet.getLong(LATEST);
        }
        LOGGER.info("Latest period start for serviceId " + serviceId + ": " + periodStart);
        statement.close();
        return periodStart;
}

    private int setSelectParams(final PreparedStatement statement, final Long latestPeriodStart, final Long serviceId) throws SQLException {
        int i = 0;
        statement.setInt(++i, HOURLY_RESOLUTION);
        statement.setLong(++i, latestPeriodStart);
        statement.setLong(++i, serviceId);
        return i;
    }

    private int setSelectParams(final PreparedStatement statement, final Long latestPeriodStart, final Long serviceId, final Integer limit) throws SQLException {
        int i = setSelectParams(statement, latestPeriodStart, serviceId);
        statement.setInt(++i, limit);
        return i;
    }

    private String createServiceMetricsDetailsQuery(final Set<Long> serviceMetricsIds) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(SERVICE_METRICS_DETAILS_QUERY);
        stringBuilder.append("(");
        stringBuilder.append(createCommaSeparatedString(serviceMetricsIds));
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private String createMappingValuesSelectQuery(final Set<Long> serviceMetricsIds) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MESSAGE_CONTEXT_VALUES_OBJECTIDS_QUERY);
        stringBuilder.append("(");
        stringBuilder.append(createCommaSeparatedString(serviceMetricsIds));
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private String createCommaSeparatedString(final Set<Long> items) {
        final StringBuilder stringBuilder = new StringBuilder();
        final Iterator<Long> iterator = items.iterator();
        for (int i = 0; i < items.size(); i++) {
            final Object item = iterator.next();
            if (item != null) {
                stringBuilder.append(String.valueOf(item));
                if (i != (items.size() - 1)) {
                    stringBuilder.append(",");
                }
            }
        }
        return stringBuilder.toString();
    }
}
