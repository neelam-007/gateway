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
import java.sql.SQLException;

/**
 * @author alex
 */
public class UserManagerImp extends HibernateEntityManager implements UserManager {
    public static final Class IMPCLASS = UserImp.class;

    public UserManagerImp( PersistenceContext context ) {
        super( context );
    }

    public User findByPrimaryKey(long oid) throws FindException {
        try {
            return (User)_manager.findByPrimaryKey( getContext(), IMPCLASS, oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public void delete(User user) throws DeleteException {
        try {
            _manager.delete( getContext(), user );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(User user) throws SaveException {
        try {
            return _manager.save( getContext(), user );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( User user ) throws UpdateException {
        try {
            _manager.update( getContext(), user );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from user in class com.l7tech.identity.imp.UserImp";
        if ( _identityProviderOid == -1 )
            throw new FindException( "Can't call findAll() without first calling setIdentityProviderOid!" );
        else {
            try {
                return _manager.find( getContext(), query + " where provider = ?", new Long( _identityProviderOid ), Long.TYPE );
            } catch ( SQLException se ) {
                throw new FindException( se.toString(), se );
            }
        }
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }

    public long _identityProviderOid = -1;
}
