/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Manager for UDDIRegistries
 *
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

import java.util.Collection;

public interface UDDIRegistryManager extends EntityManager<UDDIRegistry, EntityHeader> {

    /**
     * Get all the UDDIProxiedService which belong to this UDDIRegistry
     *
     * @param registryGoid Goid goid of owning UDDIRegistry
     * @return Collection of UDDIProxiedServices. Never null
     * @throws FindException if any problem finding entities
     */
    Collection<UDDIProxiedServiceInfo> findAllByRegistryGoid(Goid registryGoid) throws FindException;

}
