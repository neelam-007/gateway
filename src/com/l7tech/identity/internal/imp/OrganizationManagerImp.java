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
public class OrganizationManagerImp extends HibernateEntityManager implements OrganizationManager {
    public static final Class IMPCLASS = OrganizationImp.class;

    public OrganizationManagerImp( PersistenceContext context ) {
        super( context );
    }

    public Organization findByPrimaryKey(long oid) throws FindException {
        return (Organization)_manager.findByPrimaryKey( _context, IMPCLASS, oid );
    }

    public void delete(Organization organization) throws DeleteException {
        _manager.delete( _context, organization );
    }

    public long save(Organization organization) throws SaveException {
        return _manager.save( _context, organization );
    }

    public void update( Organization organization ) throws UpdateException {
        _manager.update( _context, organization );
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from organization in class com.l7tech.identity.imp.OrganizationImp";
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
