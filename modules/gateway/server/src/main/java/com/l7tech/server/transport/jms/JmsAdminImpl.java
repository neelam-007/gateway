package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.transport.jms.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.policy.variable.GatewaySecurePasswordReferenceExpander;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JmsAdminImpl implements JmsAdmin {
    private static final Logger logger = Logger.getLogger(JmsAdminImpl.class.getName());
    public static final int DEFAULT_JMS_CONSUMER_CONNECTIONS = 1;

    private final JmsConnectionManager jmsConnectionManager;
    private final JmsEndpointManager jmsEndpointManager;
    private final JmsPropertyMapper jmsPropertyMapper;
    private final Config config;

    public JmsAdminImpl(JmsConnectionManager jmsConnectionManager,
                        JmsEndpointManager jmsEndpointManager,
                        JmsPropertyMapper jmsPropertyMapper,
                        Config config) {
        this.jmsConnectionManager = jmsConnectionManager;
        this.jmsEndpointManager = jmsEndpointManager;
        this.jmsPropertyMapper = jmsPropertyMapper;
        this.config = config;
    }

    @Override
    public EnumSet<JmsProviderType> getProviderTypes() throws FindException {
        return jmsConnectionManager.findAllProviders();
    }

    /**
     * Finds all {@link JmsConnection}s in the database.
     *
     * @return an array of transient {@link JmsConnection}s
     * @throws FindException
     */
    @Override
    public JmsConnection[] findAllConnections() throws FindException {
        Collection<JmsConnection> found = jmsConnectionManager.findAll();
        if (found == null || found.size() < 1) return new JmsConnection[0];

        List<JmsConnection> results = new ArrayList<JmsConnection>();
        for (JmsConnection conn : found) {
            results.add(conn);
        }

        return results.toArray(new JmsConnection[results.size()]);
    }

    @Override
    public JmsAdmin.JmsTuple[] findAllTuples() throws FindException {
        ArrayList<JmsTuple> result = new ArrayList<JmsTuple>();
        Collection<JmsConnection> connections = jmsConnectionManager.findAll();
        for (JmsConnection connection : connections) {
            JmsEndpoint[] endpoints = jmsEndpointManager.findEndpointsForConnection(connection.getGoid());
            for (JmsEndpoint endpoint : endpoints) {
                result.add(new JmsTuple(connection, endpoint));
            }
        }

        return result.toArray(new JmsTuple[result.size()]);
    }

    /**
     * Finds the {@link JmsConnection} with the provided GOID.
     *
     * @return the {@link JmsConnection} with the provided GOID.
     * @throws FindException
     */
    @Override
    public JmsConnection findConnectionByPrimaryKey(Goid goid) throws FindException {
        return jmsConnectionManager.findByPrimaryKey(goid);
    }

    @Override
    public JmsEndpoint findEndpointByPrimaryKey(Goid goid) throws FindException {
        return jmsEndpointManager.findByPrimaryKey(goid);
    }

    @Override
    public void setEndpointMessageSource(Goid goid, boolean isMessageSource) throws FindException, UpdateException {
        JmsEndpoint endpoint = findEndpointByPrimaryKey(goid);
        if (endpoint == null) throw new FindException("No endpoint with GOID " + goid + " could be found");
        endpoint.setMessageSource(isMessageSource);
    }

    @Override
    public Goid saveConnection(JmsConnection connection) throws SaveException, VersionException {
        try {
            Goid goid = connection.getGoid();
            if (goid.equals(JmsConnection.DEFAULT_GOID))
                goid = jmsConnectionManager.save(connection);
            else
                jmsConnectionManager.update(connection);
            return goid;
        } catch (ObjectModelException e) {
            if (ExceptionUtils.causedBy(e, StaleUpdateException.class)) {
                logger.log(Level.INFO, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                throw new VersionException("Current JMS connection version is outdated.  No changes will be saved.", e);
            } else {
                throw new SaveException("Couldn't save JmsConnection", e);
            }
        }
    }

    /**
     * Returns an array of {@link JmsEndpoint}s that belong to the {@link JmsConnection} with the provided OID.
     *
     * @param connectionGoid The connection GOID
     * @return an array of {@link JmsEndpoint}s
     * @throws FindException
     */
    @Override
    public JmsEndpoint[] getEndpointsForConnection(Goid connectionGoid) throws FindException {
        return jmsEndpointManager.findEndpointsForConnection(connectionGoid);
    }

    /**
     * Test the specified JmsConnection, which may or may not exist in the database.  The Gateway will use the
     * specified settings to open a JMS connection.  If this succeeds, the caller can assume that the settings
     * are valid.
     *
     * @param connection JmsConnection settings to test.  Might not yet have an OID.
     * @throws JmsTestException if a test connection could not be established
     */
    @Override
    public void testConnection(JmsConnection connection) throws JmsTestException {
        try {
            JmsUtil.connect(connection).close();
        } catch (JMSException e) {
            logger.log(Level.INFO, "Caught JMSException while testing connection '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw new JmsTestException(e.toString());
        } catch (NamingException e) {
            logger.log(Level.INFO, "Caught NamingException while testing connection '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw new JmsTestException(e.toString());
        } catch (JmsConfigException e) {
            logger.log(Level.INFO, "Caught JmsConfigException while testing connection '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw new JmsTestException(e.toString());
        }
    }


    /**
     * Test the specified JmsEndpoint on the specified JmsConnection, either or both of which may or may not exist in
     * the database.  The Gateway will use the specified settings to open a JMS
     * connection and attempt to verify the existence of a Destination for this JmsEndpoint.
     *
     * @param conn     JmsConnection settings to test.  Might not yet have an OID.
     * @param endpoint JmsEndpoint settings to test.  Might not yet have an OID or a valid connectionOid.
     * @throws FindException   if the connection pointed to by the endpoint cannot be loaded
     */
    @Override
    public void testEndpoint(JmsConnection conn, JmsEndpoint endpoint) throws FindException, JmsTestException {
        JmsBag bag = null;
        final Collection<MessageProducer> producers = new ArrayList<MessageProducer>();
        final Collection<MessageConsumer> consumers = new ArrayList<MessageConsumer>();

        try {
            logger.finer("Connecting to connection " + conn);
            bag = JmsUtil.connect(conn, endpoint.getPasswordAuthentication(new GatewaySecurePasswordReferenceExpander(new LoggingAudit(logger))),
                    jmsPropertyMapper, true, endpoint.isQueue(), endpoint.getAcknowledgementType()==JmsAcknowledgementType.ON_COMPLETION, Session.AUTO_ACKNOWLEDGE);

            final Context jndiContext = bag.getJndiContext();
            final Connection jmsConnection = bag.getConnection();
            jmsConnection.start();

            logger.finer("Connected, getting Session...");
            final Session jmsSession = bag.getSession();
            logger.finer("Got Session...");
            if ( endpoint.isQueue() && jmsSession instanceof QueueSession) {
                QueueSession qs = ((QueueSession)jmsSession);
                // inbound queue
                Object o = jndiContext.lookup(endpoint.getDestinationName());
                if (!(o instanceof Queue))
                    throw new JmsTestException(endpoint.getDestinationName() + " is not a Queue");
                Queue q = (Queue)o;

                // we either try to create a received or a sender. some queues can only be opened in one mode and that's cool with us
                boolean canreceive = false;
                JMSException laste = null;
                try {
                    // if this is outbound, we should NOT create a receiver
                    logger.fine("Creating queue receiver for " + q);
                    consumers.add( qs.createReceiver(q) );
                    canreceive = true;
                } catch (JMSException e) {
                    logger.info("This queue cannot be opened for receiving, will test for sending");
                    laste = e;
                }
                if (!canreceive) {
                    try {
                        logger.fine("Unable to receive with this queue, will try to open a sender");
                        producers.add( qs.createSender(q) );
                    } catch (JMSException e) {
                        logger.log(Level.INFO, "This queue cannot be opened for sending nor receiving: " + JmsUtil.getJMSErrorMessage(e), ExceptionUtils.getDebugException(e));
                        if (laste != null) throw laste;
                        else throw e;
                    }
                }
                // Reply to the specified queue
                if (endpoint.getReplyToQueueName() != null) {
                    Object rq = jndiContext.lookup(endpoint.getReplyToQueueName());
                    if (!(rq instanceof Queue))
                        throw new JmsTestException(endpoint.getReplyToQueueName() + " is not a Queue");
                    Queue fq = (Queue)rq;
                    logger.fine("Creating queue sender for " + fq);
                    producers.add( qs.createSender(fq) );
                }
                // failure queue
                if (endpoint.getFailureDestinationName() != null) {
                    Object fo = jndiContext.lookup(endpoint.getFailureDestinationName());
                    if (!(fo instanceof Queue))
                        throw new JmsTestException(endpoint.getFailureDestinationName() + " is not a Queue");
                    Queue fq = (Queue)fo;
                    logger.fine("Creating queue sender for " + fq);
                    producers.add(  qs.createSender(fq) );
                }
            } else if (!endpoint.isQueue() && jmsSession instanceof TopicSession) {
                TopicSession ts = ((TopicSession)jmsSession);
                Object o = jndiContext.lookup(endpoint.getDestinationName());
                if (!(o instanceof Topic))
                    throw new JmsTestException(endpoint.getDestinationName() + " is not a Topic");
                Topic t = (Topic)o;
                
                boolean canreceive = false;
                JMSException laste = null;
                try {
                    logger.fine("Creating topic subscriber for " + t);
                    consumers.add( ts.createSubscriber(t) );
                    canreceive = true;
                } catch (JMSException e) {
                    logger.info("This topic cannot be opened for subscribing, will test for publishing");
                    laste = e;
                }
                if (!canreceive) {
                    try {
                        logger.fine("Unable to subscribe with this topic, will try to open a publisher");
                        producers.add( ts.createPublisher(t) );
                    } catch (JMSException e) {
                        logger.log(Level.INFO, "This topic cannot be opened for publishing nor subscribing: " + JmsUtil.getJMSErrorMessage(e), ExceptionUtils.getDebugException(e));
                        if (laste != null) throw laste;
                        else throw e;
                    }
                }
            } else {
                Object o = jndiContext.lookup(endpoint.getDestinationName());
                if ( endpoint.isQueue() && !(o instanceof Queue) ) throw new JmsTestException(endpoint.getDestinationName() + " is not a Queue");
                if ( !endpoint.isQueue() && !(o instanceof Topic) ) throw new JmsTestException(endpoint.getDestinationName() + " is not a Topic");

                boolean canreceive = false;
                JMSException laste = null;
                try {
                    logger.fine("Creating consumer for " + o);
                    consumers.add( jmsSession.createConsumer((Destination) o) );
                    canreceive = true;
                } catch (JMSException e) {
                    logger.info("This destination cannot be opened for consuming, will test for producing");
                    laste = e;
                }
                if (!canreceive) {
                    try {
                        logger.fine("Unable to consume with this destination, will try to open a producer");
                        producers.add( jmsSession.createProducer((Destination) o) );
                    } catch (JMSException e) {
                        logger.log(Level.INFO, "This destination cannot be opened for producing nor consuming: " + JmsUtil.getJMSErrorMessage(e), ExceptionUtils.getDebugException(e));
                        if (laste != null) throw laste;
                        else throw e;
                    }
                }

                // Reply to the specified queue
                if (endpoint.getReplyToQueueName() != null) {
                    Object rq = jndiContext.lookup(endpoint.getReplyToQueueName());
                    if (!(rq instanceof Queue))
                        throw new JmsTestException(endpoint.getReplyToQueueName() + " is not a Queue");
                    Queue fq = (Queue)rq;
                    logger.fine("Creating producer for " + fq);
                    producers.add( jmsSession.createProducer(fq) );
                }

                // failure queue
                if (endpoint.getFailureDestinationName() != null) {
                    Object fo = jndiContext.lookup(endpoint.getFailureDestinationName());
                    if (!(fo instanceof Queue))
                        throw new JmsTestException(endpoint.getFailureDestinationName() + " is not a Queue");
                    Queue fq = (Queue)fo;
                    logger.fine("Creating producer for " + fq);
                    producers.add( jmsSession.createProducer(fq) );
                }
            }
        } catch (JMSException e) {
            logger.log(Level.INFO, "Caught JMSException while testing endpoint '" + JmsUtil.getJMSErrorMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw new JmsTestException(e.toString());
        } catch (NamingException e) {
            logger.log(Level.INFO, "Caught NamingException while testing endpoint '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw new JmsTestException(e.toString());
        } catch (JmsConfigException e) {
            logger.log(Level.INFO, "Caught JmsConfigException while testing endpoint '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw new JmsTestException(e.toString());
        } catch (Throwable t) {
            logger.log(Level.INFO, "Caught Throwable while testing endpoint '" + ExceptionUtils.getMessage(t) + "'.", ExceptionUtils.getDebugException(t));
            throw new JmsTestException(t.toString());
        } finally {
            for ( final MessageProducer producer : producers ) close(producer);
            for ( final MessageConsumer consumer : consumers ) close(consumer);
            if (bag != null) bag.close();
        }
    }

    @Override
    public long getDefaultJmsMessageMaxBytes() {
        return config.getLongProperty(ServerConfigParams.PARAM_IO_JMS_MESSAGE_MAX_BYTES, 2621440L);  // ioJmsMessageMaxBytes.default = 2621440
    }

    @Override
    public Goid saveEndpoint(JmsEndpoint endpoint) throws SaveException, VersionException {
        try {
            Goid goid = endpoint.getGoid();
            if (goid.equals(JmsConnection.DEFAULT_GOID))
                goid = jmsEndpointManager.save(endpoint);
            else {
                jmsEndpointManager.update(endpoint);
            }

            return goid;
        } catch (ObjectModelException e) {
            if (ExceptionUtils.causedBy(e, StaleUpdateException.class)) {
                logger.log(Level.INFO, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                throw new VersionException("Current JMS connection version is outdated.  No changes will be saved.", e);
            } else {
                throw new SaveException("Couldn't save endpoint", e);
            }
        }
    }

    @Override
    public void deleteEndpoint(Goid endpointGoid) throws FindException, DeleteException {
        jmsEndpointManager.delete(endpointGoid);
    }

    @Override
    public void deleteConnection(Goid connectionGoOid) throws FindException, DeleteException {
        jmsConnectionManager.delete(connectionGoOid);
    }

    @Override
    public boolean isValidProperty(JmsMessagePropertyRule rule) {
        if (rule != null) {
            if (!rule.isPassThru()) {
                try {
                    JmsDefinedProperties.fromName(rule.getName());
                    String[] result = Syntax.getReferencedNames(rule.getCustomPattern());
                    if (result.length == 0) {
                        //No context variable defined
                        return JmsDefinedProperties.isValid(rule.getName(), rule.getCustomPattern());
                    }
                } catch (IllegalArgumentException e) {
                    //It is not a pre-defined jms variable.
                }
            }
        }
        return true;
    }

    @Override
    public int getDefaultConsumerConnectionSize() {
        return config.getIntProperty(ServerConfigParams.PARAM_IO_JMS_CONSUMER_CONNECTIONS, DEFAULT_JMS_CONSUMER_CONNECTIONS);
    }

    protected void initDao() throws Exception {
        checkJmsConnectionManager();
        checkJmsEndpointManager();
    }

    private void checkJmsEndpointManager() {
        if (jmsEndpointManager == null) {
            throw new IllegalArgumentException("jms endpoint manager is required");
        }
    }

    private void checkJmsConnectionManager() {
        if (jmsConnectionManager == null) {
            throw new IllegalArgumentException("jms connection manager is required");
        }
    }

    private void close( final MessageConsumer consumer ) {
        if (consumer == null) return;
        try {
            consumer.close();
        } catch (Exception e) {
            logger.log(
                    Level.WARNING,
                    "Couldn't close Message Consumer '"+ExceptionUtils.getMessage(e)+"'.",
                    ExceptionUtils.getDebugException(e));
        }
    }

    private void close( final MessageProducer mp ) {
        if (mp == null) return;
        try {
            mp.close();
        } catch (Exception e) {
            logger.log(
                    Level.WARNING,
                    "Couldn't close Message Producer '"+ExceptionUtils.getMessage(e)+"'.",
                    ExceptionUtils.getDebugException(e));
        }
    }

}
