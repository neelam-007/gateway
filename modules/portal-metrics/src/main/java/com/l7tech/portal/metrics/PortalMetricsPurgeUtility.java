package com.l7tech.portal.metrics;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Purges old data from a service metrics database.
 * <p/>
 * Relevant tables: published_service, service_metrics, service_metrics_details, message_context_mapping_keys, message_context_mapping_values.
 *
 * @author alee
 */
public class PortalMetricsPurgeUtility {
    private static final Logger LOGGER = Logger.getLogger(PortalMetricsPurgeUtility.class);
    // table names
    private static final String SERVICE_METRICS = "service_metrics";
    private static final String SERVICE_METRICS_DETAILS = "service_metrics_details";
    private static final String MAPPING_KEYS = "message_context_mapping_keys";
    private static final String MAPPING_VALUES = "message_context_mapping_values";
    private static final String PUBLISHED_SERVICE = "published_service";
    // queries
    private static final String COUNT_SERVICE_METRICS_QUERY = "SELECT COUNT(*) AS C FROM " + SERVICE_METRICS + " WHERE PERIOD_START <= ? AND RESOLUTION = ?";
    private static final String SELECT_SERVICE_METRICS_QUERY = "SELECT OBJECTID FROM " + SERVICE_METRICS + " WHERE PERIOD_START <= ? AND RESOLUTION = ? LIMIT ?";
    private static final String DELETE_SERVICE_METRICS_QUERY = "DELETE FROM " + SERVICE_METRICS + " WHERE OBJECTID IN ";
    private static final String DELETE_SERVICE_METRICS_DETAIL_QUERY = "DELETE FROM " + SERVICE_METRICS_DETAILS + " WHERE SERVICE_METRICS_OID IN ";
    private static final String DELETE_MAPPING_VALUES_QUERY = "DELETE FROM " + MAPPING_VALUES + " WHERE OBJECTID NOT IN (SELECT MAPPING_VALUES_OID FROM " + SERVICE_METRICS_DETAILS + ")";
    private static final String DELETE_MAPPING_KEYS_QUERY = "DELETE FROM " + MAPPING_KEYS + " WHERE OBJECTID NOT IN (SELECT MAPPING_KEYS_OID FROM " + MAPPING_VALUES + ")";
    private static final String DELETE_PUBLISHED_SERVICE_QUERY = "DELETE FROM " + PUBLISHED_SERVICE + " WHERE OBJECTID NOT IN (SELECT PUBLISHED_SERVICE_OID FROM " + SERVICE_METRICS + ")";

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final DataSource dataSource;

    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * @param batchSize the maximum number of service metrics rows to delete at once.
     */
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    public PortalMetricsPurgeUtility(final DataSource dataSource) {
        Validate.notNull(dataSource, "Data source cannot be null.");
        this.dataSource = dataSource;
    }

    /**
     * Purge data x days older than today.
     *
     * @param daysOld    the number of days older than today to purge.
     * @param resolution the resolution of the data to purge (ex. 1 for hourly bins, 3 for custom bins).
     * @throws SQLException
     * @throws PurgeException
     */
    public void purge(final int daysOld, final int resolution) throws SQLException {
        purge(daysOld, new Date(), resolution);
    }

    /**
     * Purge data x days older than a given date.
     *
     * @param daysBeforeDate the number of days older than the given date to purge.
     * @param date           the date to consider when calculating the data to purge.
     * @param resolution     the resolution of the data to purge (ex. 1 for hourly bins, 3 for custom bins).
     * @throws SQLException
     * @throws PurgeException
     */
    void purge(final int daysBeforeDate, final Date date, final int resolution) throws SQLException {
        Validate.isTrue(daysBeforeDate >= 1, "Days before date must be greater than or equal to one.");
        Validate.notNull(date, "Date cannot be null.");

        Connection connection = null;

        try {
            connection = dataSource.getConnection();
        } catch (final SQLException e) {
            closeConnection(connection);
            throw new PurgeException("Error establishing connection to database.", e);
        }

        if (connection != null) {
            final Date endDate = DateUtils.addDays(date, daysBeforeDate * -1);
            LOGGER.info("Purging data with resolution=" + resolution + " that is " + daysBeforeDate + " day(s) older than " + date + " (period start less than or equal to " + endDate.getTime() + ") in batches of " + batchSize + ".");

            try {
                // transactional
                connection.setAutoCommit(false);
                int numToPurge = countNumberOfMetricsToPurge(endDate, connection, resolution);
                while (numToPurge > 0) {
                    // batch
                    LOGGER.info("Number of metrics that require deletion: " + numToPurge);
                    purgeServiceMetrics(endDate, connection, resolution);
                    purgeMappingValues(connection);
                    purgeMappingKeys(connection);
                    purgePublishedServices(connection);
                    connection.commit();
                    numToPurge = countNumberOfMetricsToPurge(endDate, connection, resolution);
                }
            } catch (final SQLException e) {
                connection.rollback();
                throw new PurgeException("Transaction rolled back.", e);
            } finally {
                closeConnection(connection);
            }

            LOGGER.info("Finished purging data.");
        } else {
            throw new PurgeException("Connection is null.");
        }

    }

