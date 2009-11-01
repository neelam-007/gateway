package com.l7tech.server.uddi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

/**
 * Entity manager for UDDIRegistrySubscriptions
 */
public interface UDDIRegistrySubscriptionManager extends EntityManager<UDDIRegistrySubscription, EntityHeader> {

    /**
     * Find the UDDIRuntimeServiceInfos for a UDDI registry.
     *
     * @param uddiRegistryOid The oid of the UDDI registry
     * @return the related subscription or null
     * @throws FindException If an error occurs
     */
    UDDIRegistrySubscription findByUDDIRegistryOid( long uddiRegistryOid ) throws FindException;

    /**
     * Find the UDDIRuntimeServiceInfos that match a subscription key.
     *
     * @param subscriptionKey The subscription key
     * @return The collection of matching subscriptions (may be empty but not null)
     * @throws FindException If an error occurs
     */
    Collection<UDDIRegistrySubscription> findBySubscriptionKey( String subscriptionKey ) throws FindException;
}
