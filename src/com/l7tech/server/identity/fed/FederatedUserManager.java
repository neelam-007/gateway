/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceManager;
import com.l7tech.server.identity.internal.InternalUserManagerServer;

import java.sql.SQLException;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedUserManager extends InternalUserManagerServer {
    public FederatedUserManager( IdentityProvider provider ) {
        super(provider);

    }

    protected void preDelete( InternalUser user ) throws FindException, DeleteException {
        // No pre-processing required - don't call super
    }

    public Class getImpClass() {
        return FederatedUser.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }

    public String getTableName() {
        return "fed_user";
    }

    private long getProviderOid() {
        return provider.getConfig().getOid();
    }

    public FederatedUser findBySubjectDN(String dn) throws FindException {
        try {
            List results = PersistenceManager.find( getContext(), FIND_BY_DN,
                                                    new Object[] { new Long(getProviderOid()), dn },
                                                    new Class[] { Long.class, String.class } );
            switch( results.size() ) {
                case 0:
                    return null;
                case 1:
                    return (FederatedUser)results.get(0);
                default:
                    throw new FindException("Found multiple users with same subject DN");
            }
        } catch ( SQLException e ) {
            throw new FindException("Couldn't find user", e);
        }
    }

    private final String FIND_BY_DN = "FROM " + getTableName() + " IN CLASS " + getImpClass() +
                                      " WHERE providerOid = ? AND subjectDn = ?";
}
