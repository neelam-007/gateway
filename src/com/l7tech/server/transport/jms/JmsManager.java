/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsProvider;
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
public class JmsManager extends HibernateEntityManager {
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

    public JmsEndpoint findEndpointByPrimaryKey( long oid ) throws FindException {
        try {
            return (JmsEndpoint)PersistenceManager.findByPrimaryKey( getContext(), JmsEndpoint.class, oid );
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
    }


    public Collection findMessageSourceEndpoints() throws FindException {
        StringBuffer query = new StringBuffer( "from endpoint in class " );
        query.append( JmsEndpoint.class.getName() );
        query.append( " where endpoint.messageSource = ?" );
        try {
            Collection endpoints = PersistenceManager.find( getContext(), query.toString(), Boolean.TRUE, Boolean.TYPE );
            return endpoints;
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
    }

    public EntityHeader[] findEndpointHeadersForConnection( long connectionOid ) throws FindException {
        StringBuffer sql = new StringBuffer( "select endpoint.oid, endpoint.name, endpoint.destinationName " );
        sql.append( "from endpoint in class " );
        sql.append( JmsEndpoint.class.getName() );
        sql.append( " where endpoint.connectionOid = ?" );
        ArrayList result = new ArrayList();
        try {
            List results = PersistenceManager.find( PersistenceContext.getCurrent(), sql.toString(), new Long( connectionOid ), Long.TYPE );
            for ( Iterator i = results.iterator(); i.hasNext(); ) {
                Object[] row = (Object[]) i.next();
                if ( row[0] instanceof Long ) {
                    long oid = ((Long)row[0]).longValue();
                    EntityHeader header = new EntityHeader( oid, EntityType.JMS_ENDPOINT, (String)row[1], (String)row[2] );
                    result.add( header );
                }
            }

        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
        return (EntityHeader[])result.toArray( new EntityHeader[0] );
    }


    private void addTransactionListener( final Object source, final boolean deleted ) throws SQLException, TransactionException {
        HibernatePersistenceContext.getCurrent().registerTransactionListener( new TransactionListener() {
            public void postCommit() {
                fireChanged( source, deleted );
            }

            public void postRollback() { }
        });
    }

    public long save( final JmsConnection conn ) throws SaveException {
        _logger.info( "Saving JmsConnection " + conn );
        try {
            addTransactionListener( conn, false );
            return PersistenceManager.save( getContext(), conn );
        } catch ( SQLException e ) {
            throw new SaveException(e.toString(), e);
        } catch ( TransactionException e ) {
            throw new SaveException( e.toString(), e );
        }
    }

    public long save( final JmsEndpoint endpoint ) throws SaveException {
        _logger.info( "Saving JmsEndpoint " + endpoint );
        try {
            addTransactionListener( endpoint, false );
            return PersistenceManager.save( getContext(), endpoint );
        } catch ( SQLException e ) {
            throw new SaveException(e.toString(), e);
        } catch ( TransactionException e ) {
            throw new SaveException( e.toString(), e );
        }
    }

    public void update( final JmsConnection conn ) throws UpdateException {
        _logger.info( "Updating JmsConnection " + conn );

        try {
            addTransactionListener( conn, false );
            PersistenceManager.update( getContext(), conn );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        } catch ( TransactionException e ) {
            throw new UpdateException( e.toString(), e );
        }
    }

    public void update( final JmsEndpoint endpoint ) throws UpdateException {
        _logger.info( "Saving JmsEndpoint " + endpoint );
        try {
            addTransactionListener( endpoint, false );
            PersistenceManager.update( getContext(), endpoint );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        } catch ( TransactionException e ) {
            throw new UpdateException( e.toString(), e );
        }
    }


    public void delete( final JmsEndpoint endpoint ) throws DeleteException {
        _logger.info( "Deleting JmsEndpoint " + endpoint );
        try {
            addTransactionListener( endpoint, true );
            PersistenceManager.delete( getContext(), endpoint );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }


    public void delete( final JmsConnection connection ) throws DeleteException, FindException {
        _logger.info( "Deleting JmsConnection " + connection );

        try {
            addTransactionListener( connection, true );
            EntityHeader[] endpoints = findEndpointHeadersForConnection( connection.getOid() );

            for ( int i = 0; i < endpoints.length; i++ )
                PersistenceManager.delete( getContext(), JmsEndpoint.class, endpoints[i].getOid() );

            PersistenceManager.delete( getContext(),  connection );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new DeleteException( e.toString(), e );
        }
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

    public void addCrudListener( JmsCrudListener listener ) {
        _crudListeners.add( listener );
    }

    public void removeCrudListener( JmsCrudListener listener ) {
        _crudListeners.remove( listener );
    }

    void fireChanged( Object source, boolean deleted ) {
        for ( Iterator i = _crudListeners.iterator(); i.hasNext(); ) {
            JmsCrudListener listener = (JmsCrudListener) i.next();
            if ( source instanceof JmsEndpoint )
                if ( deleted )
                    listener.endpointDeleted( (JmsEndpoint)source );
                else
                    listener.endpointUpdated( (JmsEndpoint)source );
            else if ( source instanceof JmsConnection )
                if ( deleted )
                    listener.connectionDeleted( (JmsConnection)source );
                else
                    listener.connectionUpdated( (JmsConnection)source );
        }
    }

    private List _crudListeners = Collections.synchronizedList( new ArrayList() );
    private Logger _logger = LogManager.getInstance().getSystemLogger();
}
