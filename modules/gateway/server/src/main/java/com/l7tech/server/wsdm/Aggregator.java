package com.l7tech.server.wsdm;

import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.wsdm.subscription.ServiceStateMonitor;
import com.l7tech.util.ArrayUtils;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects runtime metrics for each service to be queried by the QoSMetricsService
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 18, 2007<br/>
 */
public class Aggregator implements ServiceStateMonitor {
    private final Logger logger = Logger.getLogger(Aggregator.class.getName());
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public class RecordedMetrics {
        RecordedMetrics() {
            creationTime = System.currentTimeMillis();
        }
        public long totalProcessingTime;
        public long nrFailures;
        public long nrSuccesses;
        public long firstHitTimestamp;
        public long lastHitTimestamp;
        public long lastProcessingTime;
        public long maxProcessingTime;
        public long totalDownTime;
        public long downtimeStart;
        public boolean isDown = false;
        public long creationTime;
    }

    private final HashMap<Long, MetricsSummaryBin> metrics = new HashMap<Long, MetricsSummaryBin>();
    private long lastMetricsTimestamp = 0L;
    private final ServiceManager serviceManager;
    private final ServiceMetricsManager metricsManager;

    public Aggregator(ServiceManager serviceManager, ServiceMetricsManager metricsManager) {
        this.serviceManager = serviceManager;
        this.metricsManager = metricsManager;

        // Calculate ESM metrics by summing all available service metrics bins.
        // See http://sarek/mediawiki/index.php?title=ESM#Calculating_ESM_Metrics for explanation.
        try {
            Map<Long,MetricsSummaryBin> dailyBins = metricsManager.summarizeByService(null, MetricsBin.RES_DAILY, null, null, null, false);
            Collection<PublishedService> services = serviceManager.findAll();
            for (PublishedService ps : services) {
                MetricsSummaryBin dailySummary = dailyBins.get(ps.getOid());
                MetricsSummaryBin hourlySummary;
                MetricsSummaryBin fineSummary;

                if(dailySummary == null) {
                    hourlySummary = metricsManager.summarizeByService(null, MetricsBin.RES_HOURLY, null, null, new long[] {ps.getOid()}, false).get(ps.getOid());
                } else {
                    hourlySummary = metricsManager.summarizeByService(null, MetricsBin.RES_HOURLY, dailySummary.getPeriodEnd(), null, new long[] {ps.getOid()}, false).get(ps.getOid());
                }

                if(hourlySummary == null) {
                    fineSummary = metricsManager.summarizeByService(null, MetricsBin.RES_FINE, dailySummary == null ? null : dailySummary.getPeriodEnd(), null, new long[] {ps.getOid()}, false).get(ps.getOid());
                } else {
                    fineSummary = metricsManager.summarizeByService(null, MetricsBin.RES_FINE, hourlySummary.getPeriodEnd(), null, new long[] {ps.getOid()}, false).get(ps.getOid());
                }

                List<MetricsBin> summaries = new ArrayList<MetricsBin>(3);
                if(dailySummary != null) summaries.add(dailySummary);
                if(hourlySummary != null) summaries.add(hourlySummary);
                if(fineSummary != null) summaries.add(fineSummary);

                MetricsSummaryBin serviceMetricsSummary;
                if(summaries.size() > 0) {
                    serviceMetricsSummary = new MetricsSummaryBin(summaries);
                } else {
                    MetricsBin emptyBin = new MetricsBin();
                    emptyBin.setServiceOid(ps.getOid());
                    List<MetricsBin> binList = new ArrayList<MetricsBin>(1);
                    binList.add(emptyBin);
                    serviceMetricsSummary = new MetricsSummaryBin(binList);
                }

                metrics.put(ps.getOid(), serviceMetricsSummary);
            }

            lastMetricsTimestamp = System.currentTimeMillis();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error reading services", e);
        }
    }

    public void onServiceDisabled(long serviceoid) {
        ensureMetricsTracked( serviceoid );
    }

    public void onServiceEnabled(long serviceoid) {
        ensureMetricsTracked( serviceoid );
    }

