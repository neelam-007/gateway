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

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        return _manager.find( _context, "from user in class com.l7tech.identity.imp.UserImp where provider = ?", new Long( _identityProviderOid ), Long.TYPE );
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }

    public long _identityProviderOid;
}
