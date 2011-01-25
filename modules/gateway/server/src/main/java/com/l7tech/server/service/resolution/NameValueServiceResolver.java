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
    public void serviceDeleted( final PublishedService service ) {
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
    public void serviceUpdated( final PublishedService service ) throws ServiceResolutionException {
        _rwlock.writeLock().lock();
        try {
            serviceDeleted(service);
            serviceCreated(service);
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    @Override
    protected void updateServiceValues( final PublishedService service, final List<T> targetValues ) {
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

    protected List<T> getTargetValues( final PublishedService service ) throws ServiceResolutionException {
        if ( service.getOid() == PublishedService.DEFAULT_OID ) {
            // Don't ever cache values for a service with a to-be-determined OID
            return buildTargetValues(service);
        } else {
            Long oid = service.getOid();
            Lock read = _rwlock.readLock();
            read.lock();
            try {
                List<T> values = serviceOidToValueListMap.get(oid);
                if ( values == null ) {
                    values = buildTargetValues(service);
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
    public void populateResolutionParameters( final Message request, final Map<String, Object> parameters ) throws ServiceResolutionException {
        if ( !isApplicableToMessage(request) ) {
            parameters.put( PROP_APPLICABLE, false );
            return; // don't process request value
        }

        final T value = getRequestValue( request );
        parameters.put( PROP_VALUE, value ); // adding a value means this resolver will later be used (even if the value is null).
    }

    @Override
    public Collection<Map<String, Object>> generateResolutionParameters( final PublishedService service,
                                                                         final Collection<Map<String, Object>> parameterCollection ) throws ServiceResolutionException {
        if ( !isApplicableToConflicts() ) {
            final List<Map<String,Object>> resultParameterList = new ArrayList<Map<String,Object>>( parameterCollection.size() );

            for ( final Map<String, Object> parameters : parameterCollection ) {
                final Map<String, Object> resultParameters = new HashMap<String, Object>( parameters );
                resultParameters.put( PROP_APPLICABLE, false );
                resultParameterList.add( resultParameters );
            }

            return resultParameterList;
        } else {
            // use doGetTargetValues since we don't want to use cached values
            final Set<T> values = new HashSet<T>( buildTargetValues( service ) );
            if ( values.isEmpty() ) {
                return parameterCollection;
            }

            final List<Map<String,Object>> resultParameterList = new ArrayList<Map<String,Object>>( Math.max( 10, parameterCollection.size() * values.size() ) );

            for ( final T value : values ) {
                for ( final Map<String, Object> parameters : parameterCollection ) {
                    final Map<String, Object> resultParameters = new HashMap<String, Object>( parameters );
                    resultParameters.put( PROP_VALUE, value );
                    resultParameterList.add( resultParameters );
                }
            }

            return resultParameterList;
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public final Result resolve( final Map<String,Object> parameters, 
                                 final Collection<PublishedService> serviceSubset ) throws ServiceResolutionException {
        final Boolean applicable = (Boolean) parameters.get( PROP_APPLICABLE );
        if ( (applicable!=null && !applicable) || !parameters.containsKey( PROP_VALUE )) return Result.NOT_APPLICABLE;
        T value = (T) parameters.get( PROP_VALUE );
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

    protected boolean isApplicableToConflicts(){ return false; }
    protected abstract boolean isApplicableToMessage(Message msg) throws ServiceResolutionException;
}
