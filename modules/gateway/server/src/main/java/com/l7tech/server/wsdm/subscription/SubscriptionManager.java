/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.wsdm.subscription;

import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.*;
import com.l7tech.server.wsdm.faults.ResourceUnknownFault;
import com.l7tech.server.wsdm.faults.UnacceptableTerminationTimeFault;
import com.l7tech.server.wsdm.Aggregator;
import com.l7tech.util.Pair;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

/** @author alex */
@Transactional(rollbackFor = Throwable.class)
public interface SubscriptionManager extends EntityManager<Subscription, EntityHeader> {
    @Transactional(readOnly = true, rollbackFor = Throwable.class)
    Subscription findByUuid(String uuid) throws FindException;

    void deleteByUuid(String uuid) throws FindException, DeleteException, ResourceUnknownFault;

    void deleteExpiredSubscriptions() throws DeleteException;

    @Transactional(readOnly = true, rollbackFor = Throwable.class)
    void renewSubscription(String subscriptionId, long newTermination, String policyGuid) throws FindException, UpdateException, ResourceUnknownFault, UnacceptableTerminationTimeFault;

    @Transactional(readOnly = true, rollbackFor = Throwable.class)
    Collection<Subscription> findByNodeAndServiceGoid(String nodeId, Goid serviceGoid) throws FindException;

    /**
     * Find subscriptions that match the given criteria.
     *
     * <p>It is expected that in normal operation there is only one
     * subscription for these arguments.</p>
     *
     * @param subscriptionKey The SubscriptionKey (required)
     * @return The matching subscriptions (may be empty, never null)
     * @throws FindException If an error occurs during search.
     */
    @Transactional(readOnly = true, rollbackFor = Throwable.class)
    Collection<Subscription> findBySubscriptionKey(SubscriptionKey subscriptionKey) throws FindException;

    /**
     * Stamp the notification time for all subscriptions for the given node.
     *
     * @param clusterNodeId The node identifier (required)
     * @throws UpdateException If an error occurs during update
     */
    void notified(String clusterNodeId) throws UpdateException;

    @Transactional(readOnly = true, rollbackFor = Throwable.class)
    Map<SubscriptionKey, Pair<Subscription, MetricsSummaryBin>> findNotifiableMetricsForNode(String nodeId, long startTime) throws FindException;

    /**
     * Looks for subscriptions that belong to other (presumably down) nodes and have not been notified since the
     * provided staleTime, and ensures that they will get notified again by setting their ownerNodeId to this node's ID.
     * @param clusterNodeId Id of the node doing the stealing
     * @param staleTime The stale period
     * @throws FindException If an error occurs
     */
    void stealSubscriptionsFromDeadNodes(String clusterNodeId, long staleTime);

    void setAggregator(Aggregator aggregator);
}
