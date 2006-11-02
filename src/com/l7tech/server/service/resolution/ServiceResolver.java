/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.Message;
import com.l7tech.service.PublishedService;

import java.io.IOException;
import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServiceResolver implements Comparable {
    public static final int FAST = 0;
    public static final int SLOW = 100;

    /**
     * Sets the <code>Set</code> of services in the system.  Concrete implementations should implement doSetServices and invalidate any caches based on this Set whenever it is called.
     * @param services A Set of all the services in the system.
     */
    public abstract void setServices(Set<PublishedService> services);
    public abstract void serviceCreated(PublishedService service);
    public abstract void serviceDeleted(PublishedService service);
    public abstract void serviceUpdated(PublishedService service);

    public abstract Set<PublishedService> resolve( Message request, Set<PublishedService> serviceSubset ) throws ServiceResolutionException;

    /**
     * Returns a Map<Long,PublishedService> of any services this ServiceResolver knows about that match the specified PublishedService.
     * @param candidateService the service to compare against the services this ServiceResolver's already knows about.
     * @param subset the Map<Long,PublishedService> to search for matches.
     * @return a Map<Long,PublishedService> of matching services, which could be empty but not null.
     */
    public Map<Long, PublishedService> matchingServices(
            PublishedService candidateService,
            Map<Long, PublishedService> subset )
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
    abstract boolean matches( PublishedService candidateService, PublishedService matchService );

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
    public abstract Set getDistinctParameters(PublishedService candidateService);
}
