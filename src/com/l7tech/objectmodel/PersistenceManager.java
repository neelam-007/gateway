/*
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.Entity;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 7-May-2003
 * Time: 12:06:26 PM
 * To change this template use Options | File Templates.
 * @version $Revision$
 */
public abstract class PersistenceManager {
    public static PersistenceManager getInstance() {
        if ( _instance == null ) throw new IllegalStateException( "A concrete PersistenceManager has not yet been initialized!");
        return _instance;
    }

    public static void setInstance( PersistenceManager instance ) {
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

    static void checkInstance() {
        if ( _instance == null ) throw new IllegalStateException( "PersistenceManager has not been initialized!" );
    }

    abstract List doFind( String query, Object param, Class paramClass );
    abstract List doFind( String query, Object[] params, Class[] paramClasses );
    abstract Entity doLoad( Class clazz, long oid );
    abstract Entity doLoadForUpdate( Class clazz, long oid );
    abstract long doSave( Entity obj );
    abstract void doDelete( Entity obj );

    static PersistenceManager _instance;
}
