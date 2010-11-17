/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */

package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.*;
import com.l7tech.objectmodel.*;
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

    private final JmsConnectionManager jmsConnectionManager;
    private final JmsEndpointManager jmsEndpointManager;
    private final JmsPropertyMapper jmsPropertyMapper;

    public JmsAdminImpl(JmsConnectionManager jmsConnectionManager,
                        JmsEndpointManager jmsEndpointManager,
                        JmsPropertyMapper jmsPropertyMapper) {
        this.jmsConnectionManager = jmsConnectionManager;
        this.jmsEndpointManager = jmsEndpointManager;
        this.jmsPropertyMapper = jmsPropertyMapper;
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
            JmsEndpoint[] endpoints = jmsEndpointManager.findEndpointsForConnection(connection.getOid());
            for (JmsEndpoint endpoint : endpoints) {
                result.add(new JmsTuple(connection, endpoint));
            }
        }

        return result.toArray(new JmsTuple[result.size()]);
    }

    /**
     * Finds the {@link JmsConnection} with the provided OID.
     *
     * @return the {@link JmsConnection} with the provided OID.
     * @throws FindException
     */
    @Override
    public JmsConnection findConnectionByPrimaryKey(long oid) throws FindException {
        return jmsConnectionManager.findByPrimaryKey(oid);
    }

    @Override
    public JmsEndpoint findEndpointByPrimaryKey(long oid) throws FindException {
        return jmsEndpointManager.findByPrimaryKey(oid);
    }

    @Override
    public void setEndpointMessageSource(long oid, boolean isMessageSource) throws FindException, UpdateException {
        JmsEndpoint endpoint = findEndpointByPrimaryKey(oid);
        if (endpoint == null) throw new FindException("No endpoint with OID " + oid + " could be found");
        endpoint.setMessageSource(isMessageSource);
    }

    @Override
    public long saveConnection(JmsConnection connection) throws SaveException, VersionException {
        try {
            long oid = connection.getOid();
            if (oid == JmsConnection.DEFAULT_OID)
                oid = jmsConnectionManager.save(connection);
            else
                jmsConnectionManager.update(connection);
            return oid;
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
     * @param connectionOid The connection OID
     * @return an array of {@link JmsEndpoint}s
     * @throws FindException
     */
    @Override
    public JmsEndpoint[] getEndpointsForConnection(long connectionOid) throws FindException {
        return jmsEndpointManager.findEndpointsForConnection(connectionOid);
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
        MessageConsumer jmsQueueReceiver = null;
        QueueSender jmsQueueSender = null;
        TopicSubscriber jmsTopicSubscriber = null;
        TopicPublisher jmsTopicPublisher = null;
        Connection jmsConnection;

        try {
            logger.finer("Connecting to connection " + conn);
            bag = JmsUtil.connect(conn, endpoint.getPasswordAuthentication(),
                    jmsPropertyMapper, endpoint.getAcknowledgementType()==JmsAcknowledgementType.ON_COMPLETION, Session.AUTO_ACKNOWLEDGE);

            Context jndiContext = bag.getJndiContext();
            jmsConnection = bag.getConnection();
            jmsConnection.start();

            logger.finer("Connected, getting Session...");
            Session jmsSession = bag.getSession();
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
                    jmsQueueReceiver = qs.createReceiver(q);
                    canreceive = true;
                } catch (JMSException e) {
                    logger.info("This queue cannot be opened for receiving, will test for sending");
                    laste = e;
                }
                if (!canreceive) {
                    try {
                        logger.fine("Unable to receive with this queue, will try to open a sender");
                        jmsQueueSender = qs.createSender(q);
                    } catch (JMSException e) {
                        logger.log(Level.INFO, "This queue cannot be opened for sending nor receiving: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
                    logger.fine("Creating queue receiver for " + fq);
                    jmsQueueSender = qs.createSender(fq);
                }
                // failure queue
                if (endpoint.getFailureDestinationName() != null) {
                    Object fo = jndiContext.lookup(endpoint.getFailureDestinationName());
                    if (!(fo instanceof Queue))
                        throw new JmsTestException(endpoint.getFailureDestinationName() + " is not a Queue");
                    Queue fq = (Queue)fo;
                    logger.fine("Creating queue receiver for " + fq);
                    jmsQueueSender = qs.createSender(fq);
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
                    jmsTopicSubscriber = ts.createSubscriber(t);
                    canreceive = true;
                } catch (JMSException e) {
                    logger.info("This topic cannot be opened for subscribing, will test for publishing");
                    laste = e;
                }
                if (!canreceive) {
                    try {
                        logger.fine("Unable to subscribe with this topic, will try to open a publisher");
                        jmsTopicPublisher = ts.createPublisher(t);
                    } catch (JMSException e) {
                        logger.log(Level.INFO, "This topic cannot be opened for publishing nor subscribing: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        if (laste != null) throw laste;
                        else throw e;
                    }
                }
            } else {
                throw new JMSException("Unknown JMS session.");
            }
        } catch (JMSException e) {
            logger.log(Level.INFO, "Caught JMSException while testing endpoint '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
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
            JmsUtil.closeQuietly(jmsQueueSender);
            JmsUtil.closeQuietly(jmsQueueReceiver);
            JmsUtil.closeQuietly(jmsTopicSubscriber);
            JmsUtil.closeQuietly(jmsTopicPublisher);
            if (bag != null) bag.close();
        }
    }

    @Override
    public long saveEndpoint(JmsEndpoint endpoint) throws SaveException, VersionException {
        try {
            long oid = endpoint.getOid();
            if (oid == JmsConnection.DEFAULT_OID)
                oid = jmsEndpointManager.save(endpoint);
            else {
                jmsEndpointManager.update(endpoint);
            }

            return oid;
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
    public void deleteEndpoint(long endpointOid) throws FindException, DeleteException {
        jmsEndpointManager.delete(endpointOid);
    }

    @Override
    public void deleteConnection(long connectionOid) throws FindException, DeleteException {
        jmsConnectionManager.delete(connectionOid);
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
}
