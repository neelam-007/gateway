/*
 * Copyright (c) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.HibernatePersistenceManager;

import java.util.Collection;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class HibernateEntityManager implements EntityManager {
    public void setPersistenceManager( PersistenceManager persistenceManager ) {
        if ( persistenceManager instanceof HibernatePersistenceManager )
            _persistenceManager = (HibernatePersistenceManager)persistenceManager;
        else
            throw new IllegalArgumentException( "HibernateEntityManager can only be initialized with a HibernatePersistenceManager!");
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

    private HibernatePersistenceManager _persistenceManager;
}
