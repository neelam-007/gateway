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

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsAdminImpl extends RemoteService implements JmsAdmin {
    public JmsAdminImpl( String[] configOptions, LifeCycle lifecycle ) throws ConfigurationException, IOException {
        super( configOptions, lifecycle );
    }

    public JmsProvider[] getProviderList() throws RemoteException, FindException {
        return (JmsProvider[])getConnectionManager().findAllProviders().toArray(new JmsProvider[0]);
    }

    public JmsConnection[] findAllConnections() throws RemoteException, FindException {
        Collection found = getConnectionManager().findAll();
        if ( found == null || found.size() < 1 ) return new JmsConnection[0];

        for (Iterator i = found.iterator(); i.hasNext();) {
            EntityHeader entityHeader = (EntityHeader) i.next();
            JmsConnection conn = getConnectionManager().findConnectionByPrimaryKey( entityHeader.getOid() );
            found.add( conn );
        }

        return (JmsConnection[])found.toArray( new JmsConnection[0] );
    }

    public JmsConnection findConnectionByPrimaryKey( long oid ) throws RemoteException, FindException {
        return getConnectionManager().findConnectionByPrimaryKey( oid );
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

    public void saveAllMonitoredEndpoints( long[] newSourceOids ) throws RemoteException, FindException, UpdateException {
        _logger.info( "Saving monitored endpoint list" );
        HibernatePersistenceContext context = null;
        try {
            final JmsEndpointManager endManager = getEndpointManager();
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            context.beginTransaction();

            // TODO make us smart! This algorithm is slow!

            // Stop all endpoints that were previously message sources that are no longer
            Collection oldSourceEnds = endManager.findMessageSourceEndpoints();
            for ( Iterator i = oldSourceEnds.iterator(); i.hasNext(); ) {
                final JmsEndpoint oldSourceEndpoint = (JmsEndpoint) i.next();
                boolean wasMessageSource = false;
                for ( int j = 0; j < newSourceOids.length; j++ ) {
                    long oid = newSourceOids[j];
                    if ( oid == oldSourceEndpoint.getOid() ) wasMessageSource = true;
                }
                if ( !wasMessageSource ) {
                    oldSourceEndpoint.setMessageSource( false );
                }
            }

            // Start all endpoints that are newly message sources
            for ( int i = 0; i < newSourceOids.length; i++ ) {
                long oid = newSourceOids[i];
                final JmsEndpoint oldEndpoint = endManager.findByPrimaryKey( oid );
                if ( oldEndpoint != null && !oldEndpoint.isMessageSource() ) {
                    oldEndpoint.setMessageSource(true);
                }
            }

            context.commitTransaction();
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new FindException( e.toString(), e );
        } finally {
            if ( context != null ) context.close();
        }
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
            if ( context != null ) context.close();
        }
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
            else
                endManager.update( endpoint );

            context.commitTransaction();
            return oid;
        } catch ( SQLException e ) {
            throw new SaveException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new SaveException( e.toString(), e );
        } finally {
            if ( context != null ) context.close();
        }
    }

    public void deleteEndpoint( long endpointOid ) throws RemoteException, FindException, DeleteException {
        HibernatePersistenceContext context = null;
        try {
            JmsEndpointManager endManager = getEndpointManager();

            context = (HibernatePersistenceContext) PersistenceContext.getCurrent();
            context.beginTransaction();

            JmsEndpoint end = endManager.findByPrimaryKey( endpointOid );
            endManager.delete( end );
            context.commitTransaction();
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new DeleteException( e.toString(), e );
        } finally {
            if ( context != null ) context.close();
        }
    }

    public void deleteConnection( long connectionOid ) throws RemoteException, FindException, DeleteException {
        HibernatePersistenceContext context = null;
        try {
            JmsConnectionManager manager = getConnectionManager();
            context = (HibernatePersistenceContext) PersistenceContext.getCurrent();
            context.beginTransaction();

            JmsConnection conn = manager.findConnectionByPrimaryKey( connectionOid );
            manager.delete( conn );
            context.commitTransaction();
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new DeleteException( e.toString(), e );
        } finally {
            if ( context != null ) context.close();
        }
    }

    public JmsEndpoint[] getEndpointsForConnection(long connectionOid) throws RemoteException, FindException {
        return getEndpointManager().findEndpointsForConnection( connectionOid );
    }

    private JmsConnectionManager getConnectionManager() {
        return (JmsConnectionManager)Locator.getDefault().lookup(JmsConnectionManager.class);
    }

    private JmsEndpointManager getEndpointManager() {
        return (JmsEndpointManager)Locator.getDefault().lookup(JmsEndpointManager.class);
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
}
