/*
 * Copyright (c) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.objectmodel.imp.EntityHeaderImp;
import com.l7tech.identity.IdentityProviderConfig;

import java.util.*;
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

    public abstract Class getImpClass();
    public abstract Class getInterfaceClass();
    public abstract String getTableName();

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

    public Collection findAllHeaders() throws FindException {
        try {
            Iterator i = _manager.find( getContext(), getAllQuery() ).iterator();
            NamedEntity config;
            EntityHeader header;
            List headers = new ArrayList(5);
            while ( i.hasNext() ) {
                config = (IdentityProviderConfig)i.next();
                header = new EntityHeaderImp( config.getOid(), getInterfaceClass(), config.getName() );
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
        if ( _context == null )
            _context = PersistenceContext.getCurrent();
        return _context;
    }

    protected PersistenceManager _manager;
    protected PersistenceContext _context;
}
