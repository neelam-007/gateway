package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.TimeUnit;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.springframework.beans.factory.DisposableBean;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.NamingException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for JMS resources.
 *
 * <p>This class manages a cache of JMS connections. Users of this class can
 * request callbacks to perform some work using the managed JMS resources.</p>
 *
 * <p>This class is currently not at all smart about connection identity, each
 * JmsConnection / JmsEndpoint pair is considered to be distinct, even if the
 * values are identical. Dynamic connections are supported based on JmsEndpoints
 * that are templates with overriding properties supplied by
 * JmsDynamicProperties.</p>
 *
 * @author: vchan
 */
public class JmsResourceManager implements DisposableBean, PropertyChangeListener {

    //- PUBLIC

    /**
     * Create a new JMS Resource manager.
     *
     * <p>The name for the manager should be unique.</p>
     *
     * @param name The name to use
     * @param config The configuration source.
     */
    public JmsResourceManager( final String name,
                               final Config config ) {
        this.config = config;
        this.connectionHolder = new ConcurrentHashMap<JmsEndpointConfig.JmsEndpointKey, CachedConnection>();
        this.timer = new ManagedTimer("JmsResourceManager-CacheCleanup-" + name);
        timer.schedule( new CacheCleanupTask(connectionHolder, cacheConfigReference ), 17371, CACHE_CLEAN_INTERVAL );
        updateConfig();
    }

    /**
     * Run a task with the specified JMS endpoint resources.
     *
     * @param endpoint The configuration to use
     * @param callback The callback
     * @throws JMSException If the given task throws a JMSException
     * @throws JmsRuntimeException If an error occurs creating the resources
     */
    public void doWithJmsResources( final JmsEndpointConfig endpoint,
                                    final JmsResourceCallback callback ) throws NamingException, JMSException, JmsRuntimeException {
        if ( !active.get() ) throw new JmsRuntimeException("JMS resource manager is stopped.");

        CachedConnection cachedConnection = null;
        try {
            cachedConnection = getConnection(endpoint);
            cachedConnection.doWithJmsResources( callback );
        } catch ( JMSException e ) {
            evict( connectionHolder, endpoint.getJmsEndpointKey(), cachedConnection );
            throw e;
        } finally {
            if ( cachedConnection!= null ) cachedConnection.unRef();
        }
    }

    /**
     * Retrieve the JMS Connection, if the connection doesn't exist in the cache, create the connection and cache it.
     * A connection reference will be incremented. Make sure to release the reference once finish the connection.
     *
     * @param endpoint The configuration to use
     * @return The Cached JMS Connection
     * @throws JmsRuntimeException If an error occurs creating the resources
     */
    protected CachedConnection getConnection(JmsEndpointConfig endpoint) throws NamingException, JmsRuntimeException {

        final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();
        CachedConnection cachedConnection = null;
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
        return cachedConnection;
    }

    /**
     * Borrow a JmsBag from the Cached Connection. Expect to return the JmsBag using returnJmsBag.
     * Fail to return the JmsBag will leave the JMS Session open, the caller is responsible to
     * close the jms session.
     *
     * @param endpointConfig The configuration endpoint to lookup the Cached Connection
     * @return A JmsBag with JmsSession
     * @throws JmsRuntimeException If error occur when getting the connection or getting the JmsBag
     */
    public JmsBag borrowJmsBag(final JmsEndpointConfig endpointConfig) throws NamingException, JmsRuntimeException {
        CachedConnection connection = getConnection(endpointConfig);
        return connection.borrowJmsBag();
    }

    /**
     * Return a JmsBag to the Cached Connection, the JmsBag can be reuse by other thread.
     * @param bag The bag return to the JmsSession, Caller should not change any state of JmsBag.
     * @throws JmsRuntimeException If error occur when returning the JmsBag
     */
    public void returnJmsBag(JmsBag bag) throws JmsRuntimeException {
        CachedConnection cachedConnection = (CachedConnection) bag.getBagOwner();
        if (cachedConnection != null) {
            cachedConnection.returnJmsBag(bag);
            cachedConnection.unRef();
        }
    }

