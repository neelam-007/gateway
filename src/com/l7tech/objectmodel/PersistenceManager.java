/*
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.Entity;
import com.l7tech.misc.Locator;

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

    public static List find( String query, Object param, Class paramClass ) {
        checkInstance();
        return _instance.doFind( query, param, paramClass );
    }

    public static List find( String query, Object[] params, Class[] paramClasses ) {
        checkInstance();
        return _instance.doFind( query, params, paramClasses );
    }

    public static Entity load( Class clazz, long oid ) {
        checkInstance();
        return _instance.doLoad( clazz, oid );
    }

    public static Entity loadForUpdate( Class clazz, long oid ) {
        checkInstance();
        return _instance.doLoadForUpdate( clazz, oid );
    }

    public static long save( Entity obj ) throws SQLException {
        checkInstance();
        return _instance.doSave( obj );
    }

    public static void delete( Entity obj ) {
        checkInstance();
        _instance.doDelete( obj );
    }

    public static EntityManager getEntityManager(Class clazz) {
        checkInstance();
        EntityManager manager = (EntityManager)Locator.getInstance().locate( clazz );
        return manager;
    }

    static void checkInstance() {
        if ( _instance == null ) throw new IllegalStateException( "A concrete PersistenceManager has not yet been initialized!");
    }

    abstract List doFind( String query, Object param, Class paramClass );
    abstract List doFind( String query, Object[] params, Class[] paramClasses );
    abstract Entity doLoad( Class clazz, long oid );
    abstract Entity doLoadForUpdate( Class clazz, long oid );
    abstract long doSave( Entity obj ) throws SQLException;
    abstract void doDelete( Entity obj );

    static PersistenceManager _instance;
}
