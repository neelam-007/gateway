/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.common.transport.jms.*;
import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.*;

import java.util.*;

/**
 * Stub-mode JMS admin interface.
 */
public class JmsAdminStub implements JmsAdmin {
    private Map connections;
    private Map endpoints;

    private static JmsProvider[] providers = new JmsProvider[] {
        new JmsProvider("WebSphere MQ 5.2.1", "com.ibm.jms.jndi.InitialContextFactory", "JmsQueueConnectionFactory"),
        new JmsProvider("OpenJMS 0.7.6-rc3", "org.exolab.jms.jndi.InitialContextFactory", "JmsQueueConnectionFactory"),
    };

    public JmsAdminStub() {
        connections = StubDataStore.defaultStore().getJmsConnections();
        endpoints = StubDataStore.defaultStore().getJmsEndpoints();
    }

    public JmsProvider[] getProviderList() {
        return providers;
    }

    public synchronized JmsConnection[] findAllConnections() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i = connections.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add((JmsConnection) connections.get(key));
        }
        return (JmsConnection[]) list.toArray(new JmsConnection[0]);
    }

    public JmsAdmin.JmsTuple[] findAllTuples() throws FindException {
        List found = new ArrayList();
        Set keys = endpoints.keySet();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) endpoints.get(i.next());
            found.add(new JmsTuple( findConnectionByPrimaryKey( endpoint.getConnectionOid() ), endpoint ));
        }
        return (JmsTuple[]) found.toArray(new JmsTuple[0]);
    }

    public synchronized JmsConnection findConnectionByPrimaryKey(long oid) throws FindException {
        return (JmsConnection) connections.get(new Long(oid));
    }

    public synchronized long saveConnection(JmsConnection connection) throws SaveException, VersionException {
        long oid = connection.getOid();
        if (oid == 0 || oid == PersistentEntity.DEFAULT_OID) {
            oid = StubDataStore.defaultStore().nextObjectId();
        }
        connection.setOid(oid);
        Long key = new Long(oid);
        connections.put(key, connection);
        return oid;
    }

    public synchronized void deleteConnection(long id) throws DeleteException {
        if (connections.remove(new Long(id)) == null) {
            throw new RuntimeException("Could not find jms connection oid= " + id);
        }
    }

    public JmsEndpoint[] getEndpointsForConnection(long connectionOid) throws FindException {
        List found = new ArrayList();
        Set keys = endpoints.keySet();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) endpoints.get(i.next());
            if (endpoint.getConnectionOid() == connectionOid)
                found.add(endpoint);
        }
        return (JmsEndpoint[]) found.toArray(new JmsEndpoint[0]);
    }

    public synchronized JmsEndpoint findEndpointByPrimaryKey(long oid) throws FindException {
        JmsEndpoint e = (JmsEndpoint)endpoints.get(new Long(oid));
        if (e == null )
            throw new FindException("No endpoint with OID " + oid + " is known");
        else
            return e;
    }

    public void setEndpointMessageSource(long oid, boolean isMessageSource) throws FindException {
        JmsEndpoint endpoint = findEndpointByPrimaryKey(oid);
        endpoint.setMessageSource(isMessageSource);        
    }

    public synchronized long saveEndpoint(JmsEndpoint endpoint) throws SaveException, VersionException {
        long oid = endpoint.getOid();
        if (oid == 0 || oid == JmsEndpoint.DEFAULT_OID) {
            oid = StubDataStore.defaultStore().nextObjectId();
            endpoint.setOid(oid);
        }
        endpoints.put(new Long(oid), endpoint);
        return oid;
    }

    public void deleteEndpoint( long endpointOid ) throws FindException, DeleteException {
        endpoints.remove(new Long(endpointOid));
    }

    public void testConnection(JmsConnection connection) throws JmsTestException {
        // automatic success in stub mode, unless the name contains "FAIL"
        if (connection.getName().indexOf("FAIL") >= 0)
            throw new JmsTestException("Invalid JMS connection settings");
    }

    public void testEndpoint(JmsConnection connection, JmsEndpoint endpoint) throws JmsTestException {
        // automatic success in stub mode, unless the name contains "FAIL"
        if (endpoint.getName().indexOf("FAIL") >= 0)
            throw new JmsTestException("Invalid Destination name");
    }
}
