package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQC;
import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.external.assertions.mqnative.server.MqNativeEndpointConfig.MqEndpointKey;
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.closeQuietly;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions.UnaryVoidThrows;
import com.l7tech.util.Option;
import com.l7tech.util.TimeUnit;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.Closeable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all outbound MQ routing connections to QueueManagers.
 */
class MqNativeResourceManager implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(MqNativeResourceManager.class.getName());

    private static final long DEFAULT_CONNECTION_MAX_AGE = TimeUnit.MINUTES.toMillis( 10 );
    private static final long DEFAULT_CONNECTION_MAX_IDLE = TimeUnit.MINUTES.toMillis( 5 );
    private static final int DEFAULT_CONNECTION_CACHE_SIZE = 100;

    private static final String PROP_CACHE_CLEAN_INTERVAL = "com.l7tech.server.transport.mq.cacheCleanInterval";
    private static final long CACHE_CLEAN_INTERVAL = ConfigFactory.getTimeUnitProperty( PROP_CACHE_CLEAN_INTERVAL, 27937L );

    private static final Object instanceSync = new Object();
    private static MqNativeResourceManager instance;

    private final ApplicationEventProxy applicationEventProxy;
    private final Config config;
    private final ConcurrentHashMap<MqNativeEndpointConfig.MqEndpointKey, CachedConnection> connectionHolder = new ConcurrentHashMap<MqNativeEndpointConfig.MqEndpointKey, CachedConnection>();
    private final AtomicReference<MqResourceManagerConfig> cacheConfigReference = new AtomicReference<MqResourceManagerConfig>( new MqResourceManagerConfig(0,0,100) );
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
        if ( !active.get() ) throw new MqNativeRuntimeException("MQ outbound task manager is stopped.");

        final MqNativeEndpointConfig.MqEndpointKey key = endpoint.getMqEndpointKey();

        CachedConnection cachedConnection = null;
        try {
            cachedConnection = connectionHolder.get(key);
            if ( cachedConnection == null || !cachedConnection.ref() ) {
                cachedConnection = null;

                synchronized( key.toString().intern() ){ // prevent concurrent creation for a key
                    cachedConnection = connectionHolder.get(key); // see if someone else created it
                    if ( cachedConnection == null || !cachedConnection.ref() ) {
                        cachedConnection = null;
                        cachedConnection = newConnection(endpoint);
                    }
                }
            }

            callback.doWork( cachedConnection.writeBag, cachedConnection.readBag );

        } catch ( MQException e ) {
            evict( connectionHolder, key, cachedConnection );
            throw e;
        } finally {
            if ( cachedConnection != null ) cachedConnection.unRef();
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
    void invalidate( final MqNativeEndpointConfig endpoint ) {
        this.invalidate(endpoint.getMqEndpointKey());
    }

    void invalidate( final MqNativeEndpointConfig.MqEndpointKey key ) {
        if ( active.get() ) {
            final CachedConnection cachedConnection = connectionHolder.get(key);
            if ( cachedConnection != null ) {
                evict( connectionHolder, key, cachedConnection );
            }
        }
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof EntityInvalidationEvent ) {
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
         * Callback to perform work with the given MQ resources.
         *
         * <p>Exceptions thrown by this class will be propagated to the caller.</p>
         *
         * @throws MQException If an error occurs.
         */
        void doWork( MqNativeResourceBag writeBag, MqNativeResourceBag readBag ) throws MQException;
    }

    /**
     * Bag of MQ resources.
     *
     * TODO [steve] Use or remove queue and replyQueue
     */
    static class MqNativeResourceBag implements Closeable {

        MqNativeResourceBag( final MQQueueManager qmgr,
                             final MQQueue queue,
                             final MQQueue replyQueue,
                             final MQGetMessageOptions gmo,
                             final MQPutMessageOptions pmo ) {
            this.qmgr = qmgr;
            this.q = queue;
            this.replyQ = replyQueue;

            if (gmo == null)
                this.gmoOptions = new MQGetMessageOptions();
            else
                this.gmoOptions = gmo;

            if (pmo == null)
                this.pmoOptions = new MQPutMessageOptions();
            else
                this.pmoOptions = pmo;
        }

        MQQueueManager getQmgr() {
            check();
            return qmgr;
        }

        MQQueue getQueue() {
            check();
            return q;
        }

        MQQueue getReplyQueue() {
            check();
            return replyQ;
        }

        MQGetMessageOptions getGmoOptions() {
            return gmoOptions;
        }

        MQPutMessageOptions getPmoOptions() {
            return pmoOptions;
        }

        public void close() {
            try {
                closeQuietly( q );
                closeQuietly( replyQ );
                closeQuietly( qmgr, Option.<UnaryVoidThrows<MQQueueManager, MQException>>some( new UnaryVoidThrows<MQQueueManager, MQException>() {
                    @Override
                    public void call( final MQQueueManager mqQueueManager ) throws MQException {
                        mqQueueManager.disconnect();
                    }
                } )  );
            } finally {
                closed = true;
            }
        }

        private void check() {
            if (closed) throw new IllegalStateException("MQ resource bag has been closed");
        }

        private static final Logger logger = Logger.getLogger(MqNativeResourceBag.class.getName());

        private MQQueueManager qmgr;
        private MQQueue q;
        private MQQueue replyQ;
        private MQGetMessageOptions gmoOptions;
        private MQPutMessageOptions pmoOptions;
        protected volatile boolean closed;
    }

    /**
     * Clear all connection references
     */
    private void doShutdown() {
        logger.info( "Shutting down MQ Connection cache." );

        applicationEventProxy.removeApplicationListener( this );

        timer.cancel();
        Collection<CachedConnection> connList = connectionHolder.values();

        for ( final CachedConnection c : connList ) {
            c.unRef();
        }

        connectionHolder.clear();
    }

    private CachedConnection newConnection( final MqNativeEndpointConfig mqCfg ) throws MqNativeRuntimeException {
        final MqNativeEndpointConfig.MqEndpointKey key = mqCfg.getMqEndpointKey();

        try {
            // create the new MqResBag for the endpoint (for outbound routing)
            MQQueueManager _qmgr = new MQQueueManager(mqCfg.getQueueManagerName(), mqCfg.getQueueManagerProperties());

            // build the MQPutMessageOptions
            MQPutMessageOptions pmo = new MQPutMessageOptions();
            pmo.options = MQC.MQPMO_NO_SYNCPOINT; // make message available immediately
            if (!mqCfg.isCorrelateWithMessageId() && mqCfg.getReplyType() == MqNativeReplyType.REPLY_SPECIFIED_QUEUE) {
                pmo.options |= MQC.MQPMO_NEW_CORREL_ID;
            }

            MqNativeResourceBag writeBag = new MqNativeResourceBag(_qmgr, null, null, null, pmo);
            MqNativeResourceBag readBag = null;

            // create readBag only if a reply is expected
            if (mqCfg.getReplyToQueueName() != null && mqCfg.getReplyToQueueName().length() > 0) {

                MQQueueManager _qmgr2 = new MQQueueManager(mqCfg.getQueueManagerName(), mqCfg.getQueueManagerProperties());

                MQGetMessageOptions gmo = new MQGetMessageOptions();
                gmo.options = MQC.MQGMO_WAIT | MQC.MQGMO_NO_SYNCPOINT;

                readBag = new MqNativeResourceBag(_qmgr2, null, null, gmo, null);
            }

            // create new cached connection wrapper
            final CachedConnection newConn = new CachedConnection(mqCfg, writeBag, readBag);
            newConn.ref(); // referenced by caller

            // server config controlled connection pool props -- may not need this
            if ( cacheConfigReference.get().maximumSize > 0 ) {
                newConn.ref(); // referenced from cache

                // replace connection if the endpoint already exists
                final CachedConnection existingConn = connectionHolder.put(key, newConn);
                if ( existingConn != null ) {
                    existingConn.unRef(); // clear cache reference
                }
            }

            logger.log(Level.INFO, "New MQ QueueManager connection created ({0}), version {1}",
                       new Object[] {newConn.name, mqCfg.getMqEndpointKey().getVersion()});

            return newConn;

        } catch ( MQException me ) {
            throw new MqNativeRuntimeException(me);
        } catch ( Throwable t ) {
            throw new MqNativeRuntimeException(t);
        }
    }

    private static void evict( final ConcurrentHashMap<MqEndpointKey, CachedConnection> connectionHolder,
                               final MqNativeEndpointConfig.MqEndpointKey key,
                               final CachedConnection connection ) {
        if ( connectionHolder.remove( key, connection ) ) {
            connection.unRef(); // clear cache reference
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

    private class CachedConnection {
        private final AtomicInteger referenceCount = new AtomicInteger(0);
        private final long createdTime = System.currentTimeMillis();
        private final AtomicLong lastAccessTime = new AtomicLong(createdTime);

        private final MqNativeResourceBag writeBag; // for routing queue
        private final MqNativeResourceBag readBag; // for reading replyTo queue
        private final String name;
        private final int resourceVersion;

        CachedConnection( final MqNativeEndpointConfig cfg, final MqNativeResourceBag writeBag, final MqNativeResourceBag readBag) {
            this.writeBag = writeBag;
            this.readBag = readBag;
            this.name = cfg.getMqEndpointKey().toString();
            this.resourceVersion = cfg.getMqEndpointKey().getVersion();
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
                logger.log(Level.INFO, "Closing MQ connection ({0}), version {1}", new Object[] {name, resourceVersion});
                writeBag.close();
                if (readBag != null)
                    readBag.close();
            }
        }
    }

    /**
     * Bean for cache configuration.
     */
    private static final class MqResourceManagerConfig {
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
    }

    /**
     * Timer task to remove idle, expired or surplus connections from the cache.
     *
     * <p>When the cache size is exceeded the oldest JMS connections are
     * removed first.</p>
     */
    private static final class CacheCleanupTask extends TimerTask {
        private static final Logger taskLogger = Logger.getLogger(CacheCleanupTask.class.getName());

        private final ConcurrentHashMap<MqNativeEndpointConfig.MqEndpointKey, CachedConnection> connectionHolder;
        private final AtomicReference<MqResourceManagerConfig> cacheConfigReference;

        private CacheCleanupTask( final ConcurrentHashMap<MqNativeEndpointConfig.MqEndpointKey, CachedConnection> connectionHolder,
                                  final AtomicReference<MqResourceManagerConfig> cacheConfigReference ) {
            this.connectionHolder = connectionHolder;
            this.cacheConfigReference = cacheConfigReference;
        }

        @Override
        public void run() {
            final MqResourceManagerConfig cacheConfig = cacheConfigReference.get();
            final long timeNow = System.currentTimeMillis();
            final int overSize = connectionHolder.size() - cacheConfig.maximumSize;
            final Set<Entry<MqEndpointKey,CachedConnection>> evictionCandidates =
                new TreeSet<Entry<MqEndpointKey,CachedConnection>>(
                    new Comparator<Entry<MqEndpointKey,CachedConnection>>(){
                        @Override
                        public int compare( final Map.Entry<MqNativeEndpointConfig.MqEndpointKey, CachedConnection> e1,
                                            final Map.Entry<MqNativeEndpointConfig.MqEndpointKey, CachedConnection> e2 ) {
                            return Long.valueOf( e1.getValue().createdTime ).compareTo( e2.getValue().createdTime );
                        }
                    }
                );

            for ( final Map.Entry<MqNativeEndpointConfig.MqEndpointKey,CachedConnection> cachedConnectionEntry : connectionHolder.entrySet() ) {
                if ( (timeNow-cachedConnectionEntry.getValue().createdTime) > cacheConfig.maximumAge && cacheConfig.maximumAge > 0) {
                    evict( connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue() );
                    // debug only - to be removed
                    taskLogger.log(Level.INFO, "Evicting MQ connection {0}:{1} due to max age exceeded",
                            new Object[] {cachedConnectionEntry.getKey().getId(), cachedConnectionEntry.getKey().getVersion()});

                } else if ( (timeNow-cachedConnectionEntry.getValue().lastAccessTime.get()) > cacheConfig.maximumIdleTime && cacheConfig.maximumIdleTime > 0) {
                    evict( connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue() );
                    // debug only - to be removed
                    taskLogger.log(Level.INFO, "Evicting MQ connection {0}:{1} due to max idleTime exceeded",
                            new Object[] {cachedConnectionEntry.getKey().getId(), cachedConnectionEntry.getKey().getVersion()});

                } else if ( overSize > 0 ) {
                    evictionCandidates.add( cachedConnectionEntry );
                }
            }

            // evict oldest first to reduce cache size
            final Iterator<Entry<MqEndpointKey,CachedConnection>> evictionIterator = evictionCandidates.iterator();
            for ( int i=0; i<overSize && evictionIterator.hasNext(); i++ ) {
                final Map.Entry<MqNativeEndpointConfig.MqEndpointKey,CachedConnection> cachedConnectionEntry =
                        evictionIterator.next();
                evict( connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue() );
            }
        }
    }
}
