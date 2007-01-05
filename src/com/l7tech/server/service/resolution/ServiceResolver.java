/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.Message;
import com.l7tech.service.PublishedService;

import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServiceResolver implements Comparable {
    public static final int FAST = 0;
    public static final int SLOW = 100;

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

    public abstract Set<PublishedService> resolve( Message request, Set<PublishedService> serviceSubset ) throws ServiceResolutionException;

    /**
     * Returns a Map<Long,PublishedService> of any services this ServiceResolver knows about that match the specified PublishedService.
     * @param candidateService the service to compare against the services this ServiceResolver's already knows about.
     * @param subset the Map<Long,PublishedService> to search for matches.
     * @return a Map<Long,PublishedService> of matching services, which could be empty but not null.
     * @throws ServiceResolutionException May be thrown if resolution cannot be performed one of the given services
     */
    public Map<Long, PublishedService> matchingServices(
            PublishedService candidateService,
            Map<Long, PublishedService> subset )  throws ServiceResolutionException
    {
        if ( subset == null || subset.isEmpty() ) return Collections.emptyMap();

        Map<Long, PublishedService> result = null;

        PublishedService matchService;
        for (Long oid : subset.keySet()) {
            matchService = subset.get(oid);
            if (matchService != null) {
                if (candidateService.getOid() != matchService.getOid()) {
                    if (matches(candidateService, matchService)) {
                        // This candidateService matches one of "mine"
                        if (result == null) result = new HashMap<Long, PublishedService>();
                        result.put(oid, matchService);
                    }
                }
            }
        }

        if ( result == null ) result = Collections.emptyMap();

        return result;
    }

    public abstract int getSpeed();
    abstract boolean matches( PublishedService candidateService, PublishedService matchService ) throws ServiceResolutionException;

    /**
     * Could throw a ClassCastException.
     */
    public int compareTo( Object obj ) {
        ServiceResolver other = (ServiceResolver)obj;
        int mySpeed = getSpeed();
        int otherSpeed = other.getSpeed();
        if ( mySpeed > otherSpeed )
            return 1;
        else if ( mySpeed == otherSpeed )
            return 0;
        else
            return -1;
    }

    /**
     * a set of distinct parameters for this service
     * @param candidateService object from which to extract parameters from
     * @return a Set containing distinct strings
     */
    public abstract Set getDistinctParameters(PublishedService candidateService) throws ServiceResolutionException ;
}
