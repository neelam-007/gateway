package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.TimeUnit;
import org.springframework.beans.factory.DisposableBean;

import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.NamingException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
        this.cleanupTask = new CacheCleanupTask(connectionHolder, cacheConfigReference );
        timer.schedule( cleanupTask, 17371, CACHE_CLEAN_INTERVAL );
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
                                    final JmsResourceCallback callback ) throws NamingException, JMSException, JmsRuntimeException, NoSuchElementException {
        if ( !active.get() ) throw new JmsRuntimeException("JMS resource manager is stopped.");

        JmsSessionHolder sessionHolder = null;
        try {
            sessionHolder = getSessionHolder(endpoint);
            sessionHolder.doWithJmsResources( callback );
        } finally {
            try {
                returnSessionHolder(endpoint, sessionHolder);
            } catch (JmsRuntimeException e) {
                //not much we can do. log it
                logger.log(Level.WARNING, "Unable to return cached connection " + sessionHolder);
            }
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

    protected JmsSessionHolder getSessionHolder(JmsEndpointConfig endpoint) throws JmsRuntimeException {
        final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();
        CachedConnection conn = connectionHolder.get(key);
        if(conn == null) {
            //create pool of connections and add it to the connection holder
            synchronized (key.toString().intern()) {
                conn = connectionHolder.get(key);
                if (conn == null)  {
                    try {
                        if(!endpoint.getEndpoint().isMessageSource()
                                && Boolean.valueOf(endpoint.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_ENABLE, Boolean.FALSE.toString()))) {
                            conn = new PooledConnection(endpoint, cacheConfigReference.get());
                        }
                        else {
                            conn = new SingleConnection(endpoint, cacheConfigReference.get());
                        }
                    } catch ( Exception e ) {
                        throw new JmsRuntimeException(e);
                    }
                    connectionHolder.put(key, conn);
                    logger.log(Level.FINE, "Created new CachedConnection " + conn.toString());
                }
            }
        }
        return conn.borrowConnection();
    }


    protected void returnSessionHolder(JmsEndpointConfig endpoint, JmsSessionHolder connection) throws JmsRuntimeException{
        try {
            final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();
            CachedConnection conn = connectionHolder.get(key);
            if(conn != null) {
                conn.returnConnection(connection);
            }
            else {
                logger.log(Level.WARNING, "JMS Connection with the key " + key.toString() + " does not exist.");
            }
        } catch ( Exception ex ) {
            throw new JmsRuntimeException(ex);
        }

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
        JmsSessionHolder sessionHolder = getSessionHolder(endpointConfig);
        return sessionHolder.borrowJmsBag();
    }

    /**
     * Return a JmsBag to the Cached Connection, the JmsBag can be reuse by other thread.
     * @param bag The bag return to the JmsSession, Caller should not change any state of JmsBag.
     * @throws JmsRuntimeException If error occur when returning the JmsBag
     */
    public void returnJmsBag(JmsBag bag) throws JmsRuntimeException {
        JmsSessionHolder sessionHolder = (JmsSessionHolder) bag.getBagOwner();
        if (sessionHolder != null) {
            sessionHolder.returnJmsBag(bag);
            sessionHolder.unRef();
        }
    }

    /**
     * Touch the connection and keep the connection active.
     *
     * @param bag The configuration endpoint to loopup the Cached Connection
     * @throws JmsRuntimeException If error when touching the Cached Connection.
     */
    public void touch(final JmsBag bag) throws JmsRuntimeException {
        JmsSessionHolder sessionHolder = (JmsSessionHolder) bag.getBagOwner();
        if (sessionHolder != null) {
            sessionHolder.touch();
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
    public synchronized void invalidate( final JmsEndpointConfig endpoint ) {
        if ( active.get() ) {
            final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();
            final CachedConnection conn = connectionHolder.get(key);
            if(conn != null) {
                if (!conn.isActive()) {
                    logger.log(Level.FINE, "Removing CachedConnection with key " + key);
                    conn.close();
                    connectionHolder.remove(key);
                }
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
    private static final long DEFAULT_CONNECTION_IDLE_TIME = TimeUnit.MINUTES.toMillis(2);
    private static final int DEFAULT_CONNECTION_CACHE_SIZE = 100;

    private static final String PROP_CACHE_CLEAN_INTERVAL = "com.l7tech.server.transport.jms.cacheCleanInterval";
    private static final long CACHE_CLEAN_INTERVAL = ConfigFactory.getLongProperty( PROP_CACHE_CLEAN_INTERVAL, 27937L );

    // need to store one connection per endpoint
    private final Config config;
    protected final ConcurrentHashMap<JmsEndpointConfig.JmsEndpointKey, CachedConnection> connectionHolder;
    private final Timer timer;
    private final TimerTask cleanupTask;
    private final AtomicReference<JmsResourceManagerConfig> cacheConfigReference = new AtomicReference<JmsResourceManagerConfig>( new JmsResourceManagerConfig(0L,0L, 0L,1,1, 1,5000L, 300000L, 1, 0, 0, 0L) );
    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * Clear all connection references
     */
    private void doShutdown() {
        logger.info( "Shutting down JMS Connection cache." );

        timer.cancel();
        Collection<CachedConnection> poolList = connectionHolder.values();

        for ( CachedConnection pooledConnection : poolList ) {
            pooledConnection.close();
        }

        connectionHolder.clear();
    }

    private void updateConfig() {
        logger.config( "(Re)loading cache configuration." );

        final long maximumAge = config.getTimeUnitProperty( "ioJmsConnectionCacheMaxAge", DEFAULT_CONNECTION_MAX_AGE );
        final long maximumIdleTime = config.getTimeUnitProperty( "ioJmsConnectionCacheMaxIdleTime", DEFAULT_CONNECTION_MAX_IDLE );
        final long idleTime = config.getTimeUnitProperty("ioJmsConnectionIdleTime", DEFAULT_CONNECTION_IDLE_TIME);
        final int maximumSize = config.getIntProperty( "ioJmsConnectionCacheSize", DEFAULT_CONNECTION_CACHE_SIZE );
        final int maximumPoolSize = config.getIntProperty("ioJmsConnectionPoolSize", JmsConnection.DEFAULT_CONNECTION_POOL_SIZE);
        final int minimumConnectionIdle = config.getIntProperty("ioJmsConnectionMinIdle", JmsConnection.DEFAULT_CONNECTION_POOL_SIZE);
        final long maximumWait = config.getTimeUnitProperty("ioJmsConnectionMaxWait", JmsConnection.DEFAULT_CONNECTION_POOL_MAX_WAIT);
        final long timeBetweenEvictions = config.getTimeUnitProperty("ioJmsConnectionTimeBetweenEviction", JmsConnection.DEFAULT_CONNECTION_POOL_EVICT_INTERVAL);
        final int evictionBatchSize = config.getIntProperty("ioJmsConnectionEvictionBatchSize", JmsConnection.DEFAULT_CONNECTION_POOL_SIZE);
        final int sessionPoolSize = config.getIntProperty("ioJmsSessionPoolSize", JmsConnection.DEFAULT_SESSION_POOL_SIZE);
        final int sessionMaxIdle = config.getIntProperty("ioJmsSessionMaxIdle", JmsConnection.DEFAULT_SESSION_POOL_SIZE);
        final long sessionMaxWait = config.getTimeUnitProperty("ioJmsSessionMaxWait", JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT);

        cacheConfigReference.set(
                new JmsResourceManagerConfig(
                rangeValidate(maximumAge, DEFAULT_CONNECTION_MAX_AGE, 0L, Long.MAX_VALUE, "JMS Connection Maximum Age" ),
                rangeValidate(maximumIdleTime, DEFAULT_CONNECTION_MAX_IDLE, 0L, Long.MAX_VALUE, "JMS Connection Maximum Idle" ),
                rangeValidate(idleTime, DEFAULT_CONNECTION_IDLE_TIME, 0L, Long.MAX_VALUE, "JMS Pool Soft Idle Timeout"),
                rangeValidate(maximumSize, DEFAULT_CONNECTION_CACHE_SIZE, 0, Integer.MAX_VALUE, "JMS Connection Cache Size" ),
                rangeValidate(maximumPoolSize, JmsConnection.DEFAULT_CONNECTION_POOL_SIZE, 1, 10000, "JMS Connection Pool Size" ),
                rangeValidate(minimumConnectionIdle, JmsConnection.DEFAULT_CONNECTION_POOL_SIZE, 0, 10000, "JMS Connection Min Idle"),
                rangeValidate(maximumWait, JmsConnection.DEFAULT_CONNECTION_POOL_MAX_WAIT, -1L, Long.MAX_VALUE, "JMS Connection Maximum Wait"),
                rangeValidate(timeBetweenEvictions, JmsConnection.DEFAULT_CONNECTION_POOL_EVICT_INTERVAL, 0L, Long.MAX_VALUE, "JMS Connection Pool Time Between Eviction"),
                rangeValidate(evictionBatchSize, JmsConnection.DEFAULT_CONNECTION_POOL_SIZE, 1, 10000, "JMS Connection Eviction Batch Size"),
                rangeValidate(sessionPoolSize, JmsConnection.DEFAULT_SESSION_POOL_SIZE, -1, Integer.MAX_VALUE, "JMS Session Pool Size"),
                rangeValidate(sessionMaxIdle, JmsConnection.DEFAULT_SESSION_POOL_SIZE, -1, Integer.MAX_VALUE, "JMS Session Max Idle"),
                rangeValidate(sessionMaxWait, JmsConnection.DEFAULT_SESSION_POOL_MAX_WAIT, -1L, Long.MAX_VALUE, "JMS Session Max Wait"))
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
     * Timer task to remove idle, expired or surplus connections from the cache.
     *
     * <p>When the cache size is exceeded the oldest JMS connections are
     * removed first.</p>
     */
    private static final class CacheCleanupTask extends TimerTask {
        private final ConcurrentHashMap<JmsEndpointConfig.JmsEndpointKey, CachedConnection> connectionHolder;
        private final AtomicReference<JmsResourceManagerConfig> cacheConfigReference;

        private CacheCleanupTask(final ConcurrentHashMap<JmsEndpointConfig.JmsEndpointKey, CachedConnection> connectionHolder,
                                 final AtomicReference<JmsResourceManagerConfig> cacheConfigReference) {
            this.connectionHolder = connectionHolder;
            this.cacheConfigReference = cacheConfigReference;
        }

        @Override
        public void run() {
            logger.log(Level.FINE, "Running CacheCleanupTask...");
            final JmsResourceManagerConfig cacheConfig = cacheConfigReference.get();
            final long timeNow = System.currentTimeMillis();
            final int overSize = connectionHolder.size() - cacheConfig.getMaximumSize();
            final Set<Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection>> evictionCandidates =
                    new TreeSet<Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection>>(
                            new Comparator<Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection>>() {
                                @Override
                                public int compare(final Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection> e1,
                                                   final Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection> e2) {
                                    return Long.valueOf(e1.getValue().getCreatedTime()).compareTo(e2.getValue().getCreatedTime());
                                }
                            }
                    );

            for (final JmsEndpointConfig.JmsEndpointKey key : connectionHolder.keySet()) {
                CachedConnection evictionCandidate = connectionHolder.get(key);
                logger.log(Level.FINE, "Check eviction candidate " + evictionCandidate.toString());
                if(logger.isLoggable(Level.FINE)) {
                    evictionCandidate.debugStatus();
                }
                if (evictionCandidate.getEndpointConfig().isEvictOnExpired()) { //do not evict inbound jms connections
                    if ( (timeNow-evictionCandidate.getCreatedTime()) > cacheConfig.getMaximumAge() && cacheConfig.getMaximumAge() > 0 ) {
                        logger.log(Level.FINE, "Maximum age expired for " + evictionCandidate.toString());
                        if(!evictionCandidate.isActive()) {
                            logger.log(Level.FINE, "Remove unused JMS connection "  + evictionCandidate.toString());
                            evictionCandidate.close();
                            connectionHolder.remove(key);
                        }
                    }
                    else if(evictionCandidate.isIdleTimeoutExpired()) {
                        logger.log(Level.FINE, "Remove expired idle JMS connection "  + evictionCandidate.toString());
                        evictionCandidate.close();
                        connectionHolder.remove(key);
                    }
                    else if (overSize > 0) {
                        evictionCandidates.add(new AbstractMap.SimpleEntry<>(key, evictionCandidate));
                    }

                }

                // evict oldest first to reduce cache size
                final Iterator<Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection>> evictionIterator = evictionCandidates.iterator();
                for (int i = 0; i < overSize && evictionIterator.hasNext(); i++) {
                    final Map.Entry<JmsEndpointConfig.JmsEndpointKey, CachedConnection> cachedConnectionEntry =
                            evictionIterator.next();
                    CachedConnection connection = cachedConnectionEntry.getValue();
                    if(!connection.isDisconnected()) {
                        logger.log(Level.FINE, "Remove unused JMS connection "  + connection.toString());
                        connection.close();
                        connectionHolder.remove(cachedConnectionEntry.getKey(), cachedConnectionEntry.getValue());
                    }
                }
            }
        }
    }
}
