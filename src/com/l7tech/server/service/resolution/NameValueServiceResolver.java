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
public abstract class NameValueServiceResolver<T> extends ServiceResolver<T> {

    public void serviceCreated( PublishedService service ) throws ServiceResolutionException {
        List<T> targetValues = getTargetValues( service );
        T value;
        Map<Long, PublishedService> serviceMap;
        Long oid = service.getOid();

        _rwlock.writeLock().lock();
        try {
            serviceOidToValueListMap.put( oid, targetValues );

            for (T targetValue : targetValues) {
                value = targetValue;
                serviceMap = getServiceMap(value);
                serviceMap.put(oid, service);
            }
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    public void serviceDeleted( PublishedService service ) {
        Long oid = service.getOid();

        _rwlock.writeLock().lock();
        try {
            serviceOidToValueListMap.remove( oid );

            for (Map serviceMap : _valueToServiceMapMap.values()) {
                serviceMap.remove(oid);
            }
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    public void serviceUpdated( PublishedService service ) throws ServiceResolutionException {
        _rwlock.writeLock().lock();
        try {

            serviceDeleted( service );
            serviceCreated( service );

        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    protected List<T> getTargetValues( PublishedService service ) throws ServiceResolutionException {
        if ( service.getOid() == PublishedService.DEFAULT_OID ) {
            // Don't ever cache values for a service with a to-be-determined OID
            return doGetTargetValues( service );
        } else {
            Long oid = service.getOid();
            Lock read = _rwlock.readLock();
            read.lock();
            try {
                List<T> values = serviceOidToValueListMap.get( oid );
                if ( values == null ) {
                    values = doGetTargetValues( service );
                    read.unlock();
                    read = null;
                    _rwlock.writeLock().lock();
                    try {
                        serviceOidToValueListMap.put( oid, values );
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

    protected abstract List<T> doGetTargetValues( PublishedService service ) throws ServiceResolutionException;

    protected abstract T getRequestValue( Message request ) throws ServiceResolutionException;

    protected boolean matches( PublishedService candidateService, PublishedService matchService ) throws ServiceResolutionException {
        // Get the match values for this service
        Set<Object> candidateValues = new HashSet<Object>( Arrays.asList( getTargetValues( candidateService ) ) );
        Set<Object> matchValues = new HashSet<Object>( Arrays.asList( getTargetValues( matchService ) ) );

        for (Object value : candidateValues) {
            if (matchValues.contains(value)) return true;
        }

        return false;
    }

    protected Map<Long, PublishedService> getServiceMap(T value) {
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
        T value = getRequestValue(request);
        return resolve(value, serviceSubset);
    }

    Set<PublishedService> resolve(T value, Set serviceSubset) throws ServiceResolutionException {
        /*if (value instanceof String) {
            String s = (String)value;
            if (s.length() > getMaxLength()) {
                s = s.substring(0,getMaxLength());
                value = s;
            }
        }*/
        Map<Long, PublishedService> serviceMap = getServiceMap(value);

        if ( serviceMap == null || serviceMap.isEmpty() ) return Collections.emptySet();

        Set<PublishedService> resultSet = null;
        List<T> targetValues;

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

    protected final Map<T, Map<Long, PublishedService>> _valueToServiceMapMap = new HashMap<T, Map<Long, PublishedService>>();
    protected final Map<Long, List<T>> serviceOidToValueListMap = new HashMap<Long, List<T>>();
    protected final ReadWriteLock _rwlock = new ReentrantReadWriteLock(false);
}
