package com.l7tech.server.service;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import org.springframework.context.ApplicationEvent;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
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

public class ServiceMetricsServicesImpl implements ServiceMetricsServices, PostStartupApplicationListener, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(ServiceMetricsServicesImpl.class.getName());
    private static final Random random = new Random();

    /**
     * When a daily task is scheduled for the next day, this is the required delay it must wait before executing to
     * ensure all hourly tasks for the last hour of the previous day have had a good chance of completing.
     * The delay takes the hourly tasks retry attempts and sleep periods into consideration.
     */
    private static final long DAILY_TASK_START_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(1) * 5;

    public ServiceMetricsServicesImpl(String clusterNodeId) {
        this.clusterNodeId = clusterNodeId;
    }

    @PostConstruct
    void start() throws Exception {
        int fineBinInterval = config().getIntProperty("metricsFineInterval", DEF_FINE_BIN_INTERVAL);
        if (fineBinInterval > MAX_FINE_BIN_INTERVAL || fineBinInterval < MIN_FINE_BIN_INTERVAL) {
            logger.warning(String.format("Configured metricsFineInterval %d is out of the valid range (%d-%d); using default %d", fineBinInterval, MIN_FINE_BIN_INTERVAL, MAX_FINE_BIN_INTERVAL, DEF_FINE_BIN_INTERVAL));
            fineBinInterval = DEF_FINE_BIN_INTERVAL;
        }
        this.fineBinInterval = fineBinInterval;

        if ( config().getBooleanProperty(CLUSTER_PROP_ENABLED, false) ) {
            enable();
        } else {
            logger.info("Service metrics collection is currently disabled.");
        }

        if ( config().getBooleanProperty(ServerConfigParams.PARAM_ADD_MAPPINGS_INTO_SERVICE_METRICS, false) ) {
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
    @Override
    public boolean isEnabled() {
        return _enabled.get();
    }

    @Override
    public int getFineInterval() {
        return fineBinInterval;
    }


    /**
     * Gets the service metrics for a given published service.
     *
     * @param serviceGoid    GOID of published service
     * @return null if service metrics processing is disabled
     */
    @Override
    public void trackServiceMetrics(final Goid serviceGoid) {
        getServiceMetrics( serviceGoid );
    }

    @Override
    public void addRequest(Goid serviceGoid, String operation, User authorizedUser, List<MessageContextMapping> mappings, boolean authorized, boolean completed, int frontTime, int backTime) {
        ServiceMetrics metrics = getServiceMetrics( serviceGoid );
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
     * @param serviceGoid    GOID of published service
     * @return null if service metrics processing is disabled
     */
    ServiceMetrics getServiceMetrics(final Goid serviceGoid) {
        if (isEnabled()) {
            ServiceMetrics serviceMetrics;
            synchronized (_serviceMetricsMapLock) {
                if (! _serviceMetricsMap.containsKey(serviceGoid)) {
                    serviceMetrics = new ServiceMetrics(serviceGoid);
                    _serviceMetricsMap.put(serviceGoid, serviceMetrics);
                } else {
                    serviceMetrics = _serviceMetricsMap.get(serviceGoid);
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

            if (timer == null) {
                logger.log(Level.WARNING, "Unable to enable service metrics -- timer not yet set");
                return;
            }

            logger.info("Enabling service metrics collection.");

            //
            // Schedule the timer tasks to close finished bins and start new bins.
            //

            final long now = System.currentTimeMillis();

            // Sets fine resolution timer to execute every fine interval; starting at the next fine period.
            logger.config("Fine resolution bin interval is " + fineBinInterval + " ms");
            final Date nextFineStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_FINE, fineBinInterval, now));
            _fineArchiver = new FineTask();
            timer.scheduleAtFixedRate(_fineArchiver, nextFineStart, fineBinInterval);
            logger.config("Scheduled first fine archive task for " + nextFineStart);

            // Sets hourly resolution timer to execute every hour; starting at the next hourly period.
            // Run slightly after the period end to allow all fine bins to be persisted
            // Schedule at a random period over a 2.5 minute window
            // WARNING: If this schedule is increased, the scheduling of daily tasks must be delayed to ensure hourly tasks are finished first.
            long hourlyRandomScheduleDelay = 15000L * random.nextInt(10);
            final Date nextHourlyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_HOURLY, 0, now) + hourlyRandomScheduleDelay);
            _hourlyArchiver = new HourlyTask();
            timer.scheduleAtFixedRate(_hourlyArchiver, nextHourlyStart, HOUR);
            logger.config("Scheduled first hourly archive task for " + nextHourlyStart);

            // Sets daily resolution timer to execute at the next daily period start (= end of current daily period).
            // But can't just schedule at fixed rate of 24-hours interval because a
            // calender day varies, e.g., when switching Daylight Savings Time.
            // Run slightly after the period end to allow all hourly bins to be persisted
            final Date nextDailyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, now) + DAILY_TASK_START_DELAY_MILLIS);
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
                        final Goid goid = service.getGoid();
                        ServiceMetrics serviceMetrics = new ServiceMetrics(service.getGoid());
                         _serviceMetricsMap.put(goid, serviceMetrics);
                        // There won't be any deleted services on startup
                        serviceStates.put(goid, service.isDisabled() ? ServiceState.DISABLED : ServiceState.ENABLED);
                    }
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Failed to fetch list of published service. Metric bins generation will not start until requests arrive. Cause: " + e.getMessage(), ExceptionUtils.getDebugException( e ) );
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

            final long fineTtl = getLongProperty( "com.l7tech.service.metrics.maxFineAge", MIN_FINE_AGE, MAX_FINE_AGE, DEF_FINE_AGE );
            final long hourlyTtl = getLongProperty( "com.l7tech.service.metrics.maxHourlyAge", MIN_HOURLY_AGE, MAX_HOURLY_AGE, DEF_HOURLY_AGE );
            final long dailyTtl = getLongProperty( "com.l7tech.service.metrics.maxDailyAge", MIN_DAILY_AGE, MAX_DAILY_AGE, DEF_DAILY_AGE );

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
                if (_dailyArchiver != null) { _dailyArchiver.cancel(); _dailyArchiver.performTask(false); _dailyArchiver = null; }
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
     * Convenience method to return a configuration property value parsed into a long
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
    private static long getLongProperty( final String name, final long lower, final long upper, final long defaultValue ) {
        final String value = ConfigFactory.getProperty( name );
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

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof GoidEntityInvalidationEvent))
            return;

        final GoidEntityInvalidationEvent eie = (GoidEntityInvalidationEvent)event;
        if (!PublishedService.class.isAssignableFrom(eie.getEntityClass())) {
            return;
        }

        for (int i = 0; i < eie.getEntityOperations().length; i++) {
            final char op = eie.getEntityOperations()[i];
            final Goid goid = eie.getEntityIds()[i];
            switch (op) {
                case GoidEntityInvalidationEvent.CREATE: // Intentional fallthrough
                case GoidEntityInvalidationEvent.UPDATE:
                    try {
                        serviceStates.put(goid, serviceMetricsManager.getCreatedOrUpdatedServiceState(goid));
                        break;
                    } catch (FindException e) {
                        if (logger.isLoggable(Level.WARNING)) {
                            logger.log(Level.WARNING, MessageFormat.format("Unable to find created/updated service #{0}", goid), e);
                        }
                        continue;
                    }
                case GoidEntityInvalidationEvent.DELETE:
                    serviceStates.put(goid, ServiceState.DELETED);
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
        @Override
        protected void doRun() {
            List<ServiceMetrics> list = new ArrayList<ServiceMetrics>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.values());
            }
            int numArchived = 0;
            for (ServiceMetrics serviceMetrics : list) {
                final ServiceState state = serviceStates.get(serviceMetrics.getServiceGoid());
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
        @Override
        protected void doRun() {
            Set<Goid> list = new HashSet<Goid>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.keySet());
            }

            // get start time for the last hourly period
            long startTime = MetricsBin.periodStartFor( MetricsBin.RES_HOURLY, 0, System.currentTimeMillis() ) - TimeUnit.HOURS.toMillis(1);
            for ( Goid serviceGoid : list ) {
                final ServiceState state = serviceStates.get( serviceGoid );
                int retriesLeft = 2;
                long retryMillis = 5000;
                boolean retry = true;
                while (retriesLeft > 0 && retry) {
                    try {
                        serviceMetricsManager.createHourlyBin(serviceGoid, state, startTime);
                        retry = false;
                    } catch (SaveException e) {
                        if (retriesLeft < 1 || ExceptionUtils.getCauseIfCausedBy(e, CannotAcquireLockException.class) == null) {
                            retry = false;
                            logger.log(Level.WARNING, "Couldn't create hourly metrics bin", e);
                        } else {
                            retry = true;
                            logger.log(Level.INFO, "Error saving hourly metrics bin, will retry in " + (retryMillis / 1000) + " seconds: "  + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            try {
                                Thread.sleep(retryMillis);
                            } catch (InterruptedException e1) {
                                // do nothing
                            }
                        }
                    } finally {
                        retriesLeft--;
                    }
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
        @Override
        protected void doRun() {
            performTask(true);
        }

        private void performTask( final boolean reschedule ) {
            Set<Goid> list = new HashSet<Goid>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.keySet());
            }

            // get start time for the last daily period
            long startTime = MetricsBin.periodStartFor( MetricsBin.RES_DAILY, 0, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1) );
            for ( Goid serviceGoid : list ) {
                final ServiceState state = serviceStates.get( serviceGoid );
                int retriesLeft = 2;
                long retryMillis = 5000;
                boolean retry = true;
                while (retriesLeft > 0 && retry) {
                    try {
                        serviceMetricsManager.createDailyBin( serviceGoid, state, startTime );
                        retry = false;
                    } catch (SaveException e) {
                        if (retriesLeft < 1 || ExceptionUtils.getCauseIfCausedBy(e, CannotAcquireLockException.class) == null) {
                            retry = false;
                            logger.log(Level.WARNING, "Couldn't create daily metrics bin", e);
                        } else {
                            retry = true;
                            logger.log(Level.INFO, "Error saving daily metrics bin, will retry in " + (retryMillis / 1000) + " seconds: "  + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            try {
                                Thread.sleep(retryMillis);
                            } catch (InterruptedException e1) {
                                // do nothing
                            }
                        }
                    } finally {
                        retriesLeft--;
                    }
                }
            }
            if (logger.isLoggable(Level.FINE))
                logger.fine("Daily archiving task completed; archived " + list.size() + " daily bins.");

            if ( reschedule ) {
                // Schedule the next timer execution at the end of current period
                // (with a new task instance because a task cannot be reused).
                Date nextTimerDate = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, System.currentTimeMillis()) + DAILY_TASK_START_DELAY_MILLIS);
                timer.schedule(new DailyTask(), nextTimerDate);
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Scheduled next daily flush task for " + nextTimerDate);
            }
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

        @Override
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
                logger.log(Level.WARNING, "Couldn't delete MetricsBins for resolution '" + _resolution + "'", e);
            }
        }

    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (CLUSTER_PROP_ENABLED.equals(event.getPropertyName())) {
            if (Boolean.valueOf((String)event.getNewValue())) {
                enable();
            } else {
                disable();
            }
        }

        if ( ServerConfigParams.PARAM_ADD_MAPPINGS_INTO_SERVICE_METRICS.equals(event.getPropertyName())) {
            if (Boolean.valueOf((String)event.getNewValue())) {
                _addMappingsIntoServiceMetrics.set(true);
            } else {
                _addMappingsIntoServiceMetrics.set(false);
                logger.info("Adding message context mappings to Service Metrics is currently disabled.");
            }
        }
    }

    private Config config() {
        return config != null ? config : ConfigFactory.getCachedConfig();
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

        @Override
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

            final MetricsBin bin = new MetricsBin( metricsSet.getStartTime(), fineBinInterval, MetricsBin.RES_FINE, clusterNodeId, metricsSet.getServiceGoid() );
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

    @Inject
    private ServiceMetricsManager serviceMetricsManager;

    private final String clusterNodeId;

    @Inject
    @Named("managedBackgroundTimer")
    private ManagedTimer timer;

    @Inject
    private Config config;

    /** Fine resolution bin interval (in milliseconds). */
    private int fineBinInterval;

    private Map<Goid, ServiceState> serviceStates = new ConcurrentHashMap<Goid, ServiceState>();

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

    private final Map<Goid, ServiceMetrics> _serviceMetricsMap = new HashMap<Goid, ServiceMetrics>();
    private final Object _serviceMetricsMapLock = new Object();
    private AtomicBoolean _addMappingsIntoServiceMetrics = new AtomicBoolean(false);
}
