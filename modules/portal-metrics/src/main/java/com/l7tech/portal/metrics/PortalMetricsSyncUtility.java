package com.l7tech.portal.metrics;

import java.sql.SQLException;
import java.util.Map;

/**
 * Syncs any new hourly service metrics data from a source database to a destination database.
 */
public interface PortalMetricsSyncUtility {

    /**
     * Copy new hourly service metrics data for the given portal managed services from source to destination database.
     *
     * @param portalManagedServiceIds the portal managed service ids for which to copy data.
     * @throws SQLException
     */
    void syncHourlyData(final Map<Long, String> portalManagedServiceIds) throws SQLException;
}
