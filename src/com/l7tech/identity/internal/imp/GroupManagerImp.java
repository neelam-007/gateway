/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal.imp;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.sql.SQLException;

/**
 * @author alex
 */
public class GroupManagerImp extends HibernateEntityManager implements GroupManager {
    public static final Class IMPCLASS = GroupImp.class;

    public GroupManagerImp( PersistenceContext context ) {
        super( context );
    }

    public Group findByPrimaryKey(long oid) throws FindException {
        try {
            return (Group)_manager.findByPrimaryKey( getContext(), IMPCLASS, oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public void delete(Group group) throws DeleteException {
        try {
            _manager.delete( getContext(), group );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(Group group) throws SaveException {
        try {
            return _manager.save( getContext(), group );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( Group group ) throws UpdateException {
        try {
            _manager.update( getContext(), group );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from group in class com.l7tech.identity.imp.GroupImp";
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
