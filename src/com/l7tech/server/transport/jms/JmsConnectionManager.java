/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.*;
import com.l7tech.logging.LogManager;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Hibernate manager for JMS connections and endpoints.  Endpoints cannot be found
 * directly using this class, only by reference from their associated Connection.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsConnectionManager extends HibernateEntityManager {
    private List _allProviders = null;

    public Collection findAllProviders() throws FindException {
        // TODO make this real, eh?!!
        if ( _allProviders == null ) {
            JmsProvider mqseries = new JmsProvider( "IBM MQSeries", "com.ibm.SomethingOrOther", "QueueConnectionFactory" );
            List list = new ArrayList();
            list.add( mqseries );
            _allProviders = list;
        }
        return _allProviders;
    }

    public JmsConnection findConnectionByPrimaryKey( long oid ) throws FindException {
        try {
            return (JmsConnection)PersistenceManager.findByPrimaryKey( getContext(), JmsConnection.class, oid );
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
    }

    public long save( final JmsConnection conn ) throws SaveException {
        _logger.info( "Saving JmsConnection " + conn );
        try {
            return PersistenceManager.save( getContext(), conn );
        } catch ( SQLException e ) {
            throw new SaveException(e.toString(), e);
        }
    }

    public void update( final JmsConnection conn ) throws UpdateException {
        _logger.info( "Updating JmsConnection " + conn );

        try {
            PersistenceManager.update( getContext(), conn );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        }
    }


    /**
     * Deletes a {@link JmsConnection} and all associated {@link com.l7tech.common.transport.jms.JmsEndpoint}s.
     *
     * Must be called within a transaction!
     * @param connection the object to be deleted.
     * @throws DeleteException if the connection, or one of its dependent endpoints, cannot be deleted.
     * @throws FindException if the connection, or one of its dependent endpoints, cannot be found.
     */
    public void delete( final JmsConnection connection ) throws DeleteException, FindException {
        _logger.info( "Deleting JmsConnection " + connection );

        try {
            JmsEndpointManager endpointManager = getEndpointManager();
            EntityHeader[] endpoints = endpointManager.findEndpointHeadersForConnection( connection.getOid() );

            for ( int i = 0; i < endpoints.length; i++ )
                endpointManager.delete( endpoints[i].getOid() );

            PersistenceManager.delete( getContext(),  connection );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }

    private JmsEndpointManager getEndpointManager() {
        if ( _endpointManager == null ) {
            _endpointManager = (JmsEndpointManager)Locator.getDefault().lookup( JmsEndpointManager.class );
        }
        return _endpointManager;
    }

    public Class getImpClass() {
        return JmsConnection.class;
    }

    public Class getInterfaceClass() {
        return JmsConnection.class;
    }

    public String getTableName() {
        return "jms_connection";
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
    private JmsEndpointManager _endpointManager;
}
