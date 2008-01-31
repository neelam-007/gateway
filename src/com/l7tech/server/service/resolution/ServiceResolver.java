/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.server.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;

import java.util.Set;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * @param <T> the type of value employed by this resolver
 */
public abstract class ServiceResolver<T> {
    protected final Logger logger = Logger.getLogger(getClass().getName()); // Not static so we get the real classname
    protected final Auditor auditor;

    public ServiceResolver(ApplicationContext spring) {
        auditor = new Auditor(this, spring, logger);
    }

    /**
     * Notify resolver of a new service.
     *
     * @param service The published service that is now available for resolution.
     * @throws ServiceResolutionException Can be thrown if resolution cannot be performed and this should be audited.
     */
    public abstract void serviceCreated(PublishedService service) throws ServiceResolutionException;

    /**
     * Notify resolver of an updated service.
     *
     * @param service The published service whose resolution parameters may have changed.
     * @throws ServiceResolutionException Can be thrown if resolution cannot be performed and this should be audited.
     */
    public abstract void serviceUpdated(PublishedService service) throws ServiceResolutionException;

    /**
     * Notify resolver of a deleted service.
     *
     * @param service The published service that is no longer available for resolution.
     */
    public abstract void serviceDeleted(PublishedService service);

    /**
     * Tries to resolve the request to a published service.
     *
     * <b><code>serviceSubset</code> was returned from <code>Collections.unmodifiableCollection()</code>, so it should
     * not be iterated over and cannot be modified.</b>
     *
     * @param request The request to process
     * @param serviceSubset The collection of published services
     * @return The resolution result
     * @throws ServiceResolutionException
     */
    public abstract Result resolve(Message request, Collection<PublishedService> serviceSubset) throws ServiceResolutionException;

    public abstract boolean usesMessageContent();

    /**
     * a set of distinct parameters for this service
     * @param candidateService object from which to extract parameters from
     * @return a Set containing distinct values
     */
    public abstract Set<T> getDistinctParameters(PublishedService candidateService) throws ServiceResolutionException ;

    public abstract boolean isSoap();
}
