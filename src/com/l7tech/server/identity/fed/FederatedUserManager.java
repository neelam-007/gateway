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
import com.l7tech.server.identity.PersistentUserManager;
import org.springframework.dao.DataAccessException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * The {@link com.l7tech.identity.UserManager} for {@link FederatedIdentityProvider}s.
 * 
 * @author alex
 * @version $Revision$
 */
public class FederatedUserManager extends PersistentUserManager {
    public FederatedUserManager(IdentityProvider identityProvider) {
        super(identityProvider);
    }

    /**
     * empty subclassing constructor (required for class proxying)
     */
    protected FederatedUserManager() {
    }

    public EntityHeader userToHeader( User user ) {
        FederatedUser imp = (FederatedUser)cast(user);
        return new EntityHeader(imp.getUniqueIdentifier(), EntityType.USER, user.getName(), null);
    }

    public User headerToUser(EntityHeader header) {
        FederatedUser fu = new FederatedUser();
        fu.setOid(header.getOid());
        fu.setName(header.getName());
        fu.setProviderId(getProviderOid());
        return fu;
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
            List results = getHibernateTemplate().find(FIND_BY_DN,
                                                    new Object[] { new Long(getProviderOid()), dn } );
            switch( results.size() ) {
                case 0:
                    return null;
                case 1:
                    return (FederatedUser)results.get(0);
                default:
                    throw new FindException("Found multiple users with same subject DN");
            }
        } catch ( DataAccessException e ) {
            throw new FindException("Couldn't find user", e);
        }
    }

    public FederatedUser findByEmail(String email) throws FindException {
        try {
            List results = getHibernateTemplate().find(FIND_BY_EMAIL, new Object[] { new Long(getProviderOid()), email } );
            switch( results.size() ) {
                case 0:
                    return null;
                case 1:
                    return (FederatedUser)results.get(0);
                default:
                    throw new FindException("Found multiple users with same email");
            }
        } catch (DataAccessException e) {
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
        findHeadersCriteria.add(Restrictions.eq("providerId", new Long(getProviderOid())));
    }

    private final String FIND_BY_ = "FROM " + getTableName() + " IN CLASS " + getImpClass() +
                                      " WHERE " + getTableName() + ".providerId = ? " +
                                      "AND " + getTableName();

    private final String FIND_BY_DN = FIND_BY_ + ".subjectDn = ?";

    private final String FIND_BY_EMAIL = FIND_BY_ + ".email = ?";

}
