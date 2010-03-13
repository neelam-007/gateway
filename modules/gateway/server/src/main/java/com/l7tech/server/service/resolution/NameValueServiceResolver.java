/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.service.resolution;

import com.l7tech.message.Message;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.audit.Auditor;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author alex
 */
public abstract class NameValueServiceResolver<T> extends ServiceResolver<T> {
    public NameValueServiceResolver( final Auditor.AuditorFactory auditorFactory ) {
        super( auditorFactory );
    }

    @Override
    public void serviceCreated(PublishedService service) throws ServiceResolutionException {
        final List<T> targetValues = getTargetValues(service);
        final Long oid = service.getOid();

        _rwlock.writeLock().lock();
        try {
            serviceOidToValueListMap.put( oid, targetValues );

            for ( T targetValue : targetValues ) {
                Map<Long, PublishedService> serviceMap = getServiceMap(targetValue);
                serviceMap.put(oid, service);
            }
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    @Override
    public void serviceDeleted(PublishedService service) {
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

    @Override
    public void serviceUpdated(PublishedService service) throws ServiceResolutionException {
        _rwlock.writeLock().lock();
        try {
            serviceDeleted(service);
            serviceCreated(service);
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    protected List<T> getTargetValues(PublishedService service) throws ServiceResolutionException {
        if ( service.getOid() == PublishedService.DEFAULT_OID ) {
            // Don't ever cache values for a service with a to-be-determined OID
            return doGetTargetValues(service);
        } else {
            Long oid = service.getOid();
            Lock read = _rwlock.readLock();
            read.lock();
            try {
                List<T> values = serviceOidToValueListMap.get(oid);
                if ( values == null ) {
                    values = doGetTargetValues(service);
                    read.unlock();
                    read = null;
                    _rwlock.writeLock().lock();
                    try {
                        serviceOidToValueListMap.put(oid, values);
                    } finally {
                        _rwlock.writeLock().unlock();
                    }
                }
                return values;
            } finally {
                if (read != null) read.unlock();
            }
        }
    }

    protected abstract List<T> doGetTargetValues(PublishedService service) throws ServiceResolutionException;

    protected abstract T getRequestValue(Message request) throws ServiceResolutionException;

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
                    _valueToServiceMapMap.put(value, serviceMap);
                } finally {
                    _rwlock.writeLock().unlock();
                }
            } 
            return serviceMap;
        } finally {
            if (read != null) read.unlock();
        }
    }

    @Override
    public final Result resolve(Message request, Collection<PublishedService> serviceSubset) throws ServiceResolutionException {
        if (!isApplicableToMessage(request)) return Result.NOT_APPLICABLE;
        T value = getRequestValue(request);
        return resolve(value, serviceSubset);
    }

    protected Result resolve( final T value, final Collection serviceSubset ) throws ServiceResolutionException {
        final Map<Long, PublishedService> serviceMap = getServiceMap(value);

        if ( serviceMap == null || serviceMap.isEmpty() ) return Result.NO_MATCH;

        final Set<PublishedService> resultSet = new HashSet<PublishedService>();

        for ( final PublishedService service : serviceMap.values()) {
            if ( serviceSubset.contains(service) ) {
                final List<T> targetValues = getTargetValues(service);
                for ( T targetValue : targetValues ) {
                    if ( (targetValue != null && targetValue.equals(value)) ||
                         (targetValue == null && value==null) ) {
                        resultSet.add(service);
                    }
                }
            }
        }

        return new Result(resultSet);
    }

    protected final Map<T, Map<Long, PublishedService>> _valueToServiceMapMap = new HashMap<T, Map<Long, PublishedService>>();
    protected final Map<Long, List<T>> serviceOidToValueListMap = new HashMap<Long, List<T>>();
    protected final ReadWriteLock _rwlock = new ReentrantReadWriteLock(false);

    public abstract boolean isApplicableToMessage(Message msg) throws ServiceResolutionException;
}
