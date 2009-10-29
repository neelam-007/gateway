package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

/**
 * EntityManager for UDDIServiceControl
 */
public interface UDDIServiceControlManager extends EntityManager<UDDIServiceControl,EntityHeader> {

    /**
     * Find a UDDIServiceControl by published service oid.
     *
     * @param serviceOid The service oid
     * @return The UDDIServiceControl or null
     * @throws FindException If an error occurs.
     */
    UDDIServiceControl findByPublishedServiceOid( long serviceOid ) throws FindException;

    /**
     * Find UDDIServiceControls by UDDI registry oid.
     *
     * @param registryOid The UDDI registry oid
     * @return The collection of UDDIServiceControls (may be empty but not null)
     * @throws FindException If an error occurs.
     */
    Collection<UDDIServiceControl> findByUDDIRegistryOid( long registryOid ) throws FindException;
}
