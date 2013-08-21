package com.l7tech.server.identity;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.password.IncorrectPasswordException;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.identity.LogonInfo;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.ntlm.adapter.NetlogonAdapter;
import com.l7tech.ntlm.protocol.AuthenticationManagerException;
import com.l7tech.objectmodel.*;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.TrustedEsmUserManager;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.AdministrativePasswordsResetEvent;
import com.l7tech.server.event.admin.AuditRevokeAllUserCertificates;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.internal.InternalUserPasswordManager;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.logon.LogonInfoManager;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private LogonInfoManager logonManager;
    private final PasswordHasher passwordHasher;
    private final Collection<Pair<AccountMinimums, IdentityProviderPasswordPolicy>> policyMinimums;

    @Inject
    private TrustedEsmUserManager trustedEsmUserManager;

    @Inject
    private Config config;

    private static final String DEFAULT_ID = Goid.toString(PersistentEntity.DEFAULT_GOID);

    public IdentityAdminImpl(final RoleManager roleManager,
                             final IdentityProviderPasswordPolicyManager passwordPolicyManger,
                             final PasswordEnforcerManager passwordEnforcerManager,
                             final DefaultKey defaultKey,
                             final PasswordHasher passwordHasher,
                             final Collection<Pair<AccountMinimums, IdentityProviderPasswordPolicy>> policyMinimums,
                             final LdapConfigTemplateManager ldapTemplateManager ) {
        if (roleManager == null) throw new IllegalArgumentException("roleManager is required");

        this.roleManager = roleManager;
        this.defaultKey = defaultKey;
        this.passwordPolicyManger = passwordPolicyManger;
        this.passwordEnforcerManager = passwordEnforcerManager;
        this.passwordHasher = passwordHasher;
        this.policyMinimums = policyMinimums;
        this.ldapTemplateManager = ldapTemplateManager;
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
        return res.toArray(new EntityHeader[res.size()]);
    }

    /**
     * @return An identity provider config object
     */
    @Override
    public IdentityProviderConfig findIdentityProviderConfigByID(Goid oid)
            throws FindException {
        return getIdProvCfgMan().findByPrimaryKey(oid);
    }

    @Override
    public Goid saveIdentityProviderConfig(IdentityProviderConfig identityProviderConfig)
            throws SaveException, UpdateException {
        try {
            Goid oid;
            if (!Goid.isDefault(identityProviderConfig.getGoid())) {
                IdentityProviderConfigManager manager = getIdProvCfgMan();
                IdentityProviderConfig originalConfig = manager.findByPrimaryKey(identityProviderConfig.getGoid());
                if ( originalConfig == null ) {
                    throw new SaveException("Identity provider not found for id '"+identityProviderConfig.getGoid()+"'.");
                }

                originalConfig.copyFrom(identityProviderConfig);
                manager.update(originalConfig);
                logger.info("Updated IDProviderConfig: " + identityProviderConfig.getGoid());
                oid = identityProviderConfig.getGoid();
            } else {
                logger.info("Saving IDProviderConfig: " + identityProviderConfig.getGoid());
                oid = getIdProvCfgMan().save(identityProviderConfig);
                getIdProvCfgMan().createRoles(identityProviderConfig);
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
    private void deleteAllUsers(Goid ipoid) throws DeleteException {
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
    private void deleteAllGroups(Goid ipoid) throws DeleteException {
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
    private void deleteAllVirtualGroups(Goid ipoid) throws DeleteException {
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
    public void deleteIdentityProviderConfig(Goid oid) throws DeleteException {
        try {
            IdentityProviderConfigManager manager = getIdProvCfgMan();

            final IdentityProviderConfig ipc = manager.findByPrimaryKey(oid);
            if ( ipc == null ) {
                throw new DeleteException("Identity provider not found for id '"+oid+"'.");
            }

            if (ipc.type() == IdentityProviderType.FEDERATED) {
                deleteAllUsers(oid);
                deleteAllGroups(oid);
                deleteAllVirtualGroups(oid);
            }

            manager.deleteRoles( ipc.getGoid() );
            manager.delete(ipc);

            trustedEsmUserManager.deleteMappingsForIdentityProvider(ipc.getGoid());
            logger.info("Deleted IDProviderConfig: " + ipc);
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException("Error finding identity provider for deletion.", e);
        }
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllUsers(Goid identityProviderConfigId) throws FindException {
        UserManager<?> userManager = retrieveUserManager(identityProviderConfigId);
        return userManager.findAllHeaders();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> searchIdentities(Goid identityProviderConfigId, EntityType[] types, String pattern)
            throws FindException {
        IdentityProvider<?, ?, ?, ?> provider = identityProviderFactory.getProvider(identityProviderConfigId);
        if (provider == null) throw new FindException("IdentityProvider could not be found");
        return provider.search(types, pattern);
    }

    @Override
    public User findUserByID(Goid identityProviderConfigId, String userId)
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
    public User findUserByLogin(Goid idProvCfgId, String login) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(idProvCfgId);
        if (provider == null) {
            logger.warning("Identity Provider #" + idProvCfgId + " does not exist");
            return null;
        }
        UserManager userManager = provider.getUserManager();

        return userManager.findByLogin(login);
    }

    @Override
    public void deleteUser(Goid cfgid, String userId)
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
    public String saveUser(Goid idProvCfgId, User user, Set<IdentityHeader> groupHeaders)
            throws SaveException, UpdateException, ObjectNotFoundException {
        try {
            return saveUser(idProvCfgId, user, groupHeaders, null);
        } catch (InvalidPasswordException e) {
            //cannot happen
            throw new IllegalStateException("InvalidPasswordException caught when updating an existing user.");
        }
    }

    @Override
    public String saveUser(Goid identityProviderConfigId, User user, Set groupHeaders, @Nullable String clearTextPassword)
            throws SaveException, UpdateException, ObjectNotFoundException, InvalidPasswordException {
        boolean isSave = true;
        try {
            String id = user.getId();
            isSave = id == null || DEFAULT_ID.equals(id);
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();
            if (user instanceof UserBean) user = userManager.reify((UserBean) user);

            if (user instanceof InternalUser) {
                InternalUser internalUser = (InternalUser) user;
                if (!isSave) {
                    //ensure password has not changed
                    final InternalUser originalUser = (InternalUser) userManager.findByPrimaryKey(internalUser.getId());
                    if (!originalUser.getHashedPassword().equals(internalUser.getHashedPassword())) {
                        throw new UpdateException("Cannot modify existing users password using this api.");
                    }
                    final String origDigest = originalUser.getHttpDigest();
                    if (origDigest == null && internalUser.getHttpDigest() != null) {
                        throw new UpdateException("Cannot modify existing users digest using this api.");
                    }
                    if (origDigest != null && !origDigest.equals(internalUser.getHttpDigest())) {
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
                    assert (updateRequired);
                }
            }

            if (user instanceof FederatedUser) {
                FederatedUser federatedUser = (FederatedUser) user;
                federatedUser.setSubjectDn(CertUtils.formatDN(federatedUser.getSubjectDn()));
            }

            if (isSave) {
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
        if (!(provider instanceof InternalIdentityProvider)) {
            throw new UpdateException("Cannot change non internal users password.");
        }

        if (newClearTextPassword == null || newClearTextPassword.trim().isEmpty()) {
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

        if (passwordHasher.isVerifierRecognized(hashedPassword)) {
            try {
                passwordHasher.verifyPassword(newClearTextPassword.getBytes(Charsets.UTF8), hashedPassword);
                //same error string that used to be shown in PasswordDialog
                throw new InvalidPasswordException("The new password is the same as the old one.\nPlease enter a different password.");
            } catch (IncorrectPasswordException e) {
                //fall through ok - this is the expected case - the password being set is different.
            }
        }

        passwordEnforcerManager.isPasswordPolicyCompliant(newClearTextPassword);
        final InternalUserPasswordManager passwordManager = userManager.getUserPasswordManager();
        final boolean updateUser = passwordManager.configureUserPasswordHashes(disconnectedUser, newClearTextPassword);
        if (!updateUser) {
            throw new IllegalStateException("Users should require update.");
        }
        passwordEnforcerManager.setUserPasswordPolicyAttributes(disconnectedUser, true);
        logger.info("Updated password for Internal User " + disconnectedUser.getLogin() + " [" + disconnectedUser.getGoid() + "]");
        // activate user
        activateUser(disconnectedUser);

        userManager.update(disconnectedUser);
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllGroups(Goid cfgid) throws FindException {
        return retrieveGroupManager(cfgid).findAllHeaders();
    }

    @Override
    public Group findGroupByID(Goid cfgid, String groupId) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
        if (provider == null) {
            logger.warning("Identity Provider #" + cfgid + " does not exist");
            return null;
        }
        GroupManager groupManager = provider.getGroupManager();

        return groupManager.findByPrimaryKey(groupId);
    }

    @Override
    public Group findGroupByName(Goid cfgid, String name) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
        if (provider == null) {
            logger.warning("Identity Provider #" + cfgid + " does not exist");
            return null;
        }
        GroupManager groupManager = provider.getGroupManager();

        return groupManager.findByName(name);
    }

    @Override
    public void deleteGroup(Goid cfgid, String groupId)
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
    public String saveGroup(Goid identityProviderConfigId, Group group, Set<IdentityHeader> userHeaders)
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
    public boolean doesCurrentUserHaveCert() throws FindException, AuthenticationException {
        final User currentUser = JaasUtils.getCurrentUser();
        if(currentUser == null) throw new AuthenticationException("Current user as not found");

        final IdentityProvider provider = identityProviderFactory.getProvider(currentUser.getProviderId());
        return provider.hasClientCert(currentUser.getLogin());
    }

    @Override
    public boolean currentUsersPasswordCanBeChanged() throws AuthenticationException, FindException {
        final User currentUser = JaasUtils.getCurrentUser();
        if(currentUser == null) throw new AuthenticationException("Current user as not found");

        final IdentityProvider provider = identityProviderFactory.getProvider(currentUser.getProviderId());
        return provider.getConfig().isWritable();
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
                InternalUser dbuser = (InternalUser) userManager.findByLogin(iuser.getLogin());
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
                        new String[]{info.getLogin(), Goid.toString(info.getProviderId()), info.getUserId()});

                UserBean userBean = new UserBean();
                userBean.setProviderId(info.getProviderId());
                userBean.setUniqueIdentifier(info.getUserId());
                userBean.setLogin(info.getLogin());
                if (clientCertManager.revokeUserCertIfIssuerMatches(userBean, caSubject)) {
                    revocationCount++;
                    logger.log(Level.INFO, "Revoked certificate for user ''{0}'' [{1}/{2}].",
                            new String[]{info.getLogin(), Goid.toString(info.getProviderId()), info.getUserId()});
                } else {
                    logger.log(Level.INFO, "Certificate not revoked for user ''{0}'' [{1}/{2}].",
                            new String[]{info.getLogin(), Goid.toString(info.getProviderId()), info.getUserId()});
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
    public IdentityProviderPasswordPolicy getPasswordPolicyForIdentityProvider(Goid providerId) throws FindException {
        return passwordPolicyManger.findByInternalIdentityProviderOid(providerId);
    }

    @Override
    public AccountMinimums getAccountMinimums() {
        final String policyName = config.getBooleanProperty( ServerConfigParams.PARAM_PCIDSS_ENABLED, false) ?
                "PCI-DSS" :
                "STIG";

        final Pair<AccountMinimums, IdentityProviderPasswordPolicy> minimums = Functions.grepFirst(
                policyMinimums,
                new Functions.Unary<Boolean, Pair<AccountMinimums, IdentityProviderPasswordPolicy>>() {
                    @Override
                    public Boolean call(final Pair<AccountMinimums, IdentityProviderPasswordPolicy> accountMinimumsIdentityProviderPasswordPolicyPair) {
                        return policyName.equals(accountMinimumsIdentityProviderPasswordPolicyPair.left.getName());
                    }
                });

        return minimums == null ? null : minimums.left;
    }

    @Override
    public Map<String, AccountMinimums> getAccountMinimumsMap() {
        return Functions.reduce(
                policyMinimums,
                new HashMap<String, AccountMinimums>(),
                new Functions.Binary<Map<String, AccountMinimums>, Map<String, AccountMinimums>, Pair<AccountMinimums, IdentityProviderPasswordPolicy>>() {
                    @Override
                    public Map<String, AccountMinimums> call(
                            final Map<String, AccountMinimums> accountsMinimumMap,
                            final Pair<AccountMinimums, IdentityProviderPasswordPolicy> minimumsPair) {
                        accountsMinimumMap.put(minimumsPair.left.getName(), minimumsPair.left);
                        return accountsMinimumMap;
                    }
                });
    }

    @Override
    public Map<String, IdentityProviderPasswordPolicy> getPasswordPolicyMinimums() {
        return Functions.reduce(
                policyMinimums,
                new HashMap<String, IdentityProviderPasswordPolicy>(),
                new Functions.Binary<Map<String, IdentityProviderPasswordPolicy>, Map<String, IdentityProviderPasswordPolicy>, Pair<AccountMinimums, IdentityProviderPasswordPolicy>>() {
                    @Override
                    public Map<String, IdentityProviderPasswordPolicy> call(
                            final Map<String, IdentityProviderPasswordPolicy> passwordPolicyMap,
                            final Pair<AccountMinimums, IdentityProviderPasswordPolicy> minimumsPair) {
                        passwordPolicyMap.put(minimumsPair.left.getName(), minimumsPair.right);
                        return passwordPolicyMap;
                    }
                });
    }

    @Override
    public String getPasswordPolicyDescriptionForIdentityProvider() throws FindException {
        User user = JaasUtils.getCurrentUser();
        if (user == null)
            return null;
        final IdentityProviderPasswordPolicy policy = passwordPolicyManger.findByInternalIdentityProviderOid(user.getProviderId());
        return policy == null ? null : policy.getDescription();
    }

    @Override
    public String updatePasswordPolicy(Goid providerId, IdentityProviderPasswordPolicy policy) throws SaveException, UpdateException, ObjectNotFoundException {
        if (!providerId.equals(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID) ){
            throw new SaveException("Cannot update password policy for identity provider [" + providerId + "].");
        }

        policy.setInternalIdentityProviderGoid(providerId);
        passwordPolicyManger.update(policy);
        return policy.getId();
    }

    @Override
    public void forceAdminUsersResetPassword(final Goid identityProviderConfigId) throws ObjectModelException {
        final IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
        if (provider instanceof InternalIdentityProvider) {
            final InternalIdentityProvider internalProvider = (InternalIdentityProvider) provider;
            final InternalUserManager internalUserManager = internalProvider.getUserManager();
            final Collection<IdentityHeader> headers = internalUserManager.findAllHeaders();
            for (final IdentityHeader header : headers) {
                final InternalUser user = internalUserManager.findByPrimaryKey(header.getStrId());
                if (isAdministrativeUser(user)) {
                    user.setChangePassword(true);
                    internalUserManager.update(user, null);
                }
            }

            applicationEventPublisher.publishEvent(new AdministrativePasswordsResetEvent(this, internalProvider.getConfig().getName()));
        }
    }

    @Override
    public void activateUser(User user) throws FindException, UpdateException {
        try {
            LogonInfo info = logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), false);
            // if user has logged in before
            if (info != null && info.getLastAttempted() > 0) {
                String action = null;
                info.setLastActivity(System.currentTimeMillis());
                info.resetFailCount(System.currentTimeMillis());
                if (info.getState().equals(LogonInfo.State.INACTIVE))
                    action = "activated";
                else if (info.getState().equals(LogonInfo.State.EXCEED_ATTEMPT))
                    action = "unlocked";
                info.setState(LogonInfo.State.ACTIVE);
                logonManager.update(info);
                if (action != null) {
                    String msg = "User '" + user.getLogin() + "' is " + action;
                    applicationEventPublisher.publishEvent(new AdminEvent(this, msg, Level.WARNING) {
                    });
                }
            }

        } catch (FindException e) {
            throw new FindException("No logon info for '" + user.getLogin() + "'", e);
        } catch (UpdateException e) {
            throw new UpdateException("No logon info for '" + user.getLogin() + "'", e);
        }
    }

    @Override
    public LogonInfo.State getLogonState(User user) throws FindException {
        try {
            LogonInfo info = logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), false);
            if (info != null)
                return info.getState();
            return null;
        } catch (FindException e) {
            throw new FindException("No logon info for '" + user.getLogin() + "'", e);
        }
    }


    /**
     * Determines if the user has roles.
     *
     * @return TRUE if has roles, otherwise FALSE
     */
    private boolean isAdministrativeUser(final User user) throws FindException {
        final Collection<Role> roles = roleManager.getAssignedRoles(user, true, false);
        return roles != null && !roles.isEmpty();
    }

    private static final SecureRandom secureRandom = new SecureRandom();

    private SecureRandom getSecureRandom() {
        return secureRandom;
    }


    @Override
    public void testNtlmConfig(Map<String, String> props) throws InvalidIdProviderCfgException {
        Map<String, String> p = new HashMap<String, String>(props);
        if(p.containsKey("service.passwordOid")) {
            try {
                Goid goid = GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, p.get("service.passwordOid"));
                String plaintextPassword = ServerVariables.getSecurePasswordByGoid(new LoggingAudit(logger), goid);
                p.put("service.password", plaintextPassword);
            } catch (FindException e) {
                throw new InvalidIdProviderCfgException("Password is invalid", e);
            } catch(IllegalArgumentException e){
                throw new InvalidIdProviderCfgException("Password is invalid", e);
            }
        }

        try {
            NetlogonAdapter testNetlogon = new NetlogonAdapter(p);
            testNetlogon.testConnect();
        } catch (AuthenticationManagerException ame) {
            logger.log(Level.FINE, "Failed to connect to Netlogon service" + ame.getMessage());
            throw new InvalidIdProviderCfgException("Invalid NTLM configuration", ame);
        }
    }

    @Override
    public void testIdProviderConfig(IdentityProviderConfig identityProviderConfig, String testUser, char[] testPassword)
            throws InvalidIdProviderCfgException
    {
        if (identityProviderConfig == null)
            throw new NullPointerException("non-null identityProviderConfig is required for test");
        try {
            identityProviderFactory.test(identityProviderConfig, testUser, testPassword);
        } catch (InvalidIdProviderCfgException e) {
            throw e;
        } catch (Throwable t) {
            logger.log(Level.INFO, "Identity Provider test failed because an exception was thrown", t);
            throw new InvalidIdProviderCfgException(t);
        }
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(Goid providerId, String userId) throws FindException {
        return retrieveGroupManager(providerId).getGroupHeaders(userId);
    }

    @Override
    public Set<IdentityHeader> getGroupHeadersForGroup(final Goid providerId, @NotNull final String groupId) throws FindException {
        return retrieveGroupManager(providerId).getGroupHeadersForNestedGroup(groupId);
    }

    public void setGroupHeaders(Goid providerId, String userId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        retrieveGroupManager(providerId).setGroupHeaders(userId, groupHeaders);
    }

    @Override
    public Set<IdentityHeader> getUserHeaders(Goid providerId, String groupId) throws FindException {
        return retrieveGroupManager(providerId).getUserHeaders(groupId);
    }

    public void setUserHeaders(Goid providerId, String groupId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
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

    public void setLogonInfoManager(LogonInfoManager logonManager) {
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


    private UserManager retrieveUserManager(Goid cfgid) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getUserManager();
    }

    private GroupManager<?, ?> retrieveGroupManager(Goid cfgid) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getGroupManager();
    }

    private IdentityProviderConfigManager identityProviderConfigManager;
    private IdentityProviderFactory identityProviderFactory;
    private LdapConfigTemplateManager ldapTemplateManager;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
