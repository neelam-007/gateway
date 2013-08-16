package com.l7tech.gateway.common.transport.jms;

import com.l7tech.gateway.common.StubDataStore;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;

import java.util.*;

/**
 * Stub-mode JMS admin interface.
 */
public class JmsAdminStub implements JmsAdmin {
    private Map<Goid,JmsConnection> connections;
    private Map<Goid, JmsEndpoint> endpoints;

    private static JmsProvider[] providers = new JmsProvider[] {
        new JmsProvider("WebSphere MQ 5.2.1", "com.ibm.jms.jndi.InitialContextFactory", "JmsQueueConnectionFactory"),
        new JmsProvider("OpenJMS 0.7.6-rc3", "org.exolab.jms.jndi.InitialContextFactory", "JmsQueueConnectionFactory"),
    };

    public JmsAdminStub() {
        connections = StubDataStore.defaultStore().getJmsConnections();
        endpoints =  StubDataStore.defaultStore().getJmsEndpoints();
    }

    @Override
    public EnumSet<JmsProviderType> getProviderTypes() {
        return EnumSet.noneOf(JmsProviderType.class);
    }

    @Override
    public synchronized JmsConnection[] findAllConnections() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i = connections.keySet().iterator(); i.hasNext();) {
            Goid key = (Goid) i.next();
            list.add( connections.get(key) );
        }
        return (JmsConnection[]) list.toArray(new JmsConnection[0]);
    }

    @Override
    public JmsAdmin.JmsTuple[] findAllTuples() throws FindException {
        List found = new ArrayList();
        Set keys = endpoints.keySet();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) endpoints.get(i.next());
            found.add(new JmsTuple( findConnectionByPrimaryKey( endpoint.getConnectionGoid() ), endpoint ));
        }
        return (JmsTuple[]) found.toArray(new JmsTuple[0]);
    }

    @Override
    public synchronized JmsConnection findConnectionByPrimaryKey(Goid oid) throws FindException {
        return (JmsConnection) connections.get(oid);
    }

    @Override
    public synchronized Goid saveConnection(JmsConnection connection) throws SaveException, VersionException {
        Goid goid = connection.getGoid();
        if (goid == null || goid.equals(GoidEntity.DEFAULT_GOID)) {
            goid = new Goid(0,StubDataStore.defaultStore().nextObjectId());
        }
        connection.setGoid(goid);
        connections.put(goid, connection);
        return goid;
    }

    @Override
    public synchronized void deleteConnection(Goid id) throws DeleteException {
        if (connections.remove(id) == null) {
            throw new RuntimeException("Could not find jms connection goid= " + id);
        }
    }

    @Override
    public JmsEndpoint[] getEndpointsForConnection(Goid connectionOid) throws FindException {
        List found = new ArrayList();
        Set keys = endpoints.keySet();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            JmsEndpoint endpoint = (JmsEndpoint) endpoints.get(i.next());
            if (endpoint.getConnectionGoid() == connectionOid)
                found.add(endpoint);
        }
        return (JmsEndpoint[]) found.toArray(new JmsEndpoint[0]);
    }

    @Override
    public synchronized JmsEndpoint findEndpointByPrimaryKey(Goid goid) throws FindException {
        JmsEndpoint e = (JmsEndpoint)endpoints.get(goid);
        if (e == null )
            throw new FindException("No endpoint with GOID " + goid + " is known");
        else
            return e;
    }

    @Override
    public void setEndpointMessageSource(Goid goid, boolean isMessageSource) throws FindException {
        JmsEndpoint endpoint = findEndpointByPrimaryKey(goid);
        endpoint.setMessageSource(isMessageSource);        
    }

    @Override
    public synchronized Goid saveEndpoint(JmsEndpoint endpoint) throws SaveException, VersionException {
        Goid oid = endpoint.getGoid();
        if (oid == null || oid.equals(JmsEndpoint.DEFAULT_GOID)) {
            oid = new Goid(0,StubDataStore.defaultStore().nextObjectId());
            endpoint.setGoid(oid);
        }
        endpoints.put(oid, endpoint);
        return oid;
    }

    @Override
    public void deleteEndpoint( Goid endpointOid ) throws FindException, DeleteException {
        endpoints.remove(endpointOid);
    }

    @Override
    public void testConnection(JmsConnection connection) throws JmsTestException {
        // automatic success in stub mode, unless the name contains "FAIL"
        if (connection.getName().indexOf("FAIL") >= 0)
            throw new JmsTestException("Invalid JMS connection settings");
    }

    @Override
    public void testEndpoint(JmsConnection connection, JmsEndpoint endpoint) throws JmsTestException {
        // automatic success in stub mode, unless the name contains "FAIL"
        if (endpoint.getName().indexOf("FAIL") >= 0)
            throw new JmsTestException("Invalid Destination name");
    }

    @Override
    public long getDefaultJmsMessageMaxBytes() {
        return 0;
    }

    @Override
    public boolean isValidProperty(JmsMessagePropertyRule rule) {
        return true;
    }

    @Override
    public boolean isValidThreadPoolSize(String size) {
        return true;
    }

    @Override
    public boolean isDedicatedThreadPoolEnabled() {
        return true;
    }
}