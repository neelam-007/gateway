package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Functions;

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
 * @author rmak
 */
public class MetricsSummaryBin extends MetricsBin {
    /** GOID of published services with policy violations. */
    private final Set<Goid> _servicesWithPolicyViolation = new HashSet<Goid>(0);

    /** GOID of published services with routing failure. */
    private final Set<Goid> _servicesWithRoutingFailure = new HashSet<Goid>(0);

    private static final Functions.Unary<Long,MetricsBin> periodStartGetter = new Functions.Unary<Long, MetricsBin>() {
        public Long call(MetricsBin metricsBin) {
            return metricsBin.getPeriodStart();
        }
    };
    
    private static final Functions.Unary<Goid,MetricsBin> serviceGoidGetter = new Functions.Unary<Goid, MetricsBin>() {
        public Goid call(MetricsBin metricsBin) {
            return metricsBin.getServiceGoid();
        }
    };

    private long firstAttemptedRequest;
    private long firstAuthorizedRequest;
    private long firstCompletedRequest;
    private long lastAttemptedRequest;
    private long lastAuthorizedRequest;
    private long lastCompletedRequest;
    private double lastAverageFrontendResponseTime;

    /**
     * Combines metrics bins with the same period start into summary bins.
     *
     * @param bins  metrics bin to combine
     * @return unsorted collection of summary bins; can be empty if zero metrics
     *         bins were supplied, but never <code>null</code>
     */
    public static Collection<MetricsSummaryBin> createSummaryMetricsBinsByPeriodStart(final Collection<MetricsBin> bins) {
        final Collection<MetricsSummaryBin> newbins = createSummaryMetricsBins(bins, periodStartGetter).values();
        List<MetricsSummaryBin> rv = new ArrayList<MetricsSummaryBin>();
        rv.addAll(newbins);
        return rv;
    }

    public static Map<Goid, MetricsSummaryBin> createSummaryMetricsBinsByServiceOid(final Collection<MetricsBin> bins) {
        return createSummaryMetricsBins(bins, serviceGoidGetter);
    }

    /**
     * Combines metrics bins with the same grouping value into summary bins.
     *
     * @param bins        metrics bins to combine
     * @param valueGetter a function that, given a MetricsBin, returns the grouping value
     * @param <GT>        the type of the grouping value (e.g. Long for service OIDs)
     * 
     * @return unsorted collection of summary bins; can be empty if zero metrics
     *         bins were supplied, but never <code>null</code>
     */
    private static <GT> Map<GT, MetricsSummaryBin> createSummaryMetricsBins(final Collection<MetricsBin> bins, final Functions.Unary<GT, MetricsBin> valueGetter) {
        if (bins == null || bins.size() == 0)
            return Collections.emptyMap();

        final Map<GT, Set<MetricsBin>> binsByGroupId = new HashMap<GT, Set<MetricsBin>>();
        for (MetricsBin bin : bins) {
            GT value = valueGetter.call(bin);
            if (value == null) continue;
            Set<MetricsBin> serviceBins = binsByGroupId.get(value);
            if (serviceBins == null) {
                serviceBins = new HashSet<MetricsBin>();
                binsByGroupId.put(value, serviceBins);
            }
            serviceBins.add(bin);
        }

        // Combines the metrics bin in each group.
        final Map<GT, MetricsSummaryBin> result = new HashMap<GT, MetricsSummaryBin>();
        for (Map.Entry<GT,Set<MetricsBin>> entry : binsByGroupId.entrySet()) {
            final Set<MetricsBin> binSet = entry.getValue();
            if (binSet == null || binSet.isEmpty()) continue;
            final MetricsSummaryBin summaryBin = new MetricsSummaryBin(binSet);
            result.put(entry.getKey(), summaryBin);
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
        if (bins == null || bins.size() == 0) {
            throw new IllegalArgumentException("Must have at least one metrics bin.");
        }

        String clusterNodeId = null;
        Goid serviceGoid = PublishedService.DEFAULT_GOID;
        int resolution = -1;
        long periodStart = -1;
        long periodEnd = -1;
        long startTime = -1;
        long endTime = -1;
        long firstAttemptedRequest = -1;
        long firstAuthorizedRequest = -1;
        long firstCompletedRequest = -1;
        long lastAttemptedRequest = -1;
        long lastAuthorizedRequest = -1;
        long lastCompletedRequest = -1;
        int numAttemptedRequest = 0;
        int numAuthorizedRequest = 0;
        int numCompletedRequest = 0;
        Integer minFrontendResponseTime = null;
        Integer maxFrontendResponseTime = null;
        long sumFrontendResponseTime = 0;
        Integer minBackendResponseTime = null;
        Integer maxBackendResponseTime = null;
        long sumBackendResponseTime = 0;

        boolean first = true;
        for (Iterator<MetricsBin> it = bins.iterator(); it.hasNext();) {
            MetricsBin bin = it.next();
            if (first) {
                clusterNodeId = bin.getClusterNodeId();
                serviceGoid = bin.getServiceGoid();
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
                if (!(clusterNodeId == null || clusterNodeId.equals(bin.getClusterNodeId())))
                    clusterNodeId = null; // Null out summarized node ID if this summary came from different nodes
                if (!Goid.equals(serviceGoid, bin.getServiceGoid()))
                    serviceGoid = PublishedService.DEFAULT_GOID;
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
                        minFrontendResponseTime = MetricsBin.min(minFrontendResponseTime==null?Integer.MAX_VALUE:minFrontendResponseTime, bin.getMinFrontendResponseTime());
                        maxFrontendResponseTime = MetricsBin.max(maxFrontendResponseTime==null?-1:maxFrontendResponseTime, bin.getMaxFrontendResponseTime());
                    }
                }
                sumFrontendResponseTime += bin.getSumFrontendResponseTime();
                if (numCompletedRequest == 0) {
                    minBackendResponseTime = bin.getMinBackendResponseTime();
                    maxBackendResponseTime = bin.getMaxBackendResponseTime();
                } else {
                    if (bin.getNumCompletedRequest() != 0) {
                        minBackendResponseTime = MetricsBin.min(minBackendResponseTime==null?Integer.MAX_VALUE:minBackendResponseTime, bin.getMinBackendResponseTime());
                        maxBackendResponseTime = MetricsBin.max(maxBackendResponseTime==null?-1:maxBackendResponseTime, bin.getMaxBackendResponseTime());
                    }
                }
                sumBackendResponseTime += bin.getSumBackendResponseTime();
                numAttemptedRequest += bin.getNumAttemptedRequest();
                numAuthorizedRequest += bin.getNumAuthorizedRequest();
                numCompletedRequest += bin.getNumCompletedRequest();

                if (!it.hasNext()) setLastAverageFrontendResponseTime(bin.getAverageFrontendResponseTime());
            }

            if (bin.getNumAttemptedRequest() > 0) {
                firstAttemptedRequest = firstAttemptedRequest == -1 ? bin.getPeriodStart() : Math.min(bin.getPeriodStart(), firstAttemptedRequest);
                lastAttemptedRequest = lastAttemptedRequest == -1 ? bin.getPeriodStart() : Math.max(bin.getPeriodStart(), lastAttemptedRequest);
            }

            if (bin.getNumAuthorizedRequest() > 0) {
                firstAuthorizedRequest = firstAuthorizedRequest == -1 ? bin.getPeriodStart() : Math.min(bin.getPeriodStart(), firstAuthorizedRequest);
                lastAuthorizedRequest = lastAuthorizedRequest == -1 ? bin.getPeriodStart() : Math.max(bin.getPeriodStart(), lastAuthorizedRequest);
            }

            if (bin.getNumCompletedRequest() > 0) {
                firstCompletedRequest = firstCompletedRequest == -1 ? bin.getPeriodStart() : Math.min(bin.getPeriodStart(), firstCompletedRequest);
                lastCompletedRequest = lastCompletedRequest == -1 ? bin.getPeriodStart() : Math.max(bin.getPeriodStart(), lastCompletedRequest);
            }

            if (bin instanceof MetricsSummaryBin) {
                final MetricsSummaryBin summaryBin = (MetricsSummaryBin)bin;
                _servicesWithPolicyViolation.addAll(summaryBin.getServicesWithPolicyViolation());
                _servicesWithRoutingFailure.addAll(summaryBin.getServicesWithRoutingFailure());
            } else {
                if (bin.getNumPolicyViolation() != 0)
                    _servicesWithPolicyViolation.add(bin.getServiceGoid());
                if (bin.getNumRoutingFailure() != 0)
                    _servicesWithRoutingFailure.add(bin.getServiceGoid());
            }
        }

