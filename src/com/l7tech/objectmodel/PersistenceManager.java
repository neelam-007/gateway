/*
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.Entity;
import com.l7tech.util.Locator;

import java.util.*;
import java.sql.SQLException;

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

    public static List find( String query, Object param, Class paramClass ) throws FindException {
        checkInstance();
        return _instance.doFind( query, param, paramClass );
    }

    public static List find( String query, Object[] params, Class[] paramClasses ) throws FindException {
        checkInstance();
        return _instance.doFind( query, params, paramClasses );
    }

/*
    public static Entity load( Class clazz, long oid ) {
        checkInstance();
        return _instance.doLoad( clazz, oid );
    }

    public static Entity loadForUpdate( Class clazz, long oid ) {
        checkInstance();
        return _instance.doLoadForUpdate( clazz, oid );
    }
*/

    public static long save( Entity obj ) throws SaveException {
        checkInstance();
        return _instance.doSave( obj );
    }

    public static void delete( Entity obj ) throws DeleteException {
        checkInstance();
        _instance.doDelete( obj );
    }

    public static void beginTransaction() throws TransactionException {
        checkInstance();
        _instance.doBeginTransaction();
    }

    public static void commitTransaction() throws TransactionException {
        checkInstance();
        _instance.doCommitTransaction();
    }

    public static void rollbackTransaction() throws TransactionException {
        checkInstance();
        _instance.doRollbackTransaction();
    }

    public static EntityManager getEntityManager(Class clazz) {
        checkInstance();
        EntityManager manager = (EntityManager)Locator.getDefault().lookup( clazz );
        return manager;
    }

    static void checkInstance() {
        if ( _instance == null ) throw new IllegalStateException( "A concrete PersistenceManager has not yet been initialized!");
    }

    abstract void doBeginTransaction() throws TransactionException;
    abstract void doCommitTransaction() throws TransactionException;
    abstract void doRollbackTransaction() throws TransactionException;
    abstract List doFind( String query, Object param, Class paramClass ) throws FindException;
    abstract List doFind( String query, Object[] params, Class[] paramClasses ) throws FindException;
    //abstract Entity doLoad( Class clazz, long oid ) throws;
    //abstract Entity doLoadForUpdate( Class clazz, long oid );
    abstract long doSave( Entity obj ) throws SaveException;
    abstract void doDelete( Entity obj ) throws DeleteException;

    static PersistenceManager _instance;
}
