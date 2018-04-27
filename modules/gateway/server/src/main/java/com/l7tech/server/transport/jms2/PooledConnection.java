package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.naming.NamingException;
import javax.validation.constraints.NotNull;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PooledConnection implements CachedConnection {
    private static final Logger logger = Logger.getLogger(PooledConnection.class.getName());

    private final GenericObjectPool.Config config;
    protected final GenericObjectPool<SessionHolder> pool;
    private final JmsEndpointConfig endpoint;
    private final long createdTime = System.currentTimeMillis();
    private AtomicLong lastAccessTime = new AtomicLong(createdTime);

    public PooledConnection(@NotNull final JmsEndpointConfig endpointConfig, @NotNull JmsResourceManagerConfig cacheConfig) {
        logger.log(Level.FINE, "Creating new PooledConnection object...");
        this.endpoint = endpointConfig;
        //set pool config initial properties. They are used even if the pool size is 0
        config = new GenericObjectPool.Config();
        config.maxActive = Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_POOL_SIZE,
                String.valueOf(cacheConfig.getConnectionPoolSize())));
        config.minEvictableIdleTimeMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_EVICTABLE_TIME,
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
        pool = new GenericObjectPool<>(new PoolableObjectFactory<SessionHolder>() {
            @Override
            public SessionHolder makeObject() throws Exception {
                return newConnection(endpoint);
            }

            @Override
            public void destroyObject(SessionHolder connection) throws Exception {
                connection.unRef();
            }

            @Override
            public boolean validateObject(SessionHolder connection) {
                return true;
            }

            @Override
            public void activateObject(SessionHolder connection) throws Exception {
            }

            @Override
            public void passivateObject(SessionHolder connection) throws Exception {

            }
        }, config);
    }

    @Override
    public synchronized SessionHolder borrowConnection() throws JmsRuntimeException, JmsConnectionMaxWaitException {
        try {
            touch();
            SessionHolder sessionHolder = pool.borrowObject();
            if (sessionHolder == null) {
                logger.log(Level.FINE, "Unable to borrow JMS session from the connection pool!");
                throw new NoSuchElementException("Unable to borrow");
            }
            return sessionHolder;
        } catch (NoSuchElementException nse) {
            logger.log(Level.FINE, "Max Wait expired!");
            throw new JmsConnectionMaxWaitException("Max Wait Expired", nse);
        } catch ( Exception e ) {
            logger.log(Level.FINE, "Unable to borrow connection", e);
            throw new JmsRuntimeException(e);
        }
    }

    @Override
    public void returnConnection(SessionHolder connection) throws JmsRuntimeException {
        try {
            if(connection != null) {
                pool.returnObject(connection);
            }
            else {
                logger.log(Level.FINE, "SessionHolder is null!");
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Unable to return connection", e);
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

    protected SessionHolder newConnection(final JmsEndpointConfig endpoint ) throws NamingException, JmsRuntimeException {
        try {
            // create the new JmsBag for the endpoint
            final JmsBag newBag = JmsUtil.connect(endpoint);
            newBag.getConnection().start();

            // create new cached connection wrapper
            final SingleSessionHolder newConn = (SingleSessionHolder) SessionHolderFactory.createSessionHolder(endpoint, newBag);
            newConn.ref(); // referenced by caller

            logger.log(Level.FINE, "New JMS connection created ({0}), {1}", new Object[] {
                    endpoint.getDisplayName(), newConn.getObjectReferenceName()
            });

            return newConn;
        }catch (NamingException ne){
            throw ne; //rethrow NamingException if it has been caught. Bug SSG-6792
        } catch ( Throwable t ) {
            throw new JmsRuntimeException(t);
        }
    }

    @Override
    public void invalidate(SessionHolder connection) throws Exception {
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
        logger.log(Level.FINE, "Active: " + pool.getNumActive() + " Idle: " + pool.getNumIdle() + " " + endpoint.getDisplayName());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : " + endpoint.getDisplayName();
    }
}
