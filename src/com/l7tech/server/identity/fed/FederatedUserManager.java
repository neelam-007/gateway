/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceManager;
import com.l7tech.server.identity.PersistentUserManager;
import net.sf.hibernate.Criteria;
import net.sf.hibernate.expression.Expression;

import java.sql.SQLException;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedUserManager extends PersistentUserManager {
    public FederatedUserManager( IdentityProvider provider ) {
        super();
        this.provider = provider;
    }

    public EntityHeader userToHeader( User user ) {
        FederatedUser imp = (FederatedUser)cast(user);
        return new EntityHeader(imp.getUniqueIdentifier(), EntityType.USER, user.getName(), null);
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

    protected PersistentUser cast( User user ) {
        FederatedUser imp;
        if ( user instanceof UserBean ) {
            imp = new FederatedUser( (UserBean)user );
        } else {
            imp = (FederatedUser)user;
        }
        return imp;
    }

    protected String getNameFieldname() {
        return "name";
    }

    protected void addFindAllCriteria( Criteria findHeadersCriteria ) {
        findHeadersCriteria.add(Expression.eq("providerId", new Long(getProviderOid())));
    }

    private final String FIND_BY_DN = "FROM " + getTableName() + " IN CLASS " + getImpClass() +
                                      " WHERE " + getTableName() + ".providerId = ? " +
                                      "AND " + getTableName() + ".subjectDn = ?";
}
