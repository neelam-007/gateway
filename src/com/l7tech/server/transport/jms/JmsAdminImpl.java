/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.admin.AccessManager;
import com.l7tech.common.transport.jms.*;
import com.l7tech.objectmodel.*;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.rmi.RemoteException;
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
public class JmsAdminImpl extends HibernateDaoSupport implements JmsAdmin {
    private JmsConnectionManager jmsConnectionManager;
    private JmsEndpointManager jmsEndpointManager;
    private final AccessManager accessManager;


    public JmsAdminImpl(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public JmsProvider[] getProviderList() throws RemoteException, FindException {
        return (JmsProvider[])jmsConnectionManager.findAllProviders().toArray(new JmsProvider[0]);
    }

    /**
     * Finds all {@link JmsConnection}s in the database.
     *
     * @return an array of transient {@link JmsConnection}s
     * @throws RemoteException
     * @throws FindException
     */
    public JmsConnection[] findAllConnections() throws RemoteException, FindException {
        Collection found = jmsConnectionManager.findAll();
        if (found == null || found.size() < 1) return new JmsConnection[0];

        List results = new ArrayList();
        for (Iterator i = found.iterator(); i.hasNext();) {
            JmsConnection conn = (JmsConnection)i.next();
            results.add(conn);
        }

        return (JmsConnection[])results.toArray(new JmsConnection[0]);
    }

    /**
     * Must be called in a transaction!
     */
    public JmsAdmin.JmsTuple[] findAllTuples() throws RemoteException, FindException {
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
     * @throws RemoteException
     * @throws FindException
     */
    public JmsConnection findConnectionByPrimaryKey(long oid) throws RemoteException, FindException {
        return jmsConnectionManager.findConnectionByPrimaryKey(oid);
    }

    public JmsEndpoint findEndpointByPrimaryKey(long oid) throws RemoteException, FindException {
        return jmsEndpointManager.findByPrimaryKey(oid);
    }

    public void setEndpointMessageSource(long oid, boolean isMessageSource) throws RemoteException, FindException, UpdateException {
        accessManager.enforceAdminRole();
        JmsEndpoint endpoint = findEndpointByPrimaryKey(oid);
        if (endpoint == null) throw new FindException("No endpoint with OID " + oid + " could be found");
        endpoint.setMessageSource(isMessageSource);
    }

    public EntityHeader[] findAllMonitoredEndpoints() throws RemoteException, FindException {
        Collection endpoints = jmsEndpointManager.findMessageSourceEndpoints();
        List list = new ArrayList();
        for (Iterator i = endpoints.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint)i.next();
            list.add(endpoint.toEntityHeader());
        }
        return (EntityHeader[])list.toArray(new EntityHeader[0]);
    }

    public long saveConnection(JmsConnection connection) throws RemoteException, SaveException, VersionException {
        try {
            accessManager.enforceAdminRole();

            long oid = connection.getOid();
            if (oid == JmsConnection.DEFAULT_OID)
                oid = jmsConnectionManager.save(connection);
            else
                jmsConnectionManager.update(connection);
            return oid;
        } catch (UpdateException e) {
            throw new SaveException("Couldn't save JmsConnection", e);
        }
    }

    /**
     * Returns an array of {@link JmsEndpoint}s that belong to the {@link JmsConnection} with the provided OID.
     *
     * @param connectionOid
     * @return an array of {@link JmsEndpoint}s
     * @throws RemoteException
     * @throws FindException
     */
    public JmsEndpoint[] getEndpointsForConnection(long connectionOid) throws RemoteException, FindException {
        return jmsEndpointManager.findEndpointsForConnection(connectionOid);
    }

    /**
     * Test the specified JmsConnection, which may or may not exist in the database.  The Gateway will use the
     * specified settings to open a JMS connection.  If this succeeds, the caller can assume that the settings
     * are valid.
     *
     * @param connection JmsConnection settings to test.  Might not yet have an OID.
     * @throws RemoteException
     * @throws JmsTestException if a test connection could not be established
     */
    public void testConnection(JmsConnection connection) throws RemoteException, JmsTestException {
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
     * @throws RemoteException
     */
    public void testEndpoint(JmsConnection conn, JmsEndpoint endpoint) throws RemoteException, FindException, JmsTestException {
        JmsBag bag = null;
        MessageConsumer jmsQueueReceiver = null;
        TopicSubscriber jmsTopicSubscriber = null;
        Connection jmsConnection = null;

        try {
            _logger.finer("Connecting to connection " + conn);
            bag = JmsUtil.connect(conn, endpoint.getPasswordAuthentication());

            Context jndiContext = bag.getJndiContext();
            jmsConnection = bag.getConnection();
            jmsConnection.start();

            _logger.finer("Connected, getting Session...");
            Session jmsSession = bag.getSession();
            _logger.finer("Got Session...");
            if (jmsSession instanceof QueueSession) {
                QueueSession qs = ((QueueSession)jmsSession);
                Object o = jndiContext.lookup(endpoint.getDestinationName());
                if (!(o instanceof Queue)) throw new JmsTestException(endpoint.getDestinationName() + " is not a Queue");
                Queue q = (Queue)o;
                _logger.fine("Creating queue receiver for " + q);
                jmsQueueReceiver = qs.createReceiver(q);
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

    public long saveEndpoint(JmsEndpoint endpoint) throws RemoteException, SaveException, VersionException {
        try {
            accessManager.enforceAdminRole();
            long oid = endpoint.getOid();
            if (oid == JmsConnection.DEFAULT_OID)
                oid = jmsEndpointManager.save(endpoint);
            else {
                jmsEndpointManager.update(endpoint);
            }

            return oid;
        } catch (UpdateException e) {
            throw new SaveException("Couldn't save endpoint", e);
        }
    }

    public void deleteEndpoint(long endpointOid) throws RemoteException, FindException, DeleteException {
        accessManager.enforceAdminRole();
        jmsEndpointManager.delete(endpointOid);
    }

    public void deleteConnection(long connectionOid) throws RemoteException, FindException, DeleteException {
        accessManager.enforceAdminRole();
        jmsConnectionManager.delete(connectionOid);
    }

    public void setJmsConnectionManager(JmsConnectionManager jmsConnectionManager) {
        this.jmsConnectionManager = jmsConnectionManager;
    }

    public void setJmsEndpointManager(JmsEndpointManager jmsEndpointManager) {
        this.jmsEndpointManager = jmsEndpointManager;
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
