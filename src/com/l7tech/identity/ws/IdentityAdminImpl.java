package com.l7tech.identity.ws;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUserManagerServer;
import com.l7tech.common.util.Locator;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.logging.LogManager;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

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
            Collection res = getIdentityProviderConfigManagerAndBeginTransaction().findAllHeaders();
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            endTransaction();
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
            Collection res = getIdentityProviderConfigManagerAndBeginTransaction().findAllHeaders(offset, windowSize);
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            endTransaction();
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
            return getIdentityProviderConfigManagerAndBeginTransaction().findByPrimaryKey(oid);
        } finally {
            endTransaction();
        }
    }
    public long saveIdentityProviderConfig(IdentityProviderConfig identityProviderConfig)
                                        throws RemoteException, SaveException, UpdateException {
        try {
            if (identityProviderConfig.getOid() > 0) {
                IdentityProviderConfigManager manager = getIdentityProviderConfigManagerAndBeginTransaction();
                IdentityProviderConfig originalConfig = manager.findByPrimaryKey(identityProviderConfig.getOid());
                originalConfig.copyFrom(identityProviderConfig);
                manager.update(originalConfig);
                logger.info("Updated IDProviderConfig: " + identityProviderConfig.getOid());
                return identityProviderConfig.getOid();
            }
            logger.info("Saving IDProviderConfig: " + identityProviderConfig.getOid());
            return getIdentityProviderConfigManagerAndBeginTransaction().save(identityProviderConfig);
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new UpdateException("This object cannot be found (it no longer exist?).", e);
        } finally {
            endTransaction();
        }
    }
    public void deleteIdentityProviderConfig(long oid) throws RemoteException, DeleteException {
        try {
            IdentityProviderConfigManager manager = getIdentityProviderConfigManagerAndBeginTransaction();
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
            UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
            Collection res = userManager.findAllHeaders();
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            endTransaction();
        }
    }
    public EntityHeader[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize)
            throws RemoteException, FindException {
        try {
            UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
            Collection res = userManager.findAllHeaders(offset, windowSize);
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            endTransaction();
        }
    }

    public EntityHeader[] searchIdentities(long identityProviderConfigId, EntityType[] types, String pattern)
                                throws RemoteException, FindException {
        try {
            IdentityProviderConfig cfg = getIdentityProviderConfigManagerAndBeginTransaction().
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
            endTransaction();
        }
    }

    public User findUserByPrimaryKey(long identityProviderConfigId, String userId)
                                throws RemoteException, FindException {
        try {
            IdentityProviderConfig cfg = getIdentityProviderConfigManagerAndBeginTransaction().
                                            findByPrimaryKey(identityProviderConfigId);
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
            endTransaction();
        }
    }
    public void deleteUser(long identityProviderConfigId, String userId)
                                    throws RemoteException, DeleteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        if (userManager == null) throw new RemoteException("Cannot retrieve the UserManager");
        try {
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

    public long saveUser(long identityProviderConfigId, User user)
                                    throws RemoteException, SaveException, UpdateException {
        try {
            IdentityProviderConfig cfg = getIdentityProviderConfigManagerAndBeginTransaction().
                                            findByPrimaryKey(identityProviderConfigId);
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


    public EntityHeader[] findAllGroups(long identityProviderConfigId) throws RemoteException, FindException {
        try {
            Collection res = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId).
                                findAllHeaders();
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            endTransaction();
        }
    }

    public EntityHeader[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize)
                                throws RemoteException, FindException {
        try {
            Collection res = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId).
                                findAllHeaders(offset, windowSize);
            return (EntityHeader[])res.toArray(new EntityHeader[]{});
        } finally {
            endTransaction();
        }
    }
    public Group findGroupByPrimaryKey(long identityProviderConfigId, String groupId)
                                throws RemoteException, FindException {
        try {
            IdentityProviderConfig cfg = getIdentityProviderConfigManagerAndBeginTransaction().
                                            findByPrimaryKey(identityProviderConfigId);
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
            endTransaction();
        }
    }
    public void deleteGroup(long identityProviderConfigId, String groupId)
                                throws RemoteException, DeleteException {
        GroupManager groupManager = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId);
        try {
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
        try {
            IdentityProviderConfig cfg = getIdentityProviderConfigManagerAndBeginTransaction().
                                            findByPrimaryKey(identityProviderConfigId);
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

    public String getUserCert(long identityProviderConfigId, String userId)
                                throws RemoteException, FindException, CertificateEncodingException {
        // currently, this is only supported in the internal user manager
        // therefore, let this cast throw if it does
        InternalUserManagerServer userManager =
                (InternalUserManagerServer)retrieveUserManagerAndBeginTransaction(identityProviderConfigId);

        Certificate cert = userManager.retrieveUserCert(userId);
        if (cert == null) return null;
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        String encodedcert = encoder.encode(cert.getEncoded());
        return encodedcert;
    }

    public void revokeCert(long identityProviderConfigId, String userId) throws RemoteException, UpdateException {
        // currently, this is only supported in the internal user manager
        // therefore, let this cast throw if it does
        InternalUserManagerServer userManager =
                (InternalUserManagerServer)retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        userManager.revokeCert(userId);
    }

    public void testIdProviderConfig(IdentityProviderConfig identityProviderConfig)
                                throws RemoteException, InvalidIdProviderCfgException {
        try {
            getIdentityProviderConfigManagerAndBeginTransaction().test(identityProviderConfig);
        } finally {
            endTransaction();
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private IdentityProviderConfigManager getIdentityProviderConfigManagerAndBeginTransaction()
            throws RemoteException {
        if (identityProviderConfigManager == null){
            initialiseConfigManager();
        }
        try {
            PersistenceContext.getCurrent().beginTransaction();
        }
        catch (java.sql.SQLException e) {
            String msg = "SQLException in IdentitiesSoapBindingImpl.initialiseConfigManager from" +
                            " Locator.getDefault().lookup:";
            logger.log(Level.SEVERE, msg, e);
            throw new RemoteException(msg + " " + e.getMessage(), e);
        } catch (TransactionException e) {
            String msg = "TransactionException in IdentitiesSoapBindingImpl.initialiseConfigManager" +
                            " from Locator.getDefault().lookup:";
            logger.log(Level.SEVERE, msg, e);
            throw new RemoteException(msg + " " + e.getMessage(), e);
        }
        return identityProviderConfigManager;
    }

    private void endTransaction() throws RemoteException {
        try {
            PersistenceContext context = PersistenceContext.getCurrent();
            context.commitTransaction();
            context.close();
        } catch (java.sql.SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("Exception commiting", e);
        } catch ( ObjectModelException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("Exception commiting", e);
        }
    }

    private UserManager retrieveUserManagerAndBeginTransaction(long identityProviderConfigId)
            throws RemoteException {
        UserManager ret = null;
        try {
            IdentityProvider provider =
                    IdentityProviderFactory.
                        makeProvider(getIdentityProviderConfigManagerAndBeginTransaction().
                            findByPrimaryKey(identityProviderConfigId));
            ret = provider.getUserManager();
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RemoteException("RemoteException in retrieveUserManager", e);
        }
        return ret;
    }

    private GroupManager retrieveGroupManagerAndBeginTransaction(long identityProviderConfigId)
            throws RemoteException {
        GroupManager ret = null;
        try {
            IdentityProvider provider =
                    IdentityProviderFactory.
                        makeProvider(getIdentityProviderConfigManagerAndBeginTransaction().
                            findByPrimaryKey(identityProviderConfigId));
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
