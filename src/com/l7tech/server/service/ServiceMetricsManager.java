package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.BoundedPriorityQueue;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.MetricsBin;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the processing and accumulation of service metrics for one SSG node.
 *
 * @author rmak
 * @author alex
 */

@Transactional(propagation=Propagation.SUPPORTS)
public class ServiceMetricsManager extends HibernateDaoSupport
        implements InitializingBean, DisposableBean {

    //- PUBLIC

    public ServiceMetricsManager(String clusterNodeId, Timer fineTimer, Timer hourlyTimer, Timer dailyTimer, Timer deleteTimer) {
        _clusterNodeId = clusterNodeId;

        if (fineTimer == null) fineTimer = new Timer("ServiceMetricsManager.fineTimer", true /* isDaemon */);
        if (hourlyTimer == null) hourlyTimer = new Timer("ServiceMetricsManager.hourlyTimer", true /* isDaemon */);
        if (dailyTimer == null) dailyTimer = new Timer("ServiceMetricsManager.dailyTimer", true /* isDaemon */);
        if (deleteTimer == null) deleteTimer = new Timer("ServiceMetricsManager.deleteTimer", true /* isDaemon */);

        _fineTimer = fineTimer;
        _hourlyTimer = hourlyTimer;
        _dailyTimer = dailyTimer;
        _deleteTimer = deleteTimer;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        _transactionManager = transactionManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        _serviceManager = serviceManager;
    }

    public void destroy() throws Exception {
        if (_fineTimer != null) _fineTimer.cancel();
        if (_hourlyTimer != null) _hourlyTimer.cancel();
        if (_dailyTimer != null) _dailyTimer.cancel();
        if (_fineDeleter != null) _fineDeleter.cancel();
        if (_hourlyDeleter != null) _hourlyDeleter.cancel();
        if (_dailyDeleter != null) _dailyDeleter.cancel();
        if (_flusher != null) _flusher.quit();
        if (_flusherThread != null) _flusherThread.interrupt();
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
            String clusterNodeId = _clusterNodeId;
            int fineBinInterval = _fineBinInterval;
            synchronized (_serviceMetricsMapLock) {
                serviceMetrics = (ServiceMetrics) _serviceMetricsMap.get(oid);
                if (serviceMetrics == null) {
                    serviceMetrics = new ServiceMetrics(serviceOid, clusterNodeId, fineBinInterval, _flusherQueue);
                    _serviceMetricsMap.put(oid, serviceMetrics);
                }
            }
            return serviceMetrics;
        } else {
            return null;
        }
    }

    @Transactional(propagation=Propagation.REQUIRED, readOnly=true, rollbackFor=Throwable.class)
    public List findBins(final String nodeId, final Long minPeriodStart,
                         final Long maxPeriodStart, final Integer resolution,
                         final Long serviceOid)
            throws FindException
    {
        try {
            return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
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

                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Couldn't find MetricsBins", e);
        }
    }

    /**
     * Summarizes the latest metrics bins in the database for the given criteria.
     *
     * @param clusterNodeId the MAC address of the cluster node to search for
     * @param serviceOid    the OID of the {@link com.l7tech.service.PublishedService}
     *                      to search for
     * @param resolution    the bin resolution to search for
     * @param duration      time duration (from latest nominal period boundary
     *                      time on gateway) to search for bins whose nominal
     *                      periods fall within
     * @return a {@link MetricsBin} summarizing the bins that fit the given
     *         criteria
     */
    public MetricsBin getLatestMetricsSummary(final String clusterNodeId,
                                              final Long serviceOid,
                                              final int resolution,
                                              final int duration) {
        // Computes the summary period by counting back from the latest nominal
        // period boundary time. This is to ensure that we will find a full
        // number of bins filling the given duration (e.g., a 24-hour duration
        // will find 24 hourly bins; when they are all available).
        final long summaryPeriodEnd = MetricsBin.periodStartFor(resolution, _fineBinInterval, System.currentTimeMillis());
        final long summaryPeriodStart = summaryPeriodEnd - duration;

        final MetricsBin summaryBin = new MetricsBin(summaryPeriodStart,
                duration, resolution, clusterNodeId,
                serviceOid == null ? -1 : serviceOid.longValue());

        List bins = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            public Object doInHibernateReadOnly(Session session) throws HibernateException {
                final Criteria criteria = session.createCriteria(MetricsBin.class);
                criteria.add(Restrictions.eq("resolution", new Integer(resolution)));
                if (clusterNodeId != null) {
                    criteria.add(Restrictions.eq("clusterNodeId", clusterNodeId));
                }
                if (serviceOid != null) {
                    criteria.add(Restrictions.eq("serviceOid", serviceOid));
                }
                criteria.add(Restrictions.ge("periodStart", new Long(summaryPeriodStart)));
                criteria.add(Restrictions.lt("periodStart", new Long(summaryPeriodEnd)));
                return criteria.list();
            }
        });

        if (bins.size() != 0) {
            MetricsBin.combine(bins, summaryBin);
        }

        summaryBin.setPeriodStart(summaryPeriodStart);
        summaryBin.setInterval(duration);
        summaryBin.setEndTime(summaryPeriodEnd);
        return summaryBin;
    }

    //- PROTECTED

    protected void initDao() throws Exception {
        if (_transactionManager == null) throw new IllegalStateException("TransactionManager must be set");
        if (_serviceManager == null) throw new IllegalStateException("ServiceManager must be set");
        if (_clusterNodeId == null) throw new IllegalStateException("clusterNodeId must be set");

        _enabled = Boolean.valueOf(System.getProperty("com.l7tech.service.metrics.enabled", "true")).booleanValue();
        if (! _enabled) {
            return;
        }

        _fineBinInterval = (int)getLongProperty("com.l7tech.service.metrics.fineBinInterval", MIN_FINE_BIN_INTERVAL, MAX_FINE_BIN_INTERVAL, DEF_FINE_BIN_INTERVAL);

        final long fineTtl = getLongProperty("com.l7tech.service.metrics.maxFineAge", MIN_FINE_AGE, MAX_FINE_AGE, DEF_FINE_AGE);
        final long hourlyTtl = getLongProperty("com.l7tech.service.metrics.maxHourlyAge", MIN_HOURLY_AGE, MAX_HOURLY_AGE, DEF_HOURLY_AGE);
        final long dailyTtl = getLongProperty("com.l7tech.service.metrics.maxDailyAge", MIN_DAILY_AGE, MAX_DAILY_AGE, DEF_DAILY_AGE);

        _fineDeleter = new DeleteTask(fineTtl, MetricsBin.RES_FINE);
        _deleteTimer.schedule(_fineDeleter, MINUTE, 5 * MINUTE);
        _logger.config("Scheduled first fine deletion task for " + new Date(System.currentTimeMillis() + MINUTE));

        _hourlyDeleter = new DeleteTask(hourlyTtl, MetricsBin.RES_HOURLY);
        _deleteTimer.schedule(_hourlyDeleter, 15 * MINUTE, 12 * HOUR);
        _logger.config("Scheduled first hourly deletion task for " + new Date(System.currentTimeMillis() + 15 * MINUTE));

        _dailyDeleter = new DeleteTask(dailyTtl, MetricsBin.RES_DAILY);
        _deleteTimer.schedule(_dailyDeleter, HOUR, 24 * HOUR);
        _logger.config("Scheduled first daily deletion task for " + new Date(System.currentTimeMillis() + HOUR));

        // Populates initial bins.
        Collection serviceHeaders = _serviceManager.findAllHeaders();
        for (Iterator i = serviceHeaders.iterator(); i.hasNext();) {
            EntityHeader service = (EntityHeader)i.next();
            getServiceMetrics(service.getOid());
        }

        // Schedule the timers.
        final long now = System.currentTimeMillis();

        // Sets fine resolution timer to excecute every fine interval; starting at the next fine period.
        final Date nextFineStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_FINE, _fineBinInterval, now));
        _fineTimer.scheduleAtFixedRate(new FineTask(), nextFineStart, _fineBinInterval);
        _logger.config("Fine archive interval is " + _fineBinInterval + "ms");
        _logger.config("Scheduled first fine archive task for " + nextFineStart);

        // Sets hourly resolution timer every hour; starting at the next hourly period.
        final Date nextHourlyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_HOURLY, 0, now));
        _hourlyTimer.scheduleAtFixedRate(new HourlyTask(), nextHourlyStart, HOUR);
        _logger.config("Scheduled first hourly archive task for " + nextHourlyStart);

        // Sets daily resolution timer to execute at the next daily period.
        // But can't just schedule at fixed rate of 24-hours interval because a
        // calender day varies, e.g., when switching Daylight Savings Time.
        final Date nextDailyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, now));
        _dailyTimer.schedule(new DailyTask(), nextDailyStart);
        _logger.config("Scheduled first daily archive task for " + nextDailyStart);

        // Flusher waits forever on {@link #_flusherQueue}.
        _flusherThread = new Thread(_flusher, _flusher.getClass().getName());
        _flusherThread.start();
    }

    //- PRIVATE

    private static final String HQL_DELETE = "DELETE FROM " + MetricsBin.class.getName() + " WHERE periodStart < ? AND resolution = ?";
    private static final int MINUTE = 60 * 1000;
    private static final int HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long YEAR = 365 * DAY;

    /** Minimum allowed fine resolution bin interval (in milliseconds). */
    private static final int MIN_FINE_BIN_INTERVAL = 1000; // 1 second

    /** Maximum allowed fine resolution bin interval (in milliseconds). */
    private static final int MAX_FINE_BIN_INTERVAL = 5 * MINUTE; // 5 minutes

    /** Default fine resolution bin interval (in milliseconds). */
    private static final int DEF_FINE_BIN_INTERVAL = 5 * 1000; // 5 seconds

    // Limits and default values for number of fine resolution bins are designed to fill 1 hour.
    private static final int MIN_FINE_AGE = HOUR;
    private static final int MAX_FINE_AGE = HOUR;
    private static final int DEF_FINE_AGE = HOUR;

    private static final long MIN_HOURLY_AGE = DAY;          // a day
    private static final long MAX_HOURLY_AGE = 31 * DAY;     // a month
    private static final long DEF_HOURLY_AGE = 7 * DAY;      // a week

    private static final long MIN_DAILY_AGE = 31 * DAY;      // a month
    private static final long MAX_DAILY_AGE = 10 * YEAR;     // 10 years
    private static final long DEF_DAILY_AGE = YEAR;          // 1 year

    private static final Logger _logger = Logger.getLogger(ServiceMetricsManager.class.getName());

    /** ID for this node. */
    private String _clusterNodeId;

    /** Whether statistics collecting is turned on. */
    private boolean _enabled;

    /** Fine resolution bin interval (in milliseconds). */
    private int _fineBinInterval;

    private final Timer _fineTimer;
    private final Timer _hourlyTimer;
    private final Timer _dailyTimer;
    private final Timer _deleteTimer;

    private DeleteTask _fineDeleter;
    private DeleteTask _hourlyDeleter;
    private DeleteTask _dailyDeleter;

    private Thread _flusherThread;
    private final Flusher _flusher = new Flusher();
    private static final BoundedPriorityQueue _flusherQueue = new BoundedPriorityQueue(500);

    private final Map _serviceMetricsMap = new HashMap/* <service OID as Long, ServiceMetrics> */();
    private final Object _serviceMetricsMapLock = new Object();

    private PlatformTransactionManager _transactionManager;

    private ServiceManager _serviceManager;

    /**
     * Convenience method to return a system property value parsed into an
     * integer, constrained by the given lower and upper limits. If the system
     * property does not exist, or is not parsable as an integer, then the given
     * default value is returned instead.
     */
    private static long getLongProperty(final String name, final long lower, final long upper, final long defaultValue) {
        final String value = System.getProperty(name);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                final long longValue = Long.parseLong(value);
                if (longValue < lower) {
                    _logger.warning("Imposing lower constraint (" + lower + ") on system property value (" + longValue + "): " + name);
                    return lower;
                } else if (longValue > upper) {
                    _logger.warning("Imposing upper constraint (" + upper + ") on system property value (" + longValue + "): " + name);
                    return upper;
                }
                return longValue;
            } catch (NumberFormatException e) {
                _logger.info("Using default value (" + defaultValue + ") for missing system property: " + name);
                return defaultValue;
            }
        }
    }

    /**
     * A timer task to execute at fine resolution binning interval; to close off
     * and archive the current fine resolution bins and start new ones.
     */
    private class FineTask extends TimerTask {
        public void run() {
            if (_logger.isLoggable(Level.FINER))
                _logger.finer("FineTask running at " + new Date());
            List metricsList = new ArrayList();
            synchronized(_serviceMetricsMapLock) {
                metricsList.addAll(_serviceMetricsMap.values());
            }
            Iterator itor = metricsList.iterator();
            while (itor.hasNext()) {
                ServiceMetrics serviceMetrics = (ServiceMetrics)itor.next();
                serviceMetrics.archiveFineBin();
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
            List metricsList = new ArrayList();
            synchronized(_serviceMetricsMapLock) {
                metricsList.addAll(_serviceMetricsMap.values());
            }
            Iterator itor = metricsList.iterator();
            while (itor.hasNext()) {
                ServiceMetrics serviceMetrics = (ServiceMetrics)itor.next();
                serviceMetrics.archiveHourlyBin();
                num++;
            }
            if (_logger.isLoggable(Level.FINE))
                _logger.fine("Hourly archiving task completed; archived " + num + " hourly bins");
        }
    }

    /**
     * A timer task to execute at every midnight; to close off and archive the
     * current daily bins and start new ones.
     */
    private class DailyTask extends TimerTask {
        public void run() {
            int num = 0;
            List metricsList = new ArrayList();
            synchronized(_serviceMetricsMapLock) {
                metricsList.addAll(_serviceMetricsMap.values());
            }
            Iterator itor = metricsList.iterator();
            while (itor.hasNext()) {
                ServiceMetrics serviceMetrics = (ServiceMetrics)itor.next();
                serviceMetrics.archiveDailyBin();
                num++;
            }
            if (_logger.isLoggable(Level.FINE))
                _logger.fine("Daily archiving task completed; archived " + num + " daily bins");

            // Schedule the next timer execution at the end of current period
            // (with a new task instance because a task cannot be reused).
            Date nextTimerDate = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, System.currentTimeMillis()));
            _dailyTimer.schedule(new DailyTask(), nextTimerDate);
            if (_logger.isLoggable(Level.FINE))
                _logger.fine("Scheduled next daily archive task for " + nextTimerDate);
        }
    }

    /**
     * Flush queued metrics bins to the database.
     */
    private class Flusher implements Runnable {
        private Object flusherLock = new Object();
        private boolean quit;

        Flusher() {
            synchronized(flusherLock) {
                quit = false;
            }
        }

        public void quit() {
            synchronized(flusherLock) {
                quit = true;
            }
        }

        public void run() {
            _logger.info("Database flusher beginning");
            while (true) {
                boolean stop;
                synchronized(flusherLock) {
                    stop = quit;
                }
                if (stop) {
                    break;
                }
                try {
                    final MetricsBin head = (MetricsBin)_flusherQueue.take();

                    if (_logger.isLoggable(Level.FINER))
                        _logger.finer("Saving " + head.toString());

                    new TransactionTemplate(_transactionManager).execute(new TransactionCallbackWithoutResult() {
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            try {
                                getHibernateTemplate().execute(new HibernateCallback(){
                                    public Object doInHibernate(Session session) throws HibernateException {
                                        Criteria criteria = session.createCriteria(MetricsBin.class);
                                        criteria.add(Restrictions.eq("clusterNodeId", head.getClusterNodeId()));
                                        criteria.add(Restrictions.eq("serviceOid", head.getServiceOid()));
                                        criteria.add(Restrictions.eq("resolution", head.getResolution()));
                                        criteria.add(Restrictions.eq("periodStart", head.getPeriodStart()));
                                        MetricsBin existing = (MetricsBin) criteria.uniqueResult();
                                        if (existing == null) {
                                            session.save(head);
                                        } else {
                                            if (_logger.isLoggable(Level.FINE)) {
                                                _logger.log(Level.FINE, "Merging contents of duplicate MetricsBin [ClusterNodeId={0}; ServiceOid={1}; Resolution={2}; PeriodStart={3}]",
                                                        new Object[]{head.getClusterNodeId(), head.getServiceOid(), head.getResolution(), head.getPeriodStart()});
                                            }
                                            existing.merge(head);
                                            session.save(existing);
                                        }
                                        return null;
                                    }
                                });
                            } catch (Exception e) {
                                throw new RuntimeException("Error saving MetricsBin", e);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    boolean isQuit;
                    synchronized(flusherLock) {
                        isQuit = quit;
                    }
                    if (!isQuit) {
                        _logger.info("Database flusher quitting due to interrupt.");
                        quit();
                    }
                } catch (DataIntegrityViolationException e) {
                    _logger.log(Level.INFO, "Failed to save a MetricsBin due to constraint violation; likely clock skew");
                } catch (Exception e) {
                    _logger.log(Level.WARNING, "Couldn't save MetricsBin", e);
                }
            }
            _logger.info("Database flusher exiting.");
        }
    }

    /**
     * Timer task to delete old metrics bins from the database.
     */
    private class DeleteTask extends TimerTask {
        private final long _ttl;
        private final int _resolution;

        private DeleteTask(long ttl, int resolution) {
            _ttl = ttl;
            _resolution = resolution;
        }

        public void run() {
            final long oldestSurvivor = System.currentTimeMillis() - _ttl;
            try {
                Integer num = (Integer)new TransactionTemplate(_transactionManager).execute(new TransactionCallback() {
                    public Object doInTransaction(TransactionStatus status) {
                        return getHibernateTemplate().execute(new HibernateCallback() {
                            public Object doInHibernate(Session session) throws HibernateException {
                                Query query = session.createQuery(HQL_DELETE);
                                query.setLong(0, oldestSurvivor);
                                query.setInteger(1, _resolution);
                                return new Integer(query.executeUpdate());
                            }
                        });
                    }
                });
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Deleted {0} {1} bins older than {2}",
                            new Object[] {
                                num,
                                MetricsBin.describeResolution(_resolution),
                                new Date(oldestSurvivor)
                            });
                }
            } catch (Exception e) {
                _logger.log(Level.WARNING, "Couldn't delete MetricsBins", e);
            }
        }
    }
}