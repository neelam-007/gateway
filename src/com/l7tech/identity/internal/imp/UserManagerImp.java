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
public class UserManagerImp extends ProviderSpecificEntityManager implements UserManager {
    public UserManagerImp( PersistenceContext context ) {
        super( context );
    }

    public UserManagerImp() {
        super();
    }

    public User findByPrimaryKey(long oid) throws FindException {
        try {
            return (User)_manager.findByPrimaryKey( getContext(), getImpClass(), oid );
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

    public String getTableName() {
        return "user";
    }

    public Class getImpClass() {
        return UserImp.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }
}
