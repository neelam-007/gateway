/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.wsdm.subscription;

import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.server.wsdm.Aggregator;
import com.l7tech.server.wsdm.faults.ResourceUnknownFault;
import com.l7tech.server.wsdm.faults.UnacceptableTerminationTimeFault;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class SubscriptionManagerImpl
    extends HibernateEntityManager<Subscription, EntityHeader>
    implements SubscriptionManager
{
    private static final Logger logger = Logger.getLogger(SubscriptionManagerImpl.class.getName());

    private static final String TABLE_NAME = "wsdm_subscription";
    private static final String COLUMN_CALLBACK = "referenceCallback";
    private static final String COLUMN_NODEID = "ownerNodeId";
    private static final String COLUMN_NOTIFIED = "lastNotificationTime";
    private static final String COLUMN_SERVICEGOID = "publishedServiceGoid";
    private static final String COLUMN_TERMINATION = "termination";
    private static final String COLUMN_TOPIC = "topic";
    private static final String COLUMN_UUID = "uuid";
    private static final String HQL_UPDATE_TIME_BY_ID = String.format("UPDATE %s set %s = :%s where %s = :%s", 
                                                                      Subscription.class.getName(),
                                                                      COLUMN_NOTIFIED,
                                                                      COLUMN_NOTIFIED,
                                                                      COLUMN_NODEID,
                                                                      COLUMN_NODEID);

    private Aggregator aggregator;

    public SubscriptionManagerImpl() {
    }

    public void setAggregator(Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    public Subscription findByUuid(final String uuid) throws FindException {
        try {
            return findSubscription(uuid);
        } catch (DataAccessException e) {
            throw new FindException("Unable to find Subscription with UUID " + uuid, e);
        }
    }

    public void deleteByUuid(String uuid) throws FindException, DeleteException, ResourceUnknownFault {
        Subscription sub = findByUuid(uuid);
        if (sub == null) throw new ResourceUnknownFault("No such subscription with id " + uuid);

        try {
            getHibernateTemplate().delete(sub);
        } catch (DataAccessException e) {
            throw new DeleteException("Unable to delete Subscription with UUID " + uuid, e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private Subscription findSubscription(final String uuid) {
        List<Subscription> subs;
        subs = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(getInterfaceClass());
                crit.add(Restrictions.eq(COLUMN_UUID, uuid));
                return crit.list();
            }
        });
        if (subs.isEmpty()) return null;
        if (subs.size() > 1) throw new IllegalStateException("Found more than one Subscription with UUID " + uuid);
        return subs.iterator().next();
    }

    public void deleteExpiredSubscriptions() throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(Subscription.class);
                    crit.add(Restrictions.lt(COLUMN_TERMINATION, System.currentTimeMillis()));
                    @SuppressWarnings({"unchecked"})
                    final List<Subscription> subs = crit.list();
                    int victims = 0;
                    for (Subscription deadGuy : subs) {
                        logger.log(Level.INFO, "Subscription {0} has expired and will be forgotten", deadGuy.getUuid());
                        try {
                            session.delete(deadGuy);
                            victims++;
                        } catch (HibernateException e) {
                            logger.log(Level.WARNING, "Unable to delete expired Subscription " + deadGuy.getUuid(), e);
                        }
                    }
                    if (victims < 1) {
                        logger.fine("Nothing to clean up");
                    } else {
                        logger.log(Level.FINE, "{0} subscriptions cleaned up", victims);
                    }
                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new DeleteException("Unable to delete expired subscriptions", e);
        }
    }

    public void renewSubscription(String subscriptionId, long newTermination, String policyGuid) throws FindException, UpdateException, ResourceUnknownFault, UnacceptableTerminationTimeFault {
        Subscription sub = findByUuid(subscriptionId);
        if (sub == null) throw new ResourceUnknownFault("No such subscription with id " + subscriptionId);

        long now = System.currentTimeMillis();
        if (newTermination < now) {//if you are trying to renew a subscription with a termination time in the past, fail
            logger.warning("Attempt to renew an ESM subscription with a termination time in the past. Returning SOAP fault");
            throw new UnacceptableTerminationTimeFault("Cannot have a termination time in the past.");
        } else {
            boolean goAhead = false;
            long oldTermTime = sub.getTermination();
            if (newTermination != oldTermTime) {
                logger.info(MessageFormat.format("Updating ESM subscription {0} with new termination time {1}",subscriptionId, newTermination));
                sub.setTermination(newTermination);
                goAhead = true;
            }

            String oldPolGuid = sub.getNotificationPolicyGuid();
            if (!StringUtils.equals(oldPolGuid, policyGuid)){
                logger.info(MessageFormat.format("Updating ESM subscription {0} with new notification policy GUID {1}",subscriptionId, policyGuid));
                sub.setNotificationPolicyGuid(policyGuid);
                goAhead = true;
            }

            if (goAhead) update(sub);
        }
    }

    @SuppressWarnings({"unchecked"})
    public Collection<Subscription> findByNodeAndServiceGoid(final String clusterNodeId, final Goid serviceGoid) throws FindException {
        return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Subscription.class);
                crit.add(Restrictions.eq(COLUMN_NODEID, clusterNodeId));
                crit.add(Restrictions.eq(COLUMN_SERVICEGOID, serviceGoid));
                return crit.list();
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    public Collection<Subscription> findBySubscriptionKey(final SubscriptionKey key) throws FindException {
        return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Subscription.class);
                crit.add(Restrictions.eq(COLUMN_SERVICEGOID, key.getServiceGoid()));
                crit.add(Restrictions.eq(COLUMN_TOPIC, key.getTopic()));
                crit.add(Restrictions.eq(COLUMN_CALLBACK, key.getNotificationUrl()));
                return crit.list();
            }
        });
    }

    public void notified( final String clusterNodeId ) throws UpdateException {
        final long now = System.currentTimeMillis();
        getHibernateTemplate().execute( new HibernateCallback(){
            public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                session.createQuery( HQL_UPDATE_TIME_BY_ID )
                        .setLong(COLUMN_NOTIFIED, now )
                        .setString(COLUMN_NODEID, clusterNodeId )
                        .executeUpdate();
                return null;
            }
        } );
    }

    @SuppressWarnings({"unchecked"})
    public Map<SubscriptionKey, Pair<Subscription, MetricsSummaryBin>> findNotifiableMetricsForNode(final String clusterNodeId,
                                                                                                    final long startTime) throws FindException {
        final long now = System.currentTimeMillis();

        final List<Subscription> myUnexpiredMetricsSubs;
        try {
            myUnexpiredMetricsSubs = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(Subscription.class);
                    crit.add(Restrictions.eq(COLUMN_NODEID, clusterNodeId));                   // This node is the one responsible for notifying this subscription
                    crit.add(Restrictions.gt(COLUMN_TERMINATION, now));                             // Subscription terminates in the future
                    crit.add(Restrictions.eq(COLUMN_TOPIC, Subscription.TOPIC_METRICS_CAPABILITY)); // Subscribtion is for Metrics (as opposed to OperationalStatus)
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Unable to find subscriptions", e);
        }

        final Map<Goid, MetricsSummaryBin> bins = aggregator.getMetricsForServices();

        if (myUnexpiredMetricsSubs == null || myUnexpiredMetricsSubs.isEmpty()) return Collections.emptyMap();

        final Map<SubscriptionKey, Pair<Subscription, MetricsSummaryBin>> result = new HashMap<SubscriptionKey, Pair<Subscription, MetricsSummaryBin>>();

        for (Subscription sub : myUnexpiredMetricsSubs) {
            final Goid goid = sub.getPublishedServiceGoid();
            final MetricsSummaryBin bin = bins.get(goid);
            if (bin != null) {
                SubscriptionKey key = new SubscriptionKey(sub);
                result.put(key, new Pair<Subscription, MetricsSummaryBin>(sub, bin));
            } else {
                logger.fine("No ServiceMetrics bins available for " + goid);
            }
        }

        return result;
    }

    @SuppressWarnings({"unchecked"})
    public void stealSubscriptionsFromDeadNodes(final String clusterNodeId, final long staleTime) {
        final long now = System.currentTimeMillis();
        final List<Subscription> staleSubs;
        try {
            staleSubs = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(Subscription.class);
                    crit.add(Restrictions.ne(COLUMN_NODEID, clusterNodeId));       // It's someone else's subscription
                    crit.add(Restrictions.lt(COLUMN_NOTIFIED, staleTime));  // Last notification was prior to the stale timestamp
                    crit.add(Restrictions.gt(COLUMN_TERMINATION, now));                 // Not expired
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            logger.log(Level.WARNING, "Unable to find subscriptions", e);
            return;
        }

        for (Subscription sub : staleSubs) {
            logger.log(Level.INFO, MessageFormat.format("Assuming control of stale subscription {0} (belonged to node {1} that is presumed dead)", sub.getUuid(), sub.getOwnerNodeId()));
            sub.setOwnerNodeId(clusterNodeId);
            sub.setLastNotificationTime(now);
            try {
                update(sub);
            } catch (UpdateException e) {
                logger.log(Level.WARNING, "Unable to reset owner ID for " + sub.getUuid());
            }
        }
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final Subscription entity) {
        Map<String, Object> map = new HashMap<String, Object>() {{
                put(COLUMN_UUID, entity.getUuid());
            }};
        return Arrays.asList(map);
    }

    public Class<? extends Entity> getImpClass() {
        return Subscription.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return Subscription.class;
    }

    public String getTableName() {
        return TABLE_NAME;
    }
}
