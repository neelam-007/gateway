package com.l7tech.objectmodel.hibernate;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 7-May-2003
 * Time: 12:06:26 PM
 * To change this template use Options | File Templates.
 */
public class PersistenceManager {
    public static PersistenceManager getInstance() {
        if ( _instance == null ) throw new IllegalStateException( "A concrete PersistenceManager has not yet been initialized!");
        return _instance;
    }

    public static void setInstance( PersistenceManager instance ) {
        if ( _instance == null )
            _instance = instance;
        else
            throw new IllegalStateException( "PersistenceManager can only be initialized once!")
    }

    static PersistenceManager _instance;
}
