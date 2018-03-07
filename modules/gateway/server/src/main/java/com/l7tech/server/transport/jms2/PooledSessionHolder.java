package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.jms.*;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache entry
 */
public class PooledSessionHolder extends SessionHolderBase {
    private static final Logger logger = Logger.getLogger(PooledSessionHolder.class.getName());

    protected final GenericObjectPool<JmsBag> pool;
    private final JmsResourceManagerConfig cacheConfig;

    protected PooledSessionHolder(final JmsEndpointConfig cfg,
                                  final JmsBag bag,
                                  JmsResourceManagerConfig cacheConfig) {
        super(cfg, bag);
        this.cacheConfig = cacheConfig;
            this.pool = new GenericObjectPool<JmsBag>(new PoolableObjectFactory<JmsBag>() {
                @Override
                public JmsBag makeObject() throws Exception {
                    return makeJmsBag();
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
            }, getSessionPoolSize(), GenericObjectPool.WHEN_EXHAUSTED_BLOCK, getSessionPoolMaxWait(), getMaxSessionIdle());
    }

    protected int getSessionPoolSize() {
        if  (endpointConfig.getEndpoint().isMessageSource()) {
            return -1;
        } else {
            return Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_SESSION_POOL_SIZE,
                    String.valueOf(cacheConfig.getSessionPoolSize())));
        }
    }

    protected int getMaxSessionIdle() {
        if  (endpointConfig.getEndpoint().isMessageSource()) {
            return -1;
        } else {
            return Integer.parseInt(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_MAX_SESSION_IDLE,
                    String.valueOf(cacheConfig.getSessionMaxIdle())));
        }
    }

    protected long getSessionPoolMaxWait() {
        if (endpointConfig.getEndpoint().isMessageSource()) {
            //The pool is never exhausted, just return a number, it should be ignored.
            return 0;
        } else {
            return Long.parseLong(endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_SESSION_POOL_MAX_WAIT,
                    String.valueOf(cacheConfig.getSessionMaxWait())));
        }
    }

    @Override
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

    @Override
    public void returnJmsBag(JmsBag jmsBag) {
        if (jmsBag != null) {
            try {
                pool.returnObject(jmsBag);
            } catch (Exception e) {
                jmsBag.closeSession();
            }
        }
    }

    @Override
    public void close() {
        logger.log(
                Level.FINE,
                "Closing JMS connection ({0}), version {1}:{2}",
                new Object[]{
                        name, connectionVersion, endpointVersion
                });
        try {
            if(pool != null)
                pool.close();
        } catch (Exception e) {
            logger.log(Level.FINE, "Unable to close the connection pool");
        }
        bag.close();
        //reset referenceCount
        referenceCount.set(0);
    }

}
