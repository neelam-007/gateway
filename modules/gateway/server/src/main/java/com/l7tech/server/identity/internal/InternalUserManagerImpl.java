package com.l7tech.server.identity.internal;

import com.l7tech.gateway.common.security.password.IncorrectPasswordException;
import com.l7tech.gateway.common.security.password.PasswordHasher;
import com.l7tech.gateway.common.security.password.PasswordHashingException;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.identity.PersistentUserManagerImpl;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.logon.LogonInfoManager;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Level;
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
    private final PasswordHasher passwordHasher;

    public InternalUserManagerImpl(final RoleManager roleManager,
                                   final ClientCertManager clientCertManager,
                                   final LogonInfoManager logonInfoManager,
                                   final PasswordHasher passwordHasher) {
        super( clientCertManager, logonInfoManager );
        this.roleManager = roleManager;
        this.passwordHasher = passwordHasher;
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
    @Transactional(propagation=Propagation.SUPPORTS)
    public IdentityHeader userToHeader(InternalUser user) {
        InternalUser imp = cast(user);
        return new IdentityHeader(imp.getProviderId(), imp.getId(), EntityType.USER, imp.getLogin(), imp.getDescription(), imp.getName(), imp.getVersion());
    }

    @Override
    public InternalUser reify(UserBean bean) {
        InternalUser iu = new InternalUser(bean.getLogin());
        iu.setDepartment(bean.getDepartment());
        iu.setEmail(bean.getEmail());
        iu.setFirstName(bean.getFirstName());
        iu.setLastName(bean.getLastName());
        iu.setName(bean.getName());
        iu.setOid(bean.getId() == null ? InternalUser.DEFAULT_OID : Long.valueOf(bean.getId()));
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
        iu.setProviderId(getProviderOid());
        iu.setOid(header.getOid());
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

    }

    @Override
    public boolean configureUserPasswordHashes(final InternalUser dbUser, final String clearTextPassword){
        final String userHashedPassword = dbUser.getHashedPassword();
        final boolean userHasHashedPassword = userHashedPassword != null && !userHashedPassword.trim().isEmpty();

        boolean userWasUpdated = false;
        final StringBuilder fineLogMsg = new StringBuilder("Updating password storage for login '" + dbUser.getLogin() + "'. ");

        //first check to see if this admin user is new or being upgraded, in which case the user needs a new hash
        if(!userHasHashedPassword){
            dbUser.setHashedPassword(passwordHasher.hashPassword(clearTextPassword.getBytes(Charsets.UTF8)));
            userWasUpdated = true;
            fineLogMsg.append("Set password hash. ");
        } else {
            //does the existing hash verify for the incoming password?
            boolean isNewPassword = true;
            try {
                if(passwordHasher.isVerifierRecognized(userHashedPassword)){
                    passwordHasher.verifyPassword(clearTextPassword.getBytes(Charsets.UTF8), userHashedPassword);
                    isNewPassword = false;
                }
            } catch (IncorrectPasswordException e) {
                //fall through ok
            } catch (PasswordHashingException e) {
                //fall through ok
            }

            if(isNewPassword){
                dbUser.setPasswordChanges(passwordHasher.hashPassword(clearTextPassword.getBytes(Charsets.UTF8)));
                userWasUpdated = true;
            }
        }

        //update digest if state has changed.
        final String httpDigestEnable = ServerConfig.getInstance().getPropertyCached("httpDigest.enable");
        boolean isHttpDigestEnabled = httpDigestEnable != null && Boolean.valueOf(httpDigestEnable);

        final String userDigest = dbUser.getHttpDigest();
        final boolean userHasDigest = userDigest != null && !userDigest.trim().isEmpty();
        final String calculatedDigest = HexUtils.encodePasswd(dbUser.getLogin(), clearTextPassword, HexUtils.REALM);

        if (isHttpDigestEnabled) {
            if (!userHasDigest || userWasUpdated) {//if hashed password changed then update the digest if enabled
                dbUser.setHttpDigest(calculatedDigest);
                userWasUpdated = true;
                fineLogMsg.append("Set digest property. ");
            }
        } else if (userHasDigest) {
            dbUser.setHttpDigest(null);
            userWasUpdated = true;
            fineLogMsg.append("Cleared digest property. ");
        }

        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, fineLogMsg.toString());
        }
        
        return userWasUpdated;
    }

    @Override
    protected void checkUpdate(InternalUser originalUser,
                               InternalUser updatedUser) throws ObjectModelException {
        final String originalUserHash = originalUser.getHashedPassword();
        final boolean isUpgrade = originalUserHash == null || originalUserHash.trim().isEmpty();
        //Note: if the original has has changed, then so has the digest, if this property is configured to be persisted

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

    private final Logger logger = Logger.getLogger(getClass().getName());

/*
    @Override
    protected Map<String, Object> getUniqueConstraints(InternalUser entity) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("login", entity.getLogin());
        map.put("providerId", entity.getProviderId());
        return map;
    }
*/

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

}
