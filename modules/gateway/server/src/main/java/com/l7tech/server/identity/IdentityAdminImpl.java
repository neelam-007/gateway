package com.l7tech.server.identity;

import com.l7tech.common.password.PasswordHashingException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.password.IncorrectPasswordException;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.identity.LogonInfo;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.TrustedEsmUserManager;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.AuditRevokeAllUserCertificates;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.internal.InternalUserPasswordManager;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.logon.LogonInfoManager;
import com.l7tech.server.logon.LogonService;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.EntityType.ID_PROVIDER_CONFIG;

/**
 * Server side implementation of the IdentityAdmin interface.
 * This was originally used with the Axis layer
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: May 26, 2003
 */
public class IdentityAdminImpl implements ApplicationEventPublisherAware, IdentityAdmin {
    private ClientCertManager clientCertManager;

    private final RoleManager roleManager;
    private final DefaultKey defaultKey;
    private ApplicationEventPublisher applicationEventPublisher;
    private final PasswordEnforcerManager passwordEnforcerManager;
    private final IdentityProviderPasswordPolicyManager passwordPolicyManger;
    private final LogonService logonServ;
    private LogonInfoManager logonManager;
    private final PasswordHasher passwordHasher;

    @Inject
    private TrustedEsmUserManager trustedEsmUserManager;

    private static final String DEFAULT_ID = Long.toString(PersistentEntity.DEFAULT_OID);

