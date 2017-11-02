package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache entry
 */
public class JmsSessionHolder {

    private static final Logger logger = Logger.getLogger(JmsSessionHolder.class.getName());
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
    private final JmsResourceManagerConfig cacheConfig;
    private final int sessionPoolMaxActive;

    protected JmsSessionHolder(final JmsEndpointConfig cfg,
                               final JmsBag bag,
                               JmsResourceManagerConfig cacheConfig) {
        this.bag = bag;
        this.name = cfg.getJmsEndpointKey().toString();
        this.endpointVersion = cfg.getEndpoint().getVersion();
        this.connectionVersion = cfg.getConnection().getVersion();
        this.endpointConfig = cfg;
        this.cacheConfig = cacheConfig;
        this.sessionPoolMaxActive = getSessionPoolSize();
        if(sessionPoolMaxActive != 0) {
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
        else {
            this.pool = null;//no pool needed
        }
    }

    protected JmsBag makeJmsBag() throws Exception {
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
                    bag.getConnection(), session, consumer, producer, this );
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

    public String getName() {
        return name;
    }

    public int getEndpointVersion() {
        return endpointVersion;
    }

    public int getConnectionVersion() {
        return connectionVersion;
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
        JmsBag jmsBag = null;
        touch();
        try {
            if(sessionPoolMaxActive != 0) {
                jmsBag = pool.borrowObject();
            }
            else {
                logger.log(Level.FINEST, "Session pool is "+ sessionPoolMaxActive + ". Creating new JMS Session.");
                jmsBag = makeJmsBag();
            }
        } catch (JMSException e) {
            throw new JmsRuntimeException(e);
        } catch (NamingException e) {
            throw e;
        } catch (JmsConfigException e) {
            throw new JmsRuntimeException(e);
        } catch (Exception e) {
            throw new JmsRuntimeException(e);
        }
        return jmsBag;
    }

    /**
     * Return a JmsBag to the Cached Connection, the JmsBag can be reuse by other Thread
     * @param jmsBag The bag return to the pool
     */
    public void returnJmsBag(JmsBag jmsBag) {
        if (jmsBag != null) {
            if(sessionPoolMaxActive != 0) {
                try {
                    pool.returnObject(jmsBag);
                } catch (Exception e) {
                    jmsBag.closeSession();
                }
            }
            else {
                logger.log(Level.FINEST, "Session pool is "+ sessionPoolMaxActive + ". Closing JMS Session.");
                jmsBag.closeSession();
            }
        }
    }

    /**
     * Caller must hold reference.
     */
    public void doWithJmsResources( final JmsResourceManager.JmsResourceCallback callback ) throws JMSException, JmsRuntimeException, NamingException {
        JmsBag jmsBag = null;
        try {
            try {
                jmsBag = borrowJmsBag();
            } catch (NamingException e) {
                throw e;
            } catch ( Throwable t ) {
                throw new JmsRuntimeException(t);
            }

            callback.doWork( jmsBag, new JmsResourceManager.JndiContextProvider(){
                @Override
                public void doWithJndiContext( final JmsResourceManager.JndiContextCallback contextCallback ) throws NamingException  {
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
            close();
        }
    }

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
        logger.log(Level.FINE, "Closed connection " + name);
    }

    public int refCount() {
        return referenceCount.get();
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public AtomicLong getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public String toString() {
        return String.format(
                "JMS connection (%s), version %s:%s",
                 name, connectionVersion, endpointVersion
        );
    }
}
