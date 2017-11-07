package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SessionHolderBase implements SessionHolder {
    private final Logger logger = Logger.getLogger(SessionHolderBase.class.getName());
    protected final AtomicInteger referenceCount = new AtomicInteger(0);
    protected final JmsBag bag;
    protected final String name;
    protected final int endpointVersion;
    protected final int connectionVersion;
    protected final JmsEndpointConfig endpointConfig;
    private final long createdTime = System.currentTimeMillis();
    private final AtomicLong lastAccessTime = new AtomicLong(createdTime);
    private final Object contextLock = new Object();

    public SessionHolderBase(final JmsEndpointConfig cfg, final JmsBag bag) {
        this.connectionVersion = cfg.getConnection().getVersion();
        this.endpointConfig = cfg;
        this.bag = bag;
        this.name = cfg.getJmsEndpointKey().toString();
        this.endpointVersion = cfg.getEndpoint().getVersion();
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getEndpointVersion() {
        return endpointVersion;
    }

    @Override
    public int getConnectionVersion() {
        return connectionVersion;
    }

    /**
     * Update the lastAccessTime, to keep the connection alive.
     */
    @Override
    public void touch() {
        lastAccessTime.set( System.currentTimeMillis() );
    }

    /**
     * Caller must hold reference.
     */
    @Override
    public void doWithJmsResources(final JmsResourceManager.JmsResourceCallback callback) throws JMSException, JmsRuntimeException, NamingException {
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
    @Override
    public boolean ref() {
        return referenceCount.getAndIncrement() > 0;
    }

    @Override
    public void unRef() {
        int references = referenceCount.decrementAndGet();
        //check if no one references the cached session or if the endpoint is inbound which we have to clean up anyways
        if ( references <= 0 || endpointConfig.getEndpoint().isMessageSource()) {
            close();
        }
    }

    @Override
    public int refCount() {
        return referenceCount.get();
    }

    @Override
    public long getCreatedTime() {
        return createdTime;
    }

    @Override
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
