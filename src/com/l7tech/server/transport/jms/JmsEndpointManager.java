/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.objectmodel.*;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.logging.LogManager;

import java.sql.SQLException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsEndpointManager extends HibernateEntityManager {

    public JmsEndpoint findByPrimaryKey( long oid ) throws FindException {
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

    public JmsEndpoint[] findEndpointsForConnection(long connectionOid) throws FindException {
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
                    result.add( findByPrimaryKey(oid) );
                }
            }

        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
        return (JmsEndpoint[])result.toArray( new JmsEndpoint[0] );
    }

    public EntityHeader[] findEndpointHeadersForConnection(long connectionOid) throws FindException {
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

    public long save( final JmsEndpoint endpoint ) throws SaveException {
        _logger.info( "Saving JmsEndpoint " + endpoint );
        try {
            return PersistenceManager.save( getContext(), endpoint );
        } catch ( SQLException e ) {
            throw new SaveException(e.toString(), e);
        }
    }

    public void update( final JmsEndpoint endpoint ) throws UpdateException {
        _logger.info( "Saving JmsEndpoint " + endpoint );
        try {
            PersistenceManager.update( getContext(), endpoint );
        } catch (SQLException e) {
            throw new UpdateException(e.toString(), e);
        }
    }

    public void delete( final JmsEndpoint endpoint ) throws DeleteException {
        _logger.info( "Deleting JmsEndpoint " + endpoint );
        try {
            PersistenceManager.delete( getContext(), endpoint );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }

    public void delete( final long oid ) throws DeleteException {
        _logger.info( "Deleting JmsEndpoint " + oid );
        try {
            PersistenceManager.delete( getContext(), JmsEndpoint.class, oid );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }

    public Class getImpClass() {
        return JmsEndpoint.class;
    }

    public Class getInterfaceClass() {
        return JmsEndpoint.class;
    }

    public String getTableName() {
        return "jms_endpoint";
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
}
