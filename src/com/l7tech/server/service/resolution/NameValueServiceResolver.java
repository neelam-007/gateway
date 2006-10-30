/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.Message;
import com.l7tech.service.PublishedService;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.Lock;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class NameValueServiceResolver extends ServiceResolver {
    public void setServices( Set services ) {
        _rwlock.writeLock().lock();
        try {
            Iterator i = services.iterator();

            _valueToServiceMapMap.clear();
            _serviceOidToValuesArrayMap.clear();
            PublishedService service;
            Object[] values;
            Map<Long, PublishedService> serviceMap;
            Long oid;
            while ( i.hasNext() ) {
                service = (PublishedService)i.next();
                oid = service.getOid();
                values = getTargetValues( service );
                _serviceOidToValuesArrayMap.put( oid, values );
                for (Object value : values) {
                    serviceMap = getServiceMap(value);
                    serviceMap.put(oid, service);
                }
            }
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    public void serviceCreated( PublishedService service ) {
        Object[] targetValues = getTargetValues( service );
        Object value;
        Map<Long, PublishedService> serviceMap;
        Long oid = service.getOid();

        _rwlock.writeLock().lock();
        try {
            _serviceOidToValuesArrayMap.put( oid, targetValues );

            for (Object targetValue : targetValues) {
                value = targetValue;
                serviceMap = getServiceMap(value);
                serviceMap.put(oid, service);
            }
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    public void serviceDeleted( PublishedService service ) {
        Object[] targetValues = getTargetValues( service );
        Object value;
        Map serviceMap;
        Long oid = service.getOid();

        _rwlock.writeLock().lock();
        try {
            _serviceOidToValuesArrayMap.remove( oid );

            for (Object targetValue : targetValues) {
                value = targetValue;
                serviceMap = getServiceMap(value);
                serviceMap.remove(oid);
            }
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    public void serviceUpdated( PublishedService service ) {
        _rwlock.writeLock().lock();
        try {

            serviceDeleted( service );
            serviceCreated( service );

        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    protected Object[] getTargetValues( PublishedService service ) {
        if ( service.getOid() == PublishedService.DEFAULT_OID ) {
            // Don't ever cache values for a service with a to-be-determined OID
            return doGetTargetValues( service );
        } else {
            Long oid = service.getOid();
            Lock read = _rwlock.readLock();
            read.lock();
            try {
                Object[] values = _serviceOidToValuesArrayMap.get( oid );
                if ( values == null ) {
                    values = doGetTargetValues( service );
                    read.unlock();
                    read = null;
                    _rwlock.writeLock().lock();
                    try {
                        _serviceOidToValuesArrayMap.put( oid, values );
                    } finally {
                        _rwlock.writeLock().unlock();
                    }
                } else {
                    read.unlock();
                    read = null;
                }
                return values;
            } finally {
                if (read != null) read.unlock();
            }
        }
    }

    protected abstract Object[] doGetTargetValues( PublishedService service );

    protected abstract Object getRequestValue( Message request ) throws ServiceResolutionException;

    protected boolean matches( PublishedService candidateService, PublishedService matchService ) {
        // Get the match values for this service
        Set<Object> candidateValues = new HashSet<Object>( Arrays.asList( getTargetValues( candidateService ) ) );
        Set<Object> matchValues = new HashSet<Object>( Arrays.asList( getTargetValues( matchService ) ) );

        for (Object value : candidateValues) {
            if (matchValues.contains(value)) return true;
        }

        return false;
    }

    protected Map<Long, PublishedService> getServiceMap( Object value ) {
        Lock read = _rwlock.readLock();
        read.lock();
        try {
            Map<Long, PublishedService> serviceMap = _valueToServiceMapMap.get(value);
            if ( serviceMap == null ) {
                serviceMap = new HashMap<Long, PublishedService>();
                read.unlock();
                read = null;
                _rwlock.writeLock().lock();
                try {
                    _valueToServiceMapMap.put( value, serviceMap );
                } finally {
                    _rwlock.writeLock().unlock();
                }
            } else {
                read.unlock();
                read = null;
            }
            return serviceMap;
        } finally {
            if (read != null) read.unlock();
        }
    }

    public Set<PublishedService> resolve( Message request, Set<PublishedService> serviceSubset ) throws ServiceResolutionException {
        Object value = getRequestValue(request);
        return resolve(value, serviceSubset);
    }

    Set<PublishedService> resolve(Object value, Set serviceSubset) throws ServiceResolutionException {
        /*if (value instanceof String) {
            String s = (String)value;
            if (s.length() > getMaxLength()) {
                s = s.substring(0,getMaxLength());
                value = s;
            }
        }*/
        Map<Long, PublishedService> serviceMap = getServiceMap( value );

        if ( serviceMap == null || serviceMap.isEmpty() ) return Collections.emptySet();

        Set<PublishedService> resultSet = null;
        Object[] targetValues;

        for (Long oid : serviceMap.keySet()) {
            PublishedService service = serviceMap.get(oid);
            if (serviceSubset.contains(service)) {
                targetValues = getTargetValues(service);
                for (Object targetValue : targetValues) {
                    if (targetValue != null && targetValue.equals(value)) {
                        if (resultSet == null) resultSet = new HashSet<PublishedService>();
                        resultSet.add(service);
                    }
                }
            }
        }

        if ( resultSet == null ) resultSet = Collections.emptySet();

        return resultSet;
    }

    private final Map<Object, Map<Long, PublishedService>> _valueToServiceMapMap = new HashMap<Object, Map<Long, PublishedService>>();
    private final Map<Long, Object[]> _serviceOidToValuesArrayMap = new HashMap<Long, Object[]>();
    private final ReadWriteLock _rwlock = new ReentrantReadWriteLock(false);
}
