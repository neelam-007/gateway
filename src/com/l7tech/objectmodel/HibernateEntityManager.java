/*
 * Copyright (c) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.HibernateException;
import cirrus.hibernate.Session;
import com.l7tech.logging.LogManager;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class HibernateEntityManager implements EntityManager {
    public static final String EMPTY_STRING = "";
    public static final String F_OID = "oid";
    public static final String F_VERSION = "version";

    /**
     * Constructs a new <code>HibernateEntityManager</code>.
     */
    public HibernateEntityManager() {
        PersistenceManager manager = PersistenceManager.getInstance();
        if ( !(manager instanceof HibernatePersistenceManager ) ) throw new IllegalStateException( "Can't instantiate a " + getClass().getName() + "without first initializing a HibernatePersistenceManager!");
        _manager = manager;
    }

    /**
     * Generates a Hibernate query string for retrieving a single field from a User.
     * @param oid The objectId of the User to query
     * @param getfield the (aliased) name of the field to return
     * @return
     */
    protected String getFieldQuery( String oid, String getfield ) {
        String alias = getTableName();
        StringBuffer sqlBuffer = new StringBuffer( "SELECT " );
        sqlBuffer.append( alias );
        sqlBuffer.append( "." );
        sqlBuffer.append( getfield );
        sqlBuffer.append( " FROM " );
        sqlBuffer.append( alias );
        sqlBuffer.append( " in class " );
        sqlBuffer.append( getImpClass().getName() );
        sqlBuffer.append( " WHERE " );
        sqlBuffer.append( alias );
        sqlBuffer.append( "." );
        sqlBuffer.append( F_OID );
        sqlBuffer.append( " = '" );
        sqlBuffer.append( oid );
        sqlBuffer.append( "'" );
        return sqlBuffer.toString();
    }

    public void checkUpdate( Entity ent ) throws UpdateException {
        String stmt = getFieldQuery( new Long( ent.getOid() ).toString(), F_VERSION );

        try {
            HibernatePersistenceContext hpc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Session s = hpc.getSession();
            List results = s.find( stmt );
            if ( results.size() == 0 ) {
                String err = "Object to be updated does not exist!";
                logger.log( Level.WARNING, err );
                throw new UpdateException( err );
            }

            int savedVersion = ((Integer)results.get(0)).intValue();

            if ( savedVersion != ent.getVersion() ) {
                String err = "Object to be updated is stale (a later version exists in the database)!";
                logger.log( Level.WARNING, err );
                throw new StaleUpdateException( err );
            }
        } catch (SQLException e) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new UpdateException( e.getMessage(), e );
        } catch (HibernateException e) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new UpdateException( e.getMessage(), e );
        }
    }

    public abstract Class getImpClass();
    public abstract Class getInterfaceClass();
    public abstract String getTableName();

    public Collection findAllHeaders() throws FindException {
        try {
            List headers = new ArrayList();
            List results = _manager.find( getContext(), allHeadersQuery);
            for (Iterator i = results.iterator(); i.hasNext();) {
                Object[] row = (Object[])i.next();
                final long id = ((Long)row[0]).longValue();
                headers.add(new EntityHeader(id, EntityType.fromInterface(getInterfaceClass()), row[1].toString(), EMPTY_STRING));
            }
            return Collections.unmodifiableList(headers);
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public Collection findAllHeaders( int offset, int windowSize ) throws FindException {
        throw new UnsupportedOperationException( "Not yet implemented!" );
    }

    public Collection findAll() throws FindException {
        try {
            return _manager.find( getContext(), getAllQuery() );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException( "Not yet implemented!" );
    }

    public String getAllQuery() {
        String alias = getTableName();
        return "from " + alias + " in class " + getImpClass().getName();
    }


    protected PersistenceContext getContext() throws SQLException {
        return PersistenceContext.getCurrent();
    }

    private String alias = getTableName();

    /**
     * all headers query,
     */
    protected final String allHeadersQuery = "select " + alias + ".oid, " +
                                             alias + ".name from " + alias + " in class "+
                                             getImpClass().getName();
    protected PersistenceManager _manager;
    protected Logger logger = LogManager.getInstance().getSystemLogger();


}
