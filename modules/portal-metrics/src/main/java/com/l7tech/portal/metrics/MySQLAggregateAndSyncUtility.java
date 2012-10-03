package com.l7tech.portal.metrics;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * MySQL implementation of PortalMetricsAggregateAndSyncUtility.
 */
public class MySQLAggregateAndSyncUtility extends AbstractPortalMetricsUtility implements PortalMetricsAggregateAndSyncUtility {
    private static final Logger LOGGER = Logger.getLogger(MySQLAggregateAndSyncUtility.class);
    private static final String SELECT_SERVICE_DISABLED = "SELECT disabled FROM " + PUBLISHED_SERVICE + " WHERE objectid=?";
    private static final String WHERE_CLAUSE = "WHERE published_service_oid=? AND resolution=? AND " +
            "period_start>=? ";
    private static final String SELECT_SERVICE_METRIC_IDS = "SELECT objectid FROM " + SERVICE_METRICS + " " + WHERE_CLAUSE;
//    private static final String COMMON_AGGREGATE = "IF(sum(attempted)>" + Integer.MAX_VALUE + ", " + Integer.MAX_VALUE + ",sum(attempted)), " +


    /**
     * This query can produce multiple rows (one row per nodeid).
     */
    static String AGGREGATE_SERVICE_METRICS = "SELECT nodeid, " +
            "min(start_time) as start_time, max(end_time) as end_time, " +
            "IF(sum(attempted)>" + Integer.MAX_VALUE + ", " + Integer.MAX_VALUE + ",sum(attempted)), " +
           "IF(sum(authorized)>" + Integer.MAX_VALUE + ", " + Integer.MAX_VALUE + ",sum(authorized)), " +
            "IF(sum(completed)>" + Integer.MAX_VALUE + ", " + Integer.MAX_VALUE + ",sum(completed)), " +
            "IF(min(coalesce(back_min, " + Integer.MAX_VALUE + "))=" + Integer.MAX_VALUE + ",NULL,min(coalesce(back_min, " + Integer.MAX_VALUE + "))), " +
            "IF(max(coalesce(back_max, -1))=-1,NULL,max(coalesce(back_max, -1))), " +
            "IF(sum(back_sum)>" + Integer.MAX_VALUE + "," + Integer.MAX_VALUE + ",sum(back_sum)), " +
            "IF(min(coalesce(front_min, " + Integer.MAX_VALUE + "))=" + Integer.MAX_VALUE + ",NULL,min(coalesce(front_min, " + Integer.MAX_VALUE + "))), " +
            "IF(max(coalesce(front_max, -1))=-1,NULL,max(coalesce(front_max, -1))), " +
            "IF(sum(front_sum)>" + Integer.MAX_VALUE + "," + Integer.MAX_VALUE + ",sum(front_sum)) " +
            "FROM " + SERVICE_METRICS + " " + WHERE_CLAUSE +
            "GROUP BY nodeid " +
            "HAVING min(start_time) > 0";
    /**
     * This query can produce multiple rows (one row per mapping_values_oid - nodeid pair).
     */
    static String AGGREGATE_DETAILS = "SELECT nodeid, mapping_values_oid," +
            "IF(sum(smd.attempted)>" + Integer.MAX_VALUE + ", " + Integer.MAX_VALUE + ",sum(smd.attempted)), " +
            "IF(sum(smd.authorized)>" + Integer.MAX_VALUE + ", " + Integer.MAX_VALUE + ",sum(smd.authorized)), " +
            "IF(sum(smd.completed)>" + Integer.MAX_VALUE + ", " + Integer.MAX_VALUE + ",sum(smd.completed)), " +
            "IF(min(coalesce(smd.back_min, " + Integer.MAX_VALUE + "))=" + Integer.MAX_VALUE + ",NULL,min(coalesce(smd.back_min, " + Integer.MAX_VALUE + "))), " +
            "IF(max(coalesce(smd.back_max, -1))=-1,NULL,max(coalesce(smd.back_max, -1))), " +
            "IF(sum(smd.back_sum)>" + Integer.MAX_VALUE + "," + Integer.MAX_VALUE + ",sum(smd.back_sum)), " +
            "IF(min(coalesce(smd.front_min, " + Integer.MAX_VALUE + "))=" + Integer.MAX_VALUE + ",NULL,min(coalesce(smd.front_min, " + Integer.MAX_VALUE + "))), " +
            "IF(max(coalesce(smd.front_max, -1))=-1,NULL,max(coalesce(smd.front_max, -1))), " +
            "IF(sum(smd.front_sum)>" + Integer.MAX_VALUE + "," + Integer.MAX_VALUE + ",sum(smd.front_sum)) " +
            "FROM " + SERVICE_METRICS + " sm JOIN " + SERVICE_METRICS_DETAILS + " smd on sm.objectid = smd.service_metrics_oid " + WHERE_CLAUSE +
            " GROUP BY nodeid, mapping_values_oid";

