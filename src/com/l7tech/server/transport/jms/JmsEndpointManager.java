/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
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
            return (EntityHeader[])result.toArray( new EntityHeader[0] );
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
    }

    public long save( final JmsEndpoint endpoint ) throws SaveException {
        _logger.info( "Saving JmsEndpoint " + endpoint );
        try {
            return PersistenceManager.save( getContext(), endpoint );
        } catch ( SQLException e ) {
            throw new SaveException(e.toString(), e);
        }
    }

    public void update( final JmsEndpoint endpoint ) throws VersionException, UpdateException {
        _logger.info( "Updating JmsEndpoint" + endpoint );

        JmsEndpoint original = null;
        // check for original endpoint
        try {
            original = findByPrimaryKey(endpoint.getOid());
        } catch (FindException e) {
            throw new UpdateException("could not get original endpoint", e);
        }

        // check version
        if (original.getVersion() != endpoint.getVersion()) {
            logger.severe("db endpoint has version: " + original.getVersion() + ". requestor endpoint has version: "
                          + endpoint.getVersion());
            throw new VersionException("the endpoint you are trying to update is no longer valid.");
        }

        // update
        PersistenceContext context = null;
        try {
            original.copyFrom(endpoint);

            context = getContext();
            PersistenceManager.update(context, original);
            logger.info( "Updated JmsEndpoint #" + endpoint.getOid() );
        } catch ( SQLException se ) {
            logger.log( Level.SEVERE, se.toString(), se );
            throw new UpdateException( se.toString(), se );
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
