package com.l7tech.server.service;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.*;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.mapping.MessageContextMappingManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ResourceUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the processing and accumulation of service metrics for one SSG node.
 *
 * @author rmak
 * @author alex
 */

@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ServiceMetricsManagerImpl extends HibernateDaoSupport implements ServiceMetricsManager {
    public ServiceMetricsManagerImpl(String clusterNodeId) {
        _clusterNodeId = clusterNodeId;
    }

    /**
     * Searches for metrics bins with the given criteria and summarizes by
     * combining bins with the same period start.
     *
     * @param nodeId            cluster node ID; null = all
     * @param serviceGoids       published service GOIDs; null = all services permitted for this user
     * @param resolution        bin resolution ({@link MetricsBin#RES_FINE},
     *                          {@link MetricsBin#RES_HOURLY} or
     *                          {@link MetricsBin#RES_DAILY}); null = all
     * @param minPeriodStart    minimum bin period start time (milliseconds since epoch); null = as far back as available
     * @param maxPeriodStart    maximum bin period statt time (milliseconds since epoch); null = up to the latest available
     * @param includeEmpty      whether to include empty uptime bins (same as include service GOID DEFAULT_GOID)
     *
     * @return collection of summary bins; can be empty but never <code>null</code>
     * @throws FindException if failure to query database
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED, readOnly=true)
    public Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId,
                                                           final Goid[] serviceGoids,
                                                           final Integer resolution,
                                                           final Long minPeriodStart,
                                                           final Long maxPeriodStart,
                                                           final boolean includeEmpty)
        throws FindException
    {
        final Collection<MetricsBin> bins = findBins(nodeId, serviceGoids, resolution, minPeriodStart, maxPeriodStart, includeEmpty);

        return MetricsSummaryBin.createSummaryMetricsBinsByPeriodStart(bins);
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    public Map<Goid, MetricsSummaryBin> summarizeByService(final String nodeId, final Integer resolution, final Long minPeriodStart, final Long maxPeriodStart, Goid[] serviceGoids, boolean includeEmpty)
        throws FindException
    {
        final Collection<MetricsBin> bins = findBins(nodeId, serviceGoids, resolution, minPeriodStart, maxPeriodStart, includeEmpty);
        return MetricsSummaryBin.createSummaryMetricsBinsByServiceOid(bins);
    }

    @SuppressWarnings({"unchecked"})
    private List<MetricsBin> findBins(
                          final String nodeId,
                          final Goid[] serviceGoids,
                          final Integer resolution,
                          final Long minPeriodStart,
                          final Long maxPeriodStart,
                          final boolean includeEmpty)
        throws FindException
    {

        // Enforces RBAC permissions.
        final Set<Goid> filteredGoids = filterPermittedPublishedServices(serviceGoids);
        if (filteredGoids != null && includeEmpty) {
            filteredGoids.add(GoidEntity.DEFAULT_GOID);      // Empty uptime bins have service GOID of DEFAULT_GOID.
        }
        if (filteredGoids != null && filteredGoids.isEmpty()) {
            return Collections.emptyList();     // No bins can possibly be found.
        }

        try {
            //noinspection UnnecessaryLocalVariable
            List<MetricsBin> bins = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    Criteria crit = session.createCriteria(MetricsBin.class);

                    if (nodeId != null)
                        crit.add(Restrictions.eq("clusterNodeId", nodeId));

                    if (filteredGoids != null) {
                        if (filteredGoids.size() == 1)
                            crit.add(Restrictions.eq("serviceGoid", filteredGoids.iterator().next()));
                        else
                            crit.add(Restrictions.in("serviceGoid", filteredGoids));
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
                                    ", serviceGoid=" + filteredGoids +
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
     * @param serviceGoids   published service GOIDs; null = all services permitted for this user
     * @param resolution    bin resolution ({@link MetricsBin#RES_FINE},
     *                      {@link MetricsBin#RES_HOURLY} or
     *                      {@link MetricsBin#RES_DAILY})
     * @param duration      time duration (milliseconds from latest nominal period boundary
     *                      time on gateway) to search backward for bins whose
     *                      nominal periods fall within
     * @param includeEmpty  whether to include empty uptime bins (same as include service OID of DEFAULT_GOID)
     *
     * @return a summary bin; <code>null</code> if no metrics bins are found
     * @throws FindException if failure to query database
     */
    @Override
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    public MetricsSummaryBin summarizeLatest(final String clusterNodeId,
                                             final Goid[] serviceGoids,
                                             final int resolution,
                                             final int duration,
                                             final boolean includeEmpty)
            throws FindException {
        // Enforces RBAC permissions.
        final Set<Goid> filteredGoids = filterPermittedPublishedServices(serviceGoids);
        if (filteredGoids != null && includeEmpty) {
            filteredGoids.add(GoidEntity.DEFAULT_GOID);      // Empty uptime bins have service OID of DEFAULT_GOID
        }
        if (filteredGoids != null && filteredGoids.isEmpty()) {
            return null;    // No bins can possibly be found.
        }

        // Computes the summary period by counting back from the latest nominal
        // period boundary time. This is to ensure that we will find a full
        // number of bins filling the given duration (e.g., a 24-hour duration
        // will find 24 hourly bins; when they are all available).
        final long summaryPeriodEnd = MetricsBin.periodStartFor(resolution, getFineInterval(), System.currentTimeMillis());
        final long summaryPeriodStart = summaryPeriodEnd - duration;

        Collection<MetricsBin> bins;
        try {
            // noinspection unchecked
            bins = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    final Criteria criteria = session.createCriteria(MetricsBin.class);
                    if (clusterNodeId != null) {
                        criteria.add(Restrictions.eq("clusterNodeId", clusterNodeId));
                    }
                    if (filteredGoids != null) {
                        if (filteredGoids.size() == 1)
                            criteria.add(Restrictions.eq("serviceGoid", filteredGoids.iterator().next()));
                        else
                            criteria.add(Restrictions.in("serviceGoid", filteredGoids));
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
                                    ", serviceGoid=" + filteredGoids +
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

    @Override
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    public ServiceState getCreatedOrUpdatedServiceState(Goid goid) throws FindException {
        if (!ready()) {
            logger.warn("Unable to check service state -- not ready");
            return ServiceState.DISABLED;
        }
        final PublishedService service = _serviceManager.findByPrimaryKey(goid);
        return service == null ? ServiceState.DELETED :
            (service.isDisabled() ? ServiceState.DISABLED : ServiceState.ENABLED);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Integer delete(final long oldestSurvivor, final int resolution) {
        return (Integer) getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Query query = session.createQuery(HQL_DELETE);
                query.setLong(0, oldestSurvivor);
                query.setInteger(1, resolution);
                return query.executeUpdate();
            }
        });
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRED, readOnly=true)
    public Collection<ServiceHeader> findAllServiceHeaders() throws FindException {
        if (!ready()) {
            logger.warn("Unable to find service headers -- not ready");
            return Collections.emptyList();
        }
        return _serviceManager.findAllHeaders(false);
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    public void doFlush(final ServiceMetrics.MetricsCollectorSet metricsSet, final MetricsBin bin) {
        //todo Add retry attempts to attempt to recover from expected database failures such as deadlocks.
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                public Object doInHibernate(Session session) throws HibernateException {
                    Criteria criteria = session.createCriteria(MetricsBin.class);
                    criteria.add(Restrictions.eq("clusterNodeId", bin.getClusterNodeId()));
                    criteria.add(Restrictions.eq("serviceGoid", bin.getServiceGoid()));
                    criteria.add(Restrictions.eq("resolution", bin.getResolution()));
                    criteria.add(Restrictions.eq("periodStart", bin.getPeriodStart()));
                    final MetricsBin existing = (MetricsBin) criteria.uniqueResult();
                    if (existing == null) {
                        Goid goid = (Goid) session.save(bin);
                        saveDetails(session, goid, metricsSet.getDetailMetrics());
                    } else {
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.log(Level.FINE, "Merging contents of duplicate MetricsBin [ClusterNodeId={0}; ServiceOid={1}; Resolution={2}; PeriodStart={3}]",
                                    new Object[]{bin.getClusterNodeId(), bin.getServiceGoid(), bin.getResolution(), bin.getPeriodStart()});
                        }
                        existing.merge(bin);
                        session.save(existing);

                        // could merge the details too
                        session.doWork( new Work(){
                            @Override
                            public void execute( final Connection connection ) throws SQLException {
                                PreparedStatement statement = null;
                                try {
                                    statement = connection.prepareStatement( SQL_DELETE_DETAILS );
                                    statement.setBytes(1, existing.getGoid().getBytes());
                                    int count = statement.executeUpdate();
                                    if (_logger.isLoggable(Level.FINE)) {
                                        _logger.log(Level.FINE, "Deleted {4} detail metrics bins [ClusterNodeId={0}; ServiceOid={1}; Resolution={2}; PeriodStart={3}]",
                                                new Object[]{bin.getClusterNodeId(), bin.getServiceGoid(), bin.getResolution(), bin.getPeriodStart(), count});
                                    }
                                } finally {
                                    ResourceUtils.closeQuietly( statement );
                                }
                            }
                        } );

                        saveDetails(session, existing.getGoid(), metricsSet.getDetailMetrics());
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error saving MetricsBin", e);
        }
    }

    //- PROTECTED

    @Override
    protected void initDao() throws Exception {
        if (_transactionManager == null) logger.warn("TransactionManager must be set");
        if (_serviceManager == null) logger.warn("ServiceManager must be set");
        if (_clusterNodeId == null) logger.warn("clusterNodeId must be set");
    }

    //- PRIVATE

    private boolean ready() {
        return _transactionManager != null && _serviceManager != null && _clusterNodeId != null;
    }

    private static final String HQL_DELETE = "DELETE FROM " + MetricsBin.class.getName() + " WHERE periodStart < ? AND resolution = ?";
    private static final String SQL_DELETE_DETAILS = "DELETE FROM service_metrics_details WHERE service_metrics_goid = ?";

    private static final String SQL_INSERT_OR_UPDATE_BIN =
        "INSERT INTO service_metrics\n" +
        "    (goid,nodeid,published_service_goid,resolution,period_start,interval_size,service_state,\n" +
        "     start_time,end_time,attempted,authorized,completed,back_min,back_max,back_sum,front_min,front_max,front_sum)\n" +
        "SELECT ?,?,?,?,?,?,?,\n" +
        "    min(start_time),\n" +
        "    max(end_time),\n" +
        "    coalesce(sum(attempted),0),\n" +
        "    coalesce(sum(authorized),0),\n" +
        "    coalesce(sum(completed),0),\n" +
        "    IF(min(coalesce(back_min, 2147483647))=2147483647,NULL,min(coalesce(back_min, 2147483647))),\n" +
        "    IF(max(coalesce(back_max, -1))=-1,NULL,max(coalesce(back_max, -1))),\n" +
        "    IF(coalesce(sum(back_sum),0)>2147483647,2147483647,coalesce(sum(back_sum),0)),\n" +
        "    IF(min(coalesce(front_min, 2147483647))=2147483647,NULL,min(coalesce(front_min, 2147483647))),\n" +
        "    IF(max(coalesce(front_max, -1))=-1,NULL,max(coalesce(front_max, -1))),\n" +
        "    IF(coalesce(sum(front_sum),0)>2147483647,2147483647,coalesce(sum(front_sum),0))\n" +
        "FROM service_metrics WHERE\n" +
        "    nodeid=? AND\n" +
        "    published_service_goid=? AND\n" +
        "    resolution=? AND\n" +
        "    period_start>=?  AND\n" +
        "    period_start+interval_size<=?\n" +
        "GROUP BY nodeid,published_service_goid,resolution\n" +
        "HAVING min(start_time) > 0\n" +
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
        "    (service_metrics_goid,mapping_values_goid,attempted,authorized,completed,back_min,back_max,back_sum,front_min,front_max,front_sum)\n" +
        "SELECT\n" +
        "    (\n" +
        "        SELECT goid FROM service_metrics WHERE\n" +
        "        nodeid=? AND\n" +
        "        published_service_goid=? AND\n" +
        "        resolution=? AND\n" +
        "        period_start=?\n" +
        "    ) as id,\n" +
        "    smd.mapping_values_goid,\n" +
        "    coalesce(sum(smd.attempted),0),\n" +
        "    coalesce(sum(smd.authorized),0),\n" +
        "    coalesce(sum(smd.completed),0),\n" +
        "    IF(min(coalesce(smd.back_min, 2147483647))=2147483647,NULL,min(coalesce(smd.back_min, 2147483647))),\n" +
        "    IF(max(coalesce(smd.back_max, -1))=-1,NULL,max(coalesce(smd.back_max, -1))),\n" +
        "    IF(coalesce(sum(smd.back_sum),0)>2147483647,2147483647,coalesce(sum(smd.back_sum),0)),\n" +
        "    IF(min(coalesce(smd.front_min, 2147483647))=2147483647,NULL,min(coalesce(smd.front_min, 2147483647))),\n" +
        "    IF(max(coalesce(smd.front_max, -1))=-1,NULL,max(coalesce(smd.front_max, -1))),\n" +
        "    IF(coalesce(sum(smd.front_sum),0)>2147483647,2147483647,coalesce(sum(smd.front_sum),0))\n" +
        "FROM service_metrics sm, service_metrics_details smd\n"+
        "WHERE " +
        "    sm.goid = smd.service_metrics_goid AND\n"+
        "    sm.nodeid=? AND\n" +
        "    sm.published_service_goid=? AND\n" +
        "    sm.resolution=? AND\n" +
        "    sm.period_start>=?  AND\n" +
        "    sm.period_start+sm.interval_size<=?\n" +
        "GROUP BY id, mapping_values_goid\n" +
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

    private static final Logger _logger = Logger.getLogger(ServiceMetricsManagerImpl.class.getName());

    private final String _clusterNodeId;
    private int fineBinInterval;

    @Inject
    private PlatformTransactionManager _transactionManager;

    @Inject
    private ServiceManager _serviceManager;

    @Inject
    private RoleManager _roleManager;

    @Inject
    private MessageContextMappingManager messageContextMappingManager;

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @Override
    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    public void createHourlyBin(final Goid serviceGoid, final ServiceState serviceState, final long startTime ) throws SaveException {
        createSummaryBin( serviceGoid, serviceState, startTime, MetricsBin.RES_HOURLY, MetricsBin.RES_FINE );
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    public void createDailyBin(final Goid serviceGoid, final ServiceState serviceState, final long startTime ) throws SaveException {
        createSummaryBin( serviceGoid, serviceState, startTime, MetricsBin.RES_DAILY, MetricsBin.RES_HOURLY );
    }

    private void createSummaryBin(final Goid serviceGoid, final ServiceState serviceState, final long startTime, final int binResolution, final int summaryResolution ) throws SaveException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                @SuppressWarnings({"deprecation"})
                public Object doInHibernate(final Session session) throws HibernateException {
                    final MetricsBin bin = new MetricsBin(startTime, getFineInterval(), binResolution, _clusterNodeId, serviceGoid);
                    final Goid goid = (Goid) ((SessionImplementor) session).getEntityPersister(null, bin).getIdentifierGenerator().generate(((SessionImplementor) session), bin);

                    session.doWork(new Work() {
                        @Override
                        public void execute(Connection connection) throws SQLException {
                            PreparedStatement statement = null;
                            try {
                                statement = connection.prepareStatement(SQL_INSERT_OR_UPDATE_BIN);

                                int i = 0;
                                // INSERT PARAMS
                                statement.setBytes(++i, goid.getBytes());
                                statement.setString(++i, _clusterNodeId);
                                statement.setBytes(++i, serviceGoid.getBytes());
                                statement.setInt(++i, binResolution);
                                statement.setLong(++i, startTime);
                                statement.setInt(++i, bin.getInterval());
                                if (serviceState == null)
                                    statement.setNull(++i, Types.VARCHAR);
                                else
                                    statement.setString(++i, serviceState.toString());

                                // FROM PARAMS
                                statement.setString(++i, _clusterNodeId);
                                statement.setBytes(++i, serviceGoid.getBytes());
                                statement.setInt(++i, summaryResolution);
                                statement.setLong(++i, startTime);
                                statement.setLong(++i, startTime + bin.getInterval());

                                int result = statement.executeUpdate();
                                statement.close();
                                statement = null;
                                _logger.log(Level.FINE, "Row count for inserting/updating summary bin is " + result + ".");

                                statement = connection.prepareStatement(SQL_INSERT_OR_UPDATE_DETAILS);
                                i = 0;

                                // INSERT BIN IDENTIFIER PARAMS
                                statement.setString(++i, _clusterNodeId);
                                statement.setBytes(++i, serviceGoid.getBytes());
                                statement.setInt(++i, binResolution);
                                statement.setLong(++i, startTime);

                                // SOURCE BIN ID PARAMS
                                statement.setString(++i, _clusterNodeId);
                                statement.setBytes(++i, serviceGoid.getBytes());
                                statement.setInt(++i, summaryResolution);
                                statement.setLong(++i, startTime);
                                statement.setLong(++i, startTime + bin.getInterval());

                                result = statement.executeUpdate();
                                _logger.log(Level.FINE, "Row count for inserting/updating summary detail bins is " + result + ".");
                            } finally {
                                ResourceUtils.closeQuietly(statement);
                            }
                        }
                    });

                    return null;
                }});
        } catch (Exception e) {
            throw new SaveException("Coudln't save metrics summary bin", e);
        }
    }

    private void saveDetails( final Session session, final Goid goid, final Map<ServiceMetrics.MetricsDetailKey,ServiceMetrics.MetricsCollector> detailMap ) {
        if ( detailMap != null ) {
            for ( Map.Entry<ServiceMetrics.MetricsDetailKey,ServiceMetrics.MetricsCollector> entry : detailMap.entrySet() ) {
                MetricsBinDetail details = new MetricsBinDetail();
                details.setMetricsBinGoid(goid);
                details.setMappingValuesId(saveMessageContextMapping(entry.getKey()));

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
     * Filters down the list of published service GOID according to the user's RBAC permissions.
     *
     * @param serviceGoids   <code>null</code> means all service permitted for user
     * @return list of services permitted; can be empty; <code>null</code> if all
     *         available services are permitted
     * @throws FindException
     */
    private Set<Goid> filterPermittedPublishedServices(final Goid[] serviceGoids) throws FindException {
        if (!ready()) {
            logger.warn("Unable to filter permitted published services -- not ready");
            return Collections.emptySet();
        }

        Set<Goid> filteredGoids = null;
        if (serviceGoids != null) {
            filteredGoids = new HashSet<Goid>();
            for (Goid serviceGoid : serviceGoids) {
                filteredGoids.add(serviceGoid);
            }
        }

        final User user = JaasUtils.getCurrentUser();
        if (user == null || _roleManager.isPermittedForAnyEntityOfType(user, OperationType.READ, EntityType.SERVICE)) {
            // No filtering needed.
        } else {
            Set<Goid> permittedGoids = new HashSet<Goid>();
            for (PublishedService service : _serviceManager.findAll()) {
                if (_roleManager.isPermittedForEntity(user, service, OperationType.READ, null)) {
                    permittedGoids.add(service.getGoid());
                }
            }

            if (filteredGoids == null) {
                filteredGoids = permittedGoids;
            } else {
                for (Iterator<Goid> i = filteredGoids.iterator(); i.hasNext(); ) {
                    if (!permittedGoids.contains(i.next())) {
                        i.remove();
                    }
                }
            }

            if (_logger.isLoggable(Level.FINER)) {
                _logger.finer("Filtered published services from " +
                        (serviceGoids == null ? "*" : serviceGoids.length) +
                        " to " + filteredGoids.size());
            }
        }

        return filteredGoids;
    }

    /**
     * NOTE: Since this method only uses the MetricsDetailKeys user if there is a user mapping
     * so do the equals/hashcode of MetricsDetailKeys.
     * @see com.l7tech.server.service.ServiceMetrics.MetricsDetailKey#hasUserMapping()
     */
    private Goid saveMessageContextMapping( final ServiceMetrics.MetricsDetailKey key ) {
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
                    if (key.getUserId() != null) {
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
            Goid mapping_keys_id = messageContextMappingManager.saveMessageContextMappingKeys(keysEntity);
            valuesEntity.setMappingKeysGoid(mapping_keys_id);

            return messageContextMappingManager.saveMessageContextMappingValues(valuesEntity);
        } catch (Exception e) {
            _logger.warning("Failed to save the keys or values of the message context mapping.");
            return null;
        }
    }

    private String describe( final Goid providerOid, final String userId ) {
        String description = null;

        try {
            final IdentityProvider provider = identityProviderFactory.getProvider( providerOid );
            if ( provider != null ) {
                final User user = provider.getUserManager().findByPrimaryKey( userId );
                description =
                        (user==null ? userId : getUserDescription(user)) +
                        " [" + provider.getConfig().getName() + "]";
            }
        } catch ( Exception fe ) {
            _logger.log( Level.WARNING, "Error accessing user details.", fe );
        }

        if ( description == null ) {
            description = userId + " [#" + providerOid + "]";
        }

        return description;
    }

    private String getUserDescription( final User user ) {
        String userName = user.getLogin();
        if (userName == null || "".equals(userName)) userName = user.getName();
        if (userName == null || "".equals(userName)) userName = user.getId();
        return userName;
    }

    private int getFineInterval() {
        int fineBinInterval = this.fineBinInterval;
        if ( fineBinInterval == 0 ) {
            fineBinInterval = ConfigFactory.getIntProperty( "metricsFineInterval", ServiceMetricsServices.DEF_FINE_BIN_INTERVAL );
            if (fineBinInterval > ServiceMetricsServices.MAX_FINE_BIN_INTERVAL || fineBinInterval < ServiceMetricsServices.MIN_FINE_BIN_INTERVAL) {
                fineBinInterval = ServiceMetricsServices.DEF_FINE_BIN_INTERVAL;
            }
            this.fineBinInterval = fineBinInterval;
        }
        return fineBinInterval;
    }

}
