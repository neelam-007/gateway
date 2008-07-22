/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.PublishedService;

/**
 * Service API. Get instance of this through the Locator class.
 */
public interface ServiceManager extends EntityManager<PublishedService, ServiceHeader> {
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
}