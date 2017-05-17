package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQPoolToken;
import com.ibm.mq.MQQueueManager;
import com.l7tech.external.assertions.mqnative.server.MqNativeEndpointConfig.MqNativeEndpointKey;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.TimeUnit;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;

/**
 * Manages all outbound MQ routing connections to QueueManagers.
 */
class MqNativeResourceManager implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(MqNativeResourceManager.class.getName());

    // This is the default maximum age for connection pools
    private static final long DEFAULT_CONNECTION_MAX_AGE = TimeUnit.MINUTES.toMillis( 10 );
    // This is the default maximum idle time for connection pool to exist
    private static final long DEFAULT_CONNECTION_MAX_IDLE = TimeUnit.MINUTES.toMillis( 5 );
    // This is the default maximum number of connection pools allowable
    private static final int DEFAULT_CONNECTION_CACHE_SIZE = 100;

    private static final String PROP_CACHE_CLEAN_INTERVAL = "com.l7tech.server.transport.mq.cacheCleanInterval";
    private static final long CACHE_CLEAN_INTERVAL = ConfigFactory.getTimeUnitProperty( PROP_CACHE_CLEAN_INTERVAL, 27937L );

    private static final Object instanceSync = new Object();
    private static MqNativeResourceManager instance;

    private final ApplicationEventProxy applicationEventProxy;
    private final Config config;
    private final ConcurrentHashMap<MqNativeEndpointKey, MqNativeCachedConnectionPool> connectionHolder = new ConcurrentHashMap<>();
    private final AtomicReference<MqResourceManagerConfig> cacheConfigReference = new AtomicReference<>(new MqResourceManagerConfig(0, 0, 100));
    private final AtomicBoolean active = new AtomicBoolean(true);
    private Timer timer;

    /**
     * Get the singleton instance of MqConnectionManager.
     *
     * @return the singleton instance
     */
    static MqNativeResourceManager getInstance( final Config config, final ApplicationEventProxy applicationEventProxy ) {
        synchronized ( instanceSync ) {
            if ( instance == null) {
                instance = new MqNativeResourceManager( config, applicationEventProxy );
                instance.init();
            }
            return instance;
        }
    }

    private MqNativeResourceManager( final Config config,
                                     final ApplicationEventProxy applicationEventProxy ){
        this.config = config;
        this.applicationEventProxy = applicationEventProxy;
    }

    private void init() {
        applicationEventProxy.addApplicationListener( this );
        timer = new ManagedTimer("MqNativeResourceManager-CacheCleanup-" + System.identityHashCode(this));
        timer.schedule( new CacheCleanupTask(connectionHolder, cacheConfigReference ), 17371, CACHE_CLEAN_INTERVAL );
        updateConfig();
    }

    /**
     * Run a task with the specified JMS endpoint resources.
     *
     * @param endpoint The configuration to use
     * @param callback The callback
     * @throws MQException If the given task throws a MQException
     * @throws MqNativeRuntimeException If an error occurs creating the resources
     */
    void doWithMqResources( final MqNativeEndpointConfig endpoint, final MqTaskCallback callback )
        throws MQException, MqNativeRuntimeException
    {
        MQPoolToken token = MQEnvironment.addConnectionPoolToken();
        MqNativeCachedConnectionPool.CachedConnection borrowedConnection = null;
        if ( !active.get() ) throw new MqNativeRuntimeException("MQ outbound task manager is stopped.");

        final MqNativeEndpointKey key = endpoint.getMqEndpointKey();

        MqNativeCachedConnectionPool cachedConnectionPool = null;
        try {
            cachedConnectionPool = connectionHolder.get(key);
            if (cachedConnectionPool == null || !cachedConnectionPool.ref()) {
                cachedConnectionPool = null;

                synchronized (key.toString().intern()) { // prevent concurrent creation for a key
                    cachedConnectionPool = connectionHolder.get(key); // see if someone else created it
                    if (cachedConnectionPool == null || !cachedConnectionPool.ref()) {
                        cachedConnectionPool = null;
                        cachedConnectionPool = newConnectionPool(endpoint);
                    }
                }
            }

            borrowedConnection = cachedConnectionPool.getCachedConnections().borrowObject();
            callback.doWork(borrowedConnection.getQueueManager());
        } catch (MQException e) {
            evict(connectionHolder, key, cachedConnectionPool);
            throw e;
        } catch (Exception e) {
            evict(connectionHolder, key, cachedConnectionPool);
            throw new MqNativeRuntimeException(e);
        } finally {
            MQEnvironment.removeConnectionPoolToken(token);
            if (borrowedConnection != null) {
                try {
                    cachedConnectionPool.getCachedConnections().returnObject(borrowedConnection);
                } catch (Exception e) {
                    //swallow
                }
            }
            if (cachedConnectionPool != null) cachedConnectionPool.unRef();
        }
    }

    /**
     * Invalidate the connection for the given endpoint.
     *
     * <p>The associated connection will not be used by any subsequent users of
     * this instance. The connection will be closed after any current users
     * have completed their work (or more likely failed if the connection is
     * not valid)</p>
     *
     * @param endpoint The configuration for the connection.
     */
    void invalidate(final MqNativeEndpointConfig endpoint) {
        this.invalidate(endpoint.getMqEndpointKey());
    }

    void invalidate(final MqNativeEndpointKey key) {
        if (active.get()) {
            final MqNativeCachedConnectionPool cachedConnection = connectionHolder.get(key);
            if (cachedConnection != null) {
                evict(connectionHolder, key, cachedConnection);
            }
        }
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof EntityInvalidationEvent) {
            final EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
            if ( ClusterProperty.class.isAssignableFrom(entityInvalidationEvent.getEntityClass() ) ) {
                updateConfig();
            }
        }
    }

    public void destroy() throws Exception {
        if ( active.compareAndSet( true, false ) ) {
            doShutdown();
        }
    }

    /**
     * Callback interface for MQ tasks.
     */
    interface MqTaskCallback {
        /**
         * Callback to perform work with the given queueManager.
         *
         * <p>Exceptions thrown by this class will be propagated to the caller.</p>
         *
         * @throws MQException If an error occurs.
         */
        void doWork( final MQQueueManager queueManager ) throws MQException;
    }

    /**
     * Clear all connection references
     */
    private void doShutdown() {
        logger.info( "Shutting down MQ Connection cache." );

        applicationEventProxy.removeApplicationListener( this );

        timer.cancel();
        Collection<MqNativeCachedConnectionPool> connList = connectionHolder.values();

        for (final MqNativeCachedConnectionPool c : connList) {
            c.unRef();
        }

        connectionHolder.clear();
    }

    protected MqNativeCachedConnectionPool newConnectionPool(final MqNativeEndpointConfig mqCfg) throws MqNativeRuntimeException {
        final MqNativeEndpointKey key = mqCfg.getMqEndpointKey();

        try {
            final MqNativeCachedConnectionPool newConn = new MqNativeCachedConnectionPool(mqCfg);
            newConn.ref(); // referenced by caller

            // server config controlled connection pool props -- may not need this
            if ( cacheConfigReference.get().maximumSize > 0 ) {
                newConn.ref(); // referenced from cache

                // replace connection if the endpoint already exists
                final MqNativeCachedConnectionPool existingConn = connectionHolder.put(key, newConn);
                if ( existingConn != null ) {
                    existingConn.unRef(); // clear cache reference
                }
            }

            return newConn;
        } catch (Throwable me) {
            throw new MqNativeRuntimeException(me);
        }
    }

    private static void evict(final ConcurrentHashMap<MqNativeEndpointKey, MqNativeCachedConnectionPool> connectionHolder,
                              final MqNativeEndpointKey key,
                              final MqNativeCachedConnectionPool connections) {
        if (connectionHolder.remove(key, connections)) {
            connections.unRef(); // clear cache reference
        }
    }

    private void updateConfig() {
        logger.config( "(Re)loading cache configuration." );

        final long maximumAge = config.getTimeUnitProperty( MQ_CONNECTION_CACHE_MAX_AGE_PROPERTY, DEFAULT_CONNECTION_MAX_AGE );
        final long maximumIdleTime = config.getTimeUnitProperty( MQ_CONNECTION_CACHE_MAX_IDLE_PROPERTY, DEFAULT_CONNECTION_MAX_IDLE );
        final int maximumSize = config.getIntProperty( MQ_CONNECTION_CACHE_MAX_SIZE_PROPERTY, DEFAULT_CONNECTION_CACHE_SIZE );

        cacheConfigReference.set( new MqResourceManagerConfig(
                rangeValidate(maximumAge, DEFAULT_CONNECTION_MAX_AGE, 0L, Long.MAX_VALUE, "MQ QueueManager Connection Maximum Age" ),
                rangeValidate(maximumIdleTime, DEFAULT_CONNECTION_MAX_IDLE, 0L, Long.MAX_VALUE, "MQ QueueManager Connection Maximum Idle" ),
                rangeValidate(maximumSize, DEFAULT_CONNECTION_CACHE_SIZE, 0, Integer.MAX_VALUE, "MQ QueueManager Connection Cache Size" ) )
        );
    }

    private <T extends Number> T rangeValidate( final T value,
                                                final T defaultValue,
                                                final T min,
                                                final T max,
                                                final String description ) {
        T validatedValue;

        if ( value.longValue() < min.longValue() ) {
            logger.log( Level.WARNING,
                    "Configuration value for {0} is invalid ({1} minimum is {2}), using default value ({3}).",
                    new Object[]{description, value, min, defaultValue} );
            validatedValue = defaultValue;
        } else if ( value.longValue() > max.longValue() ) {
            logger.log( Level.WARNING,
                    "Configuration value for {0} is invalid ({1} maximum is {2}), using default value ({3}).",
                    new Object[]{description, value, max, defaultValue} );
            validatedValue = defaultValue;
        } else {
            validatedValue = value;
        }

        return validatedValue;
    }

    /**
     * Bean for cache configuration.
     */
    static final class MqResourceManagerConfig {
        private final long maximumAge;
        private final long maximumIdleTime;
        private final int maximumSize;

        private MqResourceManagerConfig( final long maximumAge,
                                          final long maximumIdleTime,
                                          final int maximumSize ) {
            this.maximumAge = maximumAge;
            this.maximumIdleTime = maximumIdleTime;
            this.maximumSize = maximumSize;
        }

        int getMaximumSize() {
            return maximumSize;
        }
    }

    /**
     * Timer task to remove idle, expired or surplus connections from the cache.
     *
     * <p>When the cache size is exceeded the oldest JMS connections are
     * removed first.</p>
     */
    private static final class CacheCleanupTask extends TimerTask {
        private static final Logger taskLogger = Logger.getLogger(CacheCleanupTask.class.getName());

        private final ConcurrentHashMap<MqNativeEndpointKey, MqNativeCachedConnectionPool> connectionHolder;
        private final AtomicReference<MqResourceManagerConfig> cacheConfigReference;

        private CacheCleanupTask(final ConcurrentHashMap<MqNativeEndpointKey, MqNativeCachedConnectionPool> connectionHolder,
                                 final AtomicReference<MqResourceManagerConfig> cacheConfigReference) {
            this.connectionHolder = connectionHolder;
            this.cacheConfigReference = cacheConfigReference;
        }

        @Override
        public void run() {
            final MqResourceManagerConfig cacheConfig = cacheConfigReference.get();
            final long timeNow = System.currentTimeMillis();
            final int overSize = connectionHolder.size() - cacheConfig.maximumSize;
            // place eviction candidates into an ordered set, ordered by creation time
            final Set<Entry<MqNativeEndpointKey, MqNativeCachedConnectionPool>> evictionCandidates =
                    new TreeSet<>(
                            (e1, e2) -> Long.valueOf(e1.getValue().getCreatedTime()).compareTo(e2.getValue().getCreatedTime())
                    );

            for (final Map.Entry<MqNativeEndpointKey, MqNativeCachedConnectionPool> cachedConnectionEntry : connectionHolder.entrySet()) {
                final MqNativeEndpointKey endpointKey = cachedConnectionEntry.getKey();

                if ((timeNow - cachedConnectionEntry.getValue().getCreatedTime()) > cacheConfig.maximumAge && cacheConfig.maximumAge > 0) {
                    evict(connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue());
                    // debug only - to be removed
                    taskLogger.log(Level.INFO, "Evicting MQ connection pool {0}:{1}:{2}:{3}:{4} due to max age exceeded",
                            new Object[]{endpointKey.getId(), endpointKey.getVersion(), endpointKey.getMaxActive(), endpointKey.getMaxIdle(), endpointKey.getMaxWait()});

                } else if ((timeNow - cachedConnectionEntry.getValue().getLastAccessTime().get()) > cacheConfig.maximumIdleTime && cacheConfig.maximumIdleTime > 0) {
                    evict(connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue());
                    // debug only - to be removed
                    taskLogger.log(Level.INFO, "Evicting MQ connection pool {0}:{1}:{2}:{3}:{4} due to max idleTime exceeded",
                            new Object[]{endpointKey.getId(), endpointKey.getVersion(), endpointKey.getMaxActive(), endpointKey.getMaxIdle(), endpointKey.getMaxWait()});

                } else if ( overSize > 0 ) {
                    evictionCandidates.add( cachedConnectionEntry );
                }
            }

            // evict oldest first to reduce cache size
            final Iterator<Entry<MqNativeEndpointKey, MqNativeCachedConnectionPool>> evictionIterator = evictionCandidates.iterator();
            for (int i = 0; i < overSize && evictionIterator.hasNext(); i++) {
                final Map.Entry<MqNativeEndpointKey, MqNativeCachedConnectionPool> cachedConnectionEntry =
                        evictionIterator.next();
                evict( connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue() );
            }
        }
    }

    ConcurrentHashMap<MqNativeEndpointKey, MqNativeCachedConnectionPool> getConnectionHolder() {
        return connectionHolder;
    }

    AtomicReference<MqResourceManagerConfig> getCacheConfigReference() {
        return cacheConfigReference;
    }
}
