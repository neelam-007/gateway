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

    public EntityHeader[] findAllConnections() throws RemoteException, FindException {
        Collection list = new ArrayList();
        for (Iterator i = connections.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(fromConnection((JmsConnection) connections.get(key)));
        }
        return (EntityHeader[]) list.toArray(new EntityHeader[] {});
    }

    private EntityHeader fromConnection(JmsConnection p) {
        return new EntityHeader(p.getOid(), EntityType.JMS_CONNECTION, p.getName(), null);
    }

    public JmsConnection findConnectionByPrimaryKey(long oid) throws RemoteException, FindException {
        return (JmsConnection) connections.get(new Long(oid));
    }

    public EntityHeader[] findAllMonitoredEndpoints() throws RemoteException, FindException {
        Collection list = new ArrayList();
        for (Iterator i = monitoredEndpoints.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) i.next();
            list.add(fromEndpoint(endpoint));
        }
        return (EntityHeader[]) list.toArray(new EntityHeader[0]);
    }

    private EntityHeader fromEndpoint(JmsEndpoint endpoint) {
        return new EntityHeader(endpoint.getOid(), EntityType.UNDEFINED,  endpoint.getDestinationName(), endpoint.getName());
    }

    public long saveConnection(JmsConnection connection) throws RemoteException, UpdateException, SaveException, VersionException {
        long oid = connection.getOid();
        if (oid == 0 || oid == Entity.DEFAULT_OID) {
            oid = StubDataStore.defaultStore().nextObjectId();
        }
        connection.setOid(oid);
        Long key = new Long(oid);
        connections.put(key, connection);
        return oid;
    }

    public void deleteConnection(long id) throws RemoteException, DeleteException {
        if (connections.remove(new Long(id)) == null) {
            throw new RemoteException("Could not find jms connection oid= " + id);
        }
    }
}