        setClusterNodeId(clusterNodeId);
        setServiceGoid(serviceGoid);
        if(resolution == -1) {
            _resolution = -1;
        } else {
            setResolution(resolution);
        }
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
        setFirstAttemptedRequest(firstAttemptedRequest);
        setFirstAuthorizedRequest(firstAuthorizedRequest);
        setFirstCompletedRequest(firstCompletedRequest);
        setLastAttemptedRequest(lastAttemptedRequest);
        setLastAuthorizedRequest(lastAuthorizedRequest);
        setLastCompletedRequest(lastCompletedRequest);
    }

    /** @return OID of published services with policy violations */
    public Set<Goid> getServicesWithPolicyViolation() {
        return _servicesWithPolicyViolation;
    }

    /** @return OID of published services with routing failures */
    public Set<Goid> getServicesWithRoutingFailure() {
        return _servicesWithRoutingFailure;
    }

    public long getFirstAttemptedRequest() {
        return firstAttemptedRequest;
    }

    public void setFirstAttemptedRequest(long firstAttemptedRequest) {
        this.firstAttemptedRequest = firstAttemptedRequest;
    }

    public long getFirstAuthorizedRequest() {
        return firstAuthorizedRequest;
    }

    public void setFirstAuthorizedRequest(long firstAuthorizedRequest) {
        this.firstAuthorizedRequest = firstAuthorizedRequest;
    }

    public long getFirstCompletedRequest() {
        return firstCompletedRequest;
    }

    public void setFirstCompletedRequest(long firstCompletedRequest) {
        this.firstCompletedRequest = firstCompletedRequest;
    }

    public long getLastAttemptedRequest() {
        return lastAttemptedRequest;
    }

    public void setLastAttemptedRequest(long lastAttemptedRequest) {
        this.lastAttemptedRequest = lastAttemptedRequest;
    }

    public long getLastAuthorizedRequest() {
        return lastAuthorizedRequest;
    }

    public void setLastAuthorizedRequest(long lastAuthorizedRequest) {
        this.lastAuthorizedRequest = lastAuthorizedRequest;
    }

    public long getLastCompletedRequest() {
        return lastCompletedRequest;
    }

    public void setLastCompletedRequest(long lastCompletedRequest) {
        this.lastCompletedRequest = lastCompletedRequest;
    }

    public double getLastAverageFrontendResponseTime() {
        return lastAverageFrontendResponseTime;
    }

    public void setLastAverageFrontendResponseTime(double lastAverageFrontendResponseTime) {
        this.lastAverageFrontendResponseTime = lastAverageFrontendResponseTime;
    }
}
