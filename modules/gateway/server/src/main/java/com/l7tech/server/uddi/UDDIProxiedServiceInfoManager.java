/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.objectmodel.*;
import java.util.Collection;

public interface UDDIProxiedServiceInfoManager extends EntityManager<UDDIProxiedServiceInfo, EntityHeader> {

    /**
     * Find a UDDIProxiedService by published service identifier (GOID)
     *
     * @param publishedServiceGoid The identifier for the service
     * @return The UDDIProxiedService or null
     * @throws FindException if an error occurs
     */
    UDDIProxiedServiceInfo findByPublishedServiceGoid( Goid publishedServiceGoid ) throws FindException;

    /**
     * Find UDDIProxiedServiceInfos with metrics enabled for the given registry.
     *
     * @param registryGoid The registry GOID.
     * @param metricsEnabled The metrics enabled state to match
     * @return The collection of UDDIProxiedServiceInfos (may be emptry but never null)
     * @throws FindException If an error occurs
     */
    Collection<UDDIProxiedServiceInfo> findByUDDIRegistryAndMetricsState( Goid registryGoid,
                                                                          boolean metricsEnabled ) throws FindException;

    /**
     * Get all UDDIProxiedServicInfo entities for a given UDDI Registry
     * @param registryGoid Goid goid of the UDDI Registry
     * @return Collection UDDIProxiedServiceInfo of all information published as proxy info to the registry
     * @throws FindException if problem searching the db
     */
    Collection<UDDIProxiedServiceInfo> findByUDDIRegistryOid( Goid registryGoid) throws FindException;
}