     private final String INSERT_SERVICE_METRICS = "INSERT INTO " + SERVICE_METRICS + " " +
            "(nodeid,published_service_oid,resolution,period_start,interval_size,service_state," +
            "start_time,end_time,attempted,authorized,completed,back_min,back_max,back_sum,front_min,front_max,front_sum,uuid) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private final String INSERT_DETAILS = "INSERT INTO " + SERVICE_METRICS_DETAILS + " " +
            "(service_metrics_oid,mapping_values_oid,attempted,authorized,completed,back_min,back_max,back_sum," +
            "front_min,front_max,front_sum ) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    static final String DISABLED = "DISABLED";
    static final String ENABLED = "ENABLED";
    static final int SECONDS_PER_MINUTE = 60;
    static final int MILLISECONDS_PER_SECOND = 1000;

    /**
     * Map to keep track of generated service metrics primary keys.
     * <p/>
     * Key = generated primary key
     * <p/>
     * Value = [service id, period start, nodeid]
     */
    private Map<Long, String[]> generatedMetricIds;

    public MySQLAggregateAndSyncUtility(final DatabaseInfo sourceDatabaseInfo, final DatabaseInfo destDatabaseInfo) {
        super(sourceDatabaseInfo, destDatabaseInfo);
        generatedMetricIds = new HashMap<Long, String[]>();
    }

    /**
     * Aggregates the most recent metrics data for each given service id from source database and inserts the aggregate data into the destination database.
     * <p/>
     * Aggregation is performed for the following columns: attempted, authorized, completd, back_sum, front_sum.
     * If aggregation produces a value that is larger than Integer.MAX_VALUE, the aggregated value will be persisted as Integer.MAX_VALUE.
     * <p/>
     * Minimum is calculated for the following columns (may be null): back_min, front_min.
     * <p/>
     * Maximum is calculated for the following columns (may be null): back_max, front_max.
     * <p/>
     * Metrics data across multiple nodes are kept separate (not aggregated).
     *
     * @param services        the service ids for which to aggregate and copy data.
     * @param intervalInMinutes the number of minutes of data to aggregate and copy.
     * @throws SQLException
     */
    @Override
    public void aggregateAndSync(final Map<Long,String> services, final int intervalInMinutes) throws SQLException {
        if (services != null && !services.isEmpty()) {
            LOGGER.info("Aggregating and copying the last " + intervalInMinutes + " minutes of data for services: " + services);
            Connection sourceConnection = null;
            Connection destConnection = null;
            try {
                // source connection is read only
                sourceConnection = DriverManager.getConnection(sourceDatabaseInfo.getUrl(), sourceDatabaseInfo.getUsername(), sourceDatabaseInfo.getPassword());
                destConnection = DriverManager.getConnection(destDatabaseInfo.getUrl(), destDatabaseInfo.getUsername(), destDatabaseInfo.getPassword());
                // transactional
                destConnection.setAutoCommit(false);
            } catch (final Exception e) {
                closeConnections(sourceConnection, destConnection);
                throw new SyncException("Error establishing database connections.", e);
            }

            try {
                final long startTime = calculateStartTime(intervalInMinutes);
                final int intervalInMillis = intervalInMinutes * SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;

                for (final Long serviceId : services.keySet()) {
                    String uuid = services.get(serviceId);
                    copyPublishedService(sourceConnection, destConnection, serviceId, uuid);
                    aggregateAndInsertMetrics(sourceConnection, destConnection, startTime, intervalInMillis, serviceId, uuid);
                    aggregateAndInsertDetails(sourceConnection, destConnection, startTime, serviceId);
                    destConnection.commit();
                }

            } catch (final Exception e) {
                destConnection.rollback();
                throw new SyncException("Transaction rolled back.", e);
            } finally {
                closeConnections(sourceConnection, destConnection);
            }
        } else {
            LOGGER.info("Aggregation and copy skipped because there are no portal managed services.");
        }
    }

