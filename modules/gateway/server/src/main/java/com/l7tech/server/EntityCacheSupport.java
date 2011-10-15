package com.l7tech.server;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.util.Functions.Unary;
import org.springframework.context.ApplicationListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support for implementing entity caches
 */
public abstract class EntityCacheSupport<ET extends PersistentEntity, HT extends EntityHeader, EM extends EntityManager<ET,HT>> implements ApplicationListener<EntityInvalidationEvent> {

    //- PUBLIC

    public final ET findByPrimaryKey( final long oid ) {
        lock.readLock().lock();
        try {
            return entityCache.get( oid );
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public final void onApplicationEvent( final EntityInvalidationEvent event ) {
        if( getEntityClass().isAssignableFrom( event.getEntityClass() ) ) {
            ensureCacheValid();

            long entityOid = -1L;
            try {
                for( final long oid : event.getEntityIds() ) {
                    entityOid = oid;
                    ET entity = entityManager.findByPrimaryKey( oid );
                    if( entity == null ) {
                        notifyDelete( oid );
                    } else {
                        notifyUpdate( readOnly( entity ) );
                    }
                }
            } catch( FindException fe ) {
                markDirty();
                logger.log( Level.WARNING, "Error loading entity "+entityOid+" for cache", fe );
            }
        }
    }

    //- PROTECTED

    private final Logger logger = Logger.getLogger( getClass().getName() );

    protected EntityCacheSupport( final EM entityManager ) {
        this.entityManager = entityManager;
    }

    protected abstract Class<ET> getEntityClass();

    protected abstract ET readOnly( ET entity );

    protected final <R> R doWithCacheReadOnly( final Unary<R,Map<Long,ET>> callback ) {
        lock.readLock().lock();
        try {
            return callback.call( entityCache );
        } finally {
            lock.readLock().unlock();
        }
    }

    protected final void initializeCache() {
        final Lock write = lock.writeLock();
        write.lock();
        try {
            // record initialized
            initialized = true;

            // find before clear in case of error
            Collection<ET> entities = entityManager.findAll();

            // clear cached data
            entityCache.clear();

            for ( final ET entity : entities ) {
                assert entity.getOid() != PersistentEntity.DEFAULT_OID;
                entityCache.put( entity.getOid(), entity );
            }

            cacheIsInvalid = false;
        } catch ( FindException fe ) {
            markDirty();
            logger.log( Level.WARNING, "Error populating cache", fe );
        } finally {
            write.unlock();
        }
    }

    //- PRIVATE

    private final EM entityManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean cacheIsInvalid = true;
    private boolean initialized = false;
    private final Map<Long, ET> entityCache = new HashMap<Long, ET>();

    /**
     * Entity must be read only
     */
    private void notifyUpdate( final ET entity ) {
        if ( logger.isLoggable( Level.FINE )) {
            logger.log( Level.FINE,
                    "Entity #{0} has been created or updated; updating caches",
                    entity.getOid() );
        }

        final Lock write = lock.writeLock();
        write.lock();
        try {
            entityCache.put( entity.getOid(), entity );
        } finally {
            write.unlock();
        }
    }

    private void notifyDelete( final long oid ) {
        final Lock write = lock.writeLock();
        write.lock();
        try {
            final ET entity = entityCache.get( oid );
            if ( entity == null ) {
                logger.log( Level.FINE, "No entity found for id #{0}, ignoring", oid );
            } else {
                logger.log( Level.FINE, "Entity #{0} has ben deleted; removing from cache", oid );
                entityCache.remove( oid );
            }
        } finally {
            write.unlock();
        }
    }

    private void ensureCacheValid() {
        boolean isInvalid;

        final Lock read = lock.readLock();
        read.lock();
        try {
            isInvalid = initialized && cacheIsInvalid;
        } finally {
            read.unlock();
        }

        if ( isInvalid ) {
            initializeCache();
        }
    }

    /**
     * Mark entire cache as invalid
     */
    private void markDirty() {
        final Lock write = lock.writeLock();
        write.lock();
        try {
            cacheIsInvalid = true;
        } finally {
            write.unlock();
        }
    }
}
