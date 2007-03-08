package com.l7tech.service;

import java.util.*;

/**
 * A service metrics summary bin is the result of combining multiple service
 * metrics bin into one.
 * Besides combining the numeric data, a summary bin also retains information
 * on which published services have policy violations and routing failures, if
 * the bins come from multiple published services.
 * <p/>
 * If the metrics bins come from more than one cluster node,
 * then the summary bin has cluster node ID of <code>null</code>.
 * <br/>
 * If the metrics bins come from more than one published service,
 * then the summary bin has serivce OID of -1.
 * <br/>
 * If the metrics bins come from more than one resolution,
 * then the summary bin has resolution of -1.
 *
 * @auther rmak
 */
public class MetricsSummaryBin extends MetricsBin {
    /** OID of published services with policy violations. */
    private final Set<Long> _servicesWithPolicyViolation = new HashSet<Long>(0);

    /** OID of published services with routing failure. */
    private final Set<Long> _servicesWithRoutingFailure = new HashSet<Long>(0);

    /**
     * Combines metrics bins with the same period start into summary bins.
     *
     * @param bins  metrics bin to combine
     * @return unsorted collection of summary bins; can be empty if zero metrics
     *         bins were supplied, but never <code>null</code>
     */
    public static Collection<MetricsSummaryBin> createSummaryMetricsBinsByPeriodStart(final Collection<MetricsBin> bins) {
        if (bins.size() == 0)
            return Collections.emptyList();

        // Groups the metrics bins by period start.
        final Map<Long, Set<MetricsBin>> _binsByPeriod = new HashMap<Long, Set<MetricsBin>>();
        for (MetricsBin bin : bins) {
            final Long periodStart = new Long(bin.getPeriodStart());
            Set<MetricsBin> periodBins = _binsByPeriod.get(periodStart);
            if (periodBins == null) {
                periodBins = new HashSet<MetricsBin>();
                _binsByPeriod.put(periodStart, periodBins);
            }
            periodBins.add(bin);
        }

        // Combines the metrics bin in each period.
        final Collection<MetricsSummaryBin> result = new ArrayList<MetricsSummaryBin>();
        for (Collection<MetricsBin> binsInPeriod : _binsByPeriod.values()) {
            final MetricsSummaryBin summaryBin = new MetricsSummaryBin(binsInPeriod);
            result.add(summaryBin);
        }

        return result;
    }

