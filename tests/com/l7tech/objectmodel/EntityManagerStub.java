/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.objectmodel;

import java.util.Collection;
import java.util.Map;

/**
 * Stub Entity Manager
 * @author emil
 * @version Feb 17, 2005
 */
public class EntityManagerStub implements EntityManager {
    /**
         * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
         *
         * @return A <code>Collection</code> of EntityHeader objects.
         */
    public Collection findAllHeaders() throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
         * Returns an unmodifiable collection of <code>EntityHeader</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
         *
         * @return A <code>Collection</code> of EntityHeader objects.
         */
    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
         * Returns an unmodifiable collection of <code>Entity</code> objects for all instances of the entity class corresponding to this Manager.
         *
         * @return A <code>Collection</code> of Entity objects.
         */
    public Collection findAll() throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
         * Returns an unmodifiable collection of <code>Entity</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
         *
         * @return A <code>Collection</code> of EntityHeader objects.
         */
    public Collection findAll(int offset, int windowSize) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer getVersion(long oid) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map findVersionMap() throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
         * Returns the {@link Entity} with the specified OID. If the entity's version was last checked more than
         * <code>maxAge</code> milliseconds ago, check for an updated version in the database.  If the entity has been
         * updated, refresh it in the cache if the implementation doesn't complain.
         *
         * @param o      the OID of the Entity to return.
         * @param maxAge the maximum age of a cached Entity to return, in milliseconds.
         * @return the Entity with the specified OID, or <code>null</code> if it does not exist.
         * @throws FindException
         *          in the event of a database problem
         * @throws com.l7tech.objectmodel.EntityManager.CacheVeto
         *          thrown by an implementor
         */
    public Entity getCachedEntity(long o, int maxAge) throws FindException, CacheVeto {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}