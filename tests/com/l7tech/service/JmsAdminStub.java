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

import java.rmi.RemoteException;
import java.util.*;

/**
 * Stub-mode JMS admin interface.
 */
public class JmsAdminStub implements JmsAdmin {
    private Map connections;
    private Map endpoints;
    private List monitoredEndpoints = new ArrayList();

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

    public synchronized EntityHeader[] findAllConnections() throws RemoteException, FindException {
        Collection list = new ArrayList();
        for (Iterator i = connections.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(((JmsConnection) connections.get(key)).toEntityHeader());
        }
        return (EntityHeader[]) list.toArray(new EntityHeader[] {});
    }

    public synchronized JmsConnection findConnectionByPrimaryKey(long oid) throws RemoteException, FindException {
        return (JmsConnection) connections.get(new Long(oid));
    }

    public synchronized EntityHeader[] findAllMonitoredEndpoints() throws RemoteException, FindException {
        Collection list = new ArrayList();
        for (Iterator i = monitoredEndpoints.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) i.next();
            list.add(endpoint.toEntityHeader());
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
            monitoredHeaders.add(endpoint.toEntityHeader());
        }
        monitoredEndpoints = monitoredHeaders;
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

    public EntityHeader[] getEndpointHeaders( long connectionOid ) throws RemoteException, FindException {
        return new EntityHeader[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public synchronized JmsEndpoint findEndpointByPrimaryKey(long oid) throws RemoteException, FindException {
        JmsEndpoint e = (JmsEndpoint)endpoints.get(new Long(oid));
        if (e == null )
            throw new FindException("No endpoint with OID " + oid + " is known");
        else
            return e;
    }

    public synchronized long saveEndpoint(JmsEndpoint endpoint) throws RemoteException, UpdateException, SaveException, VersionException {
        JmsEndpoint oldEndpoint = null;
        try {
            oldEndpoint = findEndpointByPrimaryKey(endpoint.getOid());
            if (oldEndpoint.getConnectionOid() != endpoint.getConnectionOid())
                throw new UpdateException("New endpoint belongs to a different connection");
            endpoints.put(new Long(endpoint.getOid()), endpoint);
            return endpoint.getOid();
        } catch ( FindException e ) {
            throw new UpdateException( e.toString(), e );
        }
    }

    public void deleteEndpoint( long endpointOid ) throws RemoteException, FindException, DeleteException {
        endpoints.remove(new Long(endpointOid));
    }
}
