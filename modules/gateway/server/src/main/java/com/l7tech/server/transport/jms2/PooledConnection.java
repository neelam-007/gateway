package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PooledConnection {
    private static final Logger logger = Logger.getLogger(PooledConnection.class.getName());

    private final GenericObjectPool<CachedConnection> pool;
    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final JmsEndpointConfig endpoint;
    private final long createdTime = System.currentTimeMillis();

    public PooledConnection(final JmsEndpointConfig endpointConfig, JmsResourceManagerConfig cacheConfig) {
        this.endpoint = endpointConfig;
        GenericObjectPool.Config config = new GenericObjectPool.Config();
        config.maxActive = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_SIZE,
                String.valueOf(cacheConfig.getDefaultPoolSize())));
        config.maxIdle = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_MAX_IDLE,
                String.valueOf(cacheConfig.getDefaultPoolSize())));
        config.maxWait = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_MAX_WAIT,
                String.valueOf(cacheConfig.getDefaultWait())));
        config.minEvictableIdleTimeMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_MAX_AGE,
                String.valueOf(cacheConfig.getMaximumIdleTime())));
        config.timeBetweenEvictionRunsMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_EVICT_INTERVAL,
                String.valueOf(cacheConfig.getTimeBetweewnEviction())));
        config.numTestsPerEvictionRun = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_EVICT_BATCH_SIZE,
                String.valueOf(cacheConfig.getDefaultPoolSize())));
        pool = new GenericObjectPool<>(new PoolableObjectFactory<CachedConnection>() {
            @Override
            public CachedConnection makeObject() throws Exception {
                return newConnection(endpoint);
            }

            @Override
            public void destroyObject(CachedConnection connection) throws Exception {
                connection.unRef();
            }

            @Override
            public boolean validateObject(CachedConnection connection) {
                return true;
            }

            @Override
            public void activateObject(CachedConnection connection) throws Exception {
            }

            @Override
            public void passivateObject(CachedConnection connection) throws Exception {

            }
        },config);
    }

    public CachedConnection borrowConnection() throws JmsRuntimeException {
        try {
            return pool.borrowObject();
        } catch ( Exception e ) {
            throw new JmsRuntimeException(e);
        }
    }

    public void returnConnection(CachedConnection connection) throws JmsRuntimeException {
        try {
            pool.returnObject(connection);
        } catch ( Exception e ) {
            throw new JmsRuntimeException(e);
        }
    }

    public JmsEndpointConfig getEndpointConfig() {
        return endpoint;
    }

    /**
     * Once a connection is in the cache a return of false from this
     * method indicates that the connection is invalid and should not
     * be used.
     */
    /*public boolean ref() {
        return referenceCount.getAndIncrement() > 0;
    }*/

    public boolean unRef() {
        int references = referenceCount.decrementAndGet();
        //check if no one references the cached session or if the endpoint is inbound which we have to clean up anyways
        if ( references <= 0 || endpoint.getEndpoint().isMessageSource()) {
            logger.log(
                    Level.FINE,
                    "Closing pooled connection ", this.toString());
            try {
                pool.clear();
                pool.close();
            } catch (Exception e) {
                //Ignore if we can't close it.
            }
            return false;
        }
        return true;
    }

    public void close() {
        try {
            pool.clear();
            pool.close();
        } catch (Exception e) {
            //Ignore if we can't close it.
        }
    }

    public long getCreatedTime() {
        return createdTime;
    }

    private CachedConnection newConnection(final JmsEndpointConfig endpoint ) throws NamingException, JmsRuntimeException {
        final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();

        try {
            // create the new JmsBag for the endpoint
            final JmsBag newBag = JmsUtil.connect(endpoint);
            newBag.getConnection().start();

            // create new cached connection wrapper
            final CachedConnection newConn = new CachedConnection(endpoint, newBag);
            newConn.ref(); // referenced by caller

            logger.log(Level.FINE, "New JMS connection created ({0}), version {1}:{2}", new Object[] {
                    newConn.getName(), newConn.getConnectionVersion(), newConn.getEndpointVersion()
            });

            return newConn;
        }catch (NamingException ne){
            throw ne; //rethrow NamingException if it has been caught. Bug SSG-6792
        } catch ( Throwable t ) {
            throw new JmsRuntimeException(t);
        }
    }

    public void invalidate(CachedConnection connection) throws Exception {
        pool.invalidateObject(connection);
    }

    public boolean isPoolEmpty() {
        return pool.isClosed() || (pool.getNumActive() == 0 && pool.getNumIdle() == 0);
    }

    public void debugPoolStatus() {
        logger.log(Level.FINE, "Active: " + pool.getNumActive() + " Idle: " + pool.getNumIdle());
    }

    @Override
    public String toString() {
        return endpoint.getDisplayName();
    }
}
