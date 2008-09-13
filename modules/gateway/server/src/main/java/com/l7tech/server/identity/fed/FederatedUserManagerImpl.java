/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.PersistentUserManagerImpl;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * The {@link com.l7tech.identity.UserManager} for {@link FederatedIdentityProvider}s.
 * 
 * @author alex
 * @version $Revision$
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class FederatedUserManagerImpl
        extends PersistentUserManagerImpl<FederatedUser, FederatedGroup, FederatedUserManager, FederatedGroupManager>
        implements FederatedUserManager
{
    public FederatedUserManagerImpl( final ClientCertManager clientCertManager ) {
        super(clientCertManager);
    }

    public void configure(FederatedIdentityProvider provider) {
        this.setIdentityProvider( provider );
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public IdentityHeader userToHeader(FederatedUser user ) {
        return new IdentityHeader(user.getProviderId(), user.getId(), EntityType.USER, user.getName(), null);
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public FederatedUser headerToUser(IdentityHeader header) {
        FederatedUser fu = new FederatedUser(getProviderOid(), header.getName());
        fu.setOid(header.getOid());
        return fu;
    }

    public FederatedUser reify(UserBean bean) {
        FederatedUser fu = new FederatedUser(bean.getProviderId(), bean.getLogin());
        fu.setOid(bean.getId() == null ? FederatedUser.DEFAULT_OID : Long.valueOf(bean.getId()));
        fu.setName(bean.getName());
        fu.setDepartment(bean.getDepartment());
        fu.setEmail(bean.getEmail());
        fu.setFirstName(bean.getFirstName());
        fu.setLastName(bean.getLastName());
        fu.setSubjectDn(bean.getSubjectDn());
        return fu;
    }

    public Class<FederatedUser> getImpClass() {
        return FederatedUser.class;
    }

    public Class<User> getInterfaceClass() {
        return User.class;
    }

    public String getTableName() {
        return "fed_user";
    }

    @Transactional(readOnly=true)
    public FederatedUser findBySubjectDN(String dn) throws FindException {
        try {
            List results = getHibernateTemplate().find(FIND_BY_DN,
                                                    new Object[] { getProviderOid(), dn } );
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

    @Transactional(readOnly=true)
    public FederatedUser findByEmail(String email) throws FindException {
        try {
            List results = getHibernateTemplate().find(FIND_BY_EMAIL, new Object[] { getProviderOid(), email } );
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

    public FederatedUser cast( User user ) {
        if ( user instanceof UserBean ) {
            return reify((UserBean) user);
        } else {
            return (FederatedUser)user;
        }
    }

    protected void preSave(FederatedUser user) throws SaveException {
        // check to see if an existing user with same name exists
        if (user != null && user.getName() != null && user.getName().length() > 0) {
            FederatedUser existingDude;
            try {
                existingDude = this.findByUniqueName(user.getName());
            } catch (FindException e) {
                existingDude = null;
            }
            if (existingDude != null) {
                throw new DuplicateObjectException("Cannot save this user. Existing user with name \'"
                  + user.getName() + "\' present.");
            }
        }
    }

    @Override
    protected void addFindByNameCriteria(Criteria crit) {
        crit.add(Restrictions.eq("providerId", getProviderOid()));
    }

    protected String getNameFieldname() {
        return "name";
    }

    protected void addFindAllCriteria( Criteria findHeadersCriteria ) {
        findHeadersCriteria.add(Restrictions.eq("providerId", getProviderOid()));
    }

    @Override
    protected Map<String, Object> getUniqueAttributeMap(FederatedUser entity) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("providerId", entity.getProviderId());
        map.put("name", entity.getName());
        return map;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    private final String FIND_BY_ =
            "FROM " + getTableName() +
                " IN CLASS " + getImpClass().getName() +
                " WHERE " + getTableName() + ".providerId = ? " +
                "AND " + getTableName();

    private final String FIND_BY_DN = FIND_BY_ + ".subjectDn = ?";

    private final String FIND_BY_EMAIL = FIND_BY_ + ".email = ?";

}
