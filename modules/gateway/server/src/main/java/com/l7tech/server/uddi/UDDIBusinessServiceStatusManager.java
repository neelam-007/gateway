package com.l7tech.server.uddi;

import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 *
 */
public interface UDDIBusinessServiceStatusManager extends EntityManager<UDDIBusinessServiceStatus, EntityHeader> {

    /**
     * Find the business services for a registry with metrics status.
     *
     * @param registryGoid The registry GOID (required)
     * @param status The metrics status (optional)
     * @return The collection of matching UDDIBusinessServiceStatus (can be empty but not null)
     * @throws FindException If an error occurs
     */
    Collection<UDDIBusinessServiceStatus> findByRegistryAndMetricsStatus( Goid registryGoid,
                                                                          UDDIBusinessServiceStatus.Status status ) throws FindException;

    /**
     * Find the business services for a registry with ws-policy attachment status.
     *
     * @param registryGoid The registry GOID (required)
     * @param status The ws-policy publishing status (optional)
     * @return The collection of matching UDDIBusinessServiceStatus (can be empty but not null)
     * @throws FindException If an error occurs
     */
    Collection<UDDIBusinessServiceStatus> findByRegistryAndWsPolicyPublishStatus( Goid registryGoid,
                                                                                  UDDIBusinessServiceStatus.Status status ) throws FindException;

    /**
     * Find the business services for a published service
     *
     * @param publishedServiceGoid The published service GOID (required)
     * @return The collection of matching UDDIBusinessServiceStatus (can be empty but not null)
     * @throws FindException If an error occurs
     */
    Collection<UDDIBusinessServiceStatus> findByPublishedService( Goid publishedServiceGoid ) throws FindException;

}