    /**
     * Constructs a summary bin by combining multiple metrics bins. If the given
     * bins have the same cluster node, then the summary bin will be set to that
     * cluster node, otherwise set to -1; ditto for service OID and resolution.
     *
     * @param bins  metrics bins to be combined into this summary bin
     * @throws IllegalArgumentException if zero metrics bins are supplied
     */
    public MetricsSummaryBin(final Collection<MetricsBin> bins) {
        if (bins.size() == 0) {
            throw new IllegalArgumentException("Must have at least one metrics bin.");
        }

        String clusterNodeId = null;
        long serviceOid = -1;
        int resolution = -1;
        long periodStart = -1;
        long periodEnd = -1;
        long startTime = -1;
        long endTime = -1;
        int numAttemptedRequest = 0;
        int numAuthorizedRequest = 0;
        int numCompletedRequest = 0;
        int minFrontendResponseTime = 0;
        int maxFrontendResponseTime = 0;
        long sumFrontendResponseTime = 0;
        int minBackendResponseTime = 0;
        int maxBackendResponseTime = 0;
        long sumBackendResponseTime = 0;

        boolean first = true;
        for (MetricsBin bin : bins) {
            if (first) {
                clusterNodeId = bin.getClusterNodeId();
                serviceOid = bin.getServiceOid();
                resolution = bin.getResolution();
                periodStart = bin.getPeriodStart();
                periodEnd = bin.getPeriodEnd();
                startTime = bin.getStartTime();
                endTime = bin.getEndTime();
                minFrontendResponseTime = bin.getMinFrontendResponseTime();
                maxFrontendResponseTime = bin.getMaxFrontendResponseTime();
                sumFrontendResponseTime = bin.getSumFrontendResponseTime();
                minBackendResponseTime = bin.getMinBackendResponseTime();
                maxBackendResponseTime = bin.getMaxBackendResponseTime();
                sumBackendResponseTime = bin.getSumBackendResponseTime();
                numAttemptedRequest = bin.getNumAttemptedRequest();
                numAuthorizedRequest = bin.getNumAuthorizedRequest();
                numCompletedRequest = bin.getNumCompletedRequest();
                first = false;
            } else {
                if (clusterNodeId != bin.getClusterNodeId())
                    clusterNodeId = null;
                if (serviceOid != bin.getServiceOid())
                    serviceOid = -1;
                if (resolution != bin.getResolution())
                    resolution = -1;
                periodStart = Math.min(periodStart, bin.getPeriodStart());
                periodEnd = Math.max(periodEnd, bin.getPeriodEnd());
                startTime = Math.min(startTime, bin.getStartTime());
                endTime = Math.max(endTime, bin.getEndTime());
                if (numAttemptedRequest == 0) {
                    minFrontendResponseTime = bin.getMinFrontendResponseTime();
                    maxFrontendResponseTime = bin.getMaxFrontendResponseTime();
                } else {
                    if (bin.getNumAttemptedRequest() != 0) {
                        minFrontendResponseTime = Math.min(minFrontendResponseTime, bin.getMinFrontendResponseTime());
                        maxFrontendResponseTime = Math.max(maxFrontendResponseTime, bin.getMaxFrontendResponseTime());
                    }
                }
                sumFrontendResponseTime += bin.getSumFrontendResponseTime();
                if (numCompletedRequest == 0) {
                    minBackendResponseTime = bin.getMinBackendResponseTime();
                    maxBackendResponseTime = bin.getMaxBackendResponseTime();
                } else {
                    if (bin.getNumCompletedRequest() != 0) {
                        minBackendResponseTime = Math.min(minBackendResponseTime, bin.getMinBackendResponseTime());
                        maxBackendResponseTime = Math.max(maxBackendResponseTime, bin.getMaxBackendResponseTime());
                    }
                }
                sumBackendResponseTime += bin.getSumBackendResponseTime();
                numAttemptedRequest += bin.getNumAttemptedRequest();
                numAuthorizedRequest += bin.getNumAuthorizedRequest();
                numCompletedRequest += bin.getNumCompletedRequest();
            }

            if (bin instanceof MetricsSummaryBin) {
                final MetricsSummaryBin summaryBin = (MetricsSummaryBin)bin;
                _servicesWithPolicyViolation.addAll(summaryBin.getServicesWithPolicyViolation());
                _servicesWithRoutingFailure.addAll(summaryBin.getServicesWithRoutingFailure());
            } else {
                if (bin.getNumPolicyViolation() != 0)
                    _servicesWithPolicyViolation.add(bin.getServiceOid());
                if (bin.getNumRoutingFailure() != 0)
                    _servicesWithRoutingFailure.add(bin.getServiceOid());
            }
        }

        setClusterNodeId(clusterNodeId);
        setServiceOid(serviceOid);
        setResolution(resolution);
        setPeriodStart(periodStart);
        setInterval((int)(periodEnd - periodStart));
        setStartTime(startTime);
        setEndTime(endTime);
        setNumAttemptedRequest(numAttemptedRequest);
        setNumAuthorizedRequest(numAuthorizedRequest);
        setNumCompletedRequest(numCompletedRequest);
        setMinFrontendResponseTime(minFrontendResponseTime);
        setMaxFrontendResponseTime(maxFrontendResponseTime);
        setSumFrontendResponseTime(sumFrontendResponseTime);
        setMinBackendResponseTime(minBackendResponseTime);
        setMaxBackendResponseTime(maxBackendResponseTime);
        setSumBackendResponseTime(sumBackendResponseTime);
    }

    /** @return OID of published services with policy violations */
    public Set<Long> getServicesWithPolicyViolation() {
        return _servicesWithPolicyViolation;
    }

    /** @return OID of published services with routing failures */
    public Set<Long> getServicesWithRoutingFailure() {
        return _servicesWithRoutingFailure;
    }
}
