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
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

    public void saveAllMonitoredEndpoints( long[] oids ) throws RemoteException, FindException, UpdateException {
        HibernatePersistenceContext context = null;
        try {
            JmsManager manager = getJmsManager();
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            context.beginTransaction();

            Collection sourceEnds = manager.findMessageSourceEndpoints();
            for ( Iterator i = sourceEnds.iterator(); i.hasNext(); ) {
                JmsEndpoint endpoint = (JmsEndpoint) i.next();
                boolean found = false;
                for ( int j = 0; j < oids.length; j++ ) {
                    long oid = oids[j];
                    if ( oid == endpoint.getOid() ) found = true;
                }
                if ( !found ) endpoint.setMessageSource( false );
            }

            for ( int i = 0; i < oids.length; i++ ) {
                long oid = oids[i];
                JmsEndpoint end = manager.findEndpointByPrimaryKey( oid );
                if ( end != null )
                    end.setMessageSource(true);
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

    public long saveConnection( JmsConnection connection ) throws RemoteException, UpdateException, SaveException, VersionException {
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

    public long saveEndpoint( JmsEndpoint endpoint ) throws RemoteException, UpdateException, SaveException, VersionException {
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
}
