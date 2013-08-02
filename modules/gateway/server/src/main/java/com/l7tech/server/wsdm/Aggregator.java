package com.l7tech.server.wsdm;

import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.server.wsdm.subscription.ServiceStateMonitor;
import com.l7tech.util.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects runtime metrics for each service.
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
    private final Map<Goid, MetricsSummaryBin> metrics = new HashMap<Goid, MetricsSummaryBin>();
    private Map<Goid,MetricsSummaryBin> dailyBins;
    private long dailyBinEndTime;
    private long lastMetricsTimestamp = 0L;

    @Inject
    private ServiceManager serviceManager;

    @Inject
    private ServiceMetricsManager metricsManager;

    @Inject
    private ServiceMetricsServices metricsServices;

    @PostConstruct
    public void start() {
        rebuildMetricsSummaries();
    }

    @Override
    public void onServiceDisabled(Goid serviceGoid) {
        ensureMetricsTracked( serviceGoid );
    }

    @Override
    public void onServiceEnabled(Goid serviceGoid) {
        ensureMetricsTracked( serviceGoid );
    }

    @Override
    public void onServiceCreated(Goid serviceGoid) {
        lock.writeLock().lock();
        try {
            MetricsBin emptyBin = new MetricsBin();
            emptyBin.setServiceGoid(serviceGoid);
            List<MetricsBin> binList = new ArrayList<MetricsBin>(1);
            binList.add(emptyBin);
            MetricsSummaryBin serviceMetricsSummary = new MetricsSummaryBin(binList);

            metrics.put(serviceGoid, serviceMetricsSummary);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void onServiceDeleted(Goid serviceGoid) {
        lock.writeLock().lock();
        try {
            metrics.remove(serviceGoid);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<Goid, MetricsSummaryBin> getMetricsForServices() {
        lock.writeLock().lock();
        try {
            return getMetricsForServicesNoLock(false);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Map<Goid, MetricsSummaryBin> getMetricsForServicesNoLock(boolean includeEmpty) {
        try {
            final HashMap<Goid, MetricsSummaryBin> retVal = new HashMap<Goid, MetricsSummaryBin>();
            final long periodStart = MetricsBin.periodStartFor(MetricsBin.RES_FINE, metricsServices.getFineInterval(), lastMetricsTimestamp);
            final Goid[] serviceGoids;
            if(metrics.size() == 0) {
                serviceGoids = new Goid[0];
            } else {
                serviceGoids = metrics.keySet().toArray(new Goid[metrics.size()]);
            }

            if ( (System.currentTimeMillis() - periodStart) > TimeUnit.HOURS.toMillis(1) ) {
                rebuildMetricsSummaries();
                for(Map.Entry<Goid, MetricsSummaryBin> entry : metrics.entrySet()) {
                    if(!Goid.isDefault(entry.getKey())) {
                        lastMetricsTimestamp = Math.max(lastMetricsTimestamp, entry.getValue().getPeriodEnd());
                    }
                    retVal.put(entry.getKey(), entry.getValue());
                }
            } else {
                Map<Goid, MetricsSummaryBin> newSummaries = metricsManager.summarizeByService(null, MetricsBin.RES_FINE, periodStart, null, serviceGoids, includeEmpty);

                for(Map.Entry<Goid, MetricsSummaryBin> entry : newSummaries.entrySet()) {
                    MetricsSummaryBin oldSummary = metrics.get(entry.getKey());
                    if(oldSummary == null) {
                        if(!Goid.isDefault(entry.getKey())) {
                            metrics.put(entry.getKey(), entry.getValue());
                            lastMetricsTimestamp = Math.max(lastMetricsTimestamp,entry.getValue().getPeriodEnd());
                        }
                        retVal.put(entry.getKey(), entry.getValue());
                    } else {
                        List<MetricsBin> summaryList = new ArrayList<MetricsBin>(2);
                        summaryList.add(oldSummary);
                        summaryList.add(entry.getValue());
                        MetricsSummaryBin updatedMetrics = new MetricsSummaryBin(summaryList);
                        if(!Goid.isDefault(entry.getKey())) {
                            metrics.put(entry.getKey(), updatedMetrics);
                            lastMetricsTimestamp = Math.max(lastMetricsTimestamp, entry.getValue().getPeriodEnd());
                        }
                        retVal.put(entry.getKey(), updatedMetrics);
                    }
                }
            }

            return retVal;
        } catch(FindException e) {
            logger.log(Level.WARNING, "Error reading metrics for services", e);
            return new HashMap<Goid, MetricsSummaryBin>();
        }
    }

    private void ensureMetricsTracked(Goid serviceGoid) {
        // Ensure manager tracks metrics for this service
        // If this is not done then service status changes are not tracked
        // until the service is consumed.
        metricsServices.trackServiceMetrics( serviceGoid );
    }
    
    public MetricsSummaryBin getMetricsForService(Goid serviceGoid) {
        lock.writeLock().lock();
        try {
            if(metrics.containsKey(serviceGoid)) {
                if(System.currentTimeMillis() - lastMetricsTimestamp <= metricsServices.getFineInterval()) {
                    return metrics.get(serviceGoid);
                } else {
                    getMetricsForServicesNoLock(true);
                    return metrics.get(serviceGoid);
                }
            } else if(serviceManager.findByPrimaryKey(serviceGoid) != null) {
                Map<Goid, MetricsSummaryBin> summaries = getMetricsForServicesNoLock(true);
                if(summaries.containsKey(serviceGoid)) {
                    return summaries.get(serviceGoid);
                } else {
                    return summaries.get(GoidEntity.DEFAULT_GOID);
                }
            } else {
                return null;
            }
        } catch(FindException e) {
            logger.log(Level.WARNING, "Cannot retrieve metrics, service does not exist: #" + serviceGoid, e);
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void rebuildMetricsSummaries() {
        if (metricsManager == null) {
            // Not yet initialized
            logger.log(Level.WARNING, "Unable to rebuild metrics -- no metricsManager configured");
            return;
        }

        // Calculate ESM metrics by summing all available service metrics bins.
        // See http://sarek/mediawiki/index.php?title=ESM#Calculating_ESM_Metrics for explanation.
        try {
            if ( dailyBins == null || (System.currentTimeMillis() - dailyBinEndTime) >= TimeUnit.DAYS.toMillis(1) ) {
                dailyBins = metricsManager.summarizeByService(null, MetricsBin.RES_DAILY, null, null, null, false);
            }
            Collection<PublishedService> services = serviceManager.findAll();
            for (PublishedService ps : services) {
                MetricsSummaryBin dailySummary = dailyBins.get(ps.getGoid());
                MetricsSummaryBin hourlySummary;
                MetricsSummaryBin fineSummary;

                if(dailySummary == null) {
                    hourlySummary = metricsManager.summarizeByService(null, MetricsBin.RES_HOURLY, null, null, new Goid[] {ps.getGoid()}, false).get(ps.getGoid());
                } else {
                    hourlySummary = metricsManager.summarizeByService(null, MetricsBin.RES_HOURLY, dailySummary.getPeriodEnd(), null, new Goid[] {ps.getGoid()}, false).get(ps.getGoid());
                    dailyBinEndTime = (dailyBinEndTime >= dailySummary.getPeriodEnd()) ? dailyBinEndTime : dailySummary.getPeriodEnd();
                }

                if(hourlySummary == null) {
                    fineSummary = metricsManager.summarizeByService(null, MetricsBin.RES_FINE, dailySummary == null ? null : dailySummary.getPeriodEnd(), null, new Goid[] {ps.getGoid()}, false).get(ps.getGoid());
                } else {
                    fineSummary = metricsManager.summarizeByService(null, MetricsBin.RES_FINE, hourlySummary.getPeriodEnd(), null, new Goid[] {ps.getGoid()}, false).get(ps.getGoid());
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
                    emptyBin.setServiceGoid(ps.getGoid());
                    List<MetricsBin> binList = new ArrayList<MetricsBin>(1);
                    binList.add(emptyBin);
                    serviceMetricsSummary = new MetricsSummaryBin(binList);
                }

                metrics.put(ps.getGoid(), serviceMetricsSummary);
            }

            lastMetricsTimestamp = System.currentTimeMillis();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error reading services", e);
        }
    }
}
