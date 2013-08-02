package com.l7tech.portal.metrics;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility which uses JDBC to upgrade data in an API Portal mysql database.
 */
public class PortalMetricsUpgradeUtility extends AbstractPortalMetricsUtility {
    public PortalMetricsUpgradeUtility(@NotNull final DatabaseInfo sourceDatabaseInfo, @NotNull final DatabaseInfo destDatabaseInfo) {
        super(sourceDatabaseInfo, destDatabaseInfo);
    }

    /**
     * Sets the uuid columns of the destination published_service and service_metrics tables based on data from the source generic_entity table.
     *
     * @throws SQLException
     */
    public void upgrade2_0To2_1() throws SQLException {
        Connection sourceConnection = null;
        Connection destConnection = null;
        try {
            // source connection is read only
            sourceConnection = DriverManager.getConnection(sourceDatabaseInfo.getUrl(), sourceDatabaseInfo.getUsername(), sourceDatabaseInfo.getPassword());
            destConnection = DriverManager.getConnection(destDatabaseInfo.getUrl(), destDatabaseInfo.getUsername(), destDatabaseInfo.getPassword());
            destConnection.setAutoCommit(false);

            final Map<Long, String> serviceOidApiIdMap = getServiceOidApiIdMap(sourceConnection);
            updatePublishedServices(destConnection, serviceOidApiIdMap);
            updateServiceMetrics(destConnection, serviceOidApiIdMap);

            destConnection.commit();
        } catch (final SQLException e) {
            destConnection.rollback();
            throw e;
        } finally {
            closeConnections(sourceConnection, destConnection);
        }
    }

    private void updateServiceMetrics(final Connection destConnection, final Map<Long, String> serviceOidApiIdMap) throws SQLException {
        // update uuid columns of service_metrics table
        for (final Map.Entry<Long, String> entry : serviceOidApiIdMap.entrySet()) {
            final PreparedStatement preparedStatement = destConnection.prepareStatement(UPDATE_METRIC);
            preparedStatement.setString(1, entry.getValue());
            preparedStatement.setLong(2, entry.getKey());
            final Integer count = preparedStatement.executeUpdate();
            preparedStatement.close();
            LOGGER.info("Updated " + count + " service metrics for published service oid=" + entry.getKey());
        }
    }

    private void updatePublishedServices(final Connection destConnection, final Map<Long, String> serviceOidApiIdMap) throws SQLException {
        // update uuid columns of published_service table
        int count = 0;
        for (final Map.Entry<Long, String> entry : serviceOidApiIdMap.entrySet()) {
            final PreparedStatement preparedStatement = destConnection.prepareStatement(UPDATE_PUBLISHED_SERVICE);
            preparedStatement.setString(1, entry.getValue());
            preparedStatement.setLong(2, entry.getKey());
            count += preparedStatement.executeUpdate();
            preparedStatement.close();
        }
        LOGGER.info("Updated " + count + " published services");
    }

    private Map<Long, String> getServiceOidApiIdMap(final Connection sourceConnection) throws SQLException {
        // query source connection for service oid - api id pairs
        final Map<Long, String> serviceOidApiIdMap = new HashMap<Long, String>();
        final Statement selectStatement = sourceConnection.createStatement();
        final ResultSet resultSet = selectStatement.executeQuery(SELECT);
        while (resultSet.next()) {
            final String serviceOid = resultSet.getString("serviceGoid");
            final String apiId = resultSet.getString("apiId");
            if (StringUtils.isNumeric(serviceOid)) {
                serviceOidApiIdMap.put(Long.valueOf(serviceOid), apiId);
                LOGGER.debug("Found serviceOid-apiId pair: " + serviceOid + "-" + apiId);
            } else {
                LOGGER.warn(serviceOid + " is not a valid published service oid and will be skipped.");
            }
        }
        resultSet.close();
        selectStatement.close();
        return serviceOidApiIdMap;
    }

    static final String CLASSNAME = "com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService";
    private static final String SELECT = "select description as serviceOid, name as apiId from generic_entity where classname = '" + CLASSNAME + "'";
    private static final String UPDATE_PUBLISHED_SERVICE = "update published_service set uuid = ? where objectid = ? and uuid is null";
    private static final String UPDATE_METRIC = "update service_metrics set uuid = ? where published_service_oid = ? and uuid is null";
    private static final Logger LOGGER = Logger.getLogger(PortalMetricsUpgradeUtility.class);
}
