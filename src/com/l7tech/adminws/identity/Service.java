package com.l7tech.adminws.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.*;
import com.l7tech.util.Locator;
import java.rmi.RemoteException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 26, 2003
 *
 * Admin WS for identities (provider configs, users, groups)
 */
public class Service {

    public static final String VERSION = "20030529";

    public Service() {
    }
    /**
     * Returns a version string. This can be compared to version on client-side.
     * @return value to be compared with the client side value of Service.VERSION;
     * @throws java.rmi.RemoteException
     */
    public String echoVersion() throws java.rmi.RemoteException {
        return VERSION;
    }
    /**
     *
     * @return Array of entity headers for all existing id provider config
     * @throws java.rmi.RemoteException
     */
    public com.l7tech.objectmodel.EntityHeader[] findAllIdentityProviderConfig() throws java.rmi.RemoteException {
        try {
            java.util.Collection res = getIdentityProviderConfigManagerAndBeginTransaction().findAllHeaders();
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("FindException in findAllIdentityProviderConfig", e);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new RemoteException("Exception in findAllIdentityProviderConfig", e);
        } finally {
            endTransaction();
        }
    }
    /**
     *
     * @return Array of entity headers for all existing id provider config
     * @throws java.rmi.RemoteException
     */
    public com.l7tech.objectmodel.EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        try {
            java.util.Collection res = getIdentityProviderConfigManagerAndBeginTransaction().findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("FindException in findAllIdentityProviderConfigByOffset", e);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new RemoteException("Exception in findAllIdentityProviderConfigByOffset", e);
        } finally {
            endTransaction();
        }
    }
    /**
     *
     * @return An identity provider config object
     * @throws java.rmi.RemoteException
     */
    public com.l7tech.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException {
        try {
            return getIdentityProviderConfigManagerAndBeginTransaction().findByPrimaryKey(oid);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("FindException in findIdentityProviderConfigByPrimaryKey", e);
        } finally {
            endTransaction();
        }
    }
    public long saveIdentityProviderConfig(IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException {
        try {
            if (identityProviderConfig.getOid() > 0) {
                IdentityProviderConfigManager manager = getIdentityProviderConfigManagerAndBeginTransaction();
                IdentityProviderConfig originalConfig = manager.findByPrimaryKey(identityProviderConfig.getOid());
                originalConfig.copyFrom(identityProviderConfig);
                manager.update(originalConfig);
                return identityProviderConfig.getOid();
            }
            return getIdentityProviderConfigManagerAndBeginTransaction().save(identityProviderConfig);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("Exception in saveIdentityProviderConfig", e);
        } finally {
            endTransaction();
        }
    }
    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException {
        try {
            IdentityProviderConfigManager manager = getIdentityProviderConfigManagerAndBeginTransaction();
            manager.delete(manager.findByPrimaryKey(oid));
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("FindException in deleteIdentityProviderConfig", e);
        }
        catch (DeleteException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("DeleteException in deleteIdentityProviderConfig", e);
        } finally {
            endTransaction();
        }
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException {
        try {
            UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
            java.util.Collection res = userManager.findAllHeaders();
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("FindException in findAllUsers", e);
        } finally {
            endTransaction();
        }
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        try {
            UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
            java.util.Collection res = userManager.findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("FindException in findAllUsers", e);
        } finally {
            endTransaction();
        }
    }
    public com.l7tech.identity.User findUserByPrimaryKey(long identityProviderConfigId, String userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            return userManager.findByPrimaryKey(userId);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("FindException in findUserByPrimaryKey", e);
        } finally {
            endTransaction();
        }
    }
    public void deleteUser(long identityProviderConfigId, String userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            com.l7tech.identity.User user = userManager.findByPrimaryKey(userId);
            userManager.delete(user);
        } catch (DeleteException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("DeleteException in deleteUser", e);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("FindException in deleteUser", e);
        } finally {
            endTransaction();
        }
    }
    public long saveUser(long identityProviderConfigId, com.l7tech.identity.User user) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            if (user.getOid() > 0) {
                User originalUser = userManager.findByPrimaryKey(Long.toString(user.getOid()));
                originalUser.copyFrom(user);
                userManager.update(originalUser);
                return user.getOid();
            }
            return userManager.save(user);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("Exception in saveUser", e);
        } finally {
            endTransaction();
        }
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException {
        try {
            java.util.Collection res = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId).findAllHeaders();
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("FindException in findAllGroups", e);
        } finally {
            endTransaction();
        }
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        try {
            java.util.Collection res = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId).findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("FindException in findAllGroups", e);
        } finally {
            endTransaction();
        }
    }
    public com.l7tech.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, String groupId) throws java.rmi.RemoteException {
        try {
            return retrieveGroupManagerAndBeginTransaction(identityProviderConfigId).findByPrimaryKey(groupId);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("FindException in findGroupByPrimaryKey", e);
        } finally {
            endTransaction();
        }
    }
    public void deleteGroup(long identityProviderConfigId, String groupId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId);
        try {
            com.l7tech.identity.Group grp = groupManager.findByPrimaryKey(groupId);
            if (grp == null) throw new java.rmi.RemoteException("Group does not exist");
            groupManager.delete(grp);
        } catch (ObjectModelException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("ObjectModelException in deleteGroup", e);
        } finally {
            endTransaction();
        }
    }
    public long saveGroup(long identityProviderConfigId, com.l7tech.identity.Group group) throws java.rmi.RemoteException {
        try {
            GroupManager groupManager = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId);
            if (group.getOid() > 0) {
                // patch to fix hibernate update issue
                Group originalGroup = groupManager.findByPrimaryKey(Long.toString(group.getOid()));
                originalGroup.copyFrom(group);
                groupManager.update(originalGroup);
                // end of patch
                //groupManager.update(group);
                return group.getOid();
            }
            return groupManager.save(group);
        } catch (SaveException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("SaveException in saveGroup", e);
        } catch (UpdateException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("UpdateException in saveGroup", e);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("FindException in saveGroup", e);
        } finally {
            endTransaction();
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private IdentityProviderConfigManager getIdentityProviderConfigManagerAndBeginTransaction() throws java.rmi.RemoteException {
        if (identityProviderConfigManager == null){
            initialiseConfigManager();
        }
        try {
            PersistenceContext.getCurrent().beginTransaction();
        }
        catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("SQLException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("TransactionException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        }
        return identityProviderConfigManager;
    }

    private void endTransaction() throws java.rmi.RemoteException {
        try {
            PersistenceContext.getCurrent().commitTransaction();
        } catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("Exception commiting", e);
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("Exception commiting", e);
        }
    }

    private UserManager retrieveUserManagerAndBeginTransaction(long identityProviderConfigId) throws java.rmi.RemoteException {
        UserManager ret = null;
        try {
            IdentityProvider provider = IdentityProviderFactory.makeProvider(getIdentityProviderConfigManagerAndBeginTransaction().findByPrimaryKey(identityProviderConfigId));
            ret = provider.getUserManager();
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("RemoteException in retrieveUserManager", e);
        }
        return ret;
    }

    private GroupManager retrieveGroupManagerAndBeginTransaction(long identityProviderConfigId) throws java.rmi.RemoteException {
        GroupManager ret = null;
        try {
            IdentityProvider provider = IdentityProviderFactory.makeProvider(getIdentityProviderConfigManagerAndBeginTransaction().findByPrimaryKey(identityProviderConfigId));
            ret = provider.getGroupManager();
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("RemoteException in retrieveGroupManager", e);
        }
        return ret;
    }

    private synchronized void initialiseConfigManager() throws java.rmi.RemoteException {
        try {
            // get the config manager implementation
            identityProviderConfigManager = (com.l7tech.identity.IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
            if (identityProviderConfigManager == null) throw new java.rmi.RemoteException("Cannot instantiate the IdentityProviderConfigManager");
        } catch (ClassCastException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("ClassCastException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup", e);
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("RuntimeException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        }
    }

    private com.l7tech.objectmodel.EntityHeader[] collectionToHeaderArray(java.util.Collection input) throws java.rmi.RemoteException {
        if (input == null) return new com.l7tech.objectmodel.EntityHeader[0];
        com.l7tech.objectmodel.EntityHeader[] output = new com.l7tech.objectmodel.EntityHeader[input.size()];
        int count = 0;
        java.util.Iterator i = input.iterator();
        while (i.hasNext()) {
            try {
                output[count] = (com.l7tech.objectmodel.EntityHeader)i.next();
            } catch (ClassCastException e) {
                e.printStackTrace(System.err);
                throw new java.rmi.RemoteException("Collection contained something other than a com.l7tech.objectmodel.EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

    IdentityProviderConfigManager identityProviderConfigManager = null;
}
