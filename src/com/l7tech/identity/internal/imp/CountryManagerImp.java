/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.Country;
import com.l7tech.identity.internal.CountryManager;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author alex
 */
public class CountryManagerImp extends HibernateEntityManager implements CountryManager {
    public static final Class IMPCLASS = CountryImp.class;

    public CountryManagerImp( PersistenceContext context ) {
        super( context );
    }

    public Country findByPrimaryKey(long oid) throws FindException {
        return (Country)_manager.findByPrimaryKey( _context, IMPCLASS, oid );
    }

    public void delete(Country country) throws DeleteException {
        _manager.delete( _context, country );
    }

    public long save(Country country) throws SaveException {
        return _manager.save( _context, country );
    }

    public void update( Country country ) throws UpdateException {
        _manager.update( _context, country );
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from country in class com.l7tech.identity.imp.CountryImp";
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
