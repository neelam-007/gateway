/*
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.common.util.Locator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistenceManager {
    public static PersistenceManager getInstance() {
        checkInstance();
        return _instance;
    }

    static void setInstance( PersistenceManager instance ) {
        if ( _instance == null )
            _instance = instance;
        else
            throw new IllegalStateException( "PersistenceManager can only be initialized once!");
    }

    public static List find( PersistenceContext context, String query ) throws FindException {
        checkInstance();
        return _instance.doFind( context, query );
    }

    public static List find( PersistenceContext context, String query, Object param, Class paramClass ) throws FindException {
        checkInstance();
        return _instance.doFind( context, query, param, paramClass );
    }

    public static List find( PersistenceContext context, String query, Object[] params, Class[] paramClasses ) throws FindException {
        checkInstance();
        return _instance.doFind( context, query, params, paramClasses );
    }

    public static Entity findByPrimaryKey( PersistenceContext context, Class clazz, long oid ) throws FindException {
        checkInstance();
        return _instance.doFindByPrimaryKey( context, clazz, oid );
    }

    public static long save( PersistenceContext context, Entity obj ) throws SaveException {
        checkInstance();
        return _instance.doSave( context, obj );
    }

    public static void update( PersistenceContext context, Entity obj ) throws UpdateException {
        checkInstance();
        _instance.doUpdate( context, obj );
    }

    public static void delete( PersistenceContext context, Entity obj ) throws DeleteException {
        checkInstance();
        _instance.doDelete( context, obj );
    }

    public static void delete( PersistenceContext context, Class entityClass, long oid ) throws DeleteException {
        checkInstance();
        _instance.doDelete( context, entityClass, oid );
    }

    /*
    public static void beginTransaction( PersistenceContext context ) throws TransactionException {
        checkInstance();
        _instance.doBeginTransaction( context );
    }

    public static void commitTransaction( PersistenceContext context ) throws TransactionException {
        checkInstance();
        _instance.doCommitTransaction( context );
    }

    public static void rollbackTransaction( PersistenceContext context ) throws TransactionException {
        checkInstance();
        _instance.doRollbackTransaction( context );
    }
    */

    public static PersistenceContext makeContext() throws SQLException {
        checkInstance();
        return _instance.doMakeContext();
    }

    public static EntityManager getEntityManager(Class clazz) {
        checkInstance();
        EntityManager manager = (EntityManager)Locator.getDefault().lookup( clazz );
        return manager;
    }

    static void checkInstance() {
        if ( _instance == null ) throw new IllegalStateException( "A concrete PersistenceManager has not yet been initialized!");
    }

    /*
    abstract void doBeginTransaction( PersistenceContext context ) throws TransactionException;
    abstract void doCommitTransaction( PersistenceContext context ) throws TransactionException;
    abstract void doRollbackTransaction( PersistenceContext context ) throws TransactionException;
    */
    abstract List doFind( PersistenceContext context, String query ) throws FindException;
    abstract List doFind( PersistenceContext context, String query, Object param, Class paramClass ) throws FindException;
    abstract List doFind( PersistenceContext context, String query, Object[] params, Class[] paramClasses ) throws FindException;
    abstract Entity doFindByPrimaryKey( PersistenceContext context, Class clazz, long oid ) throws FindException;
    abstract Entity doFindByPrimaryKey( PersistenceContext context, Class clazz, long oid, boolean forUpdate ) throws FindException;
    abstract long doSave( PersistenceContext context, Entity obj ) throws SaveException;
    abstract void doUpdate( PersistenceContext context, Entity obj ) throws UpdateException;
    abstract void doDelete( PersistenceContext context, Entity obj ) throws DeleteException;
    abstract void doDelete( PersistenceContext context, Class entityClass, long oid ) throws DeleteException;
    abstract PersistenceContext doMakeContext() throws SQLException;

    static PersistenceManager _instance;
    static final List EMPTYLIST = Collections.unmodifiableList( new ArrayList() );
}