    /**
     * Touch the connection and keep the connection active.
     *
     * @param bag The configuration endpoint to loopup the Cached Connection
     * @throws JmsRuntimeException If error when touching the Cached Connection.
     */
    public void touch(final JmsBag bag) throws JmsRuntimeException {
        CachedConnection cachedConnection = (CachedConnection) bag.getBagOwner();
        if (cachedConnection != null) {
            cachedConnection.touch();
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
    public void invalidate( final JmsEndpointConfig endpoint ) {
        if ( active.get() ) {
            final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();
            final CachedConnection cachedConnection = connectionHolder.get(key);
            if ( cachedConnection != null ) {
                evict( connectionHolder, key, cachedConnection );
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        if ( active.compareAndSet( true, false ) ) {
            doShutdown();
        }
    }

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        updateConfig();
    }

    /**
     * Callback interface for JMS tasks.
     */
    public interface JmsResourceCallback {
        /**
         * Callback to perform work with the given JMS resources.
         *
         * <p>Exceptions thrown by this class will be propagated to the caller.</p>
         *
         * @param bag The JmsBag with contained the shared JMS Connection and cached session and producer
         * @param jndiContextProvider Provides access to the shared JNDI context.
         * @throws JMSException If an error occurs.
         */
        void doWork( JmsBag bag,  JndiContextProvider jndiContextProvider ) throws JMSException;
    }

    /**
     * The JNDI context provider allows access to the shared JNDI context.
     */
    public interface JndiContextProvider {
        /**
         * Run a task with the context associated with this provider.
         *
         * @param callback The task to run.
         * @throws NamingException If the task throws an exception.
         */
        void doWithJndiContext( JndiContextCallback callback ) throws NamingException ;

        /**
         * Convenience method for simple look ups.
         *
         * @param name The name to lookup.
         * @return The value
         * @throws NamingException If the look up throws
         * @see Context#lookup(String)
         */
        Object lookup( String name ) throws NamingException;
    }

    /**
     * Callback interface for JNDI context tasks.
     */
    public interface JndiContextCallback {
        /**
         * Callback to perform work with the given JNDI context.
         *
         * <p>Exceptions thrown by this class will be propagated to the caller.</p>
         *
         * @param context The context to use.
         * @throws NamingException If an error occurs
         */
        void doWork( Context context ) throws NamingException;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(JmsResourceManager.class.getName());

    private static final long DEFAULT_CONNECTION_MAX_AGE = TimeUnit.MINUTES.toMillis( 30 );
    private static final long DEFAULT_CONNECTION_MAX_IDLE = TimeUnit.MINUTES.toMillis( 5 );
    private static final int DEFAULT_CONNECTION_CACHE_SIZE = 100;

    private static final String PROP_CACHE_CLEAN_INTERVAL = "com.l7tech.server.transport.jms.cacheCleanInterval";
    private static final long CACHE_CLEAN_INTERVAL = ConfigFactory.getLongProperty( PROP_CACHE_CLEAN_INTERVAL, 27937L );

    // need to store one connection per endpoint
    private final Config config;
    protected final ConcurrentHashMap<JmsEndpointConfig.JmsEndpointKey, CachedConnection> connectionHolder;
    private final Timer timer;
    private final AtomicReference<JmsResourceManagerConfig> cacheConfigReference = new AtomicReference<JmsResourceManagerConfig>( new JmsResourceManagerConfig(0,0,100) );
    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * Clear all connection references
     */
    private void doShutdown() {
        logger.info( "Shutting down JMS Connection cache." );

        timer.cancel();
        Collection<CachedConnection> connList = connectionHolder.values();

        for ( final CachedConnection c : connList ) {
            c.unRef();
        }

        connectionHolder.clear();
    }

    private CachedConnection newConnection( final JmsEndpointConfig endpoint ) throws NamingException, JmsRuntimeException {
        final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();

        try {
            // create the new JmsBag for the endpoint
            final JmsBag newBag = JmsUtil.connect(endpoint);
            newBag.getConnection().start();

            // create new cached connection wrapper
            final CachedConnection newConn = new CachedConnection(endpoint, newBag);
            newConn.ref(); // referenced by caller

            if ( cacheConfigReference.get().maximumSize > 0 ) {
                newConn.ref(); // referenced from cache

                // replace connection if the endpoint already exists
                final CachedConnection existingConn = connectionHolder.put(key, newConn);
                if ( existingConn != null ) {
                    existingConn.unRef(); // clear cache reference
                }
            }

            logger.log(Level.FINE, "New JMS connection created ({0}), version {1}:{2}", new Object[] {
                    newConn.name, newConn.connectionVersion, newConn.endpointVersion
            });

            return newConn;
        }catch (NamingException ne){
            throw ne; //rethrow NamingException if it has been caught. Bug SSG-6792
        } catch ( Throwable t ) {
            throw new JmsRuntimeException(t);
        }
    }

    private static void evict( final ConcurrentHashMap<JmsEndpointConfig.JmsEndpointKey, CachedConnection> connectionHolder,
                               final JmsEndpointConfig.JmsEndpointKey key,
                               final CachedConnection connection ) {
        if ( connectionHolder.remove( key, connection ) ) {
            connection.unRef(); // clear cache reference
        }
    }

    private void updateConfig() {
        logger.config( "(Re)loading cache configuration." );

        final long maximumAge = config.getTimeUnitProperty( "ioJmsConnectionCacheMaxAge", DEFAULT_CONNECTION_MAX_AGE );
        final long maximumIdleTime = config.getTimeUnitProperty( "ioJmsConnectionCacheMaxIdleTime", DEFAULT_CONNECTION_MAX_IDLE );
        final int maximumSize = config.getIntProperty( "ioJmsConnectionCacheSize", DEFAULT_CONNECTION_CACHE_SIZE );

        cacheConfigReference.set( new JmsResourceManagerConfig(
                rangeValidate(maximumAge, DEFAULT_CONNECTION_MAX_AGE, 0L, Long.MAX_VALUE, "JMS Connection Maximum Age" ),
                rangeValidate(maximumIdleTime, DEFAULT_CONNECTION_MAX_IDLE, 0L, Long.MAX_VALUE, "JMS Connection Maximum Idle" ),
                rangeValidate(maximumSize, DEFAULT_CONNECTION_CACHE_SIZE, 0, Integer.MAX_VALUE, "JMS Connection Cache Size" ) )
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
     * Cache entry
     */
    protected class CachedConnection {
        private static final long MAX_CLOSE_CONNECTION_WAIT = 30; //30 seconds
        private final AtomicInteger referenceCount = new AtomicInteger(0);
        private final long createdTime = System.currentTimeMillis();
        private final AtomicLong lastAccessTime = new AtomicLong(createdTime);

        private final Object contextLock = new Object();
        private final JmsBag bag;
        private final String name;
        private final int endpointVersion;
        private final int connectionVersion;
        protected final GenericObjectPool<JmsBag> pool;
        private final JmsEndpointConfig endpointConfig;

        protected CachedConnection( final JmsEndpointConfig cfg,
                          final JmsBag bag) {
            this.bag = bag;
            this.name = cfg.getJmsEndpointKey().toString();
            this.endpointVersion = cfg.getEndpoint().getVersion();
            this.connectionVersion = cfg.getConnection().getVersion();
            this.endpointConfig = cfg;

            this.pool = new GenericObjectPool<JmsBag>( new PoolableObjectFactory<JmsBag>() {
                @Override
                public JmsBag makeObject() throws Exception {
                    Session session = null;
                    MessageConsumer consumer = null;
                    MessageProducer producer = null;
                    final String destinationName = endpointConfig.getEndpoint().getDestinationName();
                    try{
                        if (endpointConfig.getEndpoint().isMessageSource()) {
                            session = bag.getConnection().createSession(endpointConfig.isTransactional(), Session.CLIENT_ACKNOWLEDGE);
                        } else {
                            session = bag.getConnection().createSession(false, Session.CLIENT_ACKNOWLEDGE);
                        }

                        if (endpointConfig.getEndpoint().isMessageSource()) {
                            //Create the consumer
                            Destination destination = JmsUtil.cast( bag.getJndiContext().lookup( destinationName ), Destination.class );
                            consumer = JmsUtil.createMessageConsumer(session, destination);

                            Destination failureQueue = getFailureQueue(bag.getJndiContext());
                            if (failureQueue != null) {
                                producer = JmsUtil.createMessageProducer(session, failureQueue);
                            }
                        } else {
                           Destination destination = JmsUtil.cast( bag.getJndiContext().lookup( destinationName ), Destination.class );
                            producer = JmsUtil.createMessageProducer(session, destination);
                        }

                        return new JmsBag(bag.getJndiContext(), bag.getConnectionFactory(),
                                bag.getConnection(), session, consumer, producer, CachedConnection.this );
                    } catch(Exception ex) {
                        //do the clean up and rethrow the exception
                        try {
                            if(producer != null) producer.close();
                        } catch (JMSException e) {
                           logger.log(Level.WARNING, "Unable to close producer for the destination: " + destinationName);
                        }
                        try {
                            if(consumer != null) consumer.close();
                        } catch (JMSException e) {
                            logger.log(Level.WARNING, "Unable to close consumer for the destination: " + destinationName);
                        }
                        try {
                            if(session != null) session.close();
                        } catch (JMSException e) {
                            logger.log(Level.WARNING, "Unable to close JMS session");
                        }
                        throw ex;
                    }
                }

                @Override
                public void destroyObject(JmsBag jmsBag) throws Exception {
                    jmsBag.closeSession();
                }

                @Override
                public boolean validateObject(JmsBag jmsBag) {
                    return true;
                }

                @Override
                public void activateObject(JmsBag jmsBag) throws Exception {
                }

                @Override
                public void passivateObject(JmsBag jmsBag) throws Exception {
                }
            }, getSessionPoolSize(),GenericObjectPool.WHEN_EXHAUSTED_BLOCK, getSessionPoolMaxWait(), getMaxSessionIdle());
        }

        protected int getSessionPoolSize() {
            if  (endpointConfig.getEndpoint().isMessageSource()) {
                return -1;
            } else {
                return Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_SESSION_POOL_SIZE,
                    String.valueOf(JmsConnection.DEFAULT_SESSION_POOL_SIZE)));
            }
        }

        protected int getMaxSessionIdle() {
            if  (endpointConfig.getEndpoint().isMessageSource()) {
                return -1;
            } else {
                return Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_MAX_SESSION_IDLE,
                        String.valueOf(JmsConnection.DEFAULT_SESSION_POOL_SIZE)));
            }
        }

        protected long getSessionPoolMaxWait() {
            if (endpointConfig.getEndpoint().isMessageSource()) {
                //The pool is never exhausted, just return a number, it should be ignored.
                return 0;
            } else {
                return Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_SESSION_POOL_MAX_WAIT,
                    String.valueOf(JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT)));
            }
        }

        /**
         * Update the lastAccessTime, to keep the connection alive.
         */
        public void touch() {
            lastAccessTime.set( System.currentTimeMillis() );
        }

        /**
         * Borrow a JmsBag from the Cached Connection. Expect to return the JmsBag using returnJmsBag.
         * Fail to return the JmsBag will leave the JMS Session open, the caller is responsible to
         * close the jms session.
         *
         * @return A JmsBag with JmsSession
         * @throws JmsRuntimeException If error occur when getting the JmsBag
         */
        public JmsBag borrowJmsBag() throws JmsRuntimeException, NamingException {

            touch();
            try {
                return pool.borrowObject();
            } catch (JMSException e) {
                throw new JmsRuntimeException(e);
            } catch (NamingException e) {
                throw e;
            } catch (JmsConfigException e) {
                throw new JmsRuntimeException(e);
            } catch (Exception e) {
                throw new JmsRuntimeException(e);
            }
        }

        /**
         * Return a JmsBag to the Cached Connection, the JmsBag can be reuse by other Thread
         * @param jmsBag The bag return to the pool
         */
        public void returnJmsBag(JmsBag jmsBag) {
            if (jmsBag != null) {
                try {
                    pool.returnObject(jmsBag);
                } catch (Exception e) {
                    jmsBag.closeSession();
                }
            }
        }

        /**
         * Caller must hold reference.
         */
        public void doWithJmsResources( final JmsResourceCallback callback ) throws JMSException, JmsRuntimeException, NamingException {
            JmsBag jmsBag = null;
            try {
                try {
                    jmsBag = borrowJmsBag();
                } catch (NamingException e) {
                    throw e;
                } catch ( Throwable t ) {
                    throw new JmsRuntimeException(t);
                }

                callback.doWork( jmsBag, new JndiContextProvider(){
                    @Override
                    public void doWithJndiContext( final JndiContextCallback contextCallback ) throws NamingException  {
                        synchronized( contextLock ) {
                            contextCallback.doWork( bag.getJndiContext() );
                        }
                    }

                    @Override
                    public Object lookup( final String name ) throws NamingException {
                        synchronized( contextLock ) {
                            Context context = bag.getJndiContext();
                            return context.lookup( name );
                        }
                    }
                });
            } finally {
                returnJmsBag(jmsBag);
            }
        }

        protected Queue getFailureQueue(Context context) throws NamingException, JmsConfigException, JMSException, JmsRuntimeException {
            if ( endpointConfig.isTransactional() && endpointConfig.getEndpoint().getFailureDestinationName() != null) {
                logger.finest( "Getting new FailureQueue" );
                final String failureDestinationName = endpointConfig.getEndpoint().getFailureDestinationName();
                return JmsUtil.cast( context.lookup( failureDestinationName ), Queue.class );
            }
            return null;
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
            //check if no one references the cached session or if the endpoint is inbound which we have to clean up anyways
            if ( references <= 0 || endpointConfig.getEndpoint().isMessageSource()) {
                logger.log(
                        Level.FINE,
                        "Closing JMS connection ({0}), version {1}:{2}",
                        new Object[]{
                                name, connectionVersion, endpointVersion
                        });
                try {
                    pool.close();
                } catch (Exception e) {
                    //Ignore if we can't close it.
                }
                bag.close();
            }
        }
    }

