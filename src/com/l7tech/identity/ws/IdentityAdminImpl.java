package com.l7tech.identity.ws;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.common.util.Locator;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.logging.LogManager;
import com.l7tech.server.SessionManager;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.sql.SQLException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 26, 2003
 *
 * Server side implementation of the IdentityAdmin interface
 * This was originally used with the Axis layer
 */
public class IdentityAdminImpl implements IdentityAdmin {

    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/identityAdmin";

    public IdentityAdminImpl() {
        logger = LogManager.getInstance().getSystemLogger();
    }
    /**
     * Returns a version string. This can be compared to version on client-side.
     * @return value to be compared with the client side value of Service.VERSION;
     * @throws RemoteException
     */
    public String echoVersion() throws RemoteException {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }
    /**
     *
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
     *
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
     *
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
            endTransaction();
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
            endTransaction();
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
            IdentityProviderConfig cfg = getIdProvCfgMan().
                                            findByPrimaryKey(identityProviderConfigId);
            IdentityProvider provider = IdentityProviderFactory.makeProvider(cfg);
            if (types != null) {
                for (int i = 0; i < types.length; i++) {
                    types[i] = EntityType.fromValue(types[i].getVal());
                }
            }
            Collection searchResults = provider.search(types, pattern);
            return (EntityHeader[])searchResults.toArray(new EntityHeader[]{});
        } finally {
            closeContext();
        }
    }

    public User findUserByPrimaryKey(long identityProviderConfigId, String userId)
                                throws RemoteException, FindException {
        try {
            IdentityProviderConfig cfg = getIdProvCfgMan().findByPrimaryKey(identityProviderConfigId);
            IdentityProvider provider = IdentityProviderFactory.makeProvider(cfg);
            GroupManager groupManager = provider.getGroupManager();
            UserManager userManager = provider.getUserManager();

            User u = userManager.findByPrimaryKey(userId);
            User output = new User();
            output.copyFrom(u);
            Set groups = u.getGroups();
            // switch groups to group headers
            if (!groups.isEmpty()) {
                Set groupHeaders = new HashSet();
                Group g;
                EntityHeader gh;
                for (Iterator i = groups.iterator(); i.hasNext();) {
                    g = (Group)i.next();
                    gh = groupManager.groupToHeader(g);
                    groupHeaders.add( gh );
                }

                output.setGroupHeaders( groupHeaders );
                output.setGroups(new HashSet());
            }
            return output;
        } finally {
            closeContext();
        }
    }
    public void deleteUser(long cfgid, String userId) throws RemoteException, DeleteException {
        beginTransaction();
        try {
            UserManager userManager = retrieveUserManager(cfgid);
            if (userManager == null) throw new RemoteException("Cannot retrieve the UserManager");
            User user = userManager.findByPrimaryKey(userId);
            userManager.delete(user);
            logger.info("Deleted User: " + user.getName());
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        } finally {
            endTransaction();
        }
    }

    public long saveUser(long cfgid, User user) throws RemoteException, SaveException, UpdateException {
        beginTransaction();
        try {
            IdentityProviderConfig cfg = getIdProvCfgMan().findByPrimaryKey(cfgid);
            IdentityProvider provider = IdentityProviderFactory.makeProvider(cfg);
            GroupManager groupManager = provider.getGroupManager();
            UserManager userManager = provider.getUserManager();

            // transfer group header into groups
            Set groupHeaders = user.getGroupHeaders();
            Set groups = new HashSet();
            if (groupHeaders != null && groupHeaders.size() > 0) {
                for (Iterator i = groupHeaders.iterator(); i.hasNext();) {
                    EntityHeader header = (EntityHeader)i.next();
                    Group grp = groupManager.headerToGroup(header);
                    groups.add(grp);
                }
            }
            user.setGroups(groups);

            if (user.getOid() > 0) {
                userManager.update(user);
                logger.info("Updated User: " + user.getName() + "[" + user.getOid() + "]");
                return user.getOid();
            }
            logger.info("Saving User: " + user.getName());
            return userManager.save(user);
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("Exception in saveUser", e);
        } finally {
            endTransaction();
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
            IdentityProviderConfig cfg = getIdProvCfgMan().findByPrimaryKey(cfgid);
            IdentityProvider provider = IdentityProviderFactory.makeProvider(cfg);
            GroupManager groupManager = provider.getGroupManager();
            UserManager userManager = provider.getUserManager();
            Group existingGroup = groupManager.findByPrimaryKey(groupId);
            Group output = new Group();
            output.copyFrom(existingGroup);
            // transfer members into member headers
            Set members = existingGroup.getMembers();
            if (members != null && members.size() > 0) {
                Set memberHeaders = new HashSet();
                for (Iterator i = members.iterator(); i.hasNext();) {
                    User usr = (User)i.next();
                    EntityHeader header = userManager.userToHeader(usr);
                    memberHeaders.add(header);
                }
                output.setMemberHeaders(memberHeaders);
                output.setMembers(new HashSet());
            }
            return output;
        } finally {
            closeContext();
        }
    }
    public void deleteGroup(long cfgid, String groupId) throws RemoteException, DeleteException {
        beginTransaction();
        try {
            GroupManager groupManager = retrieveGroupManager(cfgid);
            Group grp = groupManager.findByPrimaryKey(groupId);
            if (grp == null) throw new RemoteException("Group does not exist");
            groupManager.delete(grp);
            logger.info("Deleted Group: " + grp.getName());
        } catch(FindException e) {
            throw new DeleteException("This object cannot be found (it no longer exist?).", e);
        } finally {
            endTransaction();
        }
    }
    public long saveGroup(long identityProviderConfigId, Group group)
                                throws RemoteException, SaveException, UpdateException {
        beginTransaction();
        try {
            IdentityProviderConfig cfg = getIdProvCfgMan().findByPrimaryKey(identityProviderConfigId);
            IdentityProvider provider = IdentityProviderFactory.makeProvider(cfg);
            GroupManager groupManager = provider.getGroupManager();
            UserManager userManager = provider.getUserManager();

            // transfer member headers into members
            Set memberHeaders = group.getMemberHeaders();
            Set members = new HashSet();
            if (memberHeaders != null) {
                for (Iterator i = memberHeaders.iterator(); i.hasNext();) {
                    EntityHeader header = (EntityHeader)i.next();
                    User usr = userManager.headerToUser(header);
                    members.add(usr);
                }
            }
            group.setMembers(members);

            if (group.getOid() > 0) {
                groupManager.update(group);
                logger.info("Updated Group: " + group.getName() + "[" + group.getOid() + "]");
                return group.getOid();
            }
            logger.info("Saving Group: " + group.getName());
            return groupManager.save(group);
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("FindException in saveGroup", e);
        } finally {
            endTransaction();
        }
    }

    public String getUserCert(User user) throws RemoteException, FindException, CertificateEncodingException {
        try {
            // get cert from internal CA
            ClientCertManager manager = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);
            Certificate cert = manager.getUserCert(user);
            if (cert == null) return null;
            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String encodedcert = encoder.encode(cert.getEncoded());
            return encodedcert;
        } finally {
            closeContext();
        }
    }

    public void revokeCert(User user) throws RemoteException, UpdateException {
        beginTransaction();
        try {
            // revoke the cert in internal CA
            ClientCertManager manager = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);
            manager.revokeUserCert(user);
            // internal users should have their password "revoked" along with their cert

            IdentityProviderConfig cfg = getIdProvCfgMan().findByPrimaryKey(user.getProviderId());
            if (cfg.type().equals(IdentityProviderType.INTERNAL)) {
                logger.finest("Cert revoked - invalidating user's password.");
                // must change the password now
                IdentityProvider provider = IdentityProviderFactory.makeProvider(cfg);
                UserManager userManager = provider.getUserManager();
                User dbuser = userManager.findByLogin(user.getLogin());
                // maybe a new password is already provided?
                String newPasswd = null;
                if (!dbuser.getPassword().equals(user.getPassword())) {
                    newPasswd = user.getPassword();
                } else {
                    byte[] randomPasswd = new byte[32];
                    SessionManager.getInstance().getRand().nextBytes(randomPasswd);
                    newPasswd = new String(randomPasswd);
                }
                dbuser.setPassword(newPasswd);
                userManager.update(dbuser);
            }
        } catch (FindException e) {
            throw new UpdateException("error resetting user's password", e);
        } finally {
            endTransaction();
        }

    }

    public void testIdProviderConfig(IdentityProviderConfig identityProviderConfig)
                                throws RemoteException, InvalidIdProviderCfgException {
        try {
            getIdProvCfgMan().test(identityProviderConfig);
        } finally {
            closeContext();
        }
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

    private void endTransaction()  {
        try {
            PersistenceContext context = PersistenceContext.getCurrent();
            context.flush();
            context.close();
        } catch (java.sql.SQLException e) {
            logger.log(Level.SEVERE, "could not end transaction", e);
        } catch ( ObjectModelException e) {
            logger.log(Level.SEVERE, "could not end transaction", e);
        }
    }

    private UserManager retrieveUserManager(long cfgid)
            throws RemoteException {
        UserManager ret = null;
        try {
            IdentityProvider provider = IdentityProviderFactory.makeProvider(getIdProvCfgMan().
                                          findByPrimaryKey(cfgid));
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
            IdentityProvider provider = IdentityProviderFactory.makeProvider(getIdProvCfgMan().
                                          findByPrimaryKey(cfgid));
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
    private Logger logger = null;
}
