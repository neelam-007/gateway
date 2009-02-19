package com.l7tech.server.service;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.mapping.MessageContextMappingManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.gateway.common.service.MetricsBinDetail;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ResourceUtils;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.sql.SQLException;

/**
 * Manages the processing and accumulation of service metrics for one SSG node.
 *
 * @author rmak
 * @author alex
 */

@Transactional(propagation=Propagation.SUPPORTS)
public class ServiceMetricsManagerImpl extends HibernateDaoSupport
        implements InitializingBean, DisposableBean, PropertyChangeListener, ApplicationListener, ServiceMetricsManager {

    //- PUBLIC

    public ServiceMetricsManagerImpl(String clusterNodeId, ManagedTimer timer) {
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

    public void setMessageContextMappingManager(MessageContextMappingManager messageContextMappingManager) {
        this.messageContextMappingManager = messageContextMappingManager;
    }

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void destroy() throws Exception {
        disable();
    }

    /**
     * @return whether collection of service metrics is currently enabled
     */
    public boolean isEnabled() {
        return _enabled.get();
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

    /**
     * Gets the service metrics for a given published service.
     *
     * @param serviceOid    OID of published service
     * @return null if service metrics processing is disabled
     */
    ServiceMetrics getServiceMetrics(final long serviceOid) {
        if (_enabled.get()) {
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
        throws FindException
    {
        final Collection<MetricsBin> bins = findBins(nodeId, serviceOids, resolution, minPeriodStart, maxPeriodStart, includeEmpty);

        return MetricsSummaryBin.createSummaryMetricsBinsByPeriodStart(bins);
    }

    public Map<Long, MetricsSummaryBin> summarizeByService(final String nodeId, final Integer resolution, final Long minPeriodStart, final Long maxPeriodStart, long[] serviceOids, boolean includeEmpty)
        throws FindException
    {
        final Collection<MetricsBin> bins = findBins(nodeId, serviceOids, resolution, minPeriodStart, maxPeriodStart, includeEmpty);
        return MetricsSummaryBin.createSummaryMetricsBinsByServiceOid(bins);
    }

    @SuppressWarnings({"unchecked"})
    private List<MetricsBin> findBins(
                          final String nodeId,
                          final long[] serviceOids,
                          final Integer resolution,
                          final Long minPeriodStart,
                          final Long maxPeriodStart,
                          final boolean includeEmpty)
        throws FindException
    {

        // Enforces RBAC permissions.
        final Set<Long> filteredOids = filterPermittedPublishedServices(serviceOids);
        if (filteredOids != null && includeEmpty) {
            filteredOids.add(-1L);      // Empty uptime bins have service OID of -1.
        }
        if (filteredOids != null && filteredOids.isEmpty()) {
            return Collections.emptyList();     // No bins can possibly be found.
        }

        try {
            //noinspection UnnecessaryLocalVariable
            List<MetricsBin> bins = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
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
                        final long currentPeriodStart = MetricsBin.periodStartFor(resolution == null ? -1 : resolution,
                                                                                  getFineInterval(),
                                                                                  currentTime - 1000);
                        final long lastestCompletedPeriodStart = MetricsBin.periodStartFor(resolution == null ? -1 : resolution,
                                                                                           getFineInterval(),
                                                                                           currentPeriodStart - 1);
                        crit.add(Restrictions.le("periodStart", lastestCompletedPeriodStart));
                    }

                    crit.addOrder(Order.asc("periodStart"));

                    return crit.list();
                }
            });

            return bins;
        } catch (DataAccessException e) {
            throw new FindException("Cannot find MetricsBins in database. " +
                                    "(nodeId=" + (nodeId==null ? "<ANY>" : nodeId) +
                                    ", serviceOid=" + filteredOids +
                                    ", resolution=" + (resolution==null ? "<ANY>" : resolution) +
                                    ", minPeriodStart=" + (minPeriodStart==null ? "<ANY>" : new Date(minPeriodStart)) +
                                    ", maxPeriodStart=" + (maxPeriodStart==null ? "<ANY>" : new Date(maxPeriodStart)) + ")",
                                    e);
        }
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

        Collection<MetricsBin> bins;
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
                    criteria.add(Restrictions.eq("resolution", resolution));
                    criteria.add(Restrictions.ge("periodStart", summaryPeriodStart));
                    criteria.add(Restrictions.lt("periodStart", summaryPeriodEnd));
                    criteria.addOrder(Order.asc("periodStart"));
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

        if (ServerConfig.PARAM_ADD_MAPPINGS_INTO_SERVICE_METRICS.equals(event.getPropertyName())) {
            if (Boolean.valueOf((String)event.getNewValue())) {
                _addMappingsIntoServiceMetrics.set(true);
            } else {
                _addMappingsIntoServiceMetrics.set(false);
                _logger.info("Adding message context mappings to Service Metrics is currently disabled.");
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

        if (Boolean.valueOf(ServerConfig.getInstance().getProperty(ServerConfig.PARAM_ADD_MAPPINGS_INTO_SERVICE_METRICS))) {
            _addMappingsIntoServiceMetrics.set(true);
        } else {
            _addMappingsIntoServiceMetrics.set(false);
            _logger.info("Adding message context mappings to Service Metrics is currently disabled.");
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServiceMetricsManagerImpl.class.getName());

    /** Name of cluster property that enables/disables service metrics collection. */
    private static final String CLUSTER_PROP_ENABLED = "serviceMetricsEnabled";

    private static final String HQL_DELETE = "DELETE FROM " + MetricsBin.class.getName() + " WHERE periodStart < ? AND resolution = ?";
    private static final String HQL_DELETE_DETAILS = "DELETE FROM " + MetricsBinDetail.class.getName() + " WHERE metricsBinOid = ?";

    private static final String SQL_INSERT_OR_UPDATE_BIN =
        "INSERT INTO service_metrics\n" +
        "    (objectid,nodeid,published_service_oid,resolution,period_start,interval_size,service_state,\n" +
        "     start_time,end_time,attempted,authorized,completed,back_min,back_max,back_sum,front_min,front_max,front_sum)\n" +
        "SELECT ?,?,?,?,?,?,?,\n" +
        "    min(start_time),\n" +
        "    max(end_time),\n" +
        "    coalesce(sum(attempted),0),\n" +
        "    coalesce(sum(authorized),0),\n" +
        "    coalesce(sum(completed),0),\n" +
        "    IF(min(coalesce(back_min, 100000000))=100000000,NULL,min(coalesce(back_min, 100000000))),\n" +
        "    IF(max(coalesce(back_max, -1))=-1,NULL,max(coalesce(back_max, -1))),\n" +
        "    coalesce(sum(back_sum),0),\n" +
        "    IF(min(coalesce(front_min, 100000000))=100000000,NULL,min(coalesce(front_min, 100000000))),\n" +
        "    IF(max(coalesce(front_max, -1))=-1,NULL,max(coalesce(front_max, -1)))," +
        "    coalesce(sum(front_sum),0)\n" +
        "FROM service_metrics WHERE\n" +
        "    nodeid=? AND\n" +
        "    published_service_oid=? AND\n" +
        "    resolution=? AND\n" +
        "    period_start>=?  AND\n" +
        "    period_start+interval_size<=?\n" +
        "GROUP BY nodeid,published_service_oid,resolution\n" +
        "ON DUPLICATE KEY UPDATE\n" +
        "    start_time=values(start_time),\n" +
        "    end_time=values(end_time),\n" +
        "    attempted=values(attempted),\n" +
        "    authorized=values(authorized),\n" +
        "    completed=values(completed),\n" +
        "    back_min=values(back_min),\n" +
        "    back_max=values(back_max),\n" +
        "    back_sum=values(back_sum),\n" +
        "    front_min=values(front_min),\n" +
        "    front_max=values(front_max),\n" +
        "    front_sum=values(front_sum);";

    private static final String SQL_INSERT_OR_UPDATE_DETAILS =
        "INSERT INTO service_metrics_details\n" +
        "    (service_metrics_oid,mapping_values_oid,attempted,authorized,completed,back_min,back_max,back_sum,front_min,front_max,front_sum)\n" +
        "SELECT\n" +
        "    (\n" +
        "        SELECT objectid FROM service_metrics WHERE\n" +
        "        nodeid=? AND\n" +
        "        published_service_oid=? AND\n" +
        "        resolution=? AND\n" +
        "        period_start=?\n" +
        "    ) as id,\n" +
        "    mapping_values_oid,\n" +
        "    coalesce(sum(attempted),0),\n" +
        "    coalesce(sum(authorized),0),\n" +
        "    coalesce(sum(completed),0),\n" +
        "    IF(min(coalesce(back_min, 100000000))=100000000,NULL,min(coalesce(back_min, 100000000))),\n" +
        "    IF(max(coalesce(back_max, -1))=-1,NULL,max(coalesce(back_max, -1))),\n" +
        "    coalesce(sum(back_sum),0),\n" +
        "    IF(min(coalesce(front_min, 100000000))=100000000,NULL,min(coalesce(front_min, 100000000))),\n" +
        "    IF(max(coalesce(front_max, -1))=-1,NULL,max(coalesce(front_max, -1))),\n" +
        "    coalesce(sum(front_sum),0)\n" +
        "FROM service_metrics_details WHERE service_metrics_oid IN\n" +
        "(\n" +
        "    SELECT objectid FROM service_metrics WHERE\n" +
        "    nodeid=? AND\n" +
        "    published_service_oid=? AND\n" +
        "    resolution=? AND\n" +
        "    period_start>=?  AND\n" +
        "    period_start+interval_size<=?\n" +
        ")\n" +
        "GROUP BY id, mapping_values_oid\n" +
        "ON DUPLICATE KEY UPDATE\n" +
        "    attempted=values(attempted),\n" +
        "    authorized=values(authorized),\n" +
        "    completed=values(completed),\n" +
        "    back_min=values(back_min),\n" +
        "    back_max=values(back_max),\n" +
        "    back_sum=values(back_sum),\n" +
        "    front_min=values(front_min),\n" +
        "    front_max=values(front_max),\n" +
        "    front_sum=values(front_sum);";

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

    private static final int MIN_FINE_AGE = MINUTE * 65; // more than 1 hour to ensure an hourly rollup bin can be created
    private static final int MAX_FINE_AGE = MINUTE * 65;
    private static final int DEF_FINE_AGE = MINUTE * 65;

    private static final long MIN_HOURLY_AGE = DAY + (5 * MINUTE); // more than 1 day to ensure an hourly rollup bin can be created
    private static final long MAX_HOURLY_AGE = 31 * DAY;     // a month
    private static final long DEF_HOURLY_AGE = 7 * DAY;      // a week

    private static final long MIN_DAILY_AGE = 31 * DAY;      // a month
    private static final long MAX_DAILY_AGE = 10 * YEAR;     // 10 years
    private static final long DEF_DAILY_AGE = YEAR;          // 1 year

    private static final Logger _logger = Logger.getLogger(ServiceMetricsManagerImpl.class.getName());

    /** ID for this node. */
    private String _clusterNodeId;

    /** Whether statistics collecting is turned on. */
    private final AtomicBoolean _enabled = new AtomicBoolean(false);

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
    private static final BlockingQueue<ServiceMetrics.MetricsCollectorSet> _flusherQueue = new ArrayBlockingQueue<ServiceMetrics.MetricsCollectorSet>(500);

    private final Map<Long, ServiceMetrics> _serviceMetricsMap = new HashMap<Long, ServiceMetrics>();
    private final Object _serviceMetricsMapLock = new Object();

    private PlatformTransactionManager _transactionManager;

    private ServiceManager _serviceManager;

    private Map<Long, ServiceState> serviceStates = new ConcurrentHashMap<Long, ServiceState>();

    private RoleManager _roleManager;

    private MessageContextMappingManager messageContextMappingManager;

    private AtomicBoolean _addMappingsIntoServiceMetrics = new AtomicBoolean(false);

    private IdentityProviderFactory identityProviderFactory;

    /** Turns on service metrics collection. */
    private void enable() {
        synchronized(_enableLock) {
            if (_enabled.get()) return;   // alreay enabled

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
            // Run slightly after the period end to allow all fine bins to be persisted
            final Date nextHourlyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_HOURLY, 0, now) + TimeUnit.MINUTES.toMillis(1));
            _hourlyArchiver = new HourlyTask();
            _timer.scheduleAtFixedRate(_hourlyArchiver, nextHourlyStart, HOUR);
            _logger.config("Scheduled first hourly archive task for " + nextHourlyStart);

            // Sets daily resolution timer to execute at the next daily period start (= end of current daily period).
            // But can't just schedule at fixed rate of 24-hours interval because a
            // calender day varies, e.g., when switching Daylight Savings Time.
            // Run slightly after the period end to allow all hourly bins to be persisted
            final Date nextDailyStart = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, now) + TimeUnit.MINUTES.toMillis(2));
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
                    Collection<ServiceHeader> serviceHeaders = _serviceManager.findAllHeaders();
                    for ( ServiceHeader service : serviceHeaders) {
                        final Long oid = service.getOid();
                        ServiceMetrics serviceMetrics = new ServiceMetrics(service.getOid());
                         _serviceMetricsMap.put(oid, serviceMetrics);
                        // There won't be any deleted services on startup
                        serviceStates.put(oid, service.isDisabled() ? ServiceState.DISABLED : ServiceState.ENABLED);
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

            _enabled.set(true);
        }
    }

    /** Turns off service metrics collection. */
    private void disable() {
        synchronized(_enableLock) {
            if (!_enabled.get()) return;  // already disabled

            _logger.info("Disabling service metrics collection.");

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

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent)event;
            if (!PublishedService.class.isAssignableFrom(eie.getEntityClass())) {
                return;
            }

            for (int i = 0; i < eie.getEntityOperations().length; i++) {
                char op = eie.getEntityOperations()[i];
                long oid = eie.getEntityIds()[i];
                switch (op) {
                    case EntityInvalidationEvent.CREATE: // Intentional fallthrough
                    case EntityInvalidationEvent.UPDATE:
                        try {
                            final PublishedService service = _serviceManager.findByPrimaryKey(oid);
                            final ServiceState state =
                                service == null ? ServiceState.DELETED :
                                    (service.isDisabled() ? ServiceState.DISABLED : ServiceState.ENABLED);
                            serviceStates.put(oid, state);
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
                        _logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                        Thread.currentThread().interrupt();
                        return;
                    }
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
            try {
                _flusherQueue.put( ServiceMetrics.getEmptyMetricsSet(periodStart, periodEnd) );
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
            Set<Long> list = new HashSet<Long>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.keySet());
            }

            // get start time for the last hourly period
            long startTime = MetricsBin.periodStartFor( MetricsBin.RES_HOURLY, 0, System.currentTimeMillis() ) - TimeUnit.HOURS.toMillis(1);
            for ( Long serviceOid : list ) {
                final ServiceState state = serviceStates.get( serviceOid );
                createHourlyBin( serviceOid, state, startTime );
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
            Set<Long> list = new HashSet<Long>();
            synchronized(_serviceMetricsMapLock) {
                list.addAll(_serviceMetricsMap.keySet());
            }

            // get start time for the last daily period
            long startTime = MetricsBin.periodStartFor( MetricsBin.RES_DAILY, 0, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1) );
            for ( Long serviceOid : list ) {
                final ServiceState state = serviceStates.get( serviceOid );
                createDailyBin( serviceOid, state, startTime );
            }
            if (_logger.isLoggable(Level.FINE))
                _logger.fine("Daily archiving task completed; archived " + list.size() + " daily bins.");

            // Schedule the next timer execution at the end of current period
            // (with a new task instance because a task cannot be reused).
            Date nextTimerDate = new Date(MetricsBin.periodEndFor(MetricsBin.RES_DAILY, 0, System.currentTimeMillis()));
            _timer.schedule(new DailyTask(), nextTimerDate);
            if (_logger.isLoggable(Level.FINE))
                _logger.fine("Scheduled next daily flush task for " + nextTimerDate);
        }
    }

    private void createHourlyBin( final long serviceOid, final ServiceState serviceState, final long startTime ) {
        createSummaryBin( serviceOid, serviceState, startTime, MetricsBin.RES_HOURLY, MetricsBin.RES_FINE );
    }

    private void createDailyBin( final long serviceOid, final ServiceState serviceState, final long startTime ) {
        createSummaryBin( serviceOid, serviceState, startTime, MetricsBin.RES_DAILY, MetricsBin.RES_HOURLY );
    }

    private void createSummaryBin( final long serviceOid, final ServiceState serviceState, final long startTime, final int binResolution, final int summaryResolution ) {
        try {
            new TransactionTemplate(_transactionManager).execute(new TransactionCallback() {
                public Object doInTransaction(final TransactionStatus status) {
                    return getHibernateTemplate().execute(new HibernateCallback() {
                        @SuppressWarnings({"deprecation"})
                        public Object doInHibernate(final Session session) throws HibernateException {
                            MetricsBin bin = new MetricsBin( startTime, _fineBinInterval, binResolution, _clusterNodeId, serviceOid );
                            Long id = (Long) ((SessionImplementor)session).getEntityPersister(null, bin).getIdentifierGenerator().generate(((SessionImplementor)session), bin);

                            Connection connection = null;
                            PreparedStatement statement = null;
                            try {
                                connection = session.connection();
                                statement = connection.prepareStatement(SQL_INSERT_OR_UPDATE_BIN);

                                int i=0;
                                // INSERT PARAMS
                                statement.setLong( ++i, id );
                                statement.setString( ++i, _clusterNodeId );
                                statement.setLong( ++i, serviceOid );
                                statement.setInt( ++i, binResolution );
                                statement.setLong( ++i, startTime );
                                statement.setInt( ++i, bin.getInterval() );
                                if( serviceState == null )
                                    statement.setNull( ++i, Types.VARCHAR );
                                else
                                    statement.setString( ++i, serviceState.toString() );

                                // FROM PARAMS
                                statement.setString( ++i, _clusterNodeId );
                                statement.setLong( ++i, serviceOid );
                                statement.setInt( ++i, summaryResolution );
                                statement.setLong( ++i, startTime );
                                statement.setLong( ++i, startTime + bin.getInterval() );

                                int result = statement.executeUpdate();
                                statement.close();
                                statement = null;
                                _logger.log(Level.FINE, "Row count for inserting/updating summary bin is " + result + ".");


                                statement = connection.prepareStatement(SQL_INSERT_OR_UPDATE_DETAILS);
                                i=0;

                                // INSERT BIN IDENTIFIER PARAMS
                                statement.setString( ++i, _clusterNodeId );
                                statement.setLong( ++i, serviceOid );
                                statement.setInt( ++i, binResolution );
                                statement.setLong( ++i, startTime );

                                // SOURCE BIN ID PARAMS
                                statement.setString( ++i, _clusterNodeId );
                                statement.setLong( ++i, serviceOid );
                                statement.setInt( ++i, summaryResolution );
                                statement.setLong( ++i, startTime );
                                statement.setLong( ++i, startTime + bin.getInterval() );
                                
                                result = statement.executeUpdate();
                                _logger.log(Level.FINE, "Row count for inserting/updating summary detail bins is " + result + ".");
                            } catch (SQLException se) {
                                status.setRollbackOnly();
                                _logger.log(Level.WARNING, "Error inserting/updating summary bin", se);
                            } finally {
                                ResourceUtils.closeQuietly( statement );
                                ResourceUtils.closeQuietly( connection );
                            }

                            return null;
                        }
                    });
                }
            });
        } catch (TransactionException te) {
            _logger.log(Level.WARNING, "Couldn't summarize MetricsBins", te);
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
                    flush();
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

        private void flush() throws InterruptedException {
            // This will wait indefinitely until there is an item in the queue.
            final ServiceMetrics.MetricsCollectorSet metricsSet = _flusherQueue.take();

            final MetricsBin bin = new MetricsBin( metricsSet.getStartTime(), _fineBinInterval, MetricsBin.RES_FINE, _clusterNodeId, metricsSet.getServiceOid() );
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

            if (_logger.isLoggable(Level.FINEST))
                _logger.finest("Saving " + bin.toString());

            new TransactionTemplate(_transactionManager).execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        getHibernateTemplate().execute(new HibernateCallback(){
                            public Object doInHibernate(Session session) throws HibernateException {
                                Criteria criteria = session.createCriteria(MetricsBin.class);
                                criteria.add(Restrictions.eq("clusterNodeId", bin.getClusterNodeId()));
                                criteria.add(Restrictions.eq("serviceOid", bin.getServiceOid()));
                                criteria.add(Restrictions.eq("resolution", bin.getResolution()));
                                criteria.add(Restrictions.eq("periodStart", bin.getPeriodStart()));
                                MetricsBin existing = (MetricsBin) criteria.uniqueResult();
                                if (existing == null) {
                                    Long id = (Long) session.save( bin );
                                    saveDetails( session, id, metricsSet.getDetailMetrics() );
                                } else {
                                    if (_logger.isLoggable(Level.FINE)) {
                                        _logger.log(Level.FINE, "Merging contents of duplicate MetricsBin [ClusterNodeId={0}; ServiceOid={1}; Resolution={2}; PeriodStart={3}]",
                                                new Object[]{bin.getClusterNodeId(), bin.getServiceOid(), bin.getResolution(), bin.getPeriodStart()});
                                    }
                                    existing.merge(bin);
                                    session.save(existing);

                                    // could merge these too
                                    Query deleteDetails = session.createQuery(HQL_DELETE_DETAILS);
                                    deleteDetails.setLong(0, existing.getOid());
                                    deleteDetails.executeUpdate();

                                    saveDetails( session, existing.getOid(), metricsSet.getDetailMetrics() );
                                }
                                return null;
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException("Error saving MetricsBin", e);
                    }
                }
            });
        }
    }

    private void saveDetails( final Session session, final Long id, final Map<ServiceMetrics.MetricsDetailKey,ServiceMetrics.MetricsCollector> detailMap ) {
        if ( detailMap != null ) {
            for ( Map.Entry<ServiceMetrics.MetricsDetailKey,ServiceMetrics.MetricsCollector> entry : detailMap.entrySet() ) {
                MetricsBinDetail details = new MetricsBinDetail();
                details.setMetricsBinOid( id );
                details.setMappingValuesOid( saveMessageContextMapping( entry.getKey() ) );

                if ( entry.getValue().getNumAttemptedRequest() > 0 ) {
                    details.setMinFrontendResponseTime( entry.getValue().getMinFrontendResponseTime() );
                    details.setMaxFrontendResponseTime( entry.getValue().getMaxFrontendResponseTime() );
                }
                details.setSumFrontendResponseTime( entry.getValue().getSumFrontendResponseTime() );

                if ( entry.getValue().getNumCompletedRequest() > 0 ) {
                    details.setMinBackendResponseTime( entry.getValue().getMinBackendResponseTime() );
                    details.setMaxBackendResponseTime( entry.getValue().getMaxBackendResponseTime() );
                }
                details.setSumBackendResponseTime( entry.getValue().getSumBackendResponseTime() );

                details.setNumAttemptedRequest( entry.getValue().getNumAttemptedRequest() );
                details.setNumAuthorizedRequest( entry.getValue().getNumAuthorizedRequest() );
                details.setNumCompletedRequest( entry.getValue().getNumCompletedRequest() );

                session.save( details );
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
                                return query.executeUpdate();
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
        if (user == null || _roleManager.isPermittedForAnyEntityOfType(user, OperationType.READ, EntityType.SERVICE)) {
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

    private Long saveMessageContextMapping( final ServiceMetrics.MetricsDetailKey key ) {
        if (messageContextMappingManager == null) return null;
        
        MessageContextMappingKeys keysEntity = new MessageContextMappingKeys();
        MessageContextMappingValues valuesEntity = new MessageContextMappingValues();

        keysEntity.setCreateTime(System.currentTimeMillis());
        valuesEntity.setCreateTime(System.currentTimeMillis());
        valuesEntity.setServiceOperation(key.getOperation());

        if ( key.getMappings() != null ) {
            int index = 0;
            for ( MessageContextMapping mapping : key.getMappings() ) {
                if ( mapping.getMappingType() == MessageContextMapping.MappingType.AUTH_USER ) {
                    if (key != null && key.getUserId() != null) {
                        valuesEntity.setAuthUserId(describe(key.getUserProviderId(), key.getUserId()));
                        valuesEntity.setAuthUserUniqueId(key.getUserId());
                        valuesEntity.setAuthUserProviderId( key.getUserProviderId() );
                    }
                } else {
                    keysEntity.setTypeAndKey(index, mapping.getMappingType(), mapping.getKey());
                    valuesEntity.setValue(index, mapping.getValue());
                    index++;
                }
            }
        }

        try {
            long mapping_keys_oid = messageContextMappingManager.saveMessageContextMappingKeys(keysEntity);
            valuesEntity.setMappingKeysOid(mapping_keys_oid);

            return messageContextMappingManager.saveMessageContextMappingValues(valuesEntity);
        } catch (Exception e) {
            _logger.warning("Failed to save the keys or values of the message context mapping.");
            return null;
        }
    }

    private String describe( final Long providerOid, final String userId ) {
        String description;

        try {
            IdentityProvider provider = identityProviderFactory.getProvider( providerOid );
            User user = provider.getUserManager().findByPrimaryKey( userId );
            description = getUserDesription(user) + " [" + provider.getConfig().getName() + "]";
        } catch ( FindException fe ) {
            description = userId + " [#" + providerOid + "]";
        }

        return description;
    }

    private String getUserDesription( final User user ) {
        String userName = user.getLogin();
        if (userName == null || "".equals(userName)) userName = user.getName();
        if (userName == null || "".equals(userName)) userName = user.getId();
        return userName;
    }

    

}