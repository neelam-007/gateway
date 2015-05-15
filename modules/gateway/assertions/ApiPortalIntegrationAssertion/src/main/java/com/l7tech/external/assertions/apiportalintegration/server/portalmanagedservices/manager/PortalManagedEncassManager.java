package com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager;

import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedEncass;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;

import java.util.List;

/**
 * Manager responsible for PortalManagedEncass CRUD operations.
 *
 * @author Victor Kazakov
 */
public interface PortalManagedEncassManager extends PortalGenericEntityManager<PortalManagedEncass> {
    /**
     * Adds a PortalManagedEncass if it does not already exist, otherwise updates the PortalManagedEncass.
     *
     * @param portalManagedEncass the PortalManagedEncass to add or update.
     * @throws FindException   if updating but unable to find the PortalManagedEncass.
     * @throws UpdateException if updating but unable to update the PortalManagedEncass.
     * @throws SaveException   if adding but unable to save the PortalManagedEncass.
     */
    PortalManagedEncass addOrUpdate(final PortalManagedEncass portalManagedEncass) throws FindException, UpdateException, SaveException;

    /**
     * Finds all PortalManagedEncass by looking at Encapsulated Assertion Configs.
     *
     * @return a list of PortalManagedEncass found from Encapsulated Assertion Configs. Can be empty but never null.
     * @throws FindException if unable to retrieve Encapsulated Assertion Config or associated policy.
     */
    List<PortalManagedEncass> findAllFromEncass() throws FindException;

    /**
     * Creates a PortalManagedEncass given an EncapsulatedAssertionConfig
     *
     * @param encapsulatedAssertionConfig the Policy from which to create a PortalManagedEncass.
     * @return the created PortalManagedEncass or null if the given Policy is not Portal Managed.
     * @throws FindException if an error occurs trying to determine if the given Policy is Portal Managed.
     */
    PortalManagedEncass fromEncass(final EncapsulatedAssertionConfig encapsulatedAssertionConfig) throws FindException;
}
