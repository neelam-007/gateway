package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;

import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleConnection implements CachedConnection {

    private static final Logger logger = Logger.getLogger(SingleConnection.class.getName());

    private final SessionHolderBase singleConnection;
    private final JmsEndpointConfig endpoint;
    private final long minEvictableIdleTimeMillis;
    private final JmsResourceManagerConfig cacheConfig;

    public SingleConnection(final JmsEndpointConfig endpointConfig, JmsResourceManagerConfig cacheConfig) throws  Exception{
        this.endpoint = endpointConfig;
        this.cacheConfig = cacheConfig;
        this.singleConnection = newConnection(endpointConfig);
        this.minEvictableIdleTimeMillis = Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_CONNECTION_MAX_AGE,
                String.valueOf(cacheConfig.getMaximumIdleTime())));
    }

    @Override
    public synchronized SessionHolderBase borrowConnection() throws JmsRuntimeException {
        touch();
        singleConnection.ref();
        return singleConnection;
    }

    @Override
    public synchronized void returnConnection(SessionHolder connection) throws JmsRuntimeException {
        singleConnection.unRef();
    }

    @Override
    public JmsEndpointConfig getEndpointConfig() {
        return endpoint;
    }

    @Override
    public void close() {
        //force to close connection and all sessions
        synchronized (singleConnection) {
            singleConnection.close();
        }
    }

    @Override
    public long getCreatedTime() {
        return singleConnection.getCreatedTime();
    }

    @Override
    public void touch() {
        singleConnection.touch();
    }

    @Override
    public AtomicLong getLastAccessTime() {
        return singleConnection.getLastAccessTime();
    }

    @Override
    public synchronized void invalidate(SessionHolder connection) throws Exception {
        singleConnection.close();
    }

    @Override
    public boolean isDisconnected() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isIdleTimeoutExpired() {
        return System.currentTimeMillis() - getLastAccessTime().get() > minEvictableIdleTimeMillis && minEvictableIdleTimeMillis > 0;
    }

    @Override
    public void debugStatus() {
        logger.log(Level.FINE, "Active: " + singleConnection.refCount() + " references " + endpoint.getDisplayName());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : " + endpoint.getDisplayName();
    }

    private PooledSessionHolder newConnection(final JmsEndpointConfig endpoint ) throws NamingException, JmsRuntimeException {
        final JmsEndpointConfig.JmsEndpointKey key = endpoint.getJmsEndpointKey();

        try {
            // create the new JmsBag for the endpoint
            final JmsBag newBag = JmsUtil.connect(endpoint);
            newBag.getConnection().start();

            // create new cached connection wrapper
            final PooledSessionHolder newConn = new PooledSessionHolder(endpoint, newBag, cacheConfig);
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
}
