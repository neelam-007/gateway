/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;
import com.l7tech.service.PublishedService;

import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class NameValueServiceResolver extends ServiceResolver {
    public synchronized void setServices( Set services ) {
        Iterator i = services.iterator();
        _valueToServiceMapMap = new HashMap();
        PublishedService service;
        Object[] values;
        Map serviceMap;
        Long oid;
        while ( i.hasNext() ) {
            service = (PublishedService)i.next();
            oid = new Long( service.getOid() );
            values = getTargetValues( service );
            for (int j = 0; j < values.length; j++) {
                Object value = values[j];
                serviceMap = getServiceMap(value);
                serviceMap.put( oid, service );
            }
        }
    }

    public synchronized void serviceCreated( PublishedService service ) {
        Object[] values = getTargetValues( service );
        Object value;
        Map serviceMap;
        Long oid = new Long( service.getOid() );
        for (int i = 0; i < values.length; i++) {
            value = values[i];
            serviceMap = getServiceMap( value );
            serviceMap.put( oid, service );
        }
    }

    public synchronized void serviceDeleted( PublishedService service ) {
        Object[] values = getTargetValues( service );
        Object value;
        Map serviceMap;
        Long oid = new Long( service.getOid() );
        for (int i = 0; i < values.length; i++) {
            value = values[i];
            serviceMap = getServiceMap( value );
            serviceMap.remove( oid );
        }
    }

    public synchronized void serviceUpdated( PublishedService service ) {
        serviceDeleted( service );
        serviceCreated( service );
    }

    protected abstract String getParameterName();
    protected abstract Object[] getTargetValues( PublishedService service );
    protected abstract Object getRequestValue( Request request ) throws ServiceResolutionException;

    protected synchronized Map getServiceMap( Object value ) {
        Map serviceMap = (Map)_valueToServiceMapMap.get(value);
        if ( serviceMap == null ) {
            serviceMap = new HashMap();
            _valueToServiceMapMap.put( value, serviceMap );
        }
        return serviceMap;
    }

    protected synchronized Set doResolve( Request request, Set set ) throws ServiceResolutionException {
        Object value = getRequestValue(request);
        Map serviceMap = getServiceMap( value );
        if ( serviceMap.isEmpty() ) return Collections.EMPTY_SET;

        Iterator i = serviceMap.keySet().iterator();
        PublishedService service;
        Set resultSet = null;
        Object[] targetValues;
        Long oid;

        while ( i.hasNext() ) {
            oid = (Long)i.next();
            service = (PublishedService)serviceMap.get( oid );
            targetValues = getTargetValues(service);
            Object targetValue;
            for ( int j = 0; j < targetValues.length; j++ ) {
                targetValue = targetValues[j];
                if ( targetValue != null && targetValue.equals(value) ) {
                    if ( resultSet == null ) resultSet = new HashSet();
                    resultSet.add(service);
                }
            }
        }

        if ( resultSet == null ) resultSet = Collections.EMPTY_SET;

        return resultSet;
    }

    protected Map _valueToServiceMapMap;
}
