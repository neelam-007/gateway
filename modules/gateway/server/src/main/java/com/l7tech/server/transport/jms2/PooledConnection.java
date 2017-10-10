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

    private final GenericObjectPool.Config config;
    private final GenericObjectPool<CachedConnection> pool;
    private final boolean isPooled;
    private final CachedConnection singleConnection;
    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final JmsEndpointConfig endpoint;
    private final long createdTime = System.currentTimeMillis();

    public PooledConnection(final JmsEndpointConfig endpointConfig, JmsResourceManagerConfig cacheConfig) throws  Exception{
        this.endpoint = endpointConfig;
        //set pool config initial properties. They are used even if the pool size is 0
        config = new GenericObjectPool.Config();
        config.maxActive = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_SIZE,
                String.valueOf(cacheConfig.getDefaultPoolSize())));
        config.minEvictableIdleTimeMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_MAX_AGE,
                String.valueOf(cacheConfig.getMaximumIdleTime())));

        isPooled = !endpointConfig.getEndpoint().isMessageSource() && Boolean.valueOf(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_ENABLE, Boolean.FALSE.toString()));

        if(isPooled) {
            //set other pool properties
            config.maxIdle = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_MAX_IDLE,
                    String.valueOf(cacheConfig.getDefaultPoolSize())));
            config.maxWait = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_MAX_WAIT,
                    String.valueOf(cacheConfig.getDefaultWait())));

            config.timeBetweenEvictionRunsMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_EVICT_INTERVAL,
                    String.valueOf(cacheConfig.getTimeBetweenEviction())));
            config.numTestsPerEvictionRun = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_EVICT_BATCH_SIZE,
                    String.valueOf(cacheConfig.getDefaultEvictionBatchSize())));
            config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
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
            }, config);
            singleConnection = null;
        }
        else {
            pool = null;
            singleConnection = newConnection(endpointConfig);
        }
    }

    public CachedConnection borrowConnection() throws JmsRuntimeException {
        if(isPooled) {
            try {
                return pool.borrowObject();
            } catch ( Exception e ) {
                throw new JmsRuntimeException(e);
            }
        }
        else {
            synchronized (singleConnection) {
                singleConnection.touch();
                singleConnection.ref();
                return singleConnection;
            }
        }
    }

    public void returnConnection(CachedConnection connection) throws JmsRuntimeException {
        if(isPooled) {
            try {
            pool.returnObject(connection);
            } catch (Exception e) {
                throw new JmsRuntimeException(e);
            }
        }
        else {
            synchronized (singleConnection) {
                singleConnection.unRef();
            }
        }
    }

    public JmsEndpointConfig getEndpointConfig() {
        return endpoint;
    }

    public void close() {
        if(isPooled) {
            try {
                pool.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "Unable to close the pool: ", e);
            }
        }
        else {
            //force to close connection and all sessions
            synchronized (singleConnection) {
                singleConnection.close();
            }
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
        if(isPooled) {
            return pool.isClosed() || (pool.getNumActive() == 0 && pool.getNumIdle() == 0);
        }
        else {
            return true;
        }
    }

    public boolean isIdleTimeoutExpired() {
        if(isPooled) {
            return isPoolEmpty();
        }
        else {
            return (System.currentTimeMillis() - singleConnection.getLastAccessTime().get() > config.minEvictableIdleTimeMillis && config.minEvictableIdleTimeMillis > 0);
        }
    }

    public void debugPoolStatus() {
        logger.log(Level.FINE, "Active: " + (isPooled? pool.getNumActive() : 1) + " Idle: " + (isPooled ? pool.getNumIdle(): 0));
    }

    @Override
    public String toString() {
        return endpoint.getDisplayName();
    }
}
