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
        _serviceOidToValuesArrayMap = new HashMap();
        PublishedService service;
        Object[] values;
        Map serviceMap;
        Long oid;
        while ( i.hasNext() ) {
            service = (PublishedService)i.next();
            oid = new Long( service.getOid() );
            values = getTargetValues( service );
            _serviceOidToValuesArrayMap.put( oid, values );
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

        _serviceOidToValuesArrayMap.put( oid, values );

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

        _serviceOidToValuesArrayMap.remove( oid );

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

    protected Object[] getTargetValues( PublishedService service ) {
        Long oid = new Long( service.getOid() );
        synchronized( _serviceOidToValuesArrayMap ) {
            Object[] values = (Object[])_serviceOidToValuesArrayMap.get( oid );
            if ( values == null ) {
                values = doGetTargetValues( service );
                _serviceOidToValuesArrayMap.put( oid, values );
            }
            return values;
        }
    }

    protected abstract Object[] doGetTargetValues( PublishedService service );

    protected abstract Object getRequestValue( Request request ) throws ServiceResolutionException;

    protected boolean matches( PublishedService matchService ) {
        // Get the match values for this service
        Object[] matchValues = getTargetValues( matchService );
        Object matchValue;
        Map serviceMap;
        for ( int i = 0; i < matchValues.length; i++ ) {
            // For each matching value...
            matchValue = matchValues[i];
            // Find out which service(s) match this value
            serviceMap = getServiceMap( matchValue );
            // If there are any, this value matches
            Set keys = serviceMap.keySet();
            Long oid;
            for (Iterator it = keys.iterator(); it.hasNext();) {
                oid = (Long)it.next();
                if ( oid.longValue() != matchService.getOid() ) return true;
            }
        }
        return false;
    }

    private synchronized Map getServiceMap( Object value ) {
        Map serviceMap = (Map)_valueToServiceMapMap.get(value);
        if ( serviceMap == null ) {
            serviceMap = new HashMap();
            _valueToServiceMapMap.put( value, serviceMap );
        }
        return serviceMap;
    }

    public Set resolve( Request request, Set serviceSubset ) throws ServiceResolutionException {
        Object value = getRequestValue(request);
        Map serviceMap = getServiceMap( value );

        if ( serviceMap == null || serviceMap.isEmpty() ) return Collections.EMPTY_SET;

        Set resultSet = null;
        Object[] targetValues;

        for (Iterator i = serviceMap.keySet().iterator(); i.hasNext();) {
            Long oid = (Long)i.next();
            PublishedService service = (PublishedService)serviceMap.get(oid);
            if ( serviceSubset.contains( service ) ) {
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
        }

        if ( resultSet == null ) resultSet = Collections.EMPTY_SET;

        return resultSet;
    }

    private Map _valueToServiceMapMap;
    private Map _serviceOidToValuesArrayMap;
}
