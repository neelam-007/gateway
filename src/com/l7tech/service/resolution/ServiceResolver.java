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
    public synchronized void setServices( Set services ) {
        _services = services;
        doSetServices(services);
    }

    public synchronized void serviceDeleted( PublishedService service ) {
        _services.remove( service );
        doSetServices( _services );
    }

    public synchronized void serviceUpdated( PublishedService service ) {
        doSetServices( _services );
    }

    public synchronized void serviceCreated( PublishedService service ) {
        _services.add( service );
        doSetServices( _services );
    }


    public synchronized Set resolve( Request request, Set set ) throws ServiceResolutionException {
        if ( set == null ) return Collections.EMPTY_SET;
        if ( set.isEmpty() ) return set;
        return doResolve( request, set );
    }

    protected abstract void doSetServices( Set services );
    protected abstract Set doResolve( Request request, Set set ) throws ServiceResolutionException;
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

    protected Set _services;
}
