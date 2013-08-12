/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.FolderedEntityManager;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.PublishedService;

import java.util.Collection;

/**
 * Service API. Get instance of this through the Locator class.
 */
public interface ServiceManager extends PropertySearchableEntityManager<ServiceHeader>, FolderedEntityManager<PublishedService, ServiceHeader>, RoleAwareGoidEntityManager<PublishedService> {
    /**
     * Get what the server sees at that url.
     * Meant to be used by admin console entity.
     *
     * @param url the url target to inspect.
     * @return the payload returned at that url (hopefully)
     */
    String resolveWsdlTarget(String url);

    /**
     * Creates a new role for the specified PublishedService.
     *
     * @param service      the PublishedService that is in need of a Role.  Must not be null.
     * @throws com.l7tech.objectmodel.SaveException  if the new Role could not be saved
     */
    public void addManageServiceRole(PublishedService service) throws SaveException;

    /**
     * Overloads findAllHeaders in ReadOnlyEntityManager to allow a caller to explicitly include
     * aliases in the results
     * @param includeAliases true if the returned Collection should contain aliases or not
     * @return Collection<PublishedService> if true was specified for includeAliases then the Collection
     * returned will contain aliases if any exist. To determine call isAlias on each ServiceHeader
     * @throws FindException if there is a problem accessing the information.
     */
    public Collection<ServiceHeader> findAllHeaders(boolean includeAliases) throws FindException;

    /**
     * Finds all published services that use the specified routing URI.
     *
     * @param routingUri the routing URI to search for.  Required.
     * @return all published services that use the specified routing URI.  May be empty, but never null.
     * @throws FindException if there is a problem accessing the information.
     */
    public Collection<PublishedService> findByRoutingUri(String routingUri) throws FindException;

}