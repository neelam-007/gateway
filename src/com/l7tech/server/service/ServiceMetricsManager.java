package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.BoundedPriorityQueue;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.service.MetricsBin;
import com.l7tech.service.MetricsSummaryBin;
import com.l7tech.service.PublishedService;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
        implements InitializingBean, DisposableBean, PropertyChangeListener {

    //- PUBLIC

    public ServiceMetricsManager(String clusterNodeId, ManagedTimer timer) {
        _clusterNodeId = clusterNodeId;

        if (timer == null) timer = new ManagedTimer("ServiceMetricsManager ManagedTimer");
        _timer = timer;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        _transactionManager = transactionManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        _serviceManager = serviceManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        _roleManager = roleManager;
    }

    public void destroy() throws Exception {
        disable();
    }

    /**
     * @return whether collection of service metrics is currently enabled
     */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * Gets the service metrics for a given published service.
     *
     * @param serviceOid    OID of published service
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

    public int getFineInterval() {
        return _fineBinInterval;
    }

    /**
     * Searches for metrics bins with the given criteria and summarizes by
     * combining bins with the same period start.
     *
     * @param nodeId            cluster node ID; null = all
     * @param serviceOids       published service OIDs; null = all services permitted for this user
     * @param resolution        bin resolution ({@link MetricsBin#RES_FINE},
     *                          {@link MetricsBin#RES_HOURLY} or
     *                          {@link MetricsBin#RES_DAILY}); null = all
     * @param minPeriodStart    minimum bin period start time (milliseconds since epoch); null = as far back as available
     * @param maxPeriodStart    maximum bin period statt time (milliseconds since epoch); null = up to the latest available
     * @param includeEmpty      whether to include empty uptime bins (same as include service OID -1)
     *
     * @return collection of summary bins; can be empty but never <code>null</code>
     * @throws FindException if failure to query database
     */
    @Transactional(propagation=Propagation.REQUIRED, readOnly=true, rollbackFor=Throwable.class)
    public Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId,
                                                           final long[] serviceOids,
                                                           final Integer resolution,
                                                           final Long minPeriodStart,
                                                           final Long maxPeriodStart,
                                                           final boolean includeEmpty)
            throws FindException {
        // Enforces RBAC permissions.
        final Set<Long> filteredOids = filterPermittedPublishedServices(serviceOids);
        if (filteredOids != null && includeEmpty) {
            filteredOids.add(-1L);      // Empty uptime bins have service OID of -1.
        }
        if (filteredOids != null && filteredOids.isEmpty()) {
            return Collections.emptyList();     // No bins can possibly be found.
        }

        Collection<MetricsBin> bins = null;
        try {
            // noinspection unchecked
            bins = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    Criteria crit = session.createCriteria(MetricsBin.class);

                    if (nodeId != null)
                        crit.add(Restrictions.eq("clusterNodeId", nodeId));

                    if (filteredOids != null) {
                        if (filteredOids.size() == 1)
                            crit.add(Restrictions.eq("serviceOid", filteredOids.iterator().next()));
                        else
                            crit.add(Restrictions.in("serviceOid", filteredOids));
                    }

                    if (resolution != null)
                        crit.add(Restrictions.eq("resolution", resolution));

                    if (minPeriodStart != null)
                        crit.add(Restrictions.ge("periodStart", minPeriodStart));

                    if (maxPeriodStart != null) {
                        crit.add(Restrictions.le("periodStart", maxPeriodStart));
                    } else {
                        // To prevent the case where not all bins of the latest
                        // period are fetched because the query was made before all
                        // latest bins of all published services have been saved to
                        // database, we relax by 1000 milliseconds for all database
                        // writes to complete. Otherwise, this can cause an obvious
                        // gap to show up in the Dashboard chart if only one service
                        // is receiving requests and it is the one that hasn't been
                        // written to database yet when queried.
                        final long currentTime = System.currentTimeMillis();
                        final long currentPeriodStart = MetricsBin.periodStartFor(resolution,
                                                                                  getFineInterval(),
                                                                                  currentTime - 1000);
                        final long lastestCompletedPeriodStart = MetricsBin.periodStartFor(resolution,
                                                                                           getFineInterval(),
                                                                                           currentPeriodStart - 1);
                        crit.add(Restrictions.le("periodStart", lastestCompletedPeriodStart));
                    }

                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find MetricsBins in database. " +
                                    "(nodeId=" + nodeId +
                                    ", serviceOid=" + filteredOids +
                                    ", resolution=" + resolution +
                                    ", minPeriodStart=" + new Date(minPeriodStart) +
                                    ", maxPeriodStart=" + new Date(maxPeriodStart) + ")",
                                    e);
        }

        if (_logger.isLoggable(Level.FINER)) {
            _logger.finer("Found " + bins.size() + " metrics bins to summarize by period.");
        }

        return MetricsSummaryBin.createSummaryMetricsBinsByPeriodStart(bins);
    }

    /**
     * Searches for the latest metrics bins for the given criteria and
     * summarizes by combining them into one summary bin.
     *
     * @param clusterNodeId cluster node ID; null = all
     * @param serviceOids   published service OIDs; null = all services permitted for this user
     * @param resolution    bin resolution ({@link MetricsBin#RES_FINE},
     *                      {@link MetricsBin#RES_HOURLY} or
     *                      {@link MetricsBin#RES_DAILY})
     * @param duration      time duration (milliseconds from latest nominal period boundary
     *                      time on gateway) to search backward for bins whose
     *                      nominal periods fall within
     * @param includeEmpty  whether to include empty uptime bins (same as include service OID -1)
     *
     * @return a summary bin; <code>null</code> if no metrics bins are found
     * @throws FindException if failure to query database
     */
    public MetricsSummaryBin summarizeLatest(final String clusterNodeId,
                                             final long[] serviceOids,
                                             final int resolution,
                                             final int duration,
                                             final boolean includeEmpty)
            throws FindException {
        // Enforces RBAC permissions.
        final Set<Long> filteredOids = filterPermittedPublishedServices(serviceOids);
        if (filteredOids != null && includeEmpty) {
            filteredOids.add(-1L);      // Empty uptime bins have service OID of -1.
        }
        if (filteredOids != null && filteredOids.isEmpty()) {
            return null;    // No bins can possibly be found.
        }

        // Computes the summary period by counting back from the latest nominal
        // period boundary time. This is to ensure that we will find a full
        // number of bins filling the given duration (e.g., a 24-hour duration
        // will find 24 hourly bins; when they are all available).
        final long summaryPeriodEnd = MetricsBin.periodStartFor(resolution, _fineBinInterval, System.currentTimeMillis());
        final long summaryPeriodStart = summaryPeriodEnd - duration;

        Collection<MetricsBin> bins = null;
        try {
            // noinspection unchecked
            bins = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    final Criteria criteria = session.createCriteria(MetricsBin.class);
                    if (clusterNodeId != null) {
                        criteria.add(Restrictions.eq("clusterNodeId", clusterNodeId));
                    }
                    if (filteredOids != null) {
                        if (filteredOids.size() == 1)
                            criteria.add(Restrictions.eq("serviceOid", filteredOids.iterator().next()));
                        else
                            criteria.add(Restrictions.in("serviceOid", filteredOids));
                    }
                    criteria.add(Restrictions.eq("resolution", new Integer(resolution)));
                    criteria.add(Restrictions.ge("periodStart", new Long(summaryPeriodStart)));
                    criteria.add(Restrictions.lt("periodStart", new Long(summaryPeriodEnd)));
                    return criteria.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find MetricsBins in database. " +
                                    "(clusterNodeId=" + clusterNodeId +
                                    ", serviceOid=" + filteredOids +
                                    ", resolution=" + resolution +
                                    ", duration=" + duration + ")",
                                    e);
        }


        if (_logger.isLoggable(Level.FINER)) {
            _logger.finer("Found " + bins.size() + " metrics bins to summarize.");
        }

        if (bins == null || bins.size() == 0)
            return null;

        final MetricsSummaryBin summaryBin = new MetricsSummaryBin(bins);
        summaryBin.setPeriodStart(summaryPeriodStart);
        summaryBin.setInterval(duration);
        summaryBin.setEndTime(summaryPeriodEnd);
        return summaryBin;
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (CLUSTER_PROP_ENABLED.equals(event.getPropertyName())) {
            if (Boolean.valueOf((String)event.getNewValue())) {
                enable();
            } else {
                disable();
            }
        }
    }

    //- PROTECTED

    protected void initDao() throws Exception {
        if (_transactionManager == null) throw new IllegalStateException("TransactionManager must be set");
        if (_serviceManager == null) throw new IllegalStateException("ServiceManager must be set");
        if (_clusterNodeId == null) throw new IllegalStateException("clusterNodeId must be set");

        if (Boolean.valueOf(ServerConfig.getInstance().getProperty(CLUSTER_PROP_ENABLED))) {
            enable();
        } else {
            _logger.info("Service metrics collection is currently disabled.");
        }
    }

    //- PRIVATE

    /** Name of cluster property that enables/disables service metrics collection. */
    private static final String CLUSTER_PROP_ENABLED = "serviceMetricsEnabled";

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

    /** For synchronizing calling {@link #enable()} and {@link #disable()}. */
    private final Object _enableLock = new Object();

    /** Fine resolution bin interval (in milliseconds). */
    private final int _fineBinInterval = (int)getLongClusterProperty("metricsFineInterval",
                                                                     MIN_FINE_BIN_INTERVAL,
                                                                     MAX_FINE_BIN_INTERVAL,
                                                                     DEF_FINE_BIN_INTERVAL);

    /** One timer for all tasks. */
    private final ManagedTimer _timer;

    // Tasks to close completed bins and generate new ones.
    private FineTask _fineArchiver;
    private HourlyTask _hourlyArchiver;
    private DailyTask _dailyArchiver;

    // Tasks to delete old bins from the database.
    private DeleteTask _fineDeleter;
    private DeleteTask _hourlyDeleter;
    private DeleteTask _dailyDeleter;

    private Flusher _flusher;
    private Thread _flusherThread;
    private static final BoundedPriorityQueue _flusherQueue = new BoundedPriorityQueue(500);

    private final Map<Long /* service OID */, ServiceMetrics> _serviceMetricsMap = new HashMap<Long, ServiceMetrics>();
    private final Object _serviceMetricsMapLock = new Object();

    private PlatformTransactionManager _transactionManager;

    private ServiceManager _serviceManager;

    private RoleManager _roleManager;

    /** Turns on service metrics collection. */
    private void enable() {
        synchronized(_enableLock) {
            if (_enabled) return;   // alreay enabled

            _logger.info("Enabling service metrics collection.");

            //
            // Schedule the timer tasks to close finished bins and start new bins.
            //

            final long now = System.currentTimeMillis();

            // Sets fine resolution timer to excecute every fine interval; starting at the next fine period.
            _logger.config("Fine resolution bin interval is " + _fineBinInterval + " ms");
            final Date nextFineStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_FINE, _fineBinInterval, now));
            _fineArchiver = new FineTask();
            _timer.scheduleAtFixedRate(_fineArchiver, nextFineStart, _fineBinInterval);
            _logger.config("Scheduled first fine archive task for " + nextFineStart);

            // Sets hourly resolution timer to excecute every hour; starting at the next hourly period.
            final Date nextHourlyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_HOURLY, 0, now));
            _hourlyArchiver = new HourlyTask();
            _timer.scheduleAtFixedRate(_hourlyArchiver, nextHourlyStart, HOUR);
            _logger.config("Scheduled first hourly archive task for " + nextHourlyStart);

            // Sets daily resolution timer to execute at the next daily period start (= end of current daily period).
            // But can't just schedule at fixed rate of 24-hours interval because a
            // calender day varies, e.g., when switching Daylight Savings Time.
            final Date nextDailyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, now));
            _dailyArchiver = new DailyTask();
            _timer.schedule(_dailyArchiver, nextDailyStart);
            _logger.config("Scheduled first daily archive task for " + nextDailyStart);

            // Initializes a service metric for each published service; which in
            // turn creates the current metric bins.
            //
            // {@link _serviceMetricsMap} should be empty here; whether because the
            // gateway is starting or cleared during the previous call to {@link #disable()}.
            try {
                synchronized (_serviceMetricsMapLock) {
                    Collection<EntityHeader> serviceHeaders = _serviceManager.findAllHeaders();
                    for (EntityHeader service : serviceHeaders) {
                        final Long oid = new Long(service.getOid());
                        ServiceMetrics serviceMetrics = new ServiceMetrics(service.getOid(), _clusterNodeId, _fineBinInterval, _flusherQueue);
                         _serviceMetricsMap.put(oid, serviceMetrics);
                    }
                }
            } catch (FindException e) {
                _logger.warning("Failed to fetch list of published service. Metric bins generation will not start until requests arrive. Cause: " + e.getMessage());
            }

            //
            // Starts the database flusher thread.
            //

            _flusher = new Flusher();
            _flusherThread = new Thread(_flusher, _flusher.getClass().getName());
            _flusherThread.start();

            //
            // Schedules timer tasks to delete old metrics bins from database.
            //

            final long fineTtl = getLongSystemProperty("com.l7tech.service.metrics.maxFineAge", MIN_FINE_AGE, MAX_FINE_AGE, DEF_FINE_AGE);
            final long hourlyTtl = getLongSystemProperty("com.l7tech.service.metrics.maxHourlyAge", MIN_HOURLY_AGE, MAX_HOURLY_AGE, DEF_HOURLY_AGE);
            final long dailyTtl = getLongSystemProperty("com.l7tech.service.metrics.maxDailyAge", MIN_DAILY_AGE, MAX_DAILY_AGE, DEF_DAILY_AGE);

            _fineDeleter = new DeleteTask(fineTtl, MetricsBin.RES_FINE);
            _timer.schedule(_fineDeleter, MINUTE, 5 * MINUTE);
            _logger.config("Scheduled first deletion task for fine resolution metric bins at " + new Date(System.currentTimeMillis() + MINUTE));

            _hourlyDeleter = new DeleteTask(hourlyTtl, MetricsBin.RES_HOURLY);
            _timer.schedule(_hourlyDeleter, 15 * MINUTE, 12 * HOUR);
            _logger.config("Scheduled first deletion task for hourly metric bins at " + new Date(System.currentTimeMillis() + 15 * MINUTE));

            _dailyDeleter = new DeleteTask(dailyTtl, MetricsBin.RES_DAILY);
            _timer.schedule(_dailyDeleter, HOUR, 24 * HOUR);
            _logger.config("Scheduled first deletion task for daily metric bins at " + new Date(System.currentTimeMillis() + HOUR));

            _enabled = true;
        }
    }

    /** Turns off service metrics collection. */
    private void disable() {
        synchronized(_enableLock) {
            if (!_enabled) return;  // already disabled

            _logger.info("Disabling service metrics collection.");

            // Cancels the timer tasks; not the timer since we don't own it.
            if (_fineArchiver != null) { _fineArchiver.cancel(); _fineArchiver = null; }
            if (_hourlyArchiver != null) { _hourlyArchiver.cancel(); _hourlyArchiver = null; }
            if (_dailyArchiver != null) { _dailyArchiver.cancel(); _dailyArchiver = null; }
            if (_fineDeleter != null) { _fineDeleter.cancel(); _fineDeleter = null; }
            if (_hourlyDeleter != null) { _hourlyDeleter.cancel(); _hourlyDeleter = null; }
            if (_dailyDeleter != null) { _dailyDeleter.cancel(); _dailyDeleter = null; }

            if (_flusher != null) { _flusher.quit(); _flusher = null; }
            if (_flusherThread != null) { _flusherThread.interrupt(); _flusherThread = null; }

            synchronized(_serviceMetricsMapLock) {
                // Discards all the currently open metric bins.
                _serviceMetricsMap.clear();
            }

            _enabled = false;
        }
    }

    /**
     * Convenience method to return a system property value parsed into a long
     * integer, constrained by the given lower and upper limits. If the system
     * property does not exist, or is not parsable as an integer, then the given
     * default value is returned instead.
     *
     * @param name          property name
     * @param lower         lower limit
     * @param upper         upper limit
     * @param defaultValue  default value
     * @return property value
     */
    private static long getLongSystemProperty(final String name, final long lower, final long upper, final long defaultValue) {
        final String value = System.getProperty(name);
        if (value == null) {
            _logger.info("Using default value (" + defaultValue + ") for missing system property: " + name);
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
                _logger.info("Using default value (" + defaultValue + ") for non-numeric system property: " + name);
                return defaultValue;
            }
        }
    }

    /**
     * Convenience method to return a cluster property value parsed into a long
     * integer, constrained by the given lower and upper limits. If the property
     * value is not parsable as an integer, then the given default value is
     * returned instead.
     *
     * @param name          property name
     * @param lower         lower limit
     * @param upper         upper limit
     * @param defaultValue  default value
     * @return property value
     */
    private static long getLongClusterProperty(final String name, final long lower, final long upper, final long defaultValue) {
        final String value = ServerConfig.getInstance().getProperty(name);
        try {
            final long longValue = Long.parseLong(value);
            if (longValue < lower) {
                _logger.warning("Imposing lower constraint (" + lower + ") on cluster property value (" + longValue + "): " + name);
                return lower;
            } else if (longValue > upper) {
                _logger.warning("Imposing upper constraint (" + upper + ") on cluster property value (" + longValue + "): " + name);
                return upper;
            }
            return longValue;
        } catch (NumberFormatException e) {
            _logger.info("Using default value (" + defaultValue + ") for non-numeric cluster property: " + name);
            return defaultValue;
        }
    }

    /**
     * A timer task to execute at fine resolution binning interval; to close off
     * and archive the current fine resolution bins and start new ones.
     *
     * <p>Also archives an empty uptime bin (since 4.0).
     */
    private class FineTask extends ManagedTimerTask {
        protected void doRun() {
            List<ServiceMetrics> list = new ArrayList<ServiceMetrics>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.values());
            }
            int numArchived = 0;
            for (ServiceMetrics serviceMetrics : list) {
                if (serviceMetrics.archiveFineBin()) {
                    ++ numArchived;
                }
            }
            if (_logger.isLoggable(Level.FINER))
                _logger.finer("Fine archiving task completed; archived " + numArchived + " fine bins.");

            // Archives an empty uptime bin with service OID -1 for 2 reasons:
            // 1. to distinguish SSG running state from shutdown state
            // 2. to keep Dashboard moving chart advancing when no request is going through a service
            final long periodEnd = MetricsBin.periodStartFor(MetricsBin.RES_FINE, _fineBinInterval, System.currentTimeMillis());
            final long periodStart = periodEnd - _fineBinInterval;
            final MetricsBin emptyBin = new MetricsBin(periodStart, _fineBinInterval, MetricsBin.RES_FINE, _clusterNodeId, -1L);
            emptyBin.setEndTime(periodEnd);
            try {
                _flusherQueue.put(emptyBin);
            } catch (InterruptedException e) {
                _logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * A timer task to execute at every hour; to close off and archive the
     * current hourly bins and start new ones.
     */
    private class HourlyTask extends ManagedTimerTask {
        protected void doRun() {
            List<ServiceMetrics> list = new ArrayList<ServiceMetrics>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.values());
            }
            for (ServiceMetrics serviceMetrics : list) {
                serviceMetrics.archiveHourlyBin();
            }
            if (_logger.isLoggable(Level.FINE))
                _logger.fine("Hourly archiving task completed; archived " + list.size() + " hourly bins.");
        }
    }

    /**
     * A timer task to execute at every midnight; to close off and archive the
     * current daily bins and start new ones.
     */
    private class DailyTask extends ManagedTimerTask {
        protected void doRun() {
            List<ServiceMetrics> list = new ArrayList<ServiceMetrics>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.values());
            }
            for (ServiceMetrics serviceMetrics : list) {
                serviceMetrics.archiveDailyBin();
            }
            if (_logger.isLoggable(Level.FINE))
                _logger.fine("Daily archiving task completed; archived " + list.size() + " daily bins.");

            // Schedule the next timer execution at the end of current period
            // (with a new task instance because a task cannot be reused).
            Date nextTimerDate = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, System.currentTimeMillis()));
            _timer.schedule(new DailyTask(), nextTimerDate);
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
                        // This will wait indefinitely until there is an item in the queue.

                    if (_logger.isLoggable(Level.FINEST))
                        _logger.finest("Saving " + head.toString());

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
    private class DeleteTask extends ManagedTimerTask {
        private final long _ttl;
        private final int _resolution;

        private DeleteTask(long ttl, int resolution) {
            _ttl = ttl;
            _resolution = resolution;
        }

        protected void doRun() {
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

    /**
     * Filters down the list of published service OID according to the user's RBAC permissions.
     *
     * @param serviceOids   <code>null</code> means all service permitted for user
     * @return list of services permitted; can be empty; <code>null</code> if all
     *         available services are permitted
     * @throws FindException
     */
    private Set<Long> filterPermittedPublishedServices(final long[] serviceOids) throws FindException {
        Set<Long> filteredOids = null;
        if (serviceOids != null) {
            filteredOids = new HashSet<Long>();
            for (long serviceOid : serviceOids) {
                filteredOids.add(serviceOid);
            }
        }

        final User user = JaasUtils.getCurrentUser();
        if (_roleManager.isPermittedForAnyEntityOfType(user, OperationType.READ, EntityType.SERVICE)) {
            // No filtering needed.
        } else {
            Set<Long> permittedOids = new HashSet<Long>();
            for (PublishedService service : _serviceManager.findAll()) {
                if (_roleManager.isPermittedForEntity(user, service, OperationType.READ, null)) {
                    permittedOids.add(service.getOid());
                }
            }

            if (filteredOids == null) {
                filteredOids = permittedOids;
            } else {
                for (Iterator<Long> i = filteredOids.iterator(); i.hasNext(); ) {
                    if (!permittedOids.contains(i.next())) {
                        i.remove();
                    }
                }
            }

            if (_logger.isLoggable(Level.FINER)) {
                _logger.finer("Filtered published services from " +
                        (serviceOids == null ? "*" : serviceOids.length) +
                        " to " + filteredOids.size());
            }
        }

        return filteredOids;
    }
}