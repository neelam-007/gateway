package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.BoundedPriorityQueue;
import com.l7tech.common.util.Background;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.MetricsBin;
import org.hibernate.*;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
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

    private static final String HQL_DELETE = "DELETE FROM " + MetricsBin.class.getName() + " WHERE periodStart < ? AND resolution = ?";
    private static final int MINUTE = 60 * 1000;
    private static final int HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long YEAR = 365 * DAY;

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
        fineDeleter.cancel();
        hourlyDeleter.cancel();
        dailyDeleter.cancel();
        flusher.quit();
    }

    /**
     * A timer task to execute at fine resolution binning interval; to close off
     * and archive the current fine resolution bins and start new ones.
     */
    private class FineTask extends TimerTask {
        public void run() {
            if (logger.isLoggable(Level.FINE))
                logger.fine("FineTask running at " + ISO8601Date.format(new Date()));
            Iterator itor = _serviceMetricsMap.values().iterator();
            while (itor.hasNext()) {
                ServiceMetrics serviceMetrics = (ServiceMetrics)itor.next();
                serviceMetrics.archiveFineBin(fineTtl);
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
                serviceMetrics.archiveHourlyBin(hourlyTtl);
                num++;
            }
            if (logger.isLoggable(Level.FINE))
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
                serviceMetrics.archiveDailyBin(dailyTtl);
                num++;
            }
            if (logger.isLoggable(Level.FINE))
                logger.fine("Daily archiving task completed; archived " + num + " daily bins");
        }
    }

    private class Flusher extends Thread {
        private volatile boolean quit;

        public Flusher() {
            super(Flusher.class.getName());
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

                    if (logger.isLoggable(Level.FINER))
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
                    Thread.currentThread().interrupt();
                    if (quit) logger.info("Database flusher exiting");
                } catch (DataIntegrityViolationException e) {
                    logger.log(Level.INFO, "Failed to save a MetricsBin due to constraint violation; likely clock skew");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Couldn't save MetricsBin", e);
                }
            }
        }
    }

    private class DeleteTask extends TimerTask {
        private final long ttl;
        private final int resolution;

        private DeleteTask(long ttl, int resolution) {
            this.ttl = ttl;
            this.resolution = resolution;
        }

        public void run() {
            final long oldestSurvivor = System.currentTimeMillis() - ttl;
            try {
                Integer num = (Integer)new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
                    public Object doInTransaction(TransactionStatus status) {
                        Query query = getSession().createQuery(HQL_DELETE);
                        query.setLong(0, oldestSurvivor);
                        query.setInteger(1, resolution);
                        return new Integer(query.executeUpdate());
                    }
                });
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Deleted {0} {1} bins older than {2}",
                            new Object[] {
                                num,
                                MetricsBin.describeResolution(resolution),
                                new Date(oldestSurvivor)
                            });
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't delete MetricsBins", e);
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
    private static final int MAX_FINE_BIN_INTERVAL = 5 * MINUTE; // 5 minutes

    /**
     * Default fine resolution bin interval (in milliseconds).
     */
    private static final int DEF_FINE_BIN_INTERVAL = 5 * 1000; // 5 seconds

    /**
     * Limit and default values for number of fine resolution bins are
     * designed to fill 1 hour.
     */
    private static final int MIN_FINE_AGE = HOUR;
    private static final int MAX_FINE_AGE = HOUR;
    private static final int DEF_FINE_AGE = HOUR;

    private static final long MIN_HOURLY_AGE = DAY;          // a day
    private static final long MAX_HOURLY_AGE = 31 * DAY;     // a month
    private static final long DEF_HOURLY_AGE = 7 * DAY;      // a week

    private static final long MIN_DAILY_AGE = 31 * DAY;           // a month
    private static final long MAX_DAILY_AGE = 10 * YEAR;     // 10 years
    private static final long DEF_DAILY_AGE = YEAR;          // 1 year

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
    private long fineTtl;

    /**
     * Maximum number of hourly resolution bins to archive.
     */
    private long hourlyTtl;

    /**
     * Maximum number of daily resolution bins to archive.
     */
    private long dailyTtl;

    private DeleteTask fineDeleter;
    private DeleteTask hourlyDeleter;
    private DeleteTask dailyDeleter;

    private final Map _serviceMetricsMap = new HashMap/* <Long, ServiceMetrics> */();
    private final Object _serviceMetricsMapLock = new Object();

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
                    logger.warning("Imposing lower constraint (" + lower + ") on system property value (" + longValue + "): " + name);
                    return lower;
                } else if (longValue > upper) {
                    logger.warning("Imposing upper constraint (" + upper + ") on system property value (" + longValue + "): " + name);
                    return upper;
                }
                return longValue;
            } catch (NumberFormatException e) {
                logger.info("Using default value (" + defaultValue + ") for missing system property: " + name);
                return defaultValue;
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

    /**
     *
     * @param resolution the resolution of the bins to be queried
     * @param startTime the minimum periodStart value to search for
     * @param duration the length of time within which periodStarts are to be found
     * @param nodeId the MAC address of the cluster node to search for
     * @param serviceOid the OID of the {@link com.l7tech.service.PublishedService}
     *                   to search for
     * @return a new MetricsBin that created by aggregating the bins matching the 
     *         query parameters
     * @throws FindException
     */
    public MetricsBin getMetricsSummary(int resolution,
                                        long startTime,
                                        int duration,
                                        String nodeId,
                                        Long serviceOid)
            throws FindException
    {
        Criteria crit = getSession().createCriteria(MetricsBin.class);
        crit.add(Restrictions.eq("resolution", new Integer(resolution)));

        crit.add(Restrictions.ne("numAttemptedRequest", new Integer(0)));

        if (nodeId != null) {
            crit.add(Restrictions.eq("clusterNodeId", nodeId));
        }

        if (serviceOid != null) {
            crit.add(Restrictions.eq("serviceOid", serviceOid));
        }

        crit.add(Restrictions.ge("periodStart", new Long(startTime)));
        crit.add(Restrictions.le("periodStart", new Long(startTime + duration)));

        ProjectionList plist = Projections.projectionList();
        plist.add(Projections.rowCount());
        plist.add(Projections.sum("sumBackendResponseTime"));
        plist.add(Projections.min("minBackendResponseTime"));
        plist.add(Projections.max("maxBackendResponseTime"));

        plist.add(Projections.sum("sumFrontendResponseTime"));
        plist.add(Projections.min("minFrontendResponseTime"));
        plist.add(Projections.max("maxFrontendResponseTime"));

        plist.add(Projections.sum("numAttemptedRequest"));
        plist.add(Projections.sum("numAuthorizedRequest"));
        plist.add(Projections.sum("numCompletedRequest"));

        plist.add(Projections.min("startTime"));
        plist.add(Projections.max("endTime"));

        crit.setProjection(plist);

        List results = crit.list();
        Object[] row = (Object[])results.get(0);

        int n = 0;
        long backSum = 0;
        int backMin = 0;
        int backMax = 0;

        long frontSum = 0;
        int frontMin = 0;
        int frontMax = 0;

        int attempted = 0;
        int authorized = 0;
        int completed = 0;

        long stime = startTime;
        long etime = startTime + duration;

        int count = ((Integer)row[n++]).intValue();
        if (count > 0) {
            backSum = ((Long)row[n++]).longValue();
            backMin = ((Integer)row[n++]).intValue();
            backMax = ((Integer)row[n++]).intValue();

            frontSum = ((Long)row[n++]).longValue();
            frontMin = ((Integer)row[n++]).intValue();
            frontMax = ((Integer)row[n++]).intValue();

            attempted = ((Integer)row[n++]).intValue();
            authorized = ((Integer)row[n++]).intValue();
            completed = ((Integer)row[n++]).intValue();

            stime = ((Long)row[n++]).longValue();
            etime = ((Long)row[n]).longValue();
        }

        MetricsBin rollup = new MetricsBin(stime, duration, resolution, nodeId, serviceOid == null ? -1 : serviceOid.longValue());
        rollup.setMinFrontendResponseTime(frontMin);
        rollup.setMaxFrontendResponseTime(frontMax);
        rollup.setSumFrontendResponseTime(frontSum);

        rollup.setMinBackendResponseTime(backMin);
        rollup.setMaxBackendResponseTime(backMax);
        rollup.setSumBackendResponseTime(backSum);

        rollup.setNumAttemptedRequest(attempted);
        rollup.setNumAuthorizedRequest(authorized);
        rollup.setNumCompletedRequest(completed);

        rollup.setEndTime(etime);

        return rollup;
    }


    protected void initDao() throws Exception {
        if (transactionManager == null) throw new IllegalStateException("TransactionManager must be set");
        if (serviceManager == null) throw new IllegalStateException("ServiceManager must be set");
        if (_clusterNodeId == null) throw new IllegalStateException("clusterNodeId must be set");

        _enabled = Boolean.valueOf(System.getProperty("com.l7tech.service.metrics.enabled", "true")).booleanValue();
        if (! _enabled) {
            return;
        }

        _fineBinInterval = (int)getLongProperty("com.l7tech.service.metrics.fineBinInterval",
                                          MIN_FINE_BIN_INTERVAL,
                                          MAX_FINE_BIN_INTERVAL,
                                          DEF_FINE_BIN_INTERVAL);

        fineTtl = getLongProperty("com.l7tech.service.metrics.maxFineAge",
                                      MIN_FINE_AGE,
                                      MAX_FINE_AGE,
                                      DEF_FINE_AGE);

        hourlyTtl = getLongProperty("com.l7tech.service.metrics.maxHourlyAge",
                                        MIN_HOURLY_AGE,
                                        MAX_HOURLY_AGE,
                                        DEF_HOURLY_AGE);

        dailyTtl = getLongProperty("com.l7tech.service.metrics.maxDailyAge",
                                       MIN_DAILY_AGE,
                                       MAX_DAILY_AGE,
                                       DEF_DAILY_AGE);

        fineDeleter = new DeleteTask(fineTtl, MetricsBin.RES_FINE);
        Background.schedule(fineDeleter, MINUTE, 5 * MINUTE);
        logger.config("Scheduled first fine deletion task for " + new Date(System.currentTimeMillis() + MINUTE));

        hourlyDeleter = new DeleteTask(hourlyTtl, MetricsBin.RES_HOURLY);
        Background.schedule(hourlyDeleter, 15 * MINUTE, 12 * HOUR);
        logger.config("Scheduled first hourly deletion task for " + new Date(System.currentTimeMillis() + 15 * MINUTE));

        dailyDeleter = new DeleteTask(dailyTtl, MetricsBin.RES_DAILY);
        Background.schedule(dailyDeleter, HOUR, 24 * HOUR);
        logger.config("Scheduled first daily deletion task for " + new Date(System.currentTimeMillis() + HOUR));

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