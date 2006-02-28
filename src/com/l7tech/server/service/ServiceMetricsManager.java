package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.BoundedPriorityQueue;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.MetricsBin;
import com.l7tech.common.util.ISO8601Date;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the processing and accumulation of service metrics for one SSG node.
 *
 * @author rmak
 */
public class ServiceMetricsManager extends HibernateDaoSupport
        implements InitializingBean, DisposableBean {

    private String _clusterNodeId;

    private final Timer fineTimer = new Timer("ServiceMetricsManager.fineTimer" /* name */, true /* isDaemon */);
    private final Timer hourlyTimer = new Timer("ServiceMetricsManager.hourlyTimer" /* name */, true /* isDaemon */);
    private final Timer dailyTimer = new Timer("ServiceMetricsManager.dailyTimer" /* name */, true /* isDaemon */);

    private final FineTask fineTask = new FineTask();
    private final HourlyTask hourlyTask = new HourlyTask();
    private final DailyTask dailyTask = new DailyTask();
    private final Flusher flusher = new Flusher();

    private PlatformTransactionManager transactionManager;

    private static final BoundedPriorityQueue queue = new BoundedPriorityQueue(500);
    private ServiceManager serviceManager;

    public ServiceMetricsManager(String clusterNodeId) {
        this._clusterNodeId = clusterNodeId;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    /**
     * @deprecated for proxying purposes only
     */
    protected ServiceMetricsManager() {
    }

    public void destroy() throws Exception {
        fineTimer.cancel();
        hourlyTimer.cancel();
        dailyTimer.cancel();
        flusher.quit();
    }

    /**
     * A timer task to execute at fine resolution binning interval; to close off
     * and archive the current fine resolution bins and start new ones.
     */
    private class FineTask extends TimerTask {
        public void run() {
            logger.fine("FineTask running at " + ISO8601Date.format(new Date()));
            Iterator itor = _serviceMetricsMap.values().iterator();
            while (itor.hasNext()) {
                ServiceMetrics serviceMetrics = (ServiceMetrics)itor.next();
                serviceMetrics.archiveFineBin(_numFineBins);
            }
        }
    }

    /**
     * A timer task to execute at every hour; to close off and archive the
     * current hourly bins and start new ones.
     */
    private class HourlyTask extends TimerTask {
        public void run() {
            int num = 0;
            Iterator itor = _serviceMetricsMap.values().iterator();
            while (itor.hasNext()) {
                ServiceMetrics serviceMetrics = (ServiceMetrics)itor.next();
                serviceMetrics.archiveHourlyBin(_numHourlyBins);
                num++;
            }
            logger.fine("Hourly archiving task completed; archived " + num + " hourly bins");
        }
    }

    /**
     * A timer task to execute at every midnight; to close off and archive the
     * current daily bins and start new ones.
     */
    private class DailyTask extends TimerTask {
        public void run() {
            int num = 0;
            Iterator itor = _serviceMetricsMap.values().iterator();
            while (itor.hasNext()) {
                ServiceMetrics serviceMetrics = (ServiceMetrics)itor.next();
                serviceMetrics.archiveDailyBin(_numDailyBins);
                num++;
            }
            logger.fine("Daily archiving task completed; archived " + num + " daily bins");
        }
    }

    private class Flusher extends Thread {
        private volatile boolean quit;

        public Flusher() {
            setDaemon(true);
        }

        public void quit() {
            quit = true;
            interrupt();
        }

        public void run() {
            logger.info("Database flusher beginning");
            while (!quit) {
                try {
                    final MetricsBin head = (MetricsBin)queue.take();

                    logger.finer("Saving " + head.toString());
                    new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            try {
                                getSession().save(head);
                            } catch (Exception e) {
                                throw new RuntimeException("Error saving MetricsBin", e);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    logger.info("Database flusher exiting");
                } catch (RuntimeException e) {
                    logger.log(Level.SEVERE, "Couldn't save MetricsBin", e);
                }
            }
        }
    }

    /**
     * Minimum allowed fine resolution bin interval (in milliseconds).
     */
    private static final int MIN_FINE_BIN_INTERVAL = 1000; // 1 second

    /**
     * Maximum allowed fine resolution bin interval (in milliseconds).
     */
    private static final int MAX_FINE_BIN_INTERVAL = 5 * 60 * 1000; // 5 minutes

    /**
     * Default fine resolution bin interval (in milliseconds).
     */
    private static final int DEF_FINE_BIN_INTERVAL = 5 * 1000; // 5 seconds

    /**
     * Limit and default values for number of fine resolution bins are
     * designed to fill 1 hour.
     */
    private static final int MIN_NUM_FINE_BINS = 60 * 60 * 1000 / MAX_FINE_BIN_INTERVAL;
    private static final int MAX_NUM_FINE_BINS = 60 * 60 * 1000 / MIN_FINE_BIN_INTERVAL;
    private static final int DEF_NUM_FINE_BINS = 60 * 60 * 1000 / DEF_FINE_BIN_INTERVAL;

    private static final int MIN_NUM_HOURLY_BINS = 24;          // a day
    private static final int MAX_NUM_HOURLY_BINS = 24 * 31;     // a month
    private static final int DEF_NUM_HOURLY_BINS = 24 * 7;      // a week

    private static final int MIN_NUM_DAILY_BINS = 31;           // a month
    private static final int MAX_NUM_DAILY_BINS = 365 * 10;     // 10 years
    private static final int DEF_NUM_DAILY_BINS = 365;          // 1 year

    private static final Logger logger = Logger.getLogger(ServiceMetricsManager.class.getName());

    /**
     * Whether statistics collecting is turned on.
     */
    private boolean _enabled;

    /**
     * Fine resolution bin interval (in milliseconds).
     */
    private int _fineBinInterval;

    /**
     * Maximum number of fine resolution bins to archive.
     */
    private int _numFineBins;

    /**
     * Maximum number of hourly resolution bins to archive.
     */
    private int _numHourlyBins;

    /**
     * Maximum number of daily resolution bins to archive.
     */
    private int _numDailyBins;

    private final Map _serviceMetricsMap = new HashMap/* <Long, ServiceMetrics> */();
    private final Object _serviceMetricsMapLock = new Object();

    /**
     * Convenience method to return a system property value parsed into an
     * integer, constrained by the given lower and upper limits. If the system
     * property does not exist, or is not parsable as an integer, then the given
     * default value is returned instead.
     */
    private static int getIntProperty(final String name, final int lower, final int upper, final int defaultInt) {
        final String value = System.getProperty(name);
        if (value == null) {
            return defaultInt;
        } else {
            try {
                final int intValue = Integer.parseInt(value);
                if (intValue < lower) {
                    logger.warning("Imposing lower constraint (" + lower + ") on system property value (" + intValue + "): " + name);
                    return lower;
                } else if (intValue > upper) {
                    logger.warning("Imposing upper constraint (" + upper + ") on system property value (" + intValue + "): " + name);
                    return upper;
                }
                return intValue;
            } catch (NumberFormatException e) {
                logger.info("Using default value (" + defaultInt + ") for missing system property: " + name);
                return defaultInt;
            }
        }
    }

    /**
     * Gets the service metrics for a given published service.
     *
     * @return null if service metrics processing is disabled
     */
    public ServiceMetrics getServiceMetrics(final long serviceOid) {
        if (_enabled) {
            final Long oid = new Long(serviceOid);
            ServiceMetrics serviceMetrics;
            synchronized (_serviceMetricsMapLock) {
                serviceMetrics = (ServiceMetrics) _serviceMetricsMap.get(oid);
                if (serviceMetrics == null) {
                    serviceMetrics = new ServiceMetrics(serviceOid, _clusterNodeId, _fineBinInterval, queue);
                    _serviceMetricsMap.put(oid, serviceMetrics);
                }
            }
            return serviceMetrics;
        } else {
            return null;
        }
    }

    public List findBins(String nodeId, Long minPeriodStart,
                         Long maxPeriodStart, Integer resolution,
                         Long serviceOid)
            throws FindException
    {
        final Session session = getSession();
        Criteria crit = session.createCriteria(MetricsBin.class);

        if (minPeriodStart != null)
            crit.add(Restrictions.ge("periodStart", minPeriodStart));

        if (maxPeriodStart != null)
            crit.add(Restrictions.le("periodStart", maxPeriodStart));

        if (nodeId != null)
            crit.add(Restrictions.eq("clusterNodeId", nodeId));

        if (serviceOid != null)
            crit.add(Restrictions.eq("serviceOid", serviceOid));

        if (resolution != null)
            crit.add(Restrictions.eq("resolution", resolution));

        FlushMode old = session.getFlushMode();
        try {
            session.setFlushMode(FlushMode.NEVER);
            return crit.list();
        } catch (HibernateException e) {
            throw new FindException("Couldn't find MetricsBins", e);
        } finally {
            if (session != null && old != null) session.setFlushMode(old);
        }
    }

    protected void initDao() throws Exception {
        if (transactionManager == null) throw new IllegalStateException("TransactionManager must be set");
        if (serviceManager == null) throw new IllegalStateException("ServiceManager must be set");
        if (_clusterNodeId == null) throw new IllegalStateException("clusterNodeId must be set");

        _enabled = Boolean.valueOf(System.getProperty("com.l7tech.service.statistics.enabled", "true")).booleanValue();
        if (! _enabled) {
            return;
        }

        _fineBinInterval = getIntProperty("com.l7tech.service.metrics.fineBinInterval",
                                          MIN_FINE_BIN_INTERVAL,
                                          MAX_FINE_BIN_INTERVAL,
                                          DEF_FINE_BIN_INTERVAL);
        _numFineBins = getIntProperty("com.l7tech.service.metrics.numFineBins",
                                      MIN_NUM_FINE_BINS,
                                      MAX_NUM_FINE_BINS,
                                      DEF_NUM_FINE_BINS);
        _numHourlyBins = getIntProperty("com.l7tech.service.metrics.numHourlyBins",
                                        MIN_NUM_HOURLY_BINS,
                                        MAX_NUM_HOURLY_BINS,
                                        DEF_NUM_HOURLY_BINS);
        _numDailyBins = getIntProperty("com.l7tech.service.metrics.numDailyBins",
                                       MIN_NUM_DAILY_BINS,
                                       MAX_NUM_DAILY_BINS,
                                       DEF_NUM_DAILY_BINS);

        // Populate initial bins
        Collection serviceHeaders = serviceManager.findAllHeaders();
        for (Iterator i = serviceHeaders.iterator(); i.hasNext();) {
            EntityHeader service = (EntityHeader)i.next();
            getServiceMetrics(service.getOid());
        }

        // Gets current time in local time zone and locale.
        final GregorianCalendar now = new GregorianCalendar();
        now.setLenient(true);

        // Sets fine resolution timer task; starting a fine interval from now.
        GregorianCalendar nextFineStart = (GregorianCalendar)now.clone();
        nextFineStart.setTimeInMillis(((now.getTimeInMillis() / _fineBinInterval) + 1) * _fineBinInterval);
        fineTimer.scheduleAtFixedRate(fineTask, nextFineStart.getTime(), _fineBinInterval);
        logger.config("Fine archive interval is " + _fineBinInterval + "ms");
        logger.config("Scheduled first fine archive task for " + nextFineStart.getTime());

        // Sets hourly resolution timer task to execute on the hour; starting at the next hour.
        GregorianCalendar nextHourStart = (GregorianCalendar)now.clone();
        nextHourStart.set(Calendar.HOUR, now.get(Calendar.HOUR) + 1);
        nextHourStart.set(Calendar.MINUTE, 0);
        nextHourStart.set(Calendar.SECOND, 0);
        hourlyTimer.scheduleAtFixedRate(hourlyTask, nextHourStart.getTime(), 60 * 60 * 1000);
        logger.config("Scheduled first hourly archive task for " + nextHourStart.getTime());

        // Sets daily resolution timer task to execute at midnight; starting at the next midnight.
        GregorianCalendar nextDayStart = (GregorianCalendar)now.clone();
        nextDayStart.set(Calendar.DATE, now.get(Calendar.DATE) + 1);
        nextDayStart.set(Calendar.HOUR, 0);
        nextDayStart.set(Calendar.MINUTE, 0);
        nextDayStart.set(Calendar.SECOND, 0);
        dailyTimer.scheduleAtFixedRate(dailyTask, nextDayStart.getTime(), 24 * 60 * 60 * 1000);
        logger.config("Scheduled first daily archive task for " + nextDayStart.getTime());

        // Flusher waits forever on {@link #queue}
        flusher.start();
    }

}