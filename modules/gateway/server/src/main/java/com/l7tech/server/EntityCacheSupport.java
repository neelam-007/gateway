package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Option;
import org.springframework.context.ApplicationListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Option.optional;

/**
 * Support for implementing entity caches
 */
public abstract class EntityCacheSupport<ET extends GoidEntity, HT extends EntityHeader, EM extends GoidEntityManager<ET,HT>> implements ApplicationListener<GoidEntityInvalidationEvent> {

    //- PUBLIC

    public final Option<ET> findByPrimaryKey( final Goid goid ) {
        lock.readLock().lock();
        try {
            return optional(entityCache.get( goid ));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public final void onApplicationEvent( final GoidEntityInvalidationEvent event ) {
        if( getEntityClass().isAssignableFrom( event.getEntityClass() ) ) {
            ensureCacheValid();

            Goid entityOid = GoidEntity.DEFAULT_GOID;
            try {
                for( final Goid goid : event.getEntityIds() ) {
                    entityOid = goid;
                    ET entity = entityManager.findByPrimaryKey( goid );
                    if( entity == null ) {
                        notifyDelete( goid );
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

    protected final Logger logger = Logger.getLogger( getClass().getName() );

    protected EntityCacheSupport( final EM entityManager ) {
        this.entityManager = entityManager;
    }

    protected abstract Class<ET> getEntityClass();

    protected abstract ET readOnly( ET entity );

    protected final <R> R doWithCacheReadOnly( final Unary<R,Map<Goid,ET>> callback ) {
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
                assert !Goid.isDefault(entity.getGoid());
                entityCache.put( entity.getGoid(), entity );
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
    private final Map<Goid, ET> entityCache = new HashMap<Goid, ET>();

    /**
     * Entity must be read only
     */
    private void notifyUpdate( final ET entity ) {
        if ( logger.isLoggable( Level.FINE )) {
            logger.log( Level.FINE,
                    "Entity #{0} has been created or updated; updating caches",
                    entity.getGoid().toHexString() );
        }

        final Lock write = lock.writeLock();
        write.lock();
        try {
            entityCache.put( entity.getGoid(), entity );
        } finally {
            write.unlock();
        }
    }

    private void notifyDelete( final Goid goid ) {
        final Lock write = lock.writeLock();
        write.lock();
        try {
            final ET entity = entityCache.get( goid );
            if ( entity == null ) {
                logger.log( Level.FINE, "No entity found for id #{0}, ignoring", goid );
            } else {
                logger.log( Level.FINE, "Entity #{0} has ben deleted; removing from cache", goid );
                entityCache.remove( goid );
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
