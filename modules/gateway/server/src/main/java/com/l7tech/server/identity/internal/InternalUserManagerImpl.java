package com.l7tech.server.identity.internal;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.PersistentUserManagerImpl;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.ExceptionUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

/**
 * SSG-side implementation of the UserManager for the internal identity provider.
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003<br/>
 *
 * @version $Revision$
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class InternalUserManagerImpl
        extends PersistentUserManagerImpl<InternalUser, InternalGroup, InternalUserManager, InternalGroupManager>
        implements InternalUserManager 
{
    private final RoleManager roleManager;

    public InternalUserManagerImpl(final RoleManager roleManager,
                                   final ClientCertManager clientCertManager) {
        super( clientCertManager );
        this.roleManager = roleManager;
    }

    public void configure(InternalIdentityProvider provider) {
        this.setIdentityProvider( provider );
    }

    public String getTableName() {
        return "internal_user";
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public IdentityHeader userToHeader(InternalUser user) {
        InternalUser imp = cast(user);
        return new IdentityHeader(imp.getProviderId(), imp.getId(), EntityType.USER, imp.getLogin(), null);
    }

    public InternalUser reify(UserBean bean) {
        InternalUser iu = new InternalUser(bean.getLogin());
        iu.setDepartment(bean.getDepartment());
        iu.setEmail(bean.getEmail());
        iu.setFirstName(bean.getFirstName());
        iu.setLastName(bean.getLastName());
        iu.setName(bean.getName());
        iu.setOid(bean.getId() == null ? InternalUser.DEFAULT_OID : Long.valueOf(bean.getId()));
        iu.setHashedPassword(bean.getHashedPassword());
        iu.setSubjectDn(bean.getSubjectDn());
        return iu;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public InternalUser headerToUser(IdentityHeader header) {
        InternalUser iu = new InternalUser();
        iu.setProviderId(getProviderOid());
        iu.setOid(header.getOid());
        iu.setLogin(header.getName());
        return iu;
    }

    public Class<? extends User> getImpClass() {
        return InternalUser.class;
    }

    public Class<User> getInterfaceClass() {
        return User.class;
    }

    protected void preSave(InternalUser user) throws SaveException {
        // check to see if an existing user with same login exist
        InternalUser existingDude;
        try {
            existingDude = findByLogin(user.getLogin());
        } catch (FindException e) {
            existingDude = null;
        }
        if (existingDude != null) {
            throw new DuplicateObjectException("Cannot save this user. Existing user with login \'"
              + user.getLogin() + "\' present.");
        }
    }

    protected void checkUpdate(InternalUser originalUser,
                               InternalUser updatedUser) throws ObjectModelException {
        // checks whether the updatedUser changed his password
        String originalPasswd = originalUser.getHashedPassword();
        String newPasswd = updatedUser.getHashedPassword();

        // if password has changed, any cert should be revoked
        if (!originalPasswd.equals(newPasswd)) {
            logger.info("Revoking cert for updatedUser " + originalUser.getLogin() + " because he is changing his passwd.");
            // must revoke the cert

            revokeCert(originalUser);
        }
    }

    /**
     * Checks whether this user is the last user assigned to the Administrator {@link Role}.
     *
     * @throws DeleteException if the proposed deletion would remove the last user assigned to the "Administrator" role.
     */
    @Override
    protected void postDelete( final InternalUser user ) throws DeleteException {        
        try {
            roleManager.validateRoleAssignments();
        } catch ( UpdateException ue ) {
            throw new DeleteException( ExceptionUtils.getMessage(ue), ue );
        }
    }

    protected String getNameFieldname() {
        return "login";
    }

    public InternalUser cast(User user) {
        if (user instanceof UserBean) {
            return reify((UserBean) user);
        } else {
            return (InternalUser)user;
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());

/*
    @Override
    protected Map<String, Object> getUniqueAttributeMap(InternalUser entity) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("login", entity.getLogin());
        map.put("providerId", entity.getProviderId());
        return map;
    }
*/

    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

}
