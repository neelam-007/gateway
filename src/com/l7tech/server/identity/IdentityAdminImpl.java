package com.l7tech.server.identity;

import com.l7tech.admin.RoleUtils;
import com.l7tech.common.Authorizer;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
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
public class IdentityAdminImpl extends ApplicationObjectSupport
  implements IdentityAdmin, InitializingBean {
    private ClientCertManager clientCertManager;

    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/identityAdmin";

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
        try {
            Collection res = getIdProvCfgMan().findAllHeaders();
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            closeContext();
        }
    }

    /**
     * @return Array of entity headers for all existing id provider config
     * @throws RemoteException
     */
    public EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize)
      throws RemoteException, FindException {
        try {
            Collection res = getIdProvCfgMan().findAllHeaders(offset, windowSize);
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            closeContext();
        }
    }

    /**
     * @return An identity provider config object
     * @throws RemoteException
     */
    public IdentityProviderConfig findIdentityProviderConfigByID(long oid)
      throws RemoteException, FindException {
        try {
            return getIdProvCfgMan().findByPrimaryKey(oid);
        } finally {
            closeContext();
        }
    }

    public long saveIdentityProviderConfig(IdentityProviderConfig identityProviderConfig)
      throws RemoteException, SaveException, UpdateException {
        beginTransaction();
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
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
        } finally {
            try {
                endTransaction();
            } catch (TransactionException e) {

                try {
                    rollbackTransaction();
                } catch (TransactionException e1) {
                    logger.log(Level.WARNING, "exception rolling back", e1);
                }

                throw new SaveException(e.getMessage());
            }
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
        beginTransaction();
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
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
        } finally {
            try {
                endTransaction();
            } catch (TransactionException e) {

                try {
                    rollbackTransaction();
                } catch (TransactionException e1) {
                    logger.log(Level.WARNING, "exception rolling back", e1);
                }

                throw new DeleteException(e.getMessage(), e);
            }
        }
    }

    public EntityHeader[] findAllUsers(long identityProviderConfigId) throws RemoteException, FindException {
        try {
            UserManager userManager = retrieveUserManager(identityProviderConfigId);
            Collection res = userManager.findAllHeaders();
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            closeContext();
        }
    }

    public EntityHeader[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize)
      throws RemoteException, FindException {
        try {
            UserManager userManager = retrieveUserManager(identityProviderConfigId);
            Collection res = userManager.findAllHeaders(offset, windowSize);
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            closeContext();
        }
    }

    public EntityHeader[] searchIdentities(long identityProviderConfigId, EntityType[] types, String pattern)
      throws RemoteException, FindException {
        try {
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            if (types != null) {
                // TODO is this necessary?   EntityType already has a readResolve()
                for (int i = 0; i < types.length; i++) {
                    types[i] = EntityType.fromValue(types[i].getVal());
                }
            }
            Collection searchResults = provider.search(types, pattern);
            if (searchResults == null) return new EntityHeader[0];
            return (EntityHeader[])searchResults.toArray(new EntityHeader[]{});
        } finally {
            closeContext();
        }
    }

    public User findUserByID(long identityProviderConfigId, String userId)
      throws RemoteException, FindException {
        try {
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();

            return userManager.findByPrimaryKey(userId);
            // Groups are separate now
        } finally {
            closeContext();
        }
    }

    public User findUserByLogin(long idProvCfgId, String login) throws RemoteException, FindException {
        try {
            IdentityProvider provider = identityProviderFactory.getProvider(idProvCfgId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();

            return userManager.findByLogin(login);
        } finally {
            closeContext();
        }
    }

    public void deleteUser(long cfgid, String userId)
      throws RemoteException, DeleteException, ObjectNotFoundException {
        beginTransaction();
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
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
        } finally {
            try {
                endTransaction();
            } catch (TransactionException e) {
                try {
                    rollbackTransaction();
                } catch (TransactionException e1) {
                    logger.log(Level.WARNING, "exception rolling back", e1);
                }

                throw new DeleteException(e.toString());
            }
        }
    }

    public String saveUser(long identityProviderConfigId, User user, Set groupHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        beginTransaction();
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();
            user.setProviderId(identityProviderConfigId);

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
        } finally {
            try {
                endTransaction();
            } catch (TransactionException e) {

                try {
                    rollbackTransaction();
                } catch (TransactionException e1) {
                    logger.log(Level.WARNING, "exception rolling back", e1);
                }

                throw new SaveException(e.toString());
            }
        }
    }


    public EntityHeader[] findAllGroups(long cfgid) throws RemoteException, FindException {
        try {
            Collection res = retrieveGroupManager(cfgid).findAllHeaders();
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            closeContext();
        }
    }

    public EntityHeader[] findAllGroupsByOffset(long cfgid, int offset, int windowSize)
      throws RemoteException, FindException {
        try {
            Collection res = retrieveGroupManager(cfgid).findAllHeaders(offset, windowSize);
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            closeContext();
        }
    }

    public Group findGroupByID(long cfgid, String groupId) throws RemoteException, FindException {
        try {
            IdentityProvider provider = identityProviderFactory.getProvider(cfgid);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            GroupManager groupManager = provider.getGroupManager();

            return groupManager.findByPrimaryKey(groupId);
        } finally {
            closeContext();
        }
    }

    public void deleteGroup(long cfgid, String groupId)
      throws RemoteException, DeleteException, ObjectNotFoundException {
        beginTransaction();
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
            GroupManager groupManager = retrieveGroupManager(cfgid);
            Group grp = groupManager.findByPrimaryKey(groupId);
            if (grp == null) throw new ObjectNotFoundException("Group does not exist");
            groupManager.delete(grp);
            logger.info("Deleted Group: " + grp.getName());
        } catch (FindException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        } finally {
            try {
                endTransaction();
            } catch (TransactionException e) {
                try {
                    rollbackTransaction();
                } catch (TransactionException e1) {
                    logger.log(Level.WARNING, "exception rolling back", e1);
                }

                throw new DeleteException(e.toString());
            }
        }
    }

    public String saveGroup(long identityProviderConfigId, Group group, Set userHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        beginTransaction();
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
            IdentityProvider provider = identityProviderFactory.getProvider(identityProviderConfigId);
            if (provider == null) throw new FindException("IdentityProvider could not be found");
            GroupManager groupManager = provider.getGroupManager();
            group.setProviderId(identityProviderConfigId);

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
        } finally {
            try {
                endTransaction();
            } catch (TransactionException e) {
                try {
                    rollbackTransaction();
                } catch (TransactionException e1) {
                    logger.log(Level.WARNING, "exception rolling back", e1);
                }

                throw new SaveException(e.toString());
            }
        }
    }

    public String getUserCert(User user) throws RemoteException, FindException, CertificateEncodingException {
        try {
            // get cert from internal CA
            Certificate cert = clientCertManager.getUserCert(user);
            if (cert == null) return null;

            String encodedcert = HexUtils.encodeBase64(cert.getEncoded());
            return encodedcert;
        } finally {
            closeContext();
        }
    }

    public void revokeCert(User user) throws RemoteException, UpdateException, ObjectNotFoundException {
        beginTransaction();
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
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
                String newPasswd = null;
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
        } finally {
            try {
                endTransaction();
            } catch (TransactionException e) {
                throw new UpdateException(e.toString());
            }
        }

    }

    public void recordNewUserCert(User user, Certificate cert) throws RemoteException, UpdateException {
        beginTransaction();
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
            // revoke the cert in internal CA
            clientCertManager.recordNewUserCert(user, cert);
        } finally {
            try {
                endTransaction();
            } catch (TransactionException e) {
                throw new UpdateException(e.toString());
            }
        }
    }

    private static SecureRandom secureRandom = null;

    private synchronized SecureRandom getSecureRandom() {
        if (secureRandom != null) return secureRandom;
        return secureRandom = new SecureRandom();
    }

    public void testIdProviderConfig(IdentityProviderConfig identityProviderConfig)
      throws RemoteException, InvalidIdProviderCfgException {
        try {
            getIdProvCfgMan().test(identityProviderConfig);
        } catch (InvalidIdProviderCfgException e) {
            throw e;
        } catch (Throwable t) {
            logger.log(Level.INFO, "Identity Provider test failed because an exception was thrown", t);
            throw new InvalidIdProviderCfgException(t);
        } finally {
            closeContext();
        }
    }

    public Set getGroupHeaders(long providerId, String userId) throws RemoteException, FindException {
        return retrieveGroupManager(providerId).getGroupHeaders(userId);
    }

    public void setGroupHeaders(long providerId, String userId, Set groupHeaders) throws RemoteException, FindException, UpdateException {
        retrieveGroupManager(providerId).setGroupHeaders(userId, groupHeaders);
    }

    public Set getUserHeaders(long providerId, String groupId) throws RemoteException, FindException {
        return retrieveGroupManager(providerId).getUserHeaders(groupId);
    }

    public void setUserHeaders(long providerId, String groupId, Set groupHeaders) throws RemoteException, FindException, UpdateException {
        retrieveGroupManager(providerId).setUserHeaders(groupId, groupHeaders);
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        return ldapTemplateManager.getTemplates();
    }

    /**
     * Determine the roles for the given subject.
     *
     * @param subject the subject for which to get roles for
     * @return the <code>Set</code> of roles (groups) the subject is memeber of
     * @throws java.rmi.RemoteException on remote invocation error
     * @throws java.security.AccessControlException
     *                                  if the current subject is not allowed to perform the operation.
     *                                  The invocation tests whether the current subject  (the subject carrying out the operaton)
     *                                  has privileges to perform the operation. The operators are not allowed to perform this operation
     *                                  except for themselves.
     */
    public Set getRoles(Subject subject) throws RemoteException, AccessControlException {
        if (subject == null) {
            throw new IllegalArgumentException();
        }

        final Subject currentSubject = Subject.getSubject(AccessController.getContext());
        if (currentSubject == null) {
            return Collections.EMPTY_SET;
        }
        if (authorizer.isSubjectInRole(currentSubject, new String[]{Group.ADMIN_GROUP_NAME})) {  //admin can ask everything
            return authorizer.getUserRoles(subject);
        } else if (authorizer.isSubjectInRole(currentSubject, new String[]{Group.OPERATOR_GROUP_NAME})) { // operator only self
            if (subject.equals(currentSubject)) {
                return authorizer.getUserRoles(subject);
            } else {
                throw new AccessControlException("Access denied, accessing subject " + subject);
            }
        }

        throw new AccessControlException("Access denied, accessing subject " + subject);
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

    public void afterPropertiesSet() throws Exception {
        checkidentityProviderFactory();
        checkidentityProviderConfigManager();
        checkClientCertManager();
        checkAuthorizer();
    }



    // ************************************************
    // PRIVATES
    // ************************************************

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

    private IdentityProviderConfigManager getIdProvCfgMan() {
        return identityProviderConfigManager;
    }

    private void beginTransaction() throws RemoteException {
        try {
            PersistenceContext.getCurrent().beginTransaction();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "exception begining transaction", e);
            throw new RemoteException("exception begining transaction", e);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "exception begining transaction", e);
            throw new RemoteException("exception begining transaction", e);
        }
    }

    private void closeContext() {
        try {
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error closing context", e);
        }
    }

    private void endTransaction() throws TransactionException {
        try {
            PersistenceContext context = PersistenceContext.getCurrent();
            //context.flush();
            context.commitTransaction();
            context.close();
        } catch (java.sql.SQLException e) {
            logger.log(Level.SEVERE, "could not end transaction", e);
            throw new TransactionException(e.getMessage());
        } catch (ObjectModelException e) {
            logger.log(Level.SEVERE, "could not end transaction", e);
            throw new TransactionException(e.getMessage());
        }
    }

    private void rollbackTransaction() throws TransactionException {
        try {
            PersistenceContext context = PersistenceContext.getCurrent();

            context.rollbackTransaction();
            context.close();
        } catch (java.sql.SQLException e) {
            logger.log(Level.SEVERE, "could not rollback transaction", e);
            throw new TransactionException(e.getMessage());
        } catch (ObjectModelException e) {
            logger.log(Level.SEVERE, "could not rollback transaction", e);
            throw new TransactionException(e.getMessage());
        }
    }

    private UserManager retrieveUserManager(long cfgid)
      throws RemoteException {
        UserManager ret = null;
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
        GroupManager ret = null;
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


    /**
     * Parse the String service ID to long (database format). Throws runtime exc
     *
     * @param ID the ID, must not be null, and .
     * @return the ID representing <code>long</code>
     * @throws IllegalArgumentException if service ID is null
     * @throws NumberFormatException    on parse error
     */
    private long toLong(String ID)
      throws IllegalArgumentException, NumberFormatException {
        if (ID == null) {
            throw new IllegalArgumentException();
        }
        return Long.parseLong(ID);
    }

    private IdentityProviderConfigManager identityProviderConfigManager = null;
    private IdentityProviderFactory identityProviderFactory = null;
    private Authorizer authorizer = null;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final LdapConfigTemplateManager ldapTemplateManager = new LdapConfigTemplateManager();
}
