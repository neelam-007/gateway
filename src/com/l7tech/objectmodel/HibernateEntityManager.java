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
        sqlBuffer.append( " = :value:" );
        return sqlBuffer.toString();
    }

    public void checkUpdate( NamedEntity ent ) throws UpdateException {
        String stmt = getFieldQuery( new Long( ent.getOid() ).toString(), F_VERSION );

        try {
            HibernatePersistenceContext hpc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Session s = hpc.getSession();
            List results = s.find( stmt );
            if ( results.size() == 0 ) {
                String err = "Object to be updated does not exist!";
                _log.log( Level.WARNING, err );
                throw new UpdateException( err );
            }

            int savedVersion = ((Integer)results.get(0)).intValue();

            if ( savedVersion != ent.getVersion() ) {
                String err = "Object to be updated is stale (a later version exists in the database)!";
                _log.log( Level.WARNING, err );
                throw new StaleUpdateException( err );
            }
        } catch (SQLException e) {
            _log.log( Level.SEVERE, e.getMessage(), e );
            throw new UpdateException( e.getMessage(), e );
        } catch (HibernateException e) {
            _log.log( Level.SEVERE, e.getMessage(), e );
            throw new UpdateException( e.getMessage(), e );
        }
    }

    public abstract Class getImpClass();
    public abstract Class getInterfaceClass();
    public abstract String getTableName();

    public Collection findAllHeaders() throws FindException {
        try {
            Iterator i = _manager.find( getContext(), getAllQuery() ).iterator();
            NamedEntity ne;
            Entity e;
            EntityHeader header;
            List headers = new ArrayList(5);
            while ( i.hasNext() ) {
                e = (Entity)i.next();
                if ( e instanceof NamedEntity ) {
                    ne = (NamedEntity)e;
                    header = new EntityHeader(ne.getOid(), EntityType.fromInterface(getInterfaceClass()), ne.getName(), EMPTY_STRING);
                } else
                    header = new EntityHeader(e.getOid(), EntityType.fromInterface(getInterfaceClass()), EMPTY_STRING, EMPTY_STRING);
                headers.add(header);
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
        return "from " + getTableName() + " in class " + getImpClass().getName();
    }

    protected PersistenceContext getContext() throws SQLException {
        return PersistenceContext.getCurrent();
    }

    protected PersistenceManager _manager;
    protected Logger _log = LogManager.getInstance().getSystemLogger();
}
