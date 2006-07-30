package com.l7tech.server.identity;

import com.l7tech.common.Authorizer;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Collection;
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
public class IdentityAdminImpl implements IdentityAdmin {
    private ClientCertManager clientCertManager;

    private final LicenseManager licenseManager;

    public IdentityAdminImpl(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            throw new RemoteException(e.getMessage());
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
            Collection res = getIdProvCfgMan().findAllHeaders();
        return (EntityHeader[])res.toArray(new EntityHeader[] {});
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
            checkLicense();
            if (identityProviderConfig.getOid() > 0) {
                IdentityProviderConfigManager manager = getIdProvCfgMan();
                IdentityProviderConfig originalConfig = manager.findByPrimaryKey(identityProviderConfig.getOid());
                originalConfig.copyFrom(identityProviderConfig);
                manager.update(originalConfig);
                logger.info("Updated IDProviderConfig: " + identityProviderConfig.getOid());
                return identityProviderConfig.getOid();
            }
            logger.info("Saving IDProviderConfig: " + identityProviderConfig.getOid());
            return getIdProvCfgMan().save(identityProviderConfig);
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
        } catch (ObjectNotFoundException e) {
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
            logger.info("Deleted IDProviderConfig: " + ipc);
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        }
    }

    public EntityHeader[] findAllUsers(long identityProviderConfigId) throws RemoteException, FindException {
            checkLicense();
            UserManager userManager = retrieveUserManager(identityProviderConfigId);
            Collection<IdentityHeader> res = userManager.findAllHeaders();
            return res.toArray(new EntityHeader[]{});
    }

    public EntityHeader[] searchIdentities(long identityProviderConfigId, EntityType[] types, String pattern)
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
            if (searchResults == null) return new EntityHeader[0];
            return (EntityHeader[])searchResults.toArray(new EntityHeader[]{});
    }

    public User findUserByID(long identityProviderConfigId, String userId)
      throws RemoteException, FindException {
            checkLicense();
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();

            return userManager.findByPrimaryKey(userId);
    }

    public User findUserByLogin(long idProvCfgId, String login) throws RemoteException, FindException {
            checkLicense();
            IdentityProvider provider = identityProviderFactory.getProvider(idProvCfgId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
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
            user.getUserBean().setProviderId(identityProviderConfigId);

            String id = user.getUniqueIdentifier();
            if (id == null || id.equals(Long.toString(Entity.DEFAULT_OID))) {
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


    public EntityHeader[] findAllGroups(long cfgid) throws RemoteException, FindException {
            checkLicense();
            Collection res = retrieveGroupManager(cfgid).findAllHeaders();
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
    }

    public Group findGroupByID(long cfgid, String groupId) throws RemoteException, FindException {
            checkLicense();
            IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
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

    public String saveGroup(long identityProviderConfigId, Group group, Set userHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        try {
            checkLicense();
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            GroupManager groupManager = provider.getGroupManager();
            if (group instanceof GroupBean) group = groupManager.reify((GroupBean) group);
            group.getGroupBean().setProviderId(identityProviderConfigId);

            String id = group.getUniqueIdentifier();
            if (id == null || id.equals(Long.toString(Entity.DEFAULT_OID))) {
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
                // must change the password now
                UserManager userManager = provider.getUserManager();
                InternalUser dbuser = (InternalUser)userManager.findByLogin(user.getLogin());
                // maybe a new password is already provided?
                String newPasswd;
                if (!dbuser.getPassword().equals(user.getPassword())) {
                    newPasswd = user.getPassword();
                } else {
                    byte[] randomPasswd = new byte[32];
                    getSecureRandom().nextBytes(randomPasswd);
                    newPasswd = new String(randomPasswd);
                }
                dbuser.setPassword(newPasswd);
                userManager.update(dbuser);
            }
        } catch (FindException e) {
            throw new UpdateException("error resetting user's password", e);
        } catch (InvalidPasswordException e) {
            throw new UpdateException("error resetting user's password", e);
        }
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

    public Set getGroupHeaders(long providerId, String userId) throws RemoteException, FindException {
        checkLicense();
        return retrieveGroupManager(providerId).getGroupHeaders(userId);
    }

    public void setGroupHeaders(long providerId, String userId, Set groupHeaders) throws RemoteException, FindException, UpdateException {
        checkLicense();
        retrieveGroupManager(providerId).setGroupHeaders(userId, groupHeaders);
    }

    public Set getUserHeaders(long providerId, String groupId) throws RemoteException, FindException {
        checkLicense();
        return retrieveGroupManager(providerId).getUserHeaders(groupId);
    }

    public void setUserHeaders(long providerId, String groupId, Set groupHeaders) throws RemoteException, FindException, UpdateException {
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

    public void setAuthorizer(Authorizer authorizer) {
        this.authorizer = authorizer;
    }

    public void initDao() throws Exception {
        checkidentityProviderFactory();
        checkidentityProviderConfigManager();
        checkClientCertManager();
        checkAuthorizer();
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

    private void checkAuthorizer() {
         if (authorizer == null) {
             throw new IllegalArgumentException("Authorizer is required");
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

    private boolean isSameUser(Subject s1, Subject s2) {
        boolean same = false;

        if (s1 != null && s2 != null) {
            User u1 = getUser(s1);
            User u2 = getUser(s2);

            if (u1 != null && u2 != null) {
                if (u1.getProviderId() == u2.getProviderId() &&
                    u1.getLogin().equals(u2.getLogin())) {
                    same = true;
                }
            }
        }

        return same;
    }

    private User getUser(Subject subject) {
        User user = null;

        for (Principal principal : subject.getPrincipals()) {
            if (principal instanceof User) {
                user = (User) principal;
                break;
            }
        }

        return user;
    }

    private IdentityProviderConfigManager identityProviderConfigManager = null;
    private IdentityProviderFactory identityProviderFactory = null;
    private Authorizer authorizer = null;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final LdapConfigTemplateManager ldapTemplateManager = new LdapConfigTemplateManager();

}
