/*
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.Entity;
import com.l7tech.misc.Locator;

import java.util.*;

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
        return _instance.find( query, param, paramClass );
    }

    public static List find( String query, Object[] params, Class[] paramClasses ) {
        checkInstance();
        return _instance.find( query, params, paramClasses );
    }

    public static Entity load( Class clazz, long oid ) {
        checkInstance();
        return _instance.load( clazz, oid );
    }

    public static Entity loadForUpdate( Class clazz, long oid ) {
        checkInstance();
        return _instance.loadForUpdate( clazz, oid );
    }

    public static long save( Entity obj ) {
        checkInstance();
        return _instance.save( obj );
    }

    public static void delete( Entity obj ) {
        checkInstance();
        _instance.delete( obj );
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
    abstract long doSave( Entity obj );
    abstract void doDelete( Entity obj );

    static PersistenceManager _instance;
}
