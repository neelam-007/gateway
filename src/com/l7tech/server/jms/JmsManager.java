/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.jms;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.jms.JmsDestination;
import com.l7tech.jms.JmsProvider;

import java.sql.SQLException;

/**
 * Hibernate manager for JMS providers and destinations.  Destinations cannot be found
 * directly using this class, only by reference from their associated Provider.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsManager extends HibernateEntityManager {
    public long save( JmsProvider provider ) throws SaveException {
        try {
            return _manager.save( getContext(), provider );
        } catch (SQLException e) {
            throw new SaveException(e.toString(), e);
        }
    }

    public long save( JmsDestination destination ) throws SaveException {
        try {
            return _manager.save( getContext(), destination );
        } catch (SQLException e) {
            throw new SaveException(e.toString(), e);
        }
    }

    public void update( JmsProvider provider ) throws UpdateException {
        try {
            _manager.update( getContext(), provider );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        }
    }

    public void update( JmsDestination destination ) throws UpdateException {
        try {
            _manager.update( getContext(), destination );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        }
    }

    public void delete( JmsProvider provider ) throws DeleteException {
        try {
            _manager.delete( getContext(), provider );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }

    public void delete( JmsDestination destination ) throws DeleteException {
        try {
            _manager.delete( getContext(), destination );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }

    public Class getImpClass() {
        return JmsProvider.class;
    }

    public Class getInterfaceClass() {
        return JmsProvider.class;
    }

    public String getTableName() {
        return "jms_provider";
    }
}
