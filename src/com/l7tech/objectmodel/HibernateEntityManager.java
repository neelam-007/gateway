/*
 * Copyright (c) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.HibernatePersistenceManager;

import java.util.Collection;
import java.sql.SQLException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class HibernateEntityManager implements EntityManager {
    /**
     * Constructs a new <code>HibernateEntityManager</code>.
     */
    public HibernateEntityManager() {
        PersistenceManager manager = PersistenceManager.getInstance();
        if ( !(manager instanceof HibernatePersistenceManager ) ) throw new IllegalStateException( "Can't instantiate a " + getClass().getName() + "without first initializing a HibernatePersistenceManager!");
        _manager = manager;
    }

    protected PersistenceContext getContext() throws SQLException {
        if ( _context == null )
            _context = PersistenceContext.getCurrent();
        return _context;
    }

    /**
     * Constructs a new <code>HibernateEntityManager</code> with a specific context.
     * @param context
     */
    public HibernateEntityManager( PersistenceContext context ) {
        PersistenceManager manager = PersistenceManager.getInstance();
        if ( !(manager instanceof HibernatePersistenceManager ) ) throw new IllegalStateException( "Can't instantiate a " + getClass().getName() + "without first initializing a HibernatePersistenceManager!");
        _manager = manager;
        _context = context;
    }

    public abstract Collection findAll() throws FindException;
    public abstract Collection findAll(int offset, int windowSize) throws FindException;

    public Collection findAllHeaders() throws FindException {
        // TODO
        return null;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        // TODO
        return null;
    }

    protected PersistenceManager _manager;
    protected PersistenceContext _context;
}
