/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SearchableEntityManager;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.PublishedService;

import java.util.Collection;

/**
 * Service API. Get instance of this through the Locator class.
 */
public interface ServiceManager extends SearchableEntityManager<PublishedService, ServiceHeader>, EntityManager<PublishedService, ServiceHeader> {
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
     */
    public Collection<ServiceHeader> findAllHeaders(boolean includeAliases) throws FindException;

}