    public IdentityAdminImpl(final RoleManager roleManager,
                             final IdentityProviderPasswordPolicyManager passwordPolicyManger,
                             final PasswordEnforcerManager passwordEnforcerManager,
                             final DefaultKey defaultKey,
                             final LogonService logonServ,
                             final PasswordHasher passwordHasher) {
        if (roleManager == null) throw new IllegalArgumentException("roleManager is required");

        this.roleManager = roleManager;
        this.defaultKey = defaultKey;
        this.passwordPolicyManger = passwordPolicyManger;
        this.passwordEnforcerManager = passwordEnforcerManager;
        this.logonServ = logonServ;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        if (this.applicationEventPublisher != null)
            throw new IllegalStateException("applicationEventPublisher is already set");

        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Returns a version string. This can be compared to version on client-side.
     *
     * @return value to be compared with the client side value of Service.VERSION;
     */
    @Override
    public String echoVersion() {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }

    /**
     * @return Array of entity headers for all existing id provider config
     */
    @Override
    public EntityHeader[] findAllIdentityProviderConfig() throws FindException {
            Collection<EntityHeader> res = getIdProvCfgMan().findAllHeaders();
        return res.toArray(new EntityHeader[0]);
    }

    /**
     * @return An identity provider config object
     */
    @Override
    public IdentityProviderConfig findIdentityProviderConfigByID(long oid)
      throws FindException {
            return getIdProvCfgMan().findByPrimaryKey(oid);
    }

    @Override
    public long saveIdentityProviderConfig(IdentityProviderConfig identityProviderConfig)
      throws SaveException, UpdateException {
        try {
            long oid;
            if (identityProviderConfig.getOid() != IdentityProviderConfig.DEFAULT_OID) {
                IdentityProviderConfigManager manager = getIdProvCfgMan();
                IdentityProviderConfig originalConfig = manager.findByPrimaryKey(identityProviderConfig.getOid());
                originalConfig.copyFrom(identityProviderConfig);
                manager.update(originalConfig);
                logger.info("Updated IDProviderConfig: " + identityProviderConfig.getOid());
                oid = identityProviderConfig.getOid();
            } else {
                logger.info("Saving IDProviderConfig: " + identityProviderConfig.getOid());
                oid = getIdProvCfgMan().save(identityProviderConfig);
                getIdProvCfgMan().addManageProviderRole(identityProviderConfig);
            }

            return oid;
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new UpdateException("This object cannot be found (it no longer exist?).", e);
        }
    }

    /**
     * Delete all users of the identity provider given the identity provider Id
     * <p/>
     * Must be called in a transaction!
     *
     * @param ipoid The identity provider id
     * @throws DeleteException
     */
    private void deleteAllUsers(long ipoid) throws DeleteException {
        try {
            UserManager userManager = retrieveUserManager(ipoid);
            if (userManager == null) throw new DeleteException("Cannot retrieve the UserManager");
            userManager.deleteAll(ipoid);
            trustedEsmUserManager.deleteMappingsForIdentityProvider(ipoid);
        } catch (FindException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    /**
     * Delete all groups of the identity provider given the identity provider Id
     * <p/>
     * Must be called in a transaction!
     *
     * @param ipoid The identity provider id
     * @throws DeleteException
     */
    private void deleteAllGroups(long ipoid) throws DeleteException {
        try {
            GroupManager groupManager = retrieveGroupManager(ipoid);
            if (groupManager == null) throw new DeleteException("Cannot retrieve the GroupManager");
            groupManager.deleteAll(ipoid);
        } catch (ObjectNotFoundException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        } catch (FindException e) {
            throw new DeleteException(e.getMessage(), e);
        }
    }

    /**
     * Delete all virtual groups of the identity provider given the identity provider Id
     * <p/>
     * Must be called in a transaction!
     *
     * @param ipoid The identity provider id
     * @throws DeleteException
     */
    private void deleteAllVirtualGroups(long ipoid) throws DeleteException {
        try {
            GroupManager groupManager = retrieveGroupManager(ipoid);
            if (groupManager == null) throw new DeleteException("Cannot retrieve the GroupManager");
            groupManager.deleteAllVirtual(ipoid);
        } catch (ObjectNotFoundException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        } catch (FindException e) {
            throw new DeleteException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteIdentityProviderConfig(long oid) throws DeleteException {
        try {
            IdentityProviderConfigManager manager = getIdProvCfgMan();

            final IdentityProviderConfig ipc = manager.findByPrimaryKey(oid);

            if (ipc.type() == IdentityProviderType.FEDERATED) {
                deleteAllUsers(oid);
                deleteAllGroups(oid);
                deleteAllVirtualGroups(oid);
            }

            manager.delete(ipc);

            roleManager.deleteEntitySpecificRoles(ID_PROVIDER_CONFIG, ipc.getOid());
            trustedEsmUserManager.deleteMappingsForIdentityProvider(ipc.getOid());
            logger.info("Deleted IDProviderConfig: " + ipc);
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllUsers(long identityProviderConfigId) throws FindException {
            UserManager userManager = retrieveUserManager(identityProviderConfigId);
            return userManager.findAllHeaders();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> searchIdentities(long identityProviderConfigId, EntityType[] types, String pattern)
      throws FindException {
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            return provider.search(types, pattern);
    }

    @Override
    public User findUserByID(long identityProviderConfigId, String userId)
                           throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
        if (provider == null) {
            logger.warning("Identity Provider #" + identityProviderConfigId + " does not exist");
            return null;
        }
        UserManager userManager = provider.getUserManager();

        return userManager.findByPrimaryKey(userId);
    }

    @Override
    public User findUserByLogin(long idProvCfgId, String login) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(idProvCfgId);
        if (provider == null) {
            logger.warning("Identity Provider #" + idProvCfgId + " does not exist");
            return null;
        }
        UserManager userManager = provider.getUserManager();

        return userManager.findByLogin(login);
    }

    @Override
    public void deleteUser(long cfgid, String userId)
      throws DeleteException, ObjectNotFoundException {
        try {
            UserManager userManager = retrieveUserManager(cfgid);
            if (userManager == null) throw new DeleteException("Cannot retrieve the UserManager");
            User user = userManager.findByPrimaryKey(userId);
            if (user == null) {
                throw new ObjectNotFoundException(" User " + userId);
            }
            if (user.equals(JaasUtils.getCurrentUser()))
                throw new DeleteException("The currently logged-in user cannot be deleted");
            userManager.delete(user);
            trustedEsmUserManager.deleteMappingsForUser(user);
            logger.info("Deleted User: " + user.getLogin());
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    @Override
    public String saveUser(long idProvCfgId, User user, Set<IdentityHeader> groupHeaders)
            throws SaveException, UpdateException, ObjectNotFoundException {
        try {
            return saveUser(idProvCfgId, user, groupHeaders, null);
        } catch (InvalidPasswordException e) {
            //cannot happen
            throw new IllegalStateException("InvalidPasswordException caught when updating an existing user.");
        }
    }

    @Override
    public String saveUser(long identityProviderConfigId, User user, Set groupHeaders, String clearTextPassword)
            throws SaveException, UpdateException, ObjectNotFoundException, InvalidPasswordException {
        boolean isSave = true;
        try {
            String id = user.getId();
            isSave = id == null || DEFAULT_ID.equals(id);
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();
            if (user instanceof UserBean) user = userManager.reify((UserBean) user);

            if(user instanceof InternalUser) {
                InternalUser internalUser = (InternalUser)user;
                if(!isSave){
                    //ensure password has not changed
                    final InternalUser originalUser = (InternalUser) userManager.findByPrimaryKey(internalUser.getId());
                    if(!originalUser.getHashedPassword().equals(internalUser.getHashedPassword())){
                        throw new UpdateException("Cannot modify existing users password using this api.");
                    }
                    final String origDigest = originalUser.getHttpDigest();
                    if(origDigest == null && internalUser.getHttpDigest() != null){
                        throw new UpdateException("Cannot modify existing users digest using this api.");
                    }
                    if(origDigest != null && !origDigest.equals(internalUser.getHttpDigest())){
                        throw new UpdateException("Cannot modify existing users digest using this api.");    
                    }
                } else {
                    //validate password
                    passwordEnforcerManager.isPasswordPolicyCompliant(clearTextPassword);
                    // Reset password expiration and force password change
                    passwordEnforcerManager.setUserPasswordPolicyAttributes(internalUser, true);
                    InternalUserManager internalManager = (InternalUserManager) userManager;
                    final InternalUserPasswordManager passwordManager = internalManager.getUserPasswordManager();
                    final boolean updateRequired = passwordManager.configureUserPasswordHashes(internalUser, clearTextPassword);
                    assert(updateRequired);
                }
            }

            if (isSave) {
                // create logon info
                LogonInfo info = new LogonInfo(identityProviderConfigId, user.getLogin());
                logonManager.save(info);
                logger.info("Logon info created for User: " + user.getLogin() + " [" + id + "]");
                id = userManager.save(user, groupHeaders);
                logger.info("Saved User: " + user.getLogin() + " [" + id + "]");
            } else {
                userManager.update(user, groupHeaders);
                logger.info("Updated User: " + user.getLogin() + " [" + id + "]");
            }

            return id;
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            if (isSave)
                throw new SaveException("Exception in saveUser", e);
            else
                throw new UpdateException("Exception in saveUser", e);
        }
    }

    @Override
    public void changeUsersPassword(User user, String newClearTextPassword)
            throws FindException, UpdateException, InvalidPasswordException {

        IdentityProvider provider = identityProviderFactory.getProvider(user.getProviderId());
        if (provider == null) throw new FindException("IdentityProvider could not be found");
        if(!(provider instanceof InternalIdentityProvider)){
            throw new UpdateException("Cannot change non internal users password.");
        }

        if(newClearTextPassword == null || newClearTextPassword.trim().isEmpty()){
            //this is a client error
            throw new InvalidPasswordException("newClearTextPassword must be supplied.");
        }

        InternalUserManager userManager = (InternalUserManager) provider.getUserManager();
        final InternalUser disconnectedUser = new InternalUser();
        {//limit session connected internal user scope
            //were ignoring the incoming entity, were just using it for rbac and a container for it's id and provider id
            final InternalUser internalUser = userManager.findByPrimaryKey(String.valueOf(user.getId()));
            disconnectedUser.copyFrom(internalUser);
            disconnectedUser.setVersion(internalUser.getVersion());
        }

        //check if users password is the same
        final String hashedPassword = disconnectedUser.getHashedPassword();

        if(passwordHasher.isVerifierRecognized(hashedPassword)){
            try {
                passwordHasher.verifyPassword(newClearTextPassword.getBytes(Charsets.UTF8), hashedPassword);
                //same error string that used to be shown in PasswordDialog
                throw new InvalidPasswordException("The new password is the same as the old one.\nPlease enter a different password.");
            } catch (IncorrectPasswordException e) {
                //fall through ok
            } catch (PasswordHashingException e) {
                //fall through ok
            }
        }

        passwordEnforcerManager.isPasswordPolicyCompliant(newClearTextPassword);
        final InternalUserPasswordManager passwordManager = userManager.getUserPasswordManager();
        final boolean updateUser = passwordManager.configureUserPasswordHashes(disconnectedUser, newClearTextPassword);
        if(!updateUser){
            throw new IllegalStateException("Users should require update.");
        }
        passwordEnforcerManager.setUserPasswordPolicyAttributes(disconnectedUser, true);
        logger.info("Updated password for Internal User " + disconnectedUser.getLogin() + " [" + disconnectedUser.getOid() + "]");
        //todo - may not be an admin user, could check first
        logonServ.resetLogonFailCount(disconnectedUser);

        userManager.update(disconnectedUser);
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllGroups(long cfgid) throws FindException {
            return retrieveGroupManager(cfgid).findAllHeaders();
    }

    @Override
    public Group findGroupByID(long cfgid, String groupId) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
        if (provider == null) {
            logger.warning("Identity Provider #" + cfgid + " does not exist");
            return null;
        }
        GroupManager groupManager = provider.getGroupManager();

        return groupManager.findByPrimaryKey(groupId);
    }

    @Override
    public Group findGroupByName(long cfgid, String name) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
        if (provider == null) {
            logger.warning("Identity Provider #" + cfgid + " does not exist");
            return null;
        }
        GroupManager groupManager = provider.getGroupManager();

        return groupManager.findByName(name);
    }

    @Override
    public void deleteGroup(long cfgid, String groupId)
      throws DeleteException, ObjectNotFoundException {
        try {
            GroupManager groupManager = retrieveGroupManager(cfgid);
            Group grp = groupManager.findByPrimaryKey(groupId);
            if (grp == null) throw new ObjectNotFoundException("Group does not exist");
            groupManager.delete(grp);
            logger.info("Deleted Group: " + grp.getName());
        } catch (FindException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    @Override
    public String saveGroup(long identityProviderConfigId, Group group, Set<IdentityHeader> userHeaders)
      throws SaveException, UpdateException, ObjectNotFoundException {
        boolean isSave = true;
        try {
            String id = group.getId();
            isSave = id == null || id.equals(DEFAULT_ID);
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            GroupManager groupManager = provider.getGroupManager();
            if (group instanceof GroupBean) group = groupManager.reify((GroupBean) group);

            if (isSave) {
                id = groupManager.save(group, userHeaders);
                logger.info("Saved Group: " + group.getName() + " [" + id + "]");
            } else {
                groupManager.update(group, userHeaders);
                logger.info("Updated Group: " + group.getName() + " [" + id + "]");
            }

            return id;
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            if (isSave)
                throw new SaveException("FindException in saveGroup", e);
            else
                throw new UpdateException("FindException in saveGroup", e);
        }
    }

    @Override
    public String getUserCert(User user) throws FindException, CertificateEncodingException {
            // get cert from internal CA
            Certificate cert = clientCertManager.getUserCert(user);
            if (cert == null) return null;

        return HexUtils.encodeBase64(cert.getEncoded());
    }

    @Override
    public void revokeCert(User user) throws UpdateException, ObjectNotFoundException {
        try {
            // revoke the cert in internal CA
            clientCertManager.revokeUserCert(user);
            // internal users should have their password "revoked" along with their cert

            IdentityProvider provider = identityProviderFactory.getProvider(user.getProviderId());
            if (provider == null) throw new FindException("IdentityProvider could not be found");

            if (IdentityProviderType.INTERNAL.equals(provider.getConfig().type())) {
                logger.finest("Cert revoked - invalidating user's password.");

                InternalUser iuser = (InternalUser) user;

                // must change the password now
                UserManager userManager = provider.getUserManager();
                InternalUser dbuser = (InternalUser)userManager.findByLogin(iuser.getLogin());
                // maybe a new password is already provided?
                String newPasswd;
                if (!dbuser.getHashedPassword().equals(iuser.getHashedPassword())) {
                    newPasswd = iuser.getHashedPassword();
                } else {
                    // Set a random password (effectively disables password-based authentication as this user)
                    byte[] randomPasswd = new byte[32];
                    getSecureRandom().nextBytes(randomPasswd);
                    newPasswd = passwordHasher.hashPassword(randomPasswd);
                }
                dbuser.setHashedPassword(newPasswd);
                userManager.update(dbuser);
            }
        } catch (FindException e) {
            throw new UpdateException("error resetting user's password", e);
        }
    }

    @Override
    public int revokeCertificates() throws UpdateException {
        logger.info("Revoking all user certificates.");

        int revocationCount = 0;
        try {
            // determine DN of internal CA
            SignerInfo caInfo = defaultKey.getCaInfo();
            if (caInfo == null) throw new UpdateException("Certificate authority is not configured.");
            X500Principal caSubject = caInfo.getCertificate().getSubjectX500Principal();

            // revoke the cert in internal CA
            List<ClientCertManager.CertInfo> infos = clientCertManager.findAll();
            for (ClientCertManager.CertInfo info : infos) {
                logger.log(Level.FINE, "Revoking certificate for user ''{0}'' [{1}/{2}].",
                        new String[]{info.getLogin(),Long.toString(info.getProviderId()),info.getUserId()});

                UserBean userBean = new UserBean();
                userBean.setProviderId(info.getProviderId());
                userBean.setUniqueIdentifier(info.getUserId());
                userBean.setLogin(info.getLogin());
                if (clientCertManager.revokeUserCertIfIssuerMatches(userBean, caSubject)) {
                    revocationCount++;
                    logger.log(Level.INFO, "Revoked certificate for user ''{0}'' [{1}/{2}].",
                            new String[]{info.getLogin(),Long.toString(info.getProviderId()),info.getUserId()});
                } else {
                    logger.log(Level.INFO, "Certificate not revoked for user ''{0}'' [{1}/{2}].",
                            new String[]{info.getLogin(),Long.toString(info.getProviderId()),info.getUserId()});
                }
            }

            applicationEventPublisher.publishEvent(new AuditRevokeAllUserCertificates(this, revocationCount));

            // internal users should have their password "revoked" along with their cert
        } catch (FindException e) {
            throw new UpdateException("Error revoking certificates", e);
        }

        return revocationCount;
    }

    @Override
    public void recordNewUserCert(User user, Certificate cert) throws UpdateException {
        // revoke the cert in internal CA
        clientCertManager.recordNewUserCert(user, cert, false);
    }

    @Override
    public IdentityProviderPasswordPolicy getPasswordPolicyForIdentityProvider(long providerId) throws FindException {
        return passwordPolicyManger.findByInternalIdentityProviderOid(providerId);  
    }

    @Override
    public String getPasswordPolicyDescriptionForIdentityProvider() throws FindException{
        User user = JaasUtils.getCurrentUser();
        if(user == null)
            return null;
        if(user instanceof InternalUser)
            return passwordPolicyManger.findByInternalIdentityProviderOid(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID).getDescription();
        return null;
    }

    @Override
    public String updatePasswordPolicy(long providerId, IdentityProviderPasswordPolicy policy) throws SaveException, UpdateException, ObjectNotFoundException {
        if(providerId != IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID){
            throw new SaveException("Cannot update password policy for identity provider [" + providerId+"].");
        }

        policy.setInternalIdentityProviderOid(providerId);
        passwordPolicyManger.update(policy);
        return  policy.getOidAsLong().toString();
    }

    @Override                                         
    public void forceAdminUsersResetPassword(long identityProviderConfigId) throws FindException, SaveException, UpdateException, InvalidPasswordException {
        if (identityProviderConfigId != IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID) return;

        IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
        if (provider == null) throw new FindException("IdentityProvider could not be found");

        UserManager userManager = provider.getUserManager();
        EntityHeaderSet<IdentityHeader> headers = findAllUsers(identityProviderConfigId);
        for(IdentityHeader header : headers){
            User user = userManager.findByPrimaryKey(Long.toString(header.getOid()));
             if(user !=  null && user instanceof InternalUser ) {
                InternalUser internalUser = (InternalUser)user;
                if (isAdministrativeUser(internalUser)){
                    internalUser.setChangePassword(true);
                    saveUser(identityProviderConfigId, internalUser, null) ;
                }
             }
        }
        IdentityProviderConfig idConfig = findIdentityProviderConfigByID(identityProviderConfigId);
        applicationEventPublisher.publishEvent(new AdminEvent(this, MessageFormat.format(SystemMessages.FORCE_PASSWORD_RESET.getMessage(), idConfig.getName())) {
                    public Level getMinimumLevel() {
                        return Level.WARNING;
                    }
                });
    }

    @Override
    public void activateUser(User user) throws FindException, UpdateException {
        try {
            LogonInfo info = logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), false);
            info.setLastActivity(System.currentTimeMillis());
            info.resetFailCount(System.currentTimeMillis());
            info.setState(LogonInfo.State.ACTIVE);
            logonManager.update(info);

        } catch (FindException e) {
            throw new FindException( "No logon info for '" + user.getLogin() + "'", e);
        } catch (UpdateException e) {
            throw new UpdateException( "No logon info for '" + user.getLogin() + "'", e);
        }
    }

    @Override
    public LogonInfo getLogonInfo(User user) throws FindException{
    try {
            return logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), false);
        } catch (FindException e) {
            throw new FindException( "No logon info for '" + user.getLogin() + "'", e);
        }
    }

            
    /**
     * Determines if the user has roles.
     *
     * @return  TRUE if has roles, otherwise FALSE
     */
    private boolean isAdministrativeUser(InternalUser internalUser) {
        try {
            Collection<Role> roles = roleManager.getAssignedRoles(internalUser);

            if (roles == null) return false;
            return !roles.isEmpty();
        } catch(FindException e) {
            return false;
        }
    }

    private static final SecureRandom secureRandom = new SecureRandom();

    private SecureRandom getSecureRandom() {
        return secureRandom;
    }

    @Override
    public void testIdProviderConfig(IdentityProviderConfig identityProviderConfig)
      throws InvalidIdProviderCfgException {
        try {
            identityProviderFactory.test(identityProviderConfig);
        } catch (InvalidIdProviderCfgException e) {
            throw e;
        } catch (Throwable t) {
            logger.log(Level.INFO, "Identity Provider test failed because an exception was thrown", t);
            throw new InvalidIdProviderCfgException(t);
        }
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(long providerId, String userId) throws FindException {
        return retrieveGroupManager(providerId).getGroupHeaders(userId);
    }

    public void setGroupHeaders(long providerId, String userId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        retrieveGroupManager(providerId).setGroupHeaders(userId, groupHeaders);
    }

    @Override
    public Set<IdentityHeader> getUserHeaders(long providerId, String groupId) throws FindException {
        return retrieveGroupManager(providerId).getUserHeaders(groupId);
    }

    public void setUserHeaders(long providerId, String groupId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        retrieveGroupManager(providerId).setUserHeaders(groupId, groupHeaders);
    }

    @Override
    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        return ldapTemplateManager.getTemplates();
    }

    public void setIdentityProviderConfigManager(IdentityProviderConfigManager icf) {
        this.identityProviderConfigManager = icf;
    }

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void setLogonInfoManager (LogonInfoManager logonManager){
       this.logonManager = logonManager;
    }

    public void initDao() throws Exception {
        checkidentityProviderFactory();
        checkidentityProviderConfigManager();
        checkClientCertManager();
    }

    private void checkidentityProviderFactory() {
        if (identityProviderFactory == null) {
            throw new IllegalArgumentException("identity provider factory is required");
        }
    }

    private void checkidentityProviderConfigManager() {
        if (identityProviderConfigManager == null) {
            throw new IllegalArgumentException("identity provider config is required");
        }
    }

    private void checkClientCertManager() {
        if (clientCertManager == null) {
            throw new IllegalArgumentException("client certificate manager required");
        }
    }

    private IdentityProviderConfigManager getIdProvCfgMan() {
        return identityProviderConfigManager;
    }


    private UserManager retrieveUserManager(long cfgid) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getUserManager();
    }

    private GroupManager retrieveGroupManager(long cfgid) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getGroupManager();
    }

    private IdentityProviderConfigManager identityProviderConfigManager = null;
    private IdentityProviderFactory identityProviderFactory = null;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final LdapConfigTemplateManager ldapTemplateManager = new LdapConfigTemplateManager();
}
