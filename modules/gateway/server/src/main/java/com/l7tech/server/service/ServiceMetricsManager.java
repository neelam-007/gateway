package com.l7tech.server.service;

import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;

import java.util.Collection;
import java.util.Map;

/**
 *
 */
public interface ServiceMetricsManager {
    /**
     * Searches for metrics bins with the given criteria and summarizes by
     * combining bins with the same period start.
     *
     * @param nodeId            cluster node ID; null = all
     * @param serviceOids       published service OIDs; null = all services permitted for this user
     * @param resolution        bin resolution ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE},
     *                          {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or
     *                          {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY}); null = all
     * @param minPeriodStart    minimum bin period start time (milliseconds since epoch); null = as far back as available
     * @param maxPeriodStart    maximum bin period statt time (milliseconds since epoch); null = up to the latest available
     * @param includeEmpty      whether to include empty uptime bins (same as include service OID -1)
     *
     * @return collection of summary bins; can be empty but never <code>null</code>
     * @throws com.l7tech.objectmodel.FindException if failure to query database
     */
    Collection<MetricsSummaryBin> summarizeByPeriod(String nodeId,
                                                           long[] serviceOids,
                                                           Integer resolution,
                                                           Long minPeriodStart,
                                                           Long maxPeriodStart,
                                                           boolean includeEmpty)
        throws FindException;

    Map<Long, MetricsSummaryBin> summarizeByService(String nodeId, Integer resolution, Long minPeriodStart, Long maxPeriodStart, long[] serviceOids, boolean includeEmpty)
            throws FindException;

    /**
     * Searches for the latest metrics bins for the given criteria and
     * summarizes by combining them into one summary bin.
     *
     * @param clusterNodeId cluster node ID; null = all
     * @param serviceOids   published service OIDs; null = all services permitted for this user
     * @param resolution    bin resolution ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE},
     *                      {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or
     *                      {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY})
     * @param duration      time duration (milliseconds from latest nominal period boundary
     *                      time on gateway) to search backward for bins whose
     *                      nominal periods fall within
     * @param includeEmpty  whether to include empty uptime bins (same as include service OID -1)
     *
     * @return a summary bin; <code>null</code> if no metrics bins are found
     * @throws com.l7tech.objectmodel.FindException if failure to query database
     */
    MetricsSummaryBin summarizeLatest(String clusterNodeId,
                                             long[] serviceOids,
                                             int resolution,
                                             int duration,
                                             boolean includeEmpty)
            throws FindException;

    /**
     * Gets the current state of a service that was recently created or updated.
     */
    ServiceState getCreatedOrUpdatedServiceState(long oid) throws FindException;

    /**
     * Deletes old metrics bins.
     */
    Integer delete(long oldestSurvivor, int resolution);

    /**
     * Finds all the ServiceHeaders currently in the database.
     */
    Collection<ServiceHeader> findAllServiceHeaders() throws FindException;

    /**
     * The database work for the MetricsBin flush.
     */
    void doFlush(ServiceMetrics.MetricsCollectorSet metricsSet, MetricsBin bin);

    /**
     * Creates and saves a new hourly bin
     */
    void createHourlyBin(Long serviceOid, ServiceState state, long startTime) throws SaveException;

    /**
     * Creates and saves a new daily bin
     */
    void createDailyBin(Long serviceOid, ServiceState state, long startTime) throws SaveException;
}