    /**
     * Bean for cache configuration.
     */
    private static final class JmsResourceManagerConfig {
        private final long maximumAge;
        private final long maximumIdleTime;
        private final int maximumSize;

        private JmsResourceManagerConfig( final long maximumAge,
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
        private final ConcurrentHashMap<JmsEndpointConfig.JmsEndpointKey, CachedConnection> connectionHolder;
        private final AtomicReference<JmsResourceManagerConfig> cacheConfigReference;

        private CacheCleanupTask( final ConcurrentHashMap<JmsEndpointConfig.JmsEndpointKey, CachedConnection> connectionHolder,
                                  final AtomicReference<JmsResourceManagerConfig> cacheConfigReference ) {
            this.connectionHolder = connectionHolder;
            this.cacheConfigReference = cacheConfigReference;
        }

        @Override
        public void run() {
            final JmsResourceManagerConfig cacheConfig = cacheConfigReference.get();
            final long timeNow = System.currentTimeMillis();
            final int overSize = connectionHolder.size() - cacheConfig.maximumSize;
            final Set<Map.Entry<JmsEndpointConfig.JmsEndpointKey,CachedConnection>> evictionCandidates =
                new TreeSet<Map.Entry<JmsEndpointConfig.JmsEndpointKey,CachedConnection>>(
                    new Comparator<Map.Entry<JmsEndpointConfig.JmsEndpointKey,CachedConnection>>(){
                        @Override
                        public int compare( final Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection> e1,
                                            final Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection> e2 ) {
                            return Long.valueOf( e1.getValue().createdTime ).compareTo( e2.getValue().createdTime );
                        }
                    }
                );

            for ( final Map.Entry<JmsEndpointConfig.JmsEndpointKey,CachedConnection> cachedConnectionEntry : connectionHolder.entrySet() ) {
                if(cachedConnectionEntry.getValue().endpointConfig.isEvictOnExpired()) { //do not evict inbound jms connections
                    if ( (timeNow-cachedConnectionEntry.getValue().createdTime) > cacheConfig.maximumAge && cacheConfig.maximumAge > 0 ) {
                    evict( connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue() );
                    } else if ( (timeNow-cachedConnectionEntry.getValue().lastAccessTime.get()) > cacheConfig.maximumIdleTime && cacheConfig.maximumIdleTime > 0) {
                        evict( connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue() );
                    } else if ( overSize > 0 ) {
                        evictionCandidates.add( cachedConnectionEntry );
                    }
                }
            }

            // evict oldest first to reduce cache size
            final Iterator<Map.Entry<JmsEndpointConfig.JmsEndpointKey,CachedConnection>> evictionIterator = evictionCandidates.iterator();
            for ( int i=0; i<overSize && evictionIterator.hasNext(); i++ ) {
                final Map.Entry<JmsEndpointConfig.JmsEndpointKey,CachedConnection> cachedConnectionEntry =
                        evictionIterator.next();
                evict( connectionHolder, cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue() );
            }
        }
    }
}
