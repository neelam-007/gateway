/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsConnection;

import java.sql.SQLException;

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
            return _manager.save( getContext(), conn );
        } catch (SQLException e) {
            throw new SaveException(e.toString(), e);
        }
    }

    public long save( JmsEndpoint endpoint ) throws SaveException {
        try {
            return _manager.save( getContext(), endpoint );
        } catch (SQLException e) {
            throw new SaveException(e.toString(), e);
        }
    }

    public void update( JmsConnection conn ) throws UpdateException {
        try {
            _manager.update( getContext(), conn );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        }
    }

    public void update( JmsEndpoint endpoint ) throws UpdateException {
        try {
            _manager.update( getContext(), endpoint );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        }
    }

    public void delete( JmsConnection connection ) throws DeleteException {
        try {
            _manager.delete( getContext(), connection );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }

    public void delete( JmsEndpoint endpoint ) throws DeleteException {
        try {
            _manager.delete( getContext(), endpoint );
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
}
