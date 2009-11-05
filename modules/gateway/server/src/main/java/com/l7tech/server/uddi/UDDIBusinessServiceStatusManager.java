package com.l7tech.server.uddi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

/**
 *
 */
public interface UDDIBusinessServiceStatusManager extends EntityManager<UDDIBusinessServiceStatus, EntityHeader> {

    /**
     * Find the business services for a published service and registry.
     *
     * @param registryOid The registry OID (required)
     * @param status The metrics status (optional)
     * @returns The collection of matching UDDIBusinessServiceStatus (can be empty but not null)
     * @throws FindException If an error occurs
     */
    Collection<UDDIBusinessServiceStatus> findByRegistryAndMetricsStatus( long registryOid,
                                                                          UDDIBusinessServiceStatus.Status status ) throws FindException;
}
