package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.message.Message;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.util.GoidUpgradeMapper;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author alex
 */
public abstract class NameValueServiceResolver<T> extends ServiceResolver<T> {

    public NameValueServiceResolver( final AuditFactory auditorFactory ) {
        super( auditorFactory );
    }

    @Override
    public void serviceDeleted( final PublishedService service ) {
        Goid goid = service.getGoid();

        _rwlock.writeLock().lock();
        try {
            serviceIdToValueListMap.remove(goid);
            //This is needed to handle the services being referenced by their old oid's
            if(GoidUpgradeMapper.prefixMatches(EntityType.SERVICE, goid.getHi()))
                serviceIdToValueListMap.remove(Long.valueOf(goid.getLow()));

            for (Map serviceMap : _valueToServiceMapMap.values()) {
                serviceMap.remove(goid);
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
        final Goid goid = service.getGoid();

        _rwlock.writeLock().lock();
        try {
            serviceIdToValueListMap.put(goid, targetValues);
            //This is needed to handle the services being referenced by their old oid's
            if(GoidUpgradeMapper.prefixMatches(EntityType.SERVICE, goid.getHi()))
                serviceIdToValueListMap.put( Long.valueOf(goid.getLow()), targetValues );

            for ( T targetValue : targetValues ) {
                Map<Goid, PublishedService> serviceMap = getServiceMap(targetValue);
                serviceMap.put(goid, service);
            }
        } finally {
            _rwlock.writeLock().unlock();
        }
    }

    protected List<T> getTargetValues( final PublishedService service ) throws ServiceResolutionException {
        if ( Goid.isDefault(service.getGoid()) ) {
            // Don't ever cache values for a service with a to-be-determined OID
            return buildTargetValues(service);
        } else {
            Goid goid = service.getGoid();
            Lock read = _rwlock.readLock();
            read.lock();
            try {
                List<T> values = serviceIdToValueListMap.get(goid);
                if ( values == null ) {
                    values = buildTargetValues(service);
                    read.unlock();
                    read = null;
                    _rwlock.writeLock().lock();
                    try {
                        serviceIdToValueListMap.put(goid, values);
                        //This is needed to handle the services being referenced by their old oid's
                        if(GoidUpgradeMapper.prefixMatches(EntityType.SERVICE, goid.getHi()))
                            serviceIdToValueListMap.put(Long.valueOf(goid.getLow()), values);
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

    protected Map<Goid, PublishedService> getServiceMap(T value) {
        Lock read = _rwlock.readLock();
        read.lock();
        try {
            Map<Goid, PublishedService> serviceMap = _valueToServiceMapMap.get(value);
            if ( serviceMap == null ) {
                serviceMap = new HashMap<Goid, PublishedService>();
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
        if ( isApplicableToMessage(request) ) {
            final T value = getRequestValue( request );
            parameters.put( PROP_VALUE, value );
        }
    }

    @Override
    public Collection<Map<String, Object>> generateResolutionParameters( final PublishedService service,
                                                                         final Collection<Map<String, Object>> parameterCollection ) throws ServiceResolutionException {
        if ( !isApplicableToConflicts() ) {
            return parameterCollection;
        } else {
            return super.generateResolutionParameters( service, parameterCollection );
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public final Result resolve( final Map<String,Object> parameters, 
                                 final Collection<PublishedService> serviceSubset ) throws ServiceResolutionException {
        if (!parameters.containsKey( PROP_VALUE )) return Result.NOT_APPLICABLE;
        T value = (T) parameters.get( PROP_VALUE );
        return resolve(value, serviceSubset);
    }

    protected Result resolve( final T value, final Collection serviceSubset ) throws ServiceResolutionException {
        final Map<Goid, PublishedService> serviceMap = getServiceMap(value);

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

    protected final Map<T, Map<Goid, PublishedService>> _valueToServiceMapMap = new HashMap<T, Map<Goid, PublishedService>>();
    protected final Map<Serializable, List<T>> serviceIdToValueListMap = new HashMap<Serializable, List<T>>();
    protected final ReadWriteLock _rwlock = new ReentrantReadWriteLock(false);

    protected boolean isApplicableToConflicts(){ return false; }
    protected abstract boolean isApplicableToMessage(Message msg) throws ServiceResolutionException;
}
