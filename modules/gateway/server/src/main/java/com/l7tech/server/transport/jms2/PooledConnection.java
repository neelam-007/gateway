package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.naming.NamingException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PooledConnection implements CachedConnection {
    private static final Logger logger = Logger.getLogger(PooledConnection.class.getName());

    private final JmsResourceManagerConfig cacheConfig;
    private final GenericObjectPool.Config config;
    private final GenericObjectPool<JmsSessionHolder> pool;
    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final JmsEndpointConfig endpoint;
    private final long createdTime = System.currentTimeMillis();
    private AtomicLong lastAccessTime = new AtomicLong(createdTime);

    public PooledConnection(final JmsEndpointConfig endpointConfig, JmsResourceManagerConfig cacheConfig) throws  Exception{
        logger.log(Level.FINE, "Creating new PooledConnection object...");
        this.endpoint = endpointConfig;
        this.cacheConfig = cacheConfig;
        //set pool config initial properties. They are used even if the pool size is 0
        config = new GenericObjectPool.Config();
        config.maxActive = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_SIZE,
                String.valueOf(cacheConfig.getConnectionPoolSize())));
        config.minEvictableIdleTimeMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_MAX_AGE,
                String.valueOf(cacheConfig.getMaximumIdleTime())));
        //set other pool properties
        config.maxIdle = config.maxActive;
        config.minIdle = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_MIN_IDLE,
                String.valueOf(cacheConfig.getConnectionMinIdle())));

        config.maxWait = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_MAX_WAIT,
                String.valueOf(cacheConfig.getConnectionMaxWait())));

        config.timeBetweenEvictionRunsMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_EVICT_INTERVAL,
                String.valueOf(cacheConfig.getTimeBetweenEviction())));
        config.numTestsPerEvictionRun = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_EVICT_BATCH_SIZE,
                String.valueOf(cacheConfig.getEvictionBatchSize())));
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;

        config.softMinEvictableIdleTimeMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_IDLE_TIMEOUT,
                String.valueOf(cacheConfig.getIdleTime())));
        pool = new GenericObjectPool<>(new PoolableObjectFactory<JmsSessionHolder>() {
            @Override
            public JmsSessionHolder makeObject() throws Exception {
                return newConnection(endpoint);
            }

            @Override
            public void destroyObject(JmsSessionHolder connection) throws Exception {
                connection.unRef();
            }

            @Override
            public boolean validateObject(JmsSessionHolder connection) {
                return true;
            }

            @Override
            public void activateObject(JmsSessionHolder connection) throws Exception {
            }

            @Override
            public void passivateObject(JmsSessionHolder connection) throws Exception {

            }
        }, config);
    }

    @Override
    public synchronized JmsSessionHolder borrowConnection() throws JmsRuntimeException {
        try {
            touch();
            JmsSessionHolder sessionHolder = pool.borrowObject();
            if (sessionHolder == null) {
                logger.log(Level.FINE, "Unable to borrow JmsSessionHolder from the pool!");
                throw new NoSuchElementException("Unable to borrow");
            }
            return sessionHolder;
        } catch (NoSuchElementException nse) {
            logger.log(Level.FINE, "Max Wait expired!");
            throw nse;
        } catch ( Exception e ) {
            logger.log(Level.FINEST, "Unable to borrow connection", e);
            throw new JmsRuntimeException(e);
        }
    }

    @Override
    public void returnConnection(JmsSessionHolder connection) throws JmsRuntimeException {
        try {
            pool.returnObject(connection);
        } catch (Exception e) {
            logger.log(Level.FINEST, "Unable to return connection", e);
            throw new JmsRuntimeException(e);
        }
    }

    @Override
    public JmsEndpointConfig getEndpointConfig() {
        return endpoint;
    }

    @Override
    public void close() {
        try {
            pool.close();
        } catch (Exception e) {
            logger.log(Level.FINE, "Unable to close the pool: ", e);
        }
    }

    @Override
    public long getCreatedTime() {
        return createdTime;
    }

    @Override
    public void touch() {
            lastAccessTime.set(System.currentTimeMillis());
    }

    @Override
    public AtomicLong getLastAccessTime() {
            return lastAccessTime;
    }

    private JmsSessionHolder newConnection(final JmsEndpointConfig endpoint ) throws NamingException, JmsRuntimeException {
        final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();

        try {
            // create the new JmsBag for the endpoint
            final JmsBag newBag = JmsUtil.connect(endpoint);
            newBag.getConnection().start();

            // create new cached connection wrapper
            final JmsSessionHolder newConn = new JmsSessionHolder(endpoint, newBag, cacheConfig);
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

    @Override
    public void invalidate(JmsSessionHolder connection) throws Exception {
        if(connection != null)
            pool.invalidateObject(connection);
     }

    @Override
    public boolean isDisconnected() {
        return pool.isClosed() || (pool.getNumActive() == 0 && pool.getNumIdle() == 0);
    }

    @Override
    public boolean isActive() {
        return pool.getNumActive() > 0;
    }

    @Override
    public boolean isIdleTimeoutExpired() {
        return (!isActive()) && (System.currentTimeMillis() - getLastAccessTime().get() > config.minEvictableIdleTimeMillis && config.minEvictableIdleTimeMillis > 0);
    }

    @Override
    public void debugStatus() {
        logger.log(Level.FINE, "Active: " + pool.getNumActive() + " Idle: " + pool.getNumIdle());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : " + endpoint.getDisplayName();
    }
}
