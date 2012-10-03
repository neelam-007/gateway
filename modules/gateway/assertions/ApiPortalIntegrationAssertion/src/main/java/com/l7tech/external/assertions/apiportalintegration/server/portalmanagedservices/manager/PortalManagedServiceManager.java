package com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager;

import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import java.util.List;

/**
 * Manager responsible for PortalManagedService CRUD operations.
 */
public interface PortalManagedServiceManager extends PortalGenericEntityManager<PortalManagedService> {

    /**
     * Adds a PortalManagedService if it does not already exist, otherwise updates the PortalManagedService.
     *
     * @param portalManagedService the PortalManagedService to add or update.
     * @throws FindException   if updating but unable to find the PortalManagedService.
     * @throws UpdateException if updating but unable to update the PortalManagedService.
     * @throws SaveException   if adding but unable to save the PortalManagedService.
     */
    PortalManagedService addOrUpdate(final PortalManagedService portalManagedService) throws FindException, UpdateException, SaveException;

    /**
     * Finds all PortalManagedServices by looking at policy.
     *
     * @return a list of PortalManagedService found from policy. Can be empty but never null.
     * @throws FindException if unable to retrieve policy.
     */
    List<PortalManagedService> findAllFromPolicy() throws FindException;

    /**
     * Creates a PortalManagedService given a PublishedService oid.
     *
     * @param publishedService the PublishedService from which to create a PortalManagedService.
     * @return the created PortalManagedService or null if the given PublishedService is not Portal Managed.
     * @throws FindException if an error occurs trying to determine if the given PublishedService is Portal Managed.
     */
    PortalManagedService fromService(final PublishedService publishedService) throws FindException;
}
