/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;

import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServiceResolver implements Comparable {
    public static final int FAST = 0;
    public static final int SLOW = 100;

    /**
     * Sets the <code>Collection</code> of services in the system.  Concrete implementations should override this method and invalidate any caches based on this Collection whenever this method is called.
     * @param services A Collection of all the services in the system.
     */
    public synchronized void setServices( Collection services ) {
        _services = services;
    }

    public synchronized Set resolve( Request request, Set set ) throws ServiceResolutionException {
        if ( set == null ) return Collections.EMPTY_SET;
        if ( set.isEmpty() ) return set;
        return doResolve( request, set );
    }

    abstract Set doResolve( Request request, Set set ) throws ServiceResolutionException;
    public abstract int getSpeed();

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

    protected Collection _services;
}