    private void aggregateAndInsertDetails(final Connection sourceConnection, final Connection destConnection, final long startTime, final Long serviceId) throws SQLException {
        final PreparedStatement aggregateStatement = createDetailsAggregationStatement(sourceConnection, startTime, serviceId);
        final ResultSet aggregatedDetails = aggregateStatement.executeQuery();
        while (aggregatedDetails.next()) {
            int j = 0;
            final String nodeId = aggregatedDetails.getString(++j);
            final long mappingValuesId = aggregatedDetails.getLong(++j);
            final int sumAttempted = aggregatedDetails.getInt(++j);
            final int sumAuthorized = aggregatedDetails.getInt(++j);
            final int sumCompleted = aggregatedDetails.getInt(++j);
            final Integer backMin = getNullableInt(aggregatedDetails, ++j);
            final Integer backMax = getNullableInt(aggregatedDetails, ++j);
            final int backSum = aggregatedDetails.getInt(++j);
            final Integer frontMin = getNullableInt(aggregatedDetails, ++j);
            final Integer frontMax = getNullableInt(aggregatedDetails, ++j);
            final int frontSum = aggregatedDetails.getInt(++j);

            // foreign key
            copyMappingValue(sourceConnection, destConnection, mappingValuesId);

            final PreparedStatement insertStatement = destConnection.prepareStatement(INSERT_DETAILS);
            int k = 0;
            final Long serviceMetricsId = getCorrespondingGeneratedMetricsId(startTime, serviceId, nodeId);
            insertStatement.setLong(++k, serviceMetricsId);
            insertStatement.setLong(++k, mappingValuesId);
            insertStatement.setInt(++k, sumAttempted);
            insertStatement.setInt(++k, sumAuthorized);
            insertStatement.setInt(++k, sumCompleted);
            insertStatement.setObject(++k, backMin, Types.INTEGER);
            insertStatement.setObject(++k, backMax, Types.INTEGER);
            insertStatement.setInt(++k, backSum);
            insertStatement.setObject(++k, frontMin, Types.INTEGER);
            insertStatement.setObject(++k, frontMax, Types.INTEGER);
            insertStatement.setInt(++k, frontSum);
//            insertStatement.setString(++k,uuid);
            final int rows = insertStatement.executeUpdate();
            LOGGER.info("Inserted " + rows + " row(s) into service_metrics_details table.");
            insertStatement.close();
        }
        aggregateStatement.close();
    }

    private Long getCorrespondingGeneratedMetricsId(final long startTime, final Long serviceId, final String nodeId) {
        Long serviceMetricsId = null;
        for (final Map.Entry<Long, String[]> entry : generatedMetricIds.entrySet()) {
            if (entry.getValue()[0].equals(String.valueOf(serviceId)) && entry.getValue()[1].equals(String.valueOf(startTime)) && entry.getValue()[2].equals(nodeId)) {
                serviceMetricsId = entry.getKey();
                break;
            }
        }
        if (serviceMetricsId == null) {
            throw new SyncException("Failed to retrieve service metrics id for serviceId=" + serviceId + ", startTime=" + startTime + ", nodeId=" + nodeId);
        }
        return serviceMetricsId;
    }

