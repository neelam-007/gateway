/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;
import java.util.*;

/**
 * Hibernate manager for JMS connections and endpoints.  Endpoints cannot be found
 * directly using this class, only by reference from their associated Connection.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsManager extends HibernateEntityManager {
    public Collection findMessageSourceEndpoints() throws FindException {
        StringBuffer query = new StringBuffer( "from endpoints in class " );
        query.append( JmsEndpoint.class.getName() );
        query.append( " where is_message_source = ?" );
        try {
            Collection endpoints = PersistenceManager.find( getContext(), query.toString(), Boolean.TRUE, Boolean.TYPE );
            return endpoints;
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
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
        try {
            addTransactionListener( endpoint, true );
            PersistenceManager.delete( getContext(), endpoint );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        } catch ( TransactionException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }


    public void delete( final JmsConnection connection ) throws DeleteException {
        try {
            addTransactionListener( connection, true );
            PersistenceManager.delete( getContext(), connection );
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

    private void fireChanged( Object source, boolean deleted ) {
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
}
