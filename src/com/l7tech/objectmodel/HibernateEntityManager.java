/*
 * Copyright (c) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import net.sf.hibernate.Criteria;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

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
     * Returns the current version (in the database) of the entity with the specified OID.
     * @param oid the OID of the entity whose version should be retrieved
     * @return The version, or null if the entity does not exist.
     * @throws FindException
     */
    public Integer getVersion( long oid ) throws FindException {
        String alias = getTableName();
        String query = "SELECT " + alias + ".version"
                       + " FROM " + alias + " IN CLASS " + getImpClass().getName()
                       + " WHERE " + alias + ".oid = ?";
        try {
            List results = PersistenceManager.find( getContext(), query, new Long(oid), Long.TYPE );
            if ( results.size() == 0 ) return null;
            if ( results.size() > 1 ) throw new FindException( "Multiple results found" );
            Object result = results.get(0);
            if ( !(result instanceof Integer) ) throw new FindException( "Found " + result.getClass().getName() + " when looking for Integer!" );
            return (Integer)result;
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
    }

    public Entity findEntity( long oid ) throws FindException {
        String alias = getTableName();
        String query = "FROM " + alias +
                       " IN CLASS " + getImpClass() +
                       " WHERE " + alias + ".oid = ?";
        try {
            List results = PersistenceManager.find( getContext(), query, new Long( oid ), Long.TYPE );
            if ( results.size() == 0 ) return null;
            if ( results.size() > 1 ) throw new FindException( "Multiple results found!" );
            Object result = results.get(0);
            if ( !(result instanceof Entity) ) throw new FindException( "Found " + result.getClass().getName() + " when looking for Entity!" );
            return (Entity)results.get(0);
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }
    }

    public Map findVersionMap() throws FindException {
        Map result = new HashMap();
        Class impClass = getImpClass();
        String alias = getTableName();
        if ( !Entity.class.isAssignableFrom( impClass ) ) throw new FindException( "Can't find non-Entities!" );

        String query = "SELECT " + alias + ".oid, " + alias + ".version" +
                       " FROM " + alias +
                       " IN CLASS " + getImpClass();

        try {
            List results = PersistenceManager.find( getContext(), query );
            if ( results.size() > 0 ) {
                for ( Iterator i = results.iterator(); i.hasNext(); ) {
                    Object[] row = (Object[])i.next();
                    if ( row[0] instanceof Long && row[1] instanceof Integer ) {
                        result.put( row[0], row[1] );
                    } else {
                        throw new FindException( "Got unexpected fields " + row[0] + " and " + row[1] + " from query!" );
                    }
                }
            }
        } catch ( SQLException e ) {
            throw new FindException( e.toString(), e );
        }

        return result;
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
        Collection entities = findAll();
        List headers = new ArrayList();
        for (Iterator i = entities.iterator(); i.hasNext();) {
            Entity entity = (Entity)i.next();
            String name = null;
            if (entity instanceof NamedEntity) name = ((NamedEntity)entity).getName();
            if ( name == null ) name = "";
            final long id = entity.getOid();
            headers.add(new EntityHeader(id, EntityType.fromInterface(getInterfaceClass()), name, EMPTY_STRING));
        }
        return Collections.unmodifiableList(headers);
    }

    /**
     * Override this method to add additional criteria to findAll(), findAllHeaders(), findByName() etc.
     * @param allHeadersCriteria
     */
    protected void addFindAllCriteria( Criteria allHeadersCriteria ) {
    }

    public Collection findAllHeaders( int offset, int windowSize ) throws FindException {
        throw new UnsupportedOperationException( "Not yet implemented!" );
    }

    public Collection findAll() throws FindException {
        try {
            Session s = getContext().getSession();
            Criteria allHeadersCriteria = s.createCriteria(getImpClass());
            addFindAllCriteria(allHeadersCriteria);
            List entities = allHeadersCriteria.list();
            return entities;
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( HibernateException e ) {
            throw new FindException( e.toString(), e );
        }
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException( "Not yet implemented!" );
    }

    public String getAllQuery() {
        String alias = getTableName();
        return "from " + alias + " in class " + getImpClass().getName();
    }

    public void delete( long oid ) throws DeleteException, FindException {
        try {
            PersistenceManager.delete( getContext(), getImpClass(), oid );
        } catch ( SQLException e ) {
            throw new DeleteException( e.toString(), e );
        }
    }

    protected HibernatePersistenceContext getContext() throws SQLException {
        return (HibernatePersistenceContext)PersistenceContext.getCurrent();
    }

    protected PersistenceManager _manager;
    protected final Logger logger = Logger.getLogger(getClass().getName());
}
