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
        return (JmsProvider[])getJmsManager().findAllProviders().toArray(new JmsProvider[0]);
    }

    public EntityHeader[] findAllConnections() throws RemoteException, FindException {
        Collection headers = getJmsManager().findAllHeaders();

        if ( headers == null || headers.size() < 1 ) return new EntityHeader[0];

        return (EntityHeader[])headers.toArray( new EntityHeader[0] );
    }

    public JmsConnection findConnectionByPrimaryKey( long oid ) throws RemoteException, FindException {
        return getJmsManager().findConnectionByPrimaryKey( oid );
    }

    public EntityHeader[] findAllMonitoredEndpoints() throws RemoteException, FindException {
        Collection endpoints = getJmsManager().findMessageSourceEndpoints();
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
            final JmsManager manager = getJmsManager();
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            context.beginTransaction();

            // TODO make us smart! This algorithm is slow!

            // Stop all endpoints that were previously message sources that are no longer
            Collection oldSourceEnds = manager.findMessageSourceEndpoints();
            for ( Iterator i = oldSourceEnds.iterator(); i.hasNext(); ) {
                final JmsEndpoint oldSourceEndpoint = (JmsEndpoint) i.next();
                boolean wasMessageSource = false;
                for ( int j = 0; j < newSourceOids.length; j++ ) {
                    long oid = newSourceOids[j];
                    if ( oid == oldSourceEndpoint.getOid() ) wasMessageSource = true;
                }
                if ( !wasMessageSource ) {
                    oldSourceEndpoint.setMessageSource( false );
                    context.registerTransactionListener( new TransactionListener() {
                        public void postCommit() {
                            manager.fireChanged(oldSourceEndpoint, false);
                        }

                        public void postRollback() { }
                    });
                }
            }

            // Start all endpoints that are newly message sources
            for ( int i = 0; i < newSourceOids.length; i++ ) {
                long oid = newSourceOids[i];
                final JmsEndpoint oldEndpoint = manager.findEndpointByPrimaryKey( oid );
                if ( oldEndpoint != null && !oldEndpoint.isMessageSource() ) {
                    oldEndpoint.setMessageSource(true);
                    context.registerTransactionListener( new TransactionListener() {
                        public void postCommit() {
                            manager.fireChanged(oldEndpoint, false);
                        }

                        public void postRollback() { }
                    });
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
            JmsManager manager = getJmsManager();
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
            JmsManager manager = getJmsManager();
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            context.beginTransaction();

            long oid = endpoint.getOid();
            if ( oid == JmsConnection.DEFAULT_OID )
                oid = manager.save( endpoint );
            else
                manager.update( endpoint );

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
            JmsManager manager = getJmsManager();
            context = (HibernatePersistenceContext) PersistenceContext.getCurrent();
            context.beginTransaction();

            JmsEndpoint conn = manager.findEndpointByPrimaryKey( endpointOid );
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

    public void deleteConnection( long connectionOid ) throws RemoteException, FindException, DeleteException {
        HibernatePersistenceContext context = null;
        try {
            JmsManager manager = getJmsManager();
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

    public EntityHeader[] getEndpointHeaders( long connectionOid ) throws RemoteException, FindException {
        return getJmsManager().findEndpointHeadersForConnection( connectionOid );
    }

    private JmsManager getJmsManager() {
        return (JmsManager)Locator.getDefault().lookup(JmsManager.class);
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
}
