package com.l7tech.server.identity.internal;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.PersistentUserManagerImpl;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.logon.LogonInfoManager;
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
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class InternalUserManagerImpl
        extends PersistentUserManagerImpl<InternalUser, InternalGroup, InternalUserManager, InternalGroupManager>
        implements InternalUserManager 
{
    private final RoleManager roleManager;
    private final PasswordHasher passwordHasher;
    private final InternalUserPasswordManager userPasswordManager;

    public InternalUserManagerImpl(final RoleManager roleManager,
                                   final ClientCertManager clientCertManager,
                                   final LogonInfoManager logonInfoManager,
                                   final PasswordHasher passwordHasher,
                                   final InternalUserPasswordManager userPasswordManager) {
        super( clientCertManager, logonInfoManager );
        this.roleManager = roleManager;
        this.passwordHasher = passwordHasher;
        this.userPasswordManager = userPasswordManager;
    }

    @Override
    public void configure(InternalIdentityProvider provider) {
        this.setIdentityProvider( provider );
    }

    @Override
    public String getTableName() {
        return "internal_user";
    }

    @Override
    protected IdentityHeader newHeader( final InternalUser entity ) {
        return new IdentityHeader(entity.getProviderId(), entity.getGoid(), EntityType.USER, entity.getLogin(), entity.getDescription(), entity.getName(), entity.getVersion(), entity.isEnabled());
    }

    @Override
    public InternalUser reify(UserBean bean) {
        InternalUser iu = new InternalUser(bean.getLogin());
        iu.setDepartment(bean.getDepartment());
        iu.setEmail(bean.getEmail());
        iu.setFirstName(bean.getFirstName());
        iu.setLastName(bean.getLastName());
        iu.setName(bean.getName());
        iu.setGoid(bean.getId() == null ? InternalUser.DEFAULT_GOID : Goid.parseGoid(bean.getId()));
        iu.setHashedPassword(bean.getHashedPassword());
        iu.setHttpDigest(bean.getHttpDigest());
        iu.setSubjectDn(bean.getSubjectDn());
        iu.setChangePassword(bean.isChangePassword());
        iu.setPasswordExpiry(bean.getPasswordExpiry());
        return iu;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public InternalUser headerToUser(IdentityHeader header) {
        InternalUser iu = new InternalUser();
        iu.setProviderId(getProviderGoid());
        iu.setGoid(header.getGoid());
        iu.setLogin(header.getName());
        return iu;
    }

    @Override
    public Class<? extends User> getImpClass() {
        return InternalUser.class;
    }

    @Override
    public Class<User> getInterfaceClass() {
        return User.class;
    }

    @Override
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

        final String newHash = user.getHashedPassword();
        if(newHash == null || newHash.trim().isEmpty()){
            throw new SaveException("Cannot save user without hashedPassword property.");
        }
        if(!passwordHasher.isVerifierRecognized(newHash)){
            throw new SaveException("Cannot save user as hashedPassword property contains an unrecognized hashing scheme.");
        }
    }

    @Override
    public InternalUserPasswordManager getUserPasswordManager() {
        return userPasswordManager;
    }

    @Override
    protected void checkUpdate(InternalUser originalUser,
                               InternalUser updatedUser) throws ObjectModelException {
        final String originalUserHash = originalUser.getHashedPassword();
        final boolean isUpgrade = originalUserHash == null || originalUserHash.trim().isEmpty();

        final String newHash = updatedUser.getHashedPassword();
        if(newHash == null || newHash.trim().isEmpty()){
            //before rejecting update, check if the user has yet to be updated.
            final String digest = originalUser.getHttpDigest();
            if(digest == null || !digest.equals(updatedUser.getHttpDigest())){
                throw new UpdateException("Cannot update user without hashedPassword property.");
            }
        } else if(!passwordHasher.isVerifierRecognized(newHash)){
            throw new UpdateException("Cannot update user as hashedPassword property contains an unrecognized hashing scheme.");
        }

        if(!isUpgrade){
            //only consider revoking the cert when the password was changed and not upgraded.

            // checks whether the updatedUser changed his password
            String originalPasswd = originalUser.getHashedPassword();
            String newPasswd = updatedUser.getHashedPassword();

            // if password has changed, any cert should be revoked
            if (!originalPasswd.equals(newPasswd)) {
                logger.info("Revoking cert for user " + originalUser.getLogin() + " as password was changed.");
                // must revoke the cert

                revokeCert(originalUser);
            }
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

    @Override
    protected void preDelete(InternalUser user) throws DeleteException {
        roleManager.deleteRoleAssignmentsForUser(user);
    }

    @Override
    protected void preUpdate(InternalUser user) throws UpdateException {
        userPasswordManager.manageHistory(user);
    }

    @Override
    protected void postUpdate(InternalUser user) throws UpdateException {
        roleManager.validateRoleAssignments();
    }

    @Override
    protected String getNameFieldname() {
        return "login";
    }

    @Override
    public InternalUser cast(User user) {
        if (user instanceof UserBean) {
            return reify((UserBean) user);
        } else {
            return (InternalUser)user;
        }
    }

    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

}
