package com.l7tech.portal.metrics;

import java.sql.SQLException;
import java.util.Map;

/**
 * Aggregates and syncs service metrics data between two databases.
 */
public interface PortalMetricsAggregateAndSyncUtility {

    /**
     * Aggregates the most recent metrics data for each given service id from source database and inserts the aggregate data into the destination database.
     *
     * @param serviceIds        the service ids for which to aggregate and copy data.
     * @param intervalInMinutes the number of minutes of data to aggregate and copy.
     * @throws SQLException
     */
    void aggregateAndSync(final Map<Long,String> serviceIds, final int intervalInMinutes) throws SQLException;
}