    private void purgePublishedServices(final Connection connection) throws SQLException {
        final Statement statement = connection.createStatement();
        final int numDeletedServices = statement.executeUpdate(DELETE_PUBLISHED_SERVICE_QUERY);
        LOGGER.info("Deleted " + numDeletedServices + " row(s) from " + PUBLISHED_SERVICE + " table.");
        statement.close();
    }

    private void purgeMappingKeys(final Connection connection) throws SQLException {
        final Statement statement = connection.createStatement();
        final int numDeletedKeys = statement.executeUpdate(DELETE_MAPPING_KEYS_QUERY);
        LOGGER.info("Deleted " + numDeletedKeys + " row(s) from " + MAPPING_KEYS + " table.");
        statement.close();
    }

    private void purgeMappingValues(final Connection connection) throws SQLException {
        final Statement statement = connection.createStatement();
        final int numDeletedValues = statement.executeUpdate(DELETE_MAPPING_VALUES_QUERY);
        LOGGER.info("Deleted " + numDeletedValues + " row(s) from " + MAPPING_VALUES + " table.");
        statement.close();
    }

    /**
     * Deleting a service metric will cascade delete its service metric details.
     */
    private void purgeServiceMetrics(final Date endDate, final Connection connection, final int resolution) throws SQLException {
        final List<Long> metricsToDelete = getMetricsIdsToDelete(connection, endDate, resolution);
        final String params = buildCommaSeparatedParams(metricsToDelete);
        final Statement delete = connection.createStatement();
        final int numDeletedServiceMetrics = delete.executeUpdate(DELETE_SERVICE_METRICS_QUERY + params);
        LOGGER.info("Deleted " + numDeletedServiceMetrics + " row(s) from " + SERVICE_METRICS + " table.");
        final int numDeletedServiceMetricsDetail = delete.executeUpdate(DELETE_SERVICE_METRICS_DETAIL_QUERY + params);
        LOGGER.info("Deleted " + numDeletedServiceMetricsDetail + " row(s) from " + DELETE_SERVICE_METRICS_DETAIL_QUERY + " table.");
        delete.close();
    }

    private String buildCommaSeparatedParams(final List<Long> params) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (int i = 0; i < params.size(); i++) {
            stringBuilder.append(params.get(i));
            if (i < params.size() - 1) {
                stringBuilder.append(",");
            }

        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private List<Long> getMetricsIdsToDelete(final Connection connection, final Date endDate, final int resolution) throws SQLException {
        final PreparedStatement select = connection.prepareStatement(SELECT_SERVICE_METRICS_QUERY);
        select.setLong(1, endDate.getTime());
        select.setInt(2, resolution);
        select.setInt(3, batchSize);
        final List<Long> metricsToDelete = new ArrayList<Long>();
        final ResultSet selectResultSet = select.executeQuery();
        while (selectResultSet.next()) {
            metricsToDelete.add(selectResultSet.getLong(1));
        }
        select.close();
        return metricsToDelete;
    }

    private int countNumberOfMetricsToPurge(final Date endDate, final Connection connection, final int resolution) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(COUNT_SERVICE_METRICS_QUERY);
        preparedStatement.setLong(1, endDate.getTime());
        preparedStatement.setInt(2, resolution);
        final ResultSet resultSet = preparedStatement.executeQuery();
        int numRows = 0;
        if (resultSet.next()) {
            numRows = resultSet.getInt("C");
        }
        preparedStatement.close();
        return numRows;
    }

    public class PurgeException extends RuntimeException {
        public PurgeException(final String s) {
            super(s);
        }

        public PurgeException(final String s, final Throwable throwable) {
            super(s, throwable);
        }
    }

    protected void closeConnection(final Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
