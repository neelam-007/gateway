/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.*;
import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;
import java.io.IOException;

import net.jini.config.ConfigurationException;

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
                end.setMessageSource(true);
            }

            context.commitTransaction();
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new FindException( e.toString(), e );
        } finally {
            try {
                if ( context != null ) context.commitTransaction();
            } catch ( TransactionException e ) {
                throw new UpdateException( e.toString(), e );
            }
        }
    }

    public long saveConnection( JmsConnection connection ) throws RemoteException, UpdateException, SaveException, VersionException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteConnection( long connectionOid ) throws RemoteException, DeleteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private JmsManager getJmsManager() {
        return (JmsManager)Locator.getDefault().lookup(JmsManager.class);
    }
}
