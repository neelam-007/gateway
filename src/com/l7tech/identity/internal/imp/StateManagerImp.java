/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.*;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author alex
 */
public class StateManagerImp extends HibernateEntityManager implements StateManager {
    public static final Class IMPCLASS = StateImp.class;

    public StateManagerImp( PersistenceContext context ) {
        super( context );
    }

    public State findByPrimaryKey(long oid) throws FindException {
        return (State)_manager.findByPrimaryKey( _context, IMPCLASS, oid );
    }

    public void delete(State state) throws DeleteException {
        _manager.delete( _context, state );
    }

    public long save(State state) throws SaveException {
        return _manager.save( _context, state );
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from State in class com.l7tech.identity.imp.StateImp";
        if ( _identityProviderOid == -1 )
            throw new FindException( "Can't call findAll() without first calling setIdentityProviderOid!" );
        else
            return _manager.find( _context, query + " where provider = ?", new Long( _identityProviderOid ), Long.TYPE );
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }

    public long _identityProviderOid = -1;
}
