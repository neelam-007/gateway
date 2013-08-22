package com.l7tech.server.identity.fed;

import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.PersistentUserManagerImpl;
import com.l7tech.server.logon.LogonInfoManager;
import com.l7tech.util.CollectionUtils;
import static org.apache.commons.lang.StringUtils.*;

import com.l7tech.util.GoidUpgradeMapper;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * The {@link com.l7tech.identity.UserManager} for {@link FederatedIdentityProvider}s.
 *
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class FederatedUserManagerImpl
        extends PersistentUserManagerImpl<FederatedUser, FederatedGroup, FederatedUserManager, FederatedGroupManager>
        implements FederatedUserManager
{
    public FederatedUserManagerImpl( final ClientCertManager clientCertManager, LogonInfoManager logonInfoManager ) {
        super( clientCertManager, logonInfoManager );
    }

    @Override
    public void configure(FederatedIdentityProvider provider) {
        this.setIdentityProvider( provider );
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public FederatedUser headerToUser(IdentityHeader header) {
        FederatedUser fu = new FederatedUser(getProviderGoid(), header.getName());
        fu.setGoid( header.getGoid() );
        return fu;
    }

    @Override
    public FederatedUser reify(UserBean bean) {
        FederatedUser fu = new FederatedUser(bean.getProviderId(), bean.getLogin());
        fu.setGoid(bean.getId() == null ? FederatedUser.DEFAULT_GOID : Goid.parseGoid(bean.getId()));
        fu.setName(bean.getName());
        fu.setDepartment(bean.getDepartment());
        fu.setEmail(bean.getEmail());
        fu.setFirstName(bean.getFirstName());
        fu.setLastName(bean.getLastName());
        fu.setSubjectDn( bean.getSubjectDn() );
        return fu;
    }

    @Override
    public Class<FederatedUser> getImpClass() {
        return FederatedUser.class;
    }

    @Override
    public Class<User> getInterfaceClass() {
        return User.class;
    }

    @Override
    @Transactional(readOnly=true)
    public FederatedUser findBySubjectDN(String dn) throws FindException {
        try {
            List results = getHibernateTemplate().find(FIND_BY_DN, getProviderGoid(), dn );
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

    @Override
    @Transactional(readOnly=true)
    public FederatedUser findByEmail(String email) throws FindException {
        try {
            List results = getHibernateTemplate().find(FIND_BY_EMAIL, getProviderGoid(), email );
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

    @Override
    public FederatedUser cast( User user ) {
        if ( user instanceof UserBean ) {
            return reify((UserBean) user);
        } else {
            return (FederatedUser)user;
        }
    }

    @Override
    protected void preUpdate( final FederatedUser user ) throws UpdateException {
        // check to see if an existing user matches
        if ( !isUnique( user ) ) {
            // don't include "name" in the error message since this is typically not updated
            throw new RuleViolationUpdateException("The users login, email, or X.509 Subject DN conflict with an existing user.");
        }
    }

    @Override
    protected void preSave( final FederatedUser user ) throws SaveException {
        // check to see if an existing user matches
        if ( !isUnique( user ) ) {
            throw new DuplicateObjectException("The users name, login, email, or X.509 Subject DN conflict with an existing user.");
        }
    }

    @Override
    protected void addFindByNameCriteria(Criteria crit) {
        crit.add(Restrictions.eq("providerId", getProviderGoid()));
    }

    @Override
    protected String getNameFieldname() {
        return "name";
    }

    @Override
    protected IdentityHeader newHeader( final FederatedUser user ) {
        return new IdentityHeader(user.getProviderId(), user.getGoid(), EntityType.USER, user.getName(), null, user.getName(), user.getVersion());
    }

    @Override
    protected void addFindAllCriteria( Criteria findHeadersCriteria ) {
        findHeadersCriteria.add(Restrictions.eq("providerId", getProviderGoid()));
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String,Object>> getUniqueConstraints( final FederatedUser user ) {
        final Collection<Map<String,Object>> constraints = new ArrayList<Map<String,Object>>();
        addConstraint( constraints, user.getProviderId(), "name", user.getName() );
        addConstraint( constraints, user.getProviderId(), "email", user.getEmail() );
        addConstraint( constraints, user.getProviderId(), "login", user.getLogin() );
        addConstraint( constraints, user.getProviderId(), "subjectDn", user.getSubjectDn() );
        return constraints;
    }

    private void addConstraint( final Collection<Map<String,Object>> constraints,
                                final Goid providerId,
                                final String propertyName,
                                final String propertyValue ) {
        if ( !isEmpty( trim( propertyValue ) ) ) {
            constraints.add( CollectionUtils.<String, Object>mapBuilder()
                            .put( "providerId", providerId )
                            .put( propertyName, propertyValue )
                            .map() );
        }
    }

    private boolean isUnique( final FederatedUser user ) {
        // check to see if an existing user matches
        List<FederatedUser> existing;
        try {
            final Conjunction conjunction = Restrictions.conjunction();
            conjunction.add( asCriterion( getUniqueConstraints( user ) ) );
            if (  !user.isUnsaved() ) {
                conjunction.add( Restrictions.ne( "goid", user.getGoid() ) );
            }
            existing = findMatching( conjunction );
        } catch (FindException e) {
            existing = null;
        }
        return existing == null || existing.isEmpty();
    }

    private final String FIND_BY_ =
            "FROM " + getTableName() +
                " IN CLASS " + getImpClass().getName() +
                " WHERE " + getTableName() + ".providerId = ? " +
                "AND " + getTableName();

    private final String FIND_BY_DN = FIND_BY_ + ".subjectDn = ?";

    private final String FIND_BY_EMAIL = FIND_BY_ + ".email = ?";

}
