/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.TimeUnit;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataIntegrityViolationException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceMetricsServicesImpl implements ServiceMetricsServices, ApplicationListener, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(ServiceMetricsServicesImpl.class.getName());

    public ServiceMetricsServicesImpl(String clusterNodeId) {
        this.clusterNodeId = clusterNodeId;
    }

    @PostConstruct
    void start() throws Exception {
        int fineBinInterval = serverConfig.getIntProperty("metricsFineInterval", DEF_FINE_BIN_INTERVAL);
        if (fineBinInterval > MAX_FINE_BIN_INTERVAL || fineBinInterval < MIN_FINE_BIN_INTERVAL) {
            logger.warning(String.format("Configured metricsFineInterval %d is out of the valid range (%d-%d); using default %d", fineBinInterval, MIN_FINE_BIN_INTERVAL, MAX_FINE_BIN_INTERVAL, DEF_FINE_BIN_INTERVAL));
            fineBinInterval = DEF_FINE_BIN_INTERVAL;
        }
        this.fineBinInterval = fineBinInterval;

        if (Boolean.valueOf(ServerConfig.getInstance().getProperty(CLUSTER_PROP_ENABLED))) {
            enable();
        } else {
            logger.info("Service metrics collection is currently disabled.");
        }

        if (Boolean.valueOf(ServerConfig.getInstance().getProperty(ServerConfig.PARAM_ADD_MAPPINGS_INTO_SERVICE_METRICS))) {
            _addMappingsIntoServiceMetrics.set(true);
        } else {
            _addMappingsIntoServiceMetrics.set(false);
            logger.info("Adding message context mappings to Service Metrics is currently disabled.");
        }
    }

    @PreDestroy
    void destroy() throws Exception {
        disable();
    }

    /**
     * @return whether collection of service metrics is currently enabled
     */
    public boolean isEnabled() {
        return _enabled.get();
    }

    public int getFineInterval() {
        return fineBinInterval;
    }


    /**
     * Gets the service metrics for a given published service.
     *
     * @param serviceOid    OID of published service
     * @return null if service metrics processing is disabled
     */
    public void trackServiceMetrics(final long serviceOid) {
        getServiceMetrics( serviceOid );
    }

    public void addRequest(long serviceOid, String operation, User authorizedUser, List<MessageContextMapping> mappings, boolean authorized, boolean completed, int frontTime, int backTime) {
        ServiceMetrics metrics = getServiceMetrics( serviceOid );
        if ( metrics != null ) {
            if (_addMappingsIntoServiceMetrics.get()) {
                metrics.addRequest(operation, authorizedUser, mappings, authorized, completed, frontTime, backTime);
            } else {
                metrics.addRequest(null, null, null, authorized, completed, frontTime, backTime);
            }
        }
    }


    /**
     * Gets the service metrics for a given published service.
     *
     * @param serviceOid    OID of published service
     * @return null if service metrics processing is disabled
     */
    ServiceMetrics getServiceMetrics(final long serviceOid) {
        if (isEnabled()) {
            ServiceMetrics serviceMetrics;
            synchronized (_serviceMetricsMapLock) {
                if (! _serviceMetricsMap.containsKey(serviceOid)) {
                    serviceMetrics = new ServiceMetrics(serviceOid);
                    _serviceMetricsMap.put(serviceOid, serviceMetrics);
                } else {
                    serviceMetrics = _serviceMetricsMap.get(serviceOid);
                }
            }
            return serviceMetrics;
        } else {
            return null;
        }
    }

    /** Turns on service metrics collection. */
    private void enable() {
        synchronized(_enableLock) {
            if (_enabled.get()) return;   // alreay enabled

            logger.info("Enabling service metrics collection.");

            //
            // Schedule the timer tasks to close finished bins and start new bins.
            //

            final long now = System.currentTimeMillis();

            // Sets fine resolution timer to excecute every fine interval; starting at the next fine period.
            logger.config("Fine resolution bin interval is " + fineBinInterval + " ms");
            final Date nextFineStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_FINE, fineBinInterval, now));
            _fineArchiver = new FineTask();
            timer.scheduleAtFixedRate(_fineArchiver, nextFineStart, fineBinInterval);
            logger.config("Scheduled first fine archive task for " + nextFineStart);

            // Sets hourly resolution timer to excecute every hour; starting at the next hourly period.
            // Run slightly after the period end to allow all fine bins to be persisted
            final Date nextHourlyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_HOURLY, 0, now) + TimeUnit.MINUTES.toMillis(1));
            _hourlyArchiver = new HourlyTask();
            timer.scheduleAtFixedRate(_hourlyArchiver, nextHourlyStart, HOUR);
            logger.config("Scheduled first hourly archive task for " + nextHourlyStart);

            // Sets daily resolution timer to execute at the next daily period start (= end of current daily period).
            // But can't just schedule at fixed rate of 24-hours interval because a
            // calender day varies, e.g., when switching Daylight Savings Time.
            // Run slightly after the period end to allow all hourly bins to be persisted
            final Date nextDailyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, now) + TimeUnit.MINUTES.toMillis(2));
            _dailyArchiver = new DailyTask();
            timer.schedule(_dailyArchiver, nextDailyStart);
            logger.config("Scheduled first daily archive task for " + nextDailyStart);

            // Initializes a service metric for each published service; which in
            // turn creates the current metric bins.
            //
            // {@link _serviceMetricsMap} should be empty here; whether because the
            // gateway is starting or cleared during the previous call to {@link #disable()}.
            try {
                synchronized (_serviceMetricsMapLock) {
                    Collection<ServiceHeader> serviceHeaders = serviceMetricsManager.findAllServiceHeaders();
                    for ( ServiceHeader service : serviceHeaders) {
                        final Long oid = service.getOid();
                        ServiceMetrics serviceMetrics = new ServiceMetrics(service.getOid());
                         _serviceMetricsMap.put(oid, serviceMetrics);
                        // There won't be any deleted services on startup
                        serviceStates.put(oid, service.isDisabled() ? ServiceState.DISABLED : ServiceState.ENABLED);
                    }
                }
            } catch (FindException e) {
                logger.warning("Failed to fetch list of published service. Metric bins generation will not start until requests arrive. Cause: " + e.getMessage());
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
            timer.schedule(_fineDeleter, MINUTE, 5 * MINUTE);
            logger.config("Scheduled first deletion task for fine resolution metric bins at " + new Date(System.currentTimeMillis() + MINUTE));

            _hourlyDeleter = new DeleteTask(hourlyTtl, MetricsBin.RES_HOURLY);
            timer.schedule(_hourlyDeleter, 15 * MINUTE, 12 * HOUR);
            logger.config("Scheduled first deletion task for hourly metric bins at " + new Date(System.currentTimeMillis() + 15 * MINUTE));

            _dailyDeleter = new DeleteTask(dailyTtl, MetricsBin.RES_DAILY);
            timer.schedule(_dailyDeleter, HOUR, 24 * HOUR);
            logger.config("Scheduled first deletion task for daily metric bins at " + new Date(System.currentTimeMillis() + HOUR));

            _enabled.set(true);
        }
    }

    /** Turns off service metrics collection. */
    private void disable() {
        synchronized(_enableLock) {
            if (!_enabled.get()) return;  // already disabled

            logger.info("Disabling service metrics collection.");

            try {
                // Cancels the timer tasks; not the timer since we don't own it.
                //
                // (Bug 5244) After cancelling, we explicitly trigger the archiving of the current
                // partial hourly and daily bins so that we don't lose any data. Note that upon
                // restart/re-enabling we don't have to reinitialized memory from matching persisted
                // partial bins because service metrics queries are always done against database,
                // not from memory.
                if (_fineArchiver != null) { _fineArchiver.cancel(); _fineArchiver = null; }
                if (_hourlyArchiver != null) { _hourlyArchiver.cancel(); _hourlyArchiver.doRun(); _hourlyArchiver = null; }
                if (_dailyArchiver != null) { _dailyArchiver.cancel(); _dailyArchiver.doRun(); _dailyArchiver = null; }
                if (_fineDeleter != null) { _fineDeleter.cancel(); _fineDeleter = null; }
                if (_hourlyDeleter != null) { _hourlyDeleter.cancel(); _hourlyDeleter = null; }
                if (_dailyDeleter != null) { _dailyDeleter.cancel(); _dailyDeleter = null; }

                if (_flusher != null) { _flusher.quit(); }
                if (_flusherThread != null) { _flusherThread.interrupt(); _flusherThread = null; }
                if (_flusher != null) {
                    try {
                        // Runs the flusher one last time for the partial hourly and daily bins.
                        // The flusher will merge similar partial bins already in database in the
                        // event disabling happens several times within the clock hour/day.
                        while (_flusherQueue.size() != 0) {
                            _flusher.flush();
                        }
                    } catch (InterruptedException e) {
                        logger.info("Final run of flusher interrupted.");
                    }
                    _flusher = null;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Caught exception while disabling ServiceMetrics collection", e);
            }

            synchronized(_serviceMetricsMapLock) {
                // Discards all the currently open metric bins.
                _serviceMetricsMap.clear();
            }

            _enabled.set(false);
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
    @Deprecated() // use ServerConfig!
    private static long getLongSystemProperty(final String name, final long lower, final long upper, final long defaultValue) {
        final String value = System.getProperty(name);
        if (value == null) {
            logger.info("Using default value (" + defaultValue + ") for missing system property: " + name);
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
                logger.info("Using default value (" + defaultValue + ") for non-numeric system property: " + name);
                return defaultValue;
            }
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof EntityInvalidationEvent))
            return;

        final EntityInvalidationEvent eie = (EntityInvalidationEvent)event;
        if (!PublishedService.class.isAssignableFrom(eie.getEntityClass())) {
            return;
        }

        for (int i = 0; i < eie.getEntityOperations().length; i++) {
            final char op = eie.getEntityOperations()[i];
            final long oid = eie.getEntityIds()[i];
            switch (op) {
                case EntityInvalidationEvent.CREATE: // Intentional fallthrough
                case EntityInvalidationEvent.UPDATE:
                    try {
                        serviceStates.put(oid, serviceMetricsManager.getCreatedOrUpdatedServiceState(oid));
                        break;
                    } catch (FindException e) {
                        if (logger.isLoggable(Level.WARNING)) {
                            logger.log(Level.WARNING, MessageFormat.format("Unable to find created/updated service #{0}", oid), e);
                        }
                        continue;
                    }
                case EntityInvalidationEvent.DELETE:
                    serviceStates.put(oid, ServiceState.DELETED);
                    break;
            }
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
                final ServiceState state = serviceStates.get(serviceMetrics.getServiceOid());
                ServiceMetrics.MetricsCollectorSet metricsSet = serviceMetrics.getMetricsCollectorSet(state);
                if ( metricsSet != null ) {
                    try {
                        _flusherQueue.put( metricsSet );
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                        Thread.currentThread().interrupt();
                        return;
                    }
                    ++ numArchived;
                }
            }
            if (logger.isLoggable(Level.FINER))
                logger.finer("Fine archiving task completed; archived " + numArchived + " fine bins.");

            // Archives an empty uptime bin with service OID -1 for 2 reasons:
            // 1. to distinguish SSG running state from shutdown state
            // 2. to keep Dashboard moving chart advancing when no request is going through a service
            final long periodEnd = MetricsBin.periodStartFor(MetricsBin.RES_FINE, fineBinInterval, System.currentTimeMillis());
            final long periodStart = periodEnd - fineBinInterval;
            try {
                _flusherQueue.put( ServiceMetrics.getEmptyMetricsSet(periodStart, periodEnd) );
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted waiting for queue", e);
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
            Set<Long> list = new HashSet<Long>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.keySet());
            }

            // get start time for the last hourly period
            long startTime = MetricsBin.periodStartFor( MetricsBin.RES_HOURLY, 0, System.currentTimeMillis() ) - TimeUnit.HOURS.toMillis(1);
            for ( Long serviceOid : list ) {
                final ServiceState state = serviceStates.get( serviceOid );
                try {
                    serviceMetricsManager.createHourlyBin( serviceOid, state, startTime );
                } catch (SaveException e) {
                    logger.log(Level.WARNING, "Couldn't create hourly metrics bin", e); // TODO reschedule?
                }
            }
            if (logger.isLoggable(Level.FINE))
                logger.fine("Hourly archiving task completed; archived " + list.size() + " hourly bins.");
        }
    }

    /**
     * A timer task to execute at every midnight; to close off and archive the
     * current daily bins and start new ones.
     */
    private class DailyTask extends ManagedTimerTask {
        protected void doRun() {
            Set<Long> list = new HashSet<Long>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.keySet());
            }

            // get start time for the last daily period
            long startTime = MetricsBin.periodStartFor( MetricsBin.RES_DAILY, 0, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1) );
            for ( Long serviceOid : list ) {
                final ServiceState state = serviceStates.get( serviceOid );
                try {
                    serviceMetricsManager.createDailyBin( serviceOid, state, startTime );
                } catch (SaveException e) {
                    logger.log(Level.WARNING, "Couldn't create daily metrics bin", e); // TODO reschedule?
                }
            }
            if (logger.isLoggable(Level.FINE))
                logger.fine("Daily archiving task completed; archived " + list.size() + " daily bins.");

            // Schedule the next timer execution at the end of current period
            // (with a new task instance because a task cannot be reused).
            Date nextTimerDate = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, System.currentTimeMillis()));
            timer.schedule(new DailyTask(), nextTimerDate);
            if (logger.isLoggable(Level.FINE))
                logger.fine("Scheduled next daily flush task for " + nextTimerDate);
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
                Integer num = serviceMetricsManager.delete(oldestSurvivor, _resolution);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Deleted {0} {1} bins older than {2}",
                            new Object[] {
                                num,
                                MetricsBin.describeResolution(_resolution),
                                new Date(oldestSurvivor)
                            });
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't delete MetricsBins", e);
            }
        }

    }

    public void propertyChange(PropertyChangeEvent event) {
        if (CLUSTER_PROP_ENABLED.equals(event.getPropertyName())) {
            if (Boolean.valueOf((String)event.getNewValue())) {
                enable();
            } else {
                disable();
            }
        }

        if (ServerConfig.PARAM_ADD_MAPPINGS_INTO_SERVICE_METRICS.equals(event.getPropertyName())) {
            if (Boolean.valueOf((String)event.getNewValue())) {
                _addMappingsIntoServiceMetrics.set(true);
            } else {
                _addMappingsIntoServiceMetrics.set(false);
                logger.info("Adding message context mappings to Service Metrics is currently disabled.");
            }
        }
    }

    /**
     * Flush queued metrics bins to the database.
     */
    private class Flusher implements Runnable {
        private final Object flusherLock = new Object();
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
            logger.info("Database flusher beginning");
            while (true) {
                boolean stop;
                synchronized(flusherLock) {
                    stop = quit;
                }
                if (stop) {
                    break;
                }
                try {
                    flush();
                } catch (InterruptedException e) {
                    boolean isQuit;
                    synchronized(flusherLock) {
                        isQuit = quit;
                    }
                    if (!isQuit) {
                        logger.info("Database flusher quitting due to interrupt.");
                        quit();
                    }
                } catch (DataIntegrityViolationException e) {
                    logger.log(Level.INFO, "Failed to save a MetricsBin due to constraint violation");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Couldn't save MetricsBin", e);
                }
            }
            logger.info("Database flusher exiting.");
        }

        private void flush() throws InterruptedException {
            // This will wait indefinitely until there is an item in the queue.
            final ServiceMetrics.MetricsCollectorSet metricsSet = _flusherQueue.take();

            final MetricsBin bin = new MetricsBin( metricsSet.getStartTime(), fineBinInterval, MetricsBin.RES_FINE, clusterNodeId, metricsSet.getServiceOid() );
            if ( metricsSet.getServiceState() != null ) bin.setServiceState( metricsSet.getServiceState() );
            bin.setEndTime( metricsSet.getEndTime() );

            if ( metricsSet.getSummaryMetrics().getNumAttemptedRequest() > 0 ) {
                bin.setMinFrontendResponseTime( metricsSet.getSummaryMetrics().getMinFrontendResponseTime() );
                bin.setMaxFrontendResponseTime( metricsSet.getSummaryMetrics().getMaxFrontendResponseTime() );
            }
            bin.setSumFrontendResponseTime( metricsSet.getSummaryMetrics().getSumFrontendResponseTime() );

            if ( metricsSet.getSummaryMetrics().getNumCompletedRequest() > 0 ) {
                bin.setMinBackendResponseTime( metricsSet.getSummaryMetrics().getMinBackendResponseTime() );
                bin.setMaxBackendResponseTime( metricsSet.getSummaryMetrics().getMaxBackendResponseTime() );
            }
            bin.setSumBackendResponseTime( metricsSet.getSummaryMetrics().getSumBackendResponseTime() );

            bin.setNumAttemptedRequest( metricsSet.getSummaryMetrics().getNumAttemptedRequest() );
            bin.setNumAuthorizedRequest( metricsSet.getSummaryMetrics().getNumAuthorizedRequest() );
            bin.setNumCompletedRequest( metricsSet.getSummaryMetrics().getNumCompletedRequest() );

            if (logger.isLoggable(Level.FINEST))
                logger.finest("Saving " + bin.toString());

            serviceMetricsManager.doFlush(metricsSet, bin);
        }

    }

    @Resource
    private ServiceMetricsManager serviceMetricsManager;

    private final String clusterNodeId;

    @Resource
    private ManagedTimer timer;

    @Resource
    private ServerConfig serverConfig;

    /** Fine resolution bin interval (in milliseconds). */
    private int fineBinInterval;

    private Map<Long, ServiceState> serviceStates = new ConcurrentHashMap<Long, ServiceState>();

    /** Whether statistics collecting is turned on. */
    private final AtomicBoolean _enabled = new AtomicBoolean(false);

    /** For synchronizing calling {@link #enable()} and {@link #disable()}. */
    private final Object _enableLock = new Object();

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
    private static final BlockingQueue<ServiceMetrics.MetricsCollectorSet> _flusherQueue = new ArrayBlockingQueue<ServiceMetrics.MetricsCollectorSet>(500);

    private final Map<Long, ServiceMetrics> _serviceMetricsMap = new HashMap<Long, ServiceMetrics>();
    private final Object _serviceMetricsMapLock = new Object();
    private AtomicBoolean _addMappingsIntoServiceMetrics = new AtomicBoolean(false);
}
