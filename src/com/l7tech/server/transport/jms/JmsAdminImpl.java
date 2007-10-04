/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.*;
import com.l7tech.objectmodel.*;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsAdminImpl implements JmsAdmin {
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

    public JmsProvider[] getProviderList() throws FindException {
        return jmsConnectionManager.findAllProviders().toArray(new JmsProvider[0]);
    }

    /**
     * Finds all {@link JmsConnection}s in the database.
     *
     * @return an array of transient {@link JmsConnection}s
     * @throws FindException
     */
    public JmsConnection[] findAllConnections() throws FindException {
        Collection found = jmsConnectionManager.findAll();
        if (found == null || found.size() < 1) return new JmsConnection[0];

        List results = new ArrayList();
        for (Iterator i = found.iterator(); i.hasNext();) {
            JmsConnection conn = (JmsConnection)i.next();
            results.add(conn);
        }

        return (JmsConnection[])results.toArray(new JmsConnection[0]);
    }

    public JmsAdmin.JmsTuple[] findAllTuples() throws FindException {
        JmsTuple tuple;
        ArrayList result = new ArrayList();
        Collection connections = jmsConnectionManager.findAll();
        for (Iterator i = connections.iterator(); i.hasNext();) {
            JmsConnection connection = (JmsConnection)i.next();
            JmsEndpoint[] endpoints = jmsEndpointManager.findEndpointsForConnection(connection.getOid());
            for (int j = 0; j < endpoints.length; j++) {
                JmsEndpoint endpoint = endpoints[j];
                tuple = new JmsTuple(connection, endpoint);
                result.add(tuple);
            }
        }

        return (JmsTuple[])result.toArray(new JmsTuple[0]);
    }

    /**
     * Finds the {@link JmsConnection} with the provided OID.
     *
     * @return the {@link JmsConnection} with the provided OID.
     * @throws FindException
     */
    public JmsConnection findConnectionByPrimaryKey(long oid) throws FindException {
        return jmsConnectionManager.findByPrimaryKey(oid);
    }

    public JmsEndpoint findEndpointByPrimaryKey(long oid) throws FindException {
        return jmsEndpointManager.findByPrimaryKey(oid);
    }

    public void setEndpointMessageSource(long oid, boolean isMessageSource) throws FindException, UpdateException {
        JmsEndpoint endpoint = findEndpointByPrimaryKey(oid);
        if (endpoint == null) throw new FindException("No endpoint with OID " + oid + " could be found");
        endpoint.setMessageSource(isMessageSource);
    }

    public long saveConnection(JmsConnection connection) throws SaveException, VersionException {
        try {
            long oid = connection.getOid();
            if (oid == JmsConnection.DEFAULT_OID)
                oid = jmsConnectionManager.save(connection);
            else
                jmsConnectionManager.update(connection);
            return oid;
        } catch (ObjectModelException e) {
            throw new SaveException("Couldn't save JmsConnection", e);
        }
    }

    /**
     * Returns an array of {@link JmsEndpoint}s that belong to the {@link JmsConnection} with the provided OID.
     *
     * @param connectionOid
     * @return an array of {@link JmsEndpoint}s
     * @throws FindException
     */
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
    public void testConnection(JmsConnection connection) throws JmsTestException {
        try {
            JmsUtil.connect(connection).close();
        } catch (JMSException e) {
            _logger.log(Level.INFO, "Caught JMSException while testing connection", e);
            throw new JmsTestException(e.toString());
        } catch (NamingException e) {
            _logger.log(Level.INFO, "Caught NamingException while testing connection", e);
            throw new JmsTestException(e.toString());
        } catch (JmsConfigException e) {
            _logger.log(Level.INFO, "Caught JmsConfigException while testing connection", e);
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
    public void testEndpoint(JmsConnection conn, JmsEndpoint endpoint) throws FindException, JmsTestException {
        JmsBag bag = null;
        MessageConsumer jmsQueueReceiver = null;
        QueueSender jmsQueueSender = null;
        TopicSubscriber jmsTopicSubscriber = null;
        Connection jmsConnection = null;

        try {
            _logger.finer("Connecting to connection " + conn);
            bag = JmsUtil.connect(conn,
                    endpoint.getPasswordAuthentication(),
                    jmsPropertyMapper,
                    endpoint.getAcknowledgementType()==JmsAcknowledgementType.AUTOMATIC);

            Context jndiContext = bag.getJndiContext();
            jmsConnection = bag.getConnection();
            jmsConnection.start();

            _logger.finer("Connected, getting Session...");
            Session jmsSession = bag.getSession();
            _logger.finer("Got Session...");
            if (jmsSession instanceof QueueSession) {
                QueueSession qs = ((QueueSession)jmsSession);
                // inbound queue
                Object o = jndiContext.lookup(endpoint.getDestinationName());
                if (!(o instanceof Queue)) throw new JmsTestException(endpoint.getDestinationName() + " is not a Queue");
                Queue q = (Queue)o;
                _logger.fine("Creating queue receiver for " + q);
                jmsQueueReceiver = qs.createReceiver(q);
                // failure queue
                if (endpoint.getFailureDestinationName() != null) {
                    Object fo = jndiContext.lookup(endpoint.getFailureDestinationName());
                    if (!(fo instanceof Queue)) throw new JmsTestException(endpoint.getFailureDestinationName() + " is not a Queue");
                    Queue fq = (Queue)fo;
                    _logger.fine("Creating queue receiver for " + fq);
                    jmsQueueSender = qs.createSender(fq);
                }
            } else if (jmsSession instanceof TopicSession) {
                TopicSession ts = ((TopicSession)jmsSession);
                Object o = jndiContext.lookup(endpoint.getDestinationName());
                if (!(o instanceof Topic)) throw new JmsTestException(endpoint.getDestinationName() + " is not a Topic");
                Topic t = (Topic)o;
                _logger.fine("Creating topic subscriber for " + t);
                jmsTopicSubscriber = ts.createSubscriber(t);
            } else {
                // Not much we can do here
            }
        } catch (JMSException e) {
            _logger.log(Level.INFO, "Caught JMSException while testing endpoint", e);
            throw new JmsTestException(e.toString());
        } catch (NamingException e) {
            _logger.log(Level.INFO, "Caught NamingException while testing endpoint", e);
            throw new JmsTestException(e.toString());
        } catch (JmsConfigException e) {
            _logger.log(Level.INFO, "Caught JmsConfigException while testing endpoint", e);
            throw new JmsTestException(e.toString());
        } catch (Throwable t) {
            _logger.log(Level.INFO, "Caught Throwable while testing endpoint", t);
            throw new JmsTestException(t.toString());
        } finally {
            try {
                if (jmsQueueSender != null) jmsQueueSender.close();
            } catch (JMSException e) {
            }

            try {
                if (jmsQueueReceiver != null) jmsQueueReceiver.close();
            } catch (JMSException e) {
            }

            try {
                if (jmsTopicSubscriber != null) jmsTopicSubscriber.close();
            } catch (JMSException e) {
            }

            if (bag != null) bag.close();
        }
    }

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
            throw new SaveException("Couldn't save endpoint", e);
        }
    }

    public void deleteEndpoint(long endpointOid) throws FindException, DeleteException {
        jmsEndpointManager.delete(endpointOid);
    }

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

    private final Logger _logger = Logger.getLogger(getClass().getName());
}
