/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import com.l7tech.common.message.Message;
import com.l7tech.service.PublishedService;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class NameValueServiceResolver extends ServiceResolver {
    public void setServices( Set services ) {
        Sync write = _rwlock.writeLock();
        try {
            Iterator i = services.iterator();

            write.acquire();
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
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring write!" );
            Thread.currentThread().interrupt();
        } finally {
            write.release();
        }
    }

    public void serviceCreated( PublishedService service ) {
        Object[] values = getTargetValues( service );
        Object value;
        Map serviceMap;
        Long oid = new Long( service.getOid() );

        Sync write = _rwlock.writeLock();
        try {
            write.acquire();
            _serviceOidToValuesArrayMap.put( oid, values );

            for (int i = 0; i < values.length; i++) {
                value = values[i];
                serviceMap = getServiceMap( value );
                serviceMap.put( oid, service );
            }
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring write lock!" );
            Thread.currentThread().interrupt();
        } finally {
            write.release();
        }
    }

    public void serviceDeleted( PublishedService service ) {
        Object[] values = getTargetValues( service );
        Object value;
        Map serviceMap;
        Long oid = new Long( service.getOid() );

        Sync write = _rwlock.writeLock();
        try {
            write.acquire();
            _serviceOidToValuesArrayMap.remove( oid );

            for (int i = 0; i < values.length; i++) {
                value = values[i];
                serviceMap = getServiceMap( value );
                serviceMap.remove( oid );
            }
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring write lock!" );
            Thread.currentThread().interrupt();
        } finally {
            write.release();
        }
    }

    public void serviceUpdated( PublishedService service ) {
        Sync write = _rwlock.writeLock();
        try {
            write.acquire();

            serviceDeleted( service );
            serviceCreated( service );

        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring write lock!" );
            Thread.currentThread().interrupt();
        } finally {
            write.release();
        }
    }

    protected Object[] getTargetValues( PublishedService service ) {
        if ( service.getOid() == PublishedService.DEFAULT_OID ) {
            // Don't ever cache values for a service with a to-be-determined OID
            return doGetTargetValues( service );
        } else {
            Long oid = new Long( service.getOid() );
            Sync read = _rwlock.readLock();
            Sync write = _rwlock.writeLock();
            try {
                read.acquire();
                Object[] values = (Object[])_serviceOidToValuesArrayMap.get( oid );
                if ( values == null ) {
                    values = doGetTargetValues( service );
                    read.release();
                    write.acquire();
                    _serviceOidToValuesArrayMap.put( oid, values );
                    write.release();
                } else {
                    read.release();
                }
                return values;
            } catch ( InterruptedException ie ) {
                logger.fine( "Interrupted acquiring read/write lock!" );
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    protected abstract Object[] doGetTargetValues( PublishedService service );

    protected abstract Object getRequestValue( Message request ) throws ServiceResolutionException;

    protected boolean matches( PublishedService candidateService, PublishedService matchService ) {
        // Get the match values for this service
        Set candidateValues = new HashSet( Arrays.asList( getTargetValues( candidateService ) ) );
        Set matchValues = new HashSet( Arrays.asList( getTargetValues( matchService ) ) );

        for (Iterator i = candidateValues.iterator(); i.hasNext();) {
            Object value = i.next();
            if ( matchValues.contains( value ) ) return true;
        }

        return false;
    }

    private Map getServiceMap( Object value ) {
        Sync read = _rwlock.readLock();
        Sync write = _rwlock.writeLock();
        try {
            read.acquire();
            Map serviceMap = (Map)_valueToServiceMapMap.get(value);
            if ( serviceMap == null ) {
                serviceMap = new HashMap();
                read.release();
                write.acquire();
                _valueToServiceMapMap.put( value, serviceMap );
                write.release();
            } else {
                read.release();
            }
            return serviceMap;
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring read lock!" );
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public Set resolve( Message request, Set serviceSubset ) throws ServiceResolutionException {
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

    private Map _valueToServiceMapMap = new HashMap();
    private Map _serviceOidToValuesArrayMap = new HashMap();
    private ReadWriteLock _rwlock = new ReentrantWriterPreferenceReadWriteLock();
    private final Logger logger = Logger.getLogger(getClass().getName());
}