    public void onServiceCreated(long serviceoid) {
        lock.writeLock().lock();
        try {
            MetricsBin emptyBin = new MetricsBin();
            emptyBin.setServiceOid(serviceoid);
            List<MetricsBin> binList = new ArrayList<MetricsBin>(1);
            binList.add(emptyBin);
            MetricsSummaryBin serviceMetricsSummary = new MetricsSummaryBin(binList);

            metrics.put(serviceoid, serviceMetricsSummary);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void onServiceDeleted(long serviceoid) {
        lock.writeLock().lock();
        try {
            metrics.remove(serviceoid);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<Long, MetricsSummaryBin> getMetricsForServices() {
        lock.writeLock().lock();
        try {
            return getMetricsForServicesNoLock(false);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Map<Long, MetricsSummaryBin> getMetricsForServicesNoLock(boolean includeEmpty) {
        try {
            long periodStart = MetricsBin.periodStartFor(MetricsBin.RES_FINE, metricsManager.getFineInterval(), lastMetricsTimestamp);
            long[] serviceOids;
            if(metrics.size() == 0) {
                serviceOids = new long[0];
            } else {
                serviceOids = ArrayUtils.unbox(metrics.keySet().toArray(new Long[metrics.size()]));
            }
            Map<Long, MetricsSummaryBin> newSummaries = metricsManager.summarizeByService(null, MetricsBin.RES_FINE, periodStart, null, serviceOids, includeEmpty);

            HashMap<Long, MetricsSummaryBin> retVal = new HashMap<Long, MetricsSummaryBin>();
            for(Map.Entry<Long, MetricsSummaryBin> entry : newSummaries.entrySet()) {
                MetricsSummaryBin oldSummary = metrics.get(entry.getKey());
                if(oldSummary == null) {
                    if(!entry.getKey().equals(-1L)) {
                        metrics.put(entry.getKey(), entry.getValue());
                        lastMetricsTimestamp = (lastMetricsTimestamp >= entry.getValue().getPeriodEnd()) ? lastMetricsTimestamp : entry.getValue().getPeriodEnd();
                    }
                    retVal.put(entry.getKey(), entry.getValue());
                } else {
                    List<MetricsBin> summaryList = new ArrayList<MetricsBin>(2);
                    summaryList.add(oldSummary);
                    summaryList.add(entry.getValue());
                    MetricsSummaryBin updatedMetrics = new MetricsSummaryBin(summaryList);
                    if(!entry.getKey().equals(-1L)) {
                        metrics.put(entry.getKey(), updatedMetrics);
                        lastMetricsTimestamp = (lastMetricsTimestamp >= entry.getValue().getPeriodEnd()) ? lastMetricsTimestamp : entry.getValue().getPeriodEnd();
                    }
                    retVal.put(entry.getKey(), updatedMetrics);
                }
            }

            return retVal;
        } catch(FindException e) {
            logger.log(Level.WARNING, "Error reading metrics for services", e);
            return new HashMap<Long, MetricsSummaryBin>();
        }
    }

    private void ensureMetricsTracked(long serviceoid) {
        // Ensure manager tracks metrics for this service
        // If this is not done then service status changes are not tracked
        // until the service is consumed.
        metricsManager.trackServiceMetrics( serviceoid );
    }
    
    public MetricsSummaryBin getMetricsForService(long serviceOid) {
        lock.writeLock().lock();
        try {
            if(metrics.containsKey(serviceOid)) {
                if(System.currentTimeMillis() - lastMetricsTimestamp <= metricsManager.getFineInterval()) {
                    return metrics.get(serviceOid);
                } else {
                    getMetricsForServicesNoLock(true);
                    return metrics.get(serviceOid);
                }
            } else if(serviceManager.findByPrimaryKey(serviceOid) != null) {
                Map<Long, MetricsSummaryBin> summaries = getMetricsForServicesNoLock(true);
                if(summaries.containsKey(serviceOid)) {
                    return summaries.get(serviceOid);
                } else {
                    return summaries.get(-1L);
                }
            } else {
                return null;
            }
        } catch(FindException e) {
            logger.log(Level.WARNING, "Cannot retrieve metrics, service does not exist: #" + serviceOid, e);
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
