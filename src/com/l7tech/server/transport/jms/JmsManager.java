/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.objectmodel.*;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsConnection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Hibernate manager for JMS connections and endpoints.  Endpoints cannot be found
 * directly using this class, only by reference from their associated Connection.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsManager extends HibernateEntityManager {
    public long save( JmsConnection conn ) throws SaveException {
        try {
            return PersistenceManager.save( getContext(), conn );
        } catch (SQLException e) {
            throw new SaveException(e.toString(), e);
        }
    }

    public long save( JmsEndpoint endpoint ) throws SaveException {
        try {
            return PersistenceManager.save( getContext(), endpoint );
        } catch (SQLException e) {
            throw new SaveException(e.toString(), e);
        }
    }

    public void update( JmsConnection conn ) throws UpdateException {
        try {
            PersistenceManager.update( getContext(), conn );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        }
    }

    public void update( JmsEndpoint endpoint ) throws UpdateException {
        try {
            PersistenceManager.update( getContext(), endpoint );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        }
    }

    public void delete( JmsConnection connection ) throws DeleteException {
        try {
            PersistenceManager.delete( getContext(), connection );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }

    public void delete( JmsEndpoint endpoint ) throws DeleteException {
        try {
            PersistenceManager.delete( getContext(), endpoint );
        } catch ( SQLException e ) {
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

    private List _crudListeners = Collections.synchronizedList( new ArrayList() );
}