    private void aggregateAndInsertMetrics(final Connection sourceConnection, final Connection destConnection, final long startTime, final int intervalInMillis, final Long serviceId, final String uuid) throws SQLException {
        Boolean serviceDisabled = null;
        final PreparedStatement aggregateStatement = createMetricsAggregationStatement(sourceConnection, startTime, serviceId);
        final ResultSet aggregatedMetrics = aggregateStatement.executeQuery();
        while (aggregatedMetrics.next()) {
            if (serviceDisabled == null) {
                serviceDisabled = isServiceDisabled(sourceConnection, serviceId);
            }
            int j = 0;
            final String nodeId = aggregatedMetrics.getString(++j);
            final long start = aggregatedMetrics.getLong(++j);
            final long end = aggregatedMetrics.getLong(++j);
            final int sumAttempted = aggregatedMetrics.getInt(++j);
            final int sumAuthorized = aggregatedMetrics.getInt(++j);
            final int sumCompleted = aggregatedMetrics.getInt(++j);
            final Integer backMin = getNullableInt(aggregatedMetrics, ++j);
            final Integer backMax = getNullableInt(aggregatedMetrics, ++j);
            final int backSum = aggregatedMetrics.getInt(++j);
            final Integer frontMin = getNullableInt(aggregatedMetrics, ++j);
            final Integer frontMax = getNullableInt(aggregatedMetrics, ++j);
            final int frontSum = aggregatedMetrics.getInt(++j);

            final PreparedStatement insertStatement = destConnection.prepareStatement(INSERT_SERVICE_METRICS, Statement.RETURN_GENERATED_KEYS);
            int k = 0;
            insertStatement.setString(++k, nodeId);
            insertStatement.setLong(++k, serviceId);
            insertStatement.setInt(++k, CUSTOM_RESOLUTION);
            insertStatement.setLong(++k, startTime);
            insertStatement.setInt(++k, intervalInMillis);
            if (serviceDisabled == null) {
                insertStatement.setNull(++k, Types.VARCHAR);
            } else {
                insertStatement.setString(++k, serviceDisabled ? DISABLED : ENABLED);
            }
            insertStatement.setLong(++k, start);
            insertStatement.setLong(++k, end);
            insertStatement.setInt(++k, sumAttempted);
            insertStatement.setInt(++k, sumAuthorized);
            insertStatement.setInt(++k, sumCompleted);
            insertStatement.setObject(++k, backMin, Types.INTEGER);
            insertStatement.setObject(++k, backMax, Types.INTEGER);
            insertStatement.setInt(++k, backSum);
            insertStatement.setObject(++k, frontMin, Types.INTEGER);
            insertStatement.setObject(++k, frontMax, Types.INTEGER);
            insertStatement.setInt(++k, frontSum);
            insertStatement.setString(++k, uuid);
            final int rows = insertStatement.executeUpdate();
            final ResultSet generatedKeys = insertStatement.getGeneratedKeys();
            while (generatedKeys.next()) {
                generatedMetricIds.put(generatedKeys.getLong(1), new String[]{String.valueOf(serviceId), String.valueOf(startTime), String.valueOf(nodeId)});
            }
            LOGGER.info("Inserted " + rows + " row(s) to service_metrics table.");
            insertStatement.close();
        }
        aggregateStatement.close();
    }

    private PreparedStatement createDetailsAggregationStatement(final Connection connection, final long startTime, final Long serviceId) throws SQLException {
        Validate.notNull(connection, "Connection cannot be null.");
        Validate.notNull(serviceId, "Service id cannot be null.");

        final PreparedStatement selectStatement = connection.prepareStatement(AGGREGATE_DETAILS);
        int i = 0;
        selectStatement.setLong(++i, serviceId);
        selectStatement.setInt(++i, FINE_RESOLUTION);
        selectStatement.setLong(++i, startTime);
        return selectStatement;
    }

    private Boolean isServiceDisabled(final Connection connection, final Long portalManagedServiceId) throws SQLException {
        Validate.notNull(connection, "Connection cannot be null.");
        Validate.notNull(portalManagedServiceId, "Service id cannot be null.");
        Boolean disabled = null;
        final PreparedStatement disabledStatement = connection.prepareStatement(SELECT_SERVICE_DISABLED);
        disabledStatement.setLong(1, portalManagedServiceId);
        final ResultSet disabledResultSet = disabledStatement.executeQuery();
        if (disabledResultSet.next()) {
            disabled = disabledResultSet.getBoolean(1);
        }
        disabledStatement.close();
        return disabled;
    }

    private PreparedStatement createMetricsAggregationStatement(final Connection connection, final long startTime, final Long portalManagedServiceId) throws SQLException {
        Validate.notNull(connection, "Connection cannot be null.");
        Validate.notNull(portalManagedServiceId, "Service id cannot be null.");
        int i = 0;
        final PreparedStatement selectStatement = connection.prepareStatement(AGGREGATE_SERVICE_METRICS);
        selectStatement.setLong(++i, portalManagedServiceId);
        selectStatement.setInt(++i, FINE_RESOLUTION);
        selectStatement.setLong(++i, startTime);
        return selectStatement;
    }

    private long calculateStartTime(final int intervalInMinutes) {
        final Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, intervalInMinutes * -1);
        return calendar.getTime().getTime();
    }
}
