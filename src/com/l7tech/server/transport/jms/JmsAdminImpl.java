/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.*;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.logging.LogManager;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import javax.jms.JMSException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsAdminImpl extends RemoteService implements JmsAdmin {
    public JmsAdminImpl( String[] configOptions, LifeCycle lifecycle ) throws ConfigurationException, IOException {
        super( configOptions, lifecycle );
    }

    public JmsProvider[] getProviderList() throws RemoteException, FindException {
        try {
            return (JmsProvider[])getConnectionManager().findAllProviders().toArray(new JmsProvider[0]);
        } finally {
            closeContext();
        }
    }

    /**
     * Finds all {@link JmsConnection}s in the database.
     *
     * @return an array of transient {@link JmsConnection}s
     * @throws RemoteException
     * @throws FindException
     */
    public JmsConnection[] findAllConnections() throws RemoteException, FindException {
        try {
            Collection found = getConnectionManager().findAll();
            if ( found == null || found.size() < 1 ) return new JmsConnection[0];

            List results = new ArrayList();
            for (Iterator i = found.iterator(); i.hasNext();) {
                JmsConnection conn = (JmsConnection)i.next();
                results.add( conn );
            }

            return (JmsConnection[])results.toArray( new JmsConnection[0] );
        } finally {
            closeContext();
        }
    }

    /**
     * Finds the {@link JmsConnection} with the provided OID.
     *
     * @return the {@link JmsConnection} with the provided OID.
     * @throws RemoteException
     * @throws FindException
     */
    public JmsConnection findConnectionByPrimaryKey( long oid ) throws RemoteException, FindException {
        try {
            return getConnectionManager().findConnectionByPrimaryKey( oid );
        } finally {
            closeContext();
        }
    }

    public JmsEndpoint findEndpointByPrimaryKey(long oid) throws RemoteException, FindException {
        return getEndpointManager().findByPrimaryKey( oid );
    }

    public void setEndpointMessageSource(long oid, boolean isMessageSource) throws RemoteException, FindException, UpdateException {
        try {
            PersistenceContext.getCurrent().beginTransaction();
            JmsEndpoint endpoint = findEndpointByPrimaryKey(oid);
            endpoint.setMessageSource(isMessageSource);
            PersistenceContext.getCurrent().commitTransaction();
        } catch (TransactionException e) {
            throw new UpdateException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new FindException(e.getMessage(), e);
        } finally {
            closeContext();
        }
    }

    public EntityHeader[] findAllMonitoredEndpoints() throws RemoteException, FindException {
        Collection endpoints = getEndpointManager().findMessageSourceEndpoints();
        List list = new ArrayList();
        for ( Iterator i = endpoints.iterator(); i.hasNext(); ) {
            JmsEndpoint endpoint = (JmsEndpoint) i.next();
            list.add( endpoint.toEntityHeader() );
        }
        return (EntityHeader[])list.toArray( new EntityHeader[0] );
    }

    public long saveConnection( JmsConnection connection ) throws RemoteException, UpdateException,
                                                                  SaveException, VersionException {
        HibernatePersistenceContext context = null;
        try {
            JmsConnectionManager manager = getConnectionManager();
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            context.beginTransaction();

            long oid = connection.getOid();
            if ( oid == JmsConnection.DEFAULT_OID )
                oid = manager.save( connection );
            else
                manager.update(connection);

            context.commitTransaction();
            return oid;
        } catch ( SQLException e ) {
            throw new SaveException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new SaveException( e.toString(), e );
        } finally {
            closeContext();
        }
    }

    /**
     * Returns an array of {@link JmsEndpoint}s that belong to the {@link JmsConnection} with the provided OID.
     * @param connectionOid
     * @return an array of {@link JmsEndpoint}s
     * @throws RemoteException
     * @throws FindException
     */
    public JmsEndpoint[] getEndpointsForConnection(long connectionOid) throws RemoteException, FindException {
        try {
            return getEndpointManager().findEndpointsForConnection( connectionOid );
        } finally {
            closeContext();
        }
    }

    public void testConnection(JmsConnection connection) throws RemoteException, JMSException {
        throw new RuntimeException("Testing JMS connections is not yet implemented on this Gateway");
    }

    public void testEndpoint(JmsEndpoint endpoint) throws RemoteException, JMSException {
        throw new RuntimeException("Testing JMS endpoints is not yet implemented on this Gateway");
    }

    public long saveEndpoint( JmsEndpoint endpoint ) throws RemoteException, UpdateException,
                                                            SaveException, VersionException {
        HibernatePersistenceContext context = null;
        try {
            JmsEndpointManager endManager = getEndpointManager();
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            context.beginTransaction();

            long oid = endpoint.getOid();
            if ( oid == JmsConnection.DEFAULT_OID )
                oid = endManager.save( endpoint );
            else {
                endManager.update( endpoint );
            }

            context.commitTransaction();
            return oid;
        } catch ( SQLException e ) {
            throw new SaveException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new SaveException( e.toString(), e );
        } finally {
            closeContext();
        }
    }

    public void deleteEndpoint( long endpointOid ) throws RemoteException, FindException, DeleteException {
        HibernatePersistenceContext context = null;
        try {
            JmsEndpointManager endManager = getEndpointManager();

            context = (HibernatePersistenceContext) PersistenceContext.getCurrent();
            context.beginTransaction();

            endManager.delete( endpointOid );
            context.commitTransaction();
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new DeleteException( e.toString(), e );
        } finally {
            closeContext();
        }
    }

    public void deleteConnection( long connectionOid ) throws RemoteException, FindException, DeleteException {
        HibernatePersistenceContext context = null;
        try {
            JmsConnectionManager manager = getConnectionManager();
            context = (HibernatePersistenceContext) PersistenceContext.getCurrent();
            context.beginTransaction();

            manager.delete( connectionOid );
            context.commitTransaction();
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new DeleteException( e.toString(), e );
        } finally {
            closeContext();
        }
    }

    private void closeContext() {
        try {
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
            _logger.log(Level.WARNING, "error closing context", e);
        }
    }

    private JmsConnectionManager getConnectionManager() {
        return (JmsConnectionManager)Locator.getDefault().lookup(JmsConnectionManager.class);
    }

    private JmsEndpointManager getEndpointManager() {
        return (JmsEndpointManager)Locator.getDefault().lookup(JmsEndpointManager.class);
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
}
