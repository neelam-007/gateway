/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal.imp;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author alex
 */
public class GroupManagerImp extends HibernateEntityManager implements GroupManager {
    public static final Class IMPCLASS = GroupImp.class;

    public GroupManagerImp( PersistenceContext context ) {
        super( context );
    }

    public Group findByPrimaryKey(long oid) throws FindException {
        return (Group)_manager.findByPrimaryKey( _context, IMPCLASS, oid );
    }

    public void delete(Group group) throws DeleteException {
        _manager.delete( _context, group );
    }

    public long save(Group group) throws SaveException {
        return _manager.save( _context, group );
    }

    public void update( Group group ) throws UpdateException {
        _manager.update( _context, group );
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from group in class com.l7tech.identity.imp.GroupImp";
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
