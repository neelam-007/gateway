/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.identity.StubDataStore;

import java.rmi.RemoteException;
import java.util.*;

/**
 * Stub-mode JMS admin interface.
 */
public class JmsAdminStub implements JmsAdmin {
    private Map connections;
    private List monitoredEndpoints = new ArrayList();

    private static JmsProvider[] providers = new JmsProvider[] {
        new JmsProvider("IBM MQSeries 5.2.1", "com.ibm.jms.jndi.InitialContextFactory", "JmsQueueConnectionFactory"),
        new JmsProvider("OpenJMS 0.7.6-rc3", "org.exolab.jms.jndi.InitialContextFactory", "JmsQueueConnectionFactory"),
    };

    public JmsAdminStub() {
        connections = StubDataStore.defaultStore().getJmsConnections();
    }

    public JmsProvider[] getProviderList() throws RemoteException {
        return providers;
    }

    public synchronized EntityHeader[] findAllConnections() throws RemoteException, FindException {
        Collection list = new ArrayList();
        for (Iterator i = connections.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(fromConnection((JmsConnection) connections.get(key)));
        }
        return (EntityHeader[]) list.toArray(new EntityHeader[] {});
    }

    private synchronized EntityHeader fromConnection(JmsConnection p) {
        return new EntityHeader(p.getOid(), EntityType.JMS_CONNECTION, p.getName(), null);
    }

    public synchronized JmsConnection findConnectionByPrimaryKey(long oid) throws RemoteException, FindException {
        return (JmsConnection) connections.get(new Long(oid));
    }

    public synchronized EntityHeader[] findAllMonitoredEndpoints() throws RemoteException, FindException {
        Collection list = new ArrayList();
        for (Iterator i = monitoredEndpoints.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) i.next();
            list.add(fromEndpoint(endpoint));
        }
        return (EntityHeader[]) list.toArray(new EntityHeader[0]);
    }

    public synchronized void saveAllMonitoredEndpoints(long[] oids) throws RemoteException, FindException {
        List foundEndpoints = new ArrayList();
        for (int i = 0; i < oids.length; i++) {
            long oid = oids[i];
            JmsEndpoint endpoint = findEndpointByPrimaryKey(oid);
            foundEndpoints.add(endpoint);
        }

        List monitoredHeaders = new ArrayList();
        for (Iterator i = foundEndpoints.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) i.next();
            monitoredHeaders.add(fromEndpoint(endpoint));
        }
        monitoredEndpoints = monitoredHeaders;
    }

    private EntityHeader fromEndpoint(JmsEndpoint endpoint) {
        return new EntityHeader(endpoint.getOid(), EntityType.UNDEFINED,  endpoint.getDestinationName(), endpoint.getName());
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

    public synchronized JmsEndpoint findEndpointByPrimaryKey(long oid) throws RemoteException, FindException {
        for (Iterator i = connections.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            JmsConnection connection = (JmsConnection) connections.get(key);
            Set endpoints = connection.getEndpoints();
            for (Iterator e = endpoints.iterator(); e.hasNext();) {
                JmsEndpoint endpoint = (JmsEndpoint) e.next();
                if (oid == endpoint.getOid())
                    return endpoint;
            }
        }
        throw new FindException("No endpoint with OID " + oid + " is known");
    }

    public synchronized void saveEndpoint(JmsEndpoint endpoint) throws RemoteException, FindException, UpdateException, SaveException, VersionException {
        JmsEndpoint oldEndpoint = findEndpointByPrimaryKey(endpoint.getOid());
        if (oldEndpoint.getConnection().getOid() != endpoint.getConnection().getOid())
            throw new FindException("New endpoint belongs to a different connection");
        JmsConnection connection = (JmsConnection) connections.get(new Long(endpoint.getConnection().getOid()));
        connection.getEndpoints().remove(oldEndpoint);
        connection.getEndpoints().add(endpoint);
        saveConnection(connection);
    }
}
