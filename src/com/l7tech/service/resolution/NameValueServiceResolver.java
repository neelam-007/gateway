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
    public synchronized void doSetServices( Collection services ) {
        Iterator i = services.iterator();
        _valueToServiceSetMap = new HashMap();
        PublishedService service;
        Object[] values;
        Set serviceSet;
        while ( i.hasNext() ) {
            service = (PublishedService)i.next();
            values = getTargetValues( service );
            for (int j = 0; j < values.length; j++) {
                Object value = values[j];
                serviceSet = getServiceSet(value);
                serviceSet.add( service );
            }
        }
    }

    protected abstract String getParameterName();
    protected abstract Object[] getTargetValues( PublishedService service );
    protected abstract Object getRequestValue( Request request ) throws ServiceResolutionException;

    protected synchronized Set getServiceSet( Object value ) {
        Set serviceSet = (Set)_valueToServiceSetMap.get(value);
        if ( serviceSet == null ) {
            serviceSet = new HashSet();
            _valueToServiceSetMap.put( value, serviceSet );
        }
        return serviceSet;
    }

    protected synchronized Set doResolve( Request request, Set set ) throws ServiceResolutionException {
        Object value = getRequestValue(request);
        Set serviceSet = getServiceSet( value );
        if ( serviceSet.isEmpty() ) return serviceSet;

        Iterator i = serviceSet.iterator();
        PublishedService service;
        Set resultSet = null;
        Object[] targetValues;

        while ( i.hasNext() ) {
            service = (PublishedService)i.next();
            targetValues = getTargetValues(service);
            Object targetValue;
            for ( int j = 0; j < targetValues.length; j++ ) {
                targetValue = targetValues[j];
                if ( targetValue.equals(value) ) {
                    if ( resultSet == null ) resultSet = new HashSet();
                    resultSet.add(service);
                }
            }
        }

        if ( resultSet == null ) resultSet = Collections.EMPTY_SET;

        return resultSet;
    }

    protected Map _valueToServiceSetMap;
}
