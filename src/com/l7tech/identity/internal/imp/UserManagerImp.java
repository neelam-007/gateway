/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal.imp;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author alex
 */
public class UserManagerImp extends HibernateEntityManager implements UserManager {
    public static final Class IMPCLASS = UserImp.class;

    public UserManagerImp( PersistenceContext context ) {
        super( context );
    }

    public User findByPrimaryKey(long oid) throws FindException {
        return (User)_manager.findByPrimaryKey( _context, IMPCLASS, oid );
    }

    public void delete(User user) throws DeleteException {
        _manager.delete( _context, user );
    }

    public long save(User user) throws SaveException {
        return _manager.save( _context, user );
    }

    public void update( User user ) throws UpdateException {
        _manager.update( _context, user );
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from user in class com.l7tech.identity.imp.UserImp";
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
