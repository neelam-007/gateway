/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.*;

import javax.jms.JMSException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Stub-mode JMS admin interface.
 */
public class JmsAdminStub implements JmsAdmin {
    private Map connections;
    private Map endpoints;

    private static JmsProvider[] providers = new JmsProvider[] {
        new JmsProvider("IBM MQSeries 5.2.1", "com.ibm.jms.jndi.InitialContextFactory", "JmsQueueConnectionFactory"),
        new JmsProvider("OpenJMS 0.7.6-rc3", "org.exolab.jms.jndi.InitialContextFactory", "JmsQueueConnectionFactory"),
    };

    public JmsAdminStub() {
        connections = StubDataStore.defaultStore().getJmsConnections();
        endpoints = StubDataStore.defaultStore().getJmsEndpoints();
    }

    public JmsProvider[] getProviderList() throws RemoteException {
        return providers;
    }

    public synchronized JmsConnection[] findAllConnections() throws RemoteException, FindException {
        Collection list = new ArrayList();
        for (Iterator i = connections.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add((JmsConnection) connections.get(key));
        }
        return (JmsConnection[]) list.toArray(new JmsConnection[0]);
    }

    public synchronized JmsConnection findConnectionByPrimaryKey(long oid) throws RemoteException, FindException {
        return (JmsConnection) connections.get(new Long(oid));
    }

    public synchronized long saveConnection(JmsConnection connection) throws RemoteException, UpdateException, SaveException, VersionException {
        long oid = connection.getOid();
        if (oid == 0 || oid == Entity.DEFAULT_OID) {
            oid = StubDataStore.defaultStore().nextObjectId();
        }
        connection.setOid(oid);
        Long key = new Long(oid);
        connections.put(key, connection);
        return oid;
    }

    public synchronized void deleteConnection(long id) throws RemoteException, DeleteException {
        if (connections.remove(new Long(id)) == null) {
            throw new RemoteException("Could not find jms connection oid= " + id);
        }
    }

    public JmsEndpoint[] getEndpointsForConnection(long connectionOid) throws RemoteException, FindException {
        List found = new ArrayList();
        Set keys = endpoints.keySet();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) endpoints.get(i.next());
            if (endpoint.getConnectionOid() == connectionOid)
                found.add(endpoint);
        }
        return (JmsEndpoint[]) found.toArray(new JmsEndpoint[0]);
    }

    public synchronized JmsEndpoint findEndpointByPrimaryKey(long oid) throws RemoteException, FindException {
        JmsEndpoint e = (JmsEndpoint)endpoints.get(new Long(oid));
        if (e == null )
            throw new FindException("No endpoint with OID " + oid + " is known");
        else
            return e;
    }

    public void setEndpointMessageSource(long oid, boolean isMessageSource) throws RemoteException, FindException {
        JmsEndpoint endpoint = findEndpointByPrimaryKey(oid);
        endpoint.setMessageSource(isMessageSource);        
    }

    public synchronized long saveEndpoint(JmsEndpoint endpoint) throws RemoteException, UpdateException, SaveException, VersionException {
        long oid = endpoint.getOid();
        if (oid == 0 || oid == JmsEndpoint.DEFAULT_OID) {
            oid = StubDataStore.defaultStore().nextObjectId();
            endpoint.setOid(oid);
        }
        endpoints.put(new Long(oid), endpoint);
        return oid;
    }

    public void deleteEndpoint( long endpointOid ) throws RemoteException, FindException, DeleteException {
        endpoints.remove(new Long(endpointOid));
    }

    public void testConnection(JmsConnection connection) throws RemoteException, JMSException {
        // automatic success in stub mode, unless the name contains "FAIL"
        if (connection.getName().indexOf("FAIL") >= 0)
            throw new JMSException("Invalid JMS connection settings");
    }

    public void testEndpoint(JmsEndpoint endpoint) throws RemoteException, JMSException {
        // automatic success in stub mode, unless the name contains "FAIL"
        if (endpoint.getName().indexOf("FAIL") >= 0)
            throw new JMSException("Invalid Destination name");
    }
}
