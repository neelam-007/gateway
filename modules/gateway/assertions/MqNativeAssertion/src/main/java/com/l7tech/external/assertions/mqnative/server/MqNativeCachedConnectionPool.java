package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQQueueManager;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.pool.impl.GenericObjectPool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cached connection pool
 */
class MqNativeCachedConnectionPool {

    private static final Logger logger = Logger.getLogger(MqNativeCachedConnectionPool.class.getName());

    protected static class CachedConnection {
        private final MQQueueManager queueManager;
        private final String name;
        private final int resourceVersion;

        CachedConnection(final MQQueueManager queueManager, final String name, final int resourceVersion) {
            this.queueManager = queueManager;
            this.name = name;
            this.resourceVersion = resourceVersion;
        }

        public String getName() {
            return name;
        }

        public int getResourceVersion() {
            return resourceVersion;
        }

        public MQQueueManager getQueueManager() {
            return queueManager;
        }
    }

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final long createdTime = System.currentTimeMillis();
    private final AtomicLong lastAccessTime = new AtomicLong(createdTime);

    private GenericObjectPool<CachedConnection> cachedConnections;

    // This constructor is mainly for testing
    MqNativeCachedConnectionPool() {
        cachedConnections = null;
    }

    MqNativeCachedConnectionPool( final MqNativeEndpointConfig cfg ) {
        cachedConnections = new GenericObjectPool<>(new PoolableCachedConnectionFactory(cfg), cfg.getConnectionPoolMaxActive(),
                GenericObjectPool.WHEN_EXHAUSTED_BLOCK, cfg.getConnectionPoolMaxWait(), cfg.getConnectionPoolMaxIdle());
    }

    /**
     * Once a connection is in the cache a return of false from this
     * method indicates that the connection is invalid and should not
     * be used.
     */
    public boolean ref() {
        return referenceCount.getAndIncrement() > 0;
    }

    public void unRef() {
        int references = referenceCount.decrementAndGet();
        if ( references <= 0 ) {
            try {
                cachedConnections.close();
            } catch (Exception e) {
                if ( logger.isLoggable( Level.FINE ) ) {
                    logger.log( Level.FINE,
                            "Error closing cached connection pool: " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException( e ) );
                }
            }
        }
    }

    /**
     * Get time created
     * @return time created
     */
    public long getCreatedTime() {
        return createdTime;
    }

    /**
     * Get last access time
     * @return last access time
     */
    public AtomicLong getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * Get cached connections
     * @return cached connections
     */
    public GenericObjectPool<CachedConnection> getCachedConnections() {
        return cachedConnections;
    }

    /**
     * Set cached connections
     * @param cachedConnections Cached connections
     */
    public void setCachedConnections(final GenericObjectPool<CachedConnection> cachedConnections) {
        this.cachedConnections = cachedConnections;
    }
}
