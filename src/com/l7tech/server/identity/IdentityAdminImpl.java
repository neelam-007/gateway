package com.l7tech.server.identity;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.protocol.SecureSpanConstants;
import static com.l7tech.common.security.rbac.EntityType.ID_PROVIDER_CONFIG;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.security.rbac.RoleManager;

import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.UnsupportedEncodingException;
import javax.security.auth.x500.X500Principal;

/**
 * Server side implementation of the IdentityAdmin interface.
 * This was originally used with the Axis layer
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: May 26, 2003
 */
public class IdentityAdminImpl implements IdentityAdmin {
    private ClientCertManager clientCertManager;

    private final LicenseManager licenseManager;
    private final RoleManager roleManager;
    private final X509Certificate certificateAuthorityCertificate;

    private static final String DEFAULT_ID = Long.toString(PersistentEntity.DEFAULT_OID);

    public IdentityAdminImpl(final LicenseManager licenseManager,
                             final RoleManager roleManager,
                             final X509Certificate caCert) {
        if (licenseManager == null) throw new IllegalArgumentException("licenseManager is required");
        if (roleManager == null) throw new IllegalArgumentException("roleManager is required");

        this.licenseManager = licenseManager;
        this.roleManager = roleManager;
        this.certificateAuthorityCertificate = caCert;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
        }
    }

    /**
     * Returns a version string. This can be compared to version on client-side.
     *
     * @return value to be compared with the client side value of Service.VERSION;
     * @throws RemoteException
     */
    public String echoVersion() throws RemoteException {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }

    /**
     * @return Array of entity headers for all existing id provider config
     * @throws RemoteException
     */
    public EntityHeader[] findAllIdentityProviderConfig() throws RemoteException, FindException {
            Collection<EntityHeader> res = getIdProvCfgMan().findAllHeaders();
        return res.toArray(new EntityHeader[0]);
    }

    /**
     * @return An identity provider config object
     * @throws RemoteException
     */
    public IdentityProviderConfig findIdentityProviderConfigByID(long oid)
      throws RemoteException, FindException {
            return getIdProvCfgMan().findByPrimaryKey(oid);
    }

    public long saveIdentityProviderConfig(IdentityProviderConfig identityProviderConfig)
      throws RemoteException, SaveException, UpdateException {
        try {
            long oid;
            checkLicense();
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
     * @throws RemoteException
     * @throws DeleteException
     */
    private void deleteAllUsers(long ipoid) throws RemoteException, DeleteException {
        try {
            UserManager userManager = retrieveUserManager(ipoid);
            if (userManager == null) throw new RemoteException("Cannot retrieve the UserManager");
            userManager.deleteAll(ipoid);
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
     * @throws RemoteException
     * @throws DeleteException
     */
    private void deleteAllGroups(long ipoid) throws RemoteException, DeleteException {
        try {
            GroupManager groupManager = retrieveGroupManager(ipoid);
            if (groupManager == null) throw new RemoteException("Cannot retrieve the GroupManager");
            groupManager.deleteAll(ipoid);
        } catch (ObjectNotFoundException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    /**
     * Delete all virtual groups of the identity provider given the identity provider Id
     * <p/>
     * Must be called in a transaction!
     *
     * @param ipoid The identity provider id
     * @throws RemoteException
     * @throws DeleteException
     */
    private void deleteAllVirtualGroups(long ipoid) throws RemoteException, DeleteException {
        try {
            GroupManager groupManager = retrieveGroupManager(ipoid);
            if (groupManager == null) throw new RemoteException("Cannot retrieve the GroupManager");
            groupManager.deleteAllVirtual(ipoid);
        } catch (ObjectNotFoundException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    public void deleteIdentityProviderConfig(long oid) throws RemoteException, DeleteException {
        try {
            checkLicense();
            IdentityProviderConfigManager manager = getIdProvCfgMan();

            final IdentityProviderConfig ipc = manager.findByPrimaryKey(oid);

            if (ipc.type() == IdentityProviderType.FEDERATED) {
                deleteAllUsers(oid);
                deleteAllGroups(oid);
                deleteAllVirtualGroups(oid);
            }

            manager.delete(ipc);

            roleManager.deleteEntitySpecificRole(ID_PROVIDER_CONFIG, ipc);
            logger.info("Deleted IDProviderConfig: " + ipc);
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    public IdentityHeader[] findAllUsers(long identityProviderConfigId) throws RemoteException, FindException {
            checkLicense();
            UserManager userManager = retrieveUserManager(identityProviderConfigId);
            Collection<IdentityHeader> res = userManager.findAllHeaders();
            return res.toArray(new IdentityHeader[0]);
    }

    public IdentityHeader[] searchIdentities(long identityProviderConfigId, EntityType[] types, String pattern)
      throws RemoteException, FindException {
            checkLicense();
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            if (types != null) {
                // TODO is this necessary?   EntityType already has a readResolve()
                for (int i = 0; i < types.length; i++) {
                    types[i] = EntityType.fromValue(types[i].getVal());
                }
            }
            Collection<IdentityHeader> searchResults = provider.search(types, pattern);
            if (searchResults == null) return new IdentityHeader[0];
            return searchResults.toArray(new IdentityHeader[0]);
    }

    public User findUserByID(long identityProviderConfigId, String userId)
                           throws RemoteException, FindException {
        checkLicense();
        IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
        if (provider == null) {
            logger.warning("Identity Provider #" + identityProviderConfigId + " does not exist");
            return null;
        }
        UserManager userManager = provider.getUserManager();

        return userManager.findByPrimaryKey(userId);
    }

    public User findUserByLogin(long idProvCfgId, String login) throws RemoteException, FindException {
        checkLicense();
        IdentityProvider provider = identityProviderFactory.getProvider(idProvCfgId);
        if (provider == null) {
            logger.warning("Identity Provider #" + idProvCfgId + " does not exist");
            return null;
        }
        UserManager userManager = provider.getUserManager();

        return userManager.findByLogin(login);
    }

    public void deleteUser(long cfgid, String userId)
      throws RemoteException, DeleteException, ObjectNotFoundException {
        try {
            checkLicense();
            UserManager userManager = retrieveUserManager(cfgid);
            if (userManager == null) throw new RemoteException("Cannot retrieve the UserManager");
            User user = userManager.findByPrimaryKey(userId);
            if (user == null) {
                throw new ObjectNotFoundException(" User " + userId);
            }
            if (user.equals(JaasUtils.getCurrentUser()))
                throw new DeleteException("The currently logged-in user cannot be deleted");
            userManager.delete(user);
            logger.info("Deleted User: " + user.getLogin());
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    public String saveUser(long identityProviderConfigId, User user, Set groupHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        try {
            checkLicense();
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();
            if (user instanceof UserBean) user = userManager.reify((UserBean) user);

            String id = user.getId();
            if (id == null || DEFAULT_ID.equals(id)) {
                id = userManager.save(user, groupHeaders);
                logger.info("Saved User: " + user.getLogin() + " [" + id + "]");
            } else {
                userManager.update(user, groupHeaders);
                logger.info("Updated User: " + user.getLogin() + " [" + id + "]");
            }

            return id;
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("Exception in saveUser", e);
        }
    }


    public IdentityHeader[] findAllGroups(long cfgid) throws RemoteException, FindException {
            checkLicense();
            Collection<IdentityHeader> res = retrieveGroupManager(cfgid).findAllHeaders();
            return res.toArray(new IdentityHeader[0]);
    }

    public Group findGroupByID(long cfgid, String groupId) throws RemoteException, FindException {
        checkLicense();
        IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
        if (provider == null) {
            logger.warning("Identity Provider #" + cfgid + " does not exist");
            return null;
        }
        GroupManager groupManager = provider.getGroupManager();

        return groupManager.findByPrimaryKey(groupId);
    }

    public void deleteGroup(long cfgid, String groupId)
      throws RemoteException, DeleteException, ObjectNotFoundException {
        try {
            checkLicense();
            GroupManager groupManager = retrieveGroupManager(cfgid);
            Group grp = groupManager.findByPrimaryKey(groupId);
            if (grp == null) throw new ObjectNotFoundException("Group does not exist");
            groupManager.delete(grp);
            logger.info("Deleted Group: " + grp.getName());
        } catch (FindException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    public String saveGroup(long identityProviderConfigId, Group group, Set<IdentityHeader> userHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        try {
            checkLicense();
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            GroupManager groupManager = provider.getGroupManager();
            if (group instanceof GroupBean) group = groupManager.reify((GroupBean) group);

            String id = group.getId();
            if (id == null || id.equals(DEFAULT_ID)) {
                id = groupManager.save(group, userHeaders);
                logger.info("Saved Group: " + group.getName() + " [" + id + "]");
            } else {
                groupManager.update(group, userHeaders);
                logger.info("Updated Group: " + group.getName() + " [" + id + "]");
            }

            return id;
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("FindException in saveGroup", e);
        }
    }

    public String getUserCert(User user) throws RemoteException, FindException, CertificateEncodingException {
            checkLicense();
            // get cert from internal CA
            Certificate cert = clientCertManager.getUserCert(user);
            if (cert == null) return null;

        return HexUtils.encodeBase64(cert.getEncoded());
    }

    public void revokeCert(User user) throws RemoteException, UpdateException, ObjectNotFoundException {
        try {
            checkLicense();
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
                    try {
                        newPasswd = HexUtils.encodePasswd(iuser.getLogin(), new String(randomPasswd, "ISO8859-1"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e); // Can't happen
                    }
                }
                dbuser.setHashedPassword(newPasswd);
                userManager.update(dbuser);
            }
        } catch (FindException e) {
            throw new UpdateException("error resetting user's password", e);
        }
    }

    public int revokeCertificates() throws RemoteException, UpdateException {
        logger.info("Revoking all user certificates.");

        int revocationCount = 0;
        try {
            checkLicense();

            // determine DN of internal CA
            X509Certificate caCert = certificateAuthorityCertificate;
            if (caCert == null) throw new UpdateException("Certificate authority is not configured.");
            X500Principal caSubject = caCert.getSubjectX500Principal();            

            // revoke the cert in internal CA
            List<ClientCertManager.CertInfo> infos = clientCertManager.findAll();
            for (ClientCertManager.CertInfo info : infos) {
                logger.log(Level.INFO, "Revoking certificate for user ''{0}''.", info.getLogin());

                UserBean userBean = new UserBean();
                userBean.setProviderId(info.getProviderId());
                userBean.setUniqueIdentifier(info.getUserId());
                userBean.setLogin(info.getLogin());
                if (clientCertManager.revokeUserCertIfIssuerMatches(userBean, caSubject)) {
                    revocationCount++;
                    logger.log(Level.INFO, "Revoked certificate for user ''{0}''.", info.getLogin());
                } else {
                    logger.log(Level.INFO, "Certificate not revoked for user ''{0}''.", info.getLogin());
                }
            }

            // internal users should have their password "revoked" along with their cert
        } catch (FindException e) {
            throw new UpdateException("Error revoking certificates", e);
        } catch (ObjectNotFoundException onfe) {
            throw new UpdateException("Error revoking certificates", onfe);
        }

        return revocationCount;
    }

    public void recordNewUserCert(User user, Certificate cert) throws RemoteException, UpdateException {
        checkLicense();
        // revoke the cert in internal CA
        clientCertManager.recordNewUserCert(user, cert, false);
    }

    private static SecureRandom secureRandom = null;

    private synchronized SecureRandom getSecureRandom() {
        if (secureRandom != null) return secureRandom;
        return secureRandom = new SecureRandom();
    }

    public void testIdProviderConfig(IdentityProviderConfig identityProviderConfig)
      throws RemoteException, InvalidIdProviderCfgException {
        try {
            checkLicense();
            getIdProvCfgMan().test(identityProviderConfig);
        } catch (InvalidIdProviderCfgException e) {
            throw e;
        } catch (Throwable t) {
            logger.log(Level.INFO, "Identity Provider test failed because an exception was thrown", t);
            throw new InvalidIdProviderCfgException(t);
        }
    }

    public Set<IdentityHeader> getGroupHeaders(long providerId, String userId) throws RemoteException, FindException {
        checkLicense();
        return retrieveGroupManager(providerId).getGroupHeaders(userId);
    }

    public void setGroupHeaders(long providerId, String userId, Set<IdentityHeader> groupHeaders) throws RemoteException, FindException, UpdateException {
        checkLicense();
        retrieveGroupManager(providerId).setGroupHeaders(userId, groupHeaders);
    }

    public Set<IdentityHeader> getUserHeaders(long providerId, String groupId) throws RemoteException, FindException {
        checkLicense();
        return retrieveGroupManager(providerId).getUserHeaders(groupId);
    }

    public void setUserHeaders(long providerId, String groupId, Set<IdentityHeader> groupHeaders) throws RemoteException, FindException, UpdateException {
        checkLicense();
        retrieveGroupManager(providerId).setUserHeaders(groupId, groupHeaders);
    }

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

    public void initDao() throws Exception {
        checkidentityProviderFactory();
        checkidentityProviderConfigManager();
        checkClientCertManager();
        checkLicenseManager();
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

    private void checkLicenseManager() {
        if (licenseManager == null) {
            throw new IllegalArgumentException("LicenseManager is required");
        }
    }

    private IdentityProviderConfigManager getIdProvCfgMan() {
        return identityProviderConfigManager;
    }


    private UserManager retrieveUserManager(long cfgid)
      throws RemoteException {
        UserManager ret;
        try {
            IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            ret = provider.getUserManager();
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("RemoteException in retrieveUserManager", e);
        }
        return ret;
    }

    private GroupManager retrieveGroupManager(long cfgid)
      throws RemoteException {
        GroupManager ret;
        try {
            IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            ret = provider.getGroupManager();
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("RemoteException in retrieveGroupManager", e);
        }
        return ret;
    }

    private IdentityProviderConfigManager identityProviderConfigManager = null;
    private IdentityProviderFactory identityProviderFactory = null;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final LdapConfigTemplateManager ldapTemplateManager = new LdapConfigTemplateManager();
}
