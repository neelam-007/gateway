package com.l7tech.server.identity;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the IdentityAdmin interface.
 * This was originally used with the Axis layer
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: May 26, 2003
 */
public class IdentityAdminImpl extends RemoteService implements IdentityAdmin {
    public IdentityAdminImpl( String[] options, LifeCycle lifeCycle ) throws ConfigurationException, IOException {
        super( options, lifeCycle );
    }

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
    public IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid)
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

    public void deleteIdentityProviderConfig(long oid) throws RemoteException, DeleteException {
        beginTransaction();
        try {
            IdentityProviderConfigManager manager = getIdProvCfgMan();
            manager.delete(manager.findByPrimaryKey(oid));
            logger.info("Deleted IDProviderConfig: " + oid);
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
            IdentityProvider provider = IdentityProviderFactory.getProvider(identityProviderConfigId);
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");
            if (types != null) {
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

    public User findUserByPrimaryKey(long identityProviderConfigId, String userId)
      throws RemoteException, FindException {
        try {
            IdentityProvider provider = IdentityProviderFactory.getProvider(identityProviderConfigId);
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();

            return userManager.findByPrimaryKey(userId);
            // Groups are separate now
        } finally {
            closeContext();
        }
    }

    public User findUserByLogin( long idProvCfgId, String login ) throws RemoteException, FindException {
        try {
            IdentityProvider provider = IdentityProviderFactory.getProvider(idProvCfgId);
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");
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
            UserManager userManager = retrieveUserManager(cfgid);
            if (userManager == null) throw new RemoteException("Cannot retrieve the UserManager");
            User user = userManager.findByPrimaryKey(userId);
            if (user == null) {
                throw new ObjectNotFoundException(" User "+userId);
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
            IdentityProvider provider = IdentityProviderFactory.getProvider(identityProviderConfigId);
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");
            UserManager userManager = provider.getUserManager();
            user.setProviderId(identityProviderConfigId);

            String id = user.getUniqueIdentifier();
            if (id == null) {
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

    public Group findGroupByPrimaryKey(long cfgid, String groupId) throws RemoteException, FindException {
        try {
            IdentityProvider provider = IdentityProviderFactory.getProvider(cfgid);
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");
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
            IdentityProvider provider = IdentityProviderFactory.getProvider(identityProviderConfigId);
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");
            GroupManager groupManager = provider.getGroupManager();
            group.setProviderId(identityProviderConfigId);

            String id = group.getUniqueIdentifier();
            if (id == null) {
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
            ClientCertManager manager = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);
            Certificate cert = manager.getUserCert(user);
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
            // revoke the cert in internal CA
            ClientCertManager manager = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);
            manager.revokeUserCert(user);
            // internal users should have their password "revoked" along with their cert

            IdentityProvider provider = IdentityProviderFactory.getProvider(user.getProviderId());
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");

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
            // revoke the cert in internal CA
            ClientCertManager manager = (ClientCertManager) Locator.getDefault().lookup(ClientCertManager.class);
            manager.recordNewUserCert(user, cert);
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
        } catch ( InvalidIdProviderCfgException e ) {
            throw e;
        } catch ( RemoteException e ) {
            throw e;
        } catch ( Throwable t ) {
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

    // ************************************************
    // PRIVATES
    // ************************************************

    private IdentityProviderConfigManager getIdProvCfgMan() throws RemoteException {
        if (identityProviderConfigManager == null) {
            initialiseConfigManager();
        }
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

    private void endTransaction() throws TransactionException{
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

    private void rollbackTransaction() throws TransactionException{
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
            IdentityProvider provider = IdentityProviderFactory.getProvider(cfgid);
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");
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
            IdentityProvider provider = IdentityProviderFactory.getProvider(cfgid);
            if ( provider == null ) throw new FindException("IdentityProvider could not be found");
            ret = provider.getGroupManager();
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("RemoteException in retrieveGroupManager", e);
        }
        return ret;
    }

    private synchronized void initialiseConfigManager() throws RemoteException {
        try {
            // get the config manager implementation
            identityProviderConfigManager = (IdentityProviderConfigManager)Locator.
              getDefault().
              lookup(IdentityProviderConfigManager.class);
            if (identityProviderConfigManager == null) {
                throw new RemoteException("Cannot instantiate the IdentityProviderConfigManager");
            }
        } catch (ClassCastException e) {
            String msg = "ClassCastException in IdentitiesSoapBindingImpl.initialiseConfigManager " +
              "from Locator.getDefault().lookup";
            logger.log(Level.SEVERE, msg, e);
            throw new RemoteException(msg, e);
        } catch (RuntimeException e) {
            String msg = "RuntimeException in IdentitiesSoapBindingImpl.initialiseConfigManager" +
              " from Locator.getDefault().lookup: ";
            logger.log(Level.SEVERE, msg, e);
            throw new RemoteException(msg + e.getMessage(), e);
        }
    }

    IdentityProviderConfigManager identityProviderConfigManager = null;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final LdapConfigTemplateManager ldapTemplateManager = new LdapConfigTemplateManager();
}
