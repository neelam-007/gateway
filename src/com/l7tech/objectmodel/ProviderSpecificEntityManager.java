/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ProviderSpecificEntityManager extends HibernateEntityManager {
    public ProviderSpecificEntityManager() {
        super();
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public long getIdentityProviderOid() {
        return _identityProviderOid;
    }

    public String getAllQuery() {
        if ( _identityProviderOid == -1 )
            throw new IllegalStateException( "Can't call findAll() methods without first calling setIdentityProviderOid!" );
        else {
            StringBuffer query = new StringBuffer( super.getAllQuery() );
            query.append( " where provider = ");
            query.append( _identityProviderOid );
            return query.toString();
        }
    }

    public long _identityProviderOid = -1;
}
