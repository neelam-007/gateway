/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;
import com.l7tech.service.ServiceListener;
import com.l7tech.service.PublishedService;

import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServiceResolver implements Comparable, ServiceListener {
    public static final int FAST = 0;
    public static final int SLOW = 100;

    /**
     * Sets the <code>Set</code> of services in the system.  Concrete implementations should implement doSetServices and invalidate any caches based on this Set whenever it is called.
     * @param services A Set of all the services in the system.
     */
    public abstract void setServices( Set services );

    public synchronized Set resolve( Request request, Set set ) throws ServiceResolutionException {
        if ( set == null ) return Collections.EMPTY_SET;
        if ( set.isEmpty() ) return set;
        return doResolve( request, set );
    }

    /**
     * Returns a Map<Long,PublishedService> of any services this ServiceResolver knows about that match the specified PublishedService.
     * @param candidateService the service to compare against the services this ServiceResolver's already knows about.
     * @param subset the Map<Long,PublishedService> to search for matches.
     * @return a Map<Long,PublishedService> of matching services, which could be empty but not null.
     */
    public Map matchingServices( PublishedService candidateService, Map subset ) {
        if ( subset == null || subset.isEmpty() ) return Collections.EMPTY_MAP;

        Map result = null;

        PublishedService matchService;
        Long oid;
        for (Iterator i = subset.keySet().iterator(); i.hasNext();) {
            oid = (Long)i.next();
            matchService = (PublishedService)subset.get(oid);
            if ( matchService != null ) {
                if ( candidateService.getOid() != matchService.getOid() ) {
                    if ( matches( candidateService ) ) {
                        // This candidateService matches one of "mine"
                        if ( result == null ) result = new HashMap();
                        result.put( oid, matchService );
                    }
                }
            }
        }

        if ( result == null ) result = Collections.EMPTY_MAP;

        return result;
    }

    protected abstract Set doResolve( Request request, Set set ) throws ServiceResolutionException;
    public abstract int getSpeed();
    abstract boolean matches( PublishedService service );

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
}
