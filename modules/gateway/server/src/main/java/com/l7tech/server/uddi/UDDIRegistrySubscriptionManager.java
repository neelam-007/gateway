package com.l7tech.server.uddi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntityManager;

import java.util.Collection;

/**
 * Entity manager for UDDIRegistrySubscriptions
 */
public interface UDDIRegistrySubscriptionManager extends GoidEntityManager<UDDIRegistrySubscription, EntityHeader> {

    /**
     * Find the UDDIRuntimeServiceInfos for a UDDI registry.
     *
     * @param uddiRegistryGoid The goid of the UDDI registry
     * @return the related subscription or null
     * @throws FindException If an error occurs
     */
    UDDIRegistrySubscription findByUDDIRegistryGoid( Goid uddiRegistryGoid ) throws FindException;

    /**
     * Find the UDDIRuntimeServiceInfos that match a subscription key.
     *
     * @param subscriptionKey The subscription key
     * @return The collection of matching subscriptions (may be empty but not null)
     * @throws FindException If an error occurs
     */
    Collection<UDDIRegistrySubscription> findBySubscriptionKey( String subscriptionKey ) throws FindException;
}
