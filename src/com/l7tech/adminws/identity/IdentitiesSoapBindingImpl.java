package com.l7tech.adminws.identity;

import com.l7tech.adminws.translation.TypeTranslator;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;
import com.l7tech.util.Locator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.DeleteException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 13, 2003
 *
 * Implementation of the Identity admin web service.
 * This service is consumed by the console-side managers relating to identity management.
 */
public class IdentitiesSoapBindingImpl implements com.l7tech.adminws.identity.Identity{
    public com.l7tech.adminws.identity.Header[] findAlllIdentityProviderConfig() throws java.rmi.RemoteException {
        return TypeTranslator.collectionToServiceHeaders(getIdentityProviderConfigManager().findAllHeaders());
    }

    public com.l7tech.adminws.identity.Header[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        return TypeTranslator.collectionToServiceHeaders(getIdentityProviderConfigManager().findAllHeaders(offset, windowSize));
    }

    public com.l7tech.adminws.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException {
        try {
            return TypeTranslator.genericToServiceIdProviderConfig(getIdentityProviderConfigManager().findByPrimaryKey(oid));
        } catch (FindException e) {
            throw new java.rmi.RemoteException("IdentityProviderConfigManager FindException : " + e.getMessage());
        }
    }

    public long saveIdentityProviderConfig(com.l7tech.adminws.identity.IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException {
        try {
            return getIdentityProviderConfigManager().save(TypeTranslator.serviceIdentityProviderConfigToGenericOne(identityProviderConfig));
        } catch (SaveException e) {
            throw new java.rmi.RemoteException("IdentityProviderConfigManager SaveException : " + e.getMessage());
        }
    }

    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException {
        try {
            getIdentityProviderConfigManager().delete(getIdentityProviderConfigManager().findByPrimaryKey(oid));
        } catch (FindException e) {
            throw new java.rmi.RemoteException("IdentityProviderConfigManager FindException : " + e.getMessage());
        }
        catch (DeleteException e) {
            throw new java.rmi.RemoteException("IdentityProviderConfigManager DeleteException : " + e.getMessage());
        }
    }

    public com.l7tech.adminws.identity.User findUserByPrimaryKey(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            com.l7tech.identity.User user = userManager.findByPrimaryKey(userId);
            return TypeTranslator.genUserToServiceUser(user);
        } catch (FindException e) {
            throw new java.rmi.RemoteException("IdentityProviderConfigManager FindException : " + e.getMessage());
        }
    }

    public void deleteUser(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            com.l7tech.identity.User user = userManager.findByPrimaryKey(userId);
            userManager.delete(user);
        } catch (DeleteException e) {
            throw new java.rmi.RemoteException("IdentityProviderConfigManager DeleteException : " + e.getMessage());
        } catch (FindException e) {
            throw new java.rmi.RemoteException("IdentityProviderConfigManager FindException : " + e.getMessage());
        }

    }

    public long saveUser(long identityProviderConfigId, com.l7tech.adminws.identity.User user) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            return userManager.save(TypeTranslator.serviceUserToGenUser(user));
        } catch (SaveException e) {
            throw new java.rmi.RemoteException("IdentityProviderConfigManager SaveException : " + e.getMessage());
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        return TypeTranslator.collectionToServiceHeaders(userManager.findAllHeaders());
    }

    public com.l7tech.adminws.identity.Header[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        return TypeTranslator.collectionToServiceHeaders(userManager.findAllHeaders(offset, windowSize));
    }

    public com.l7tech.adminws.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
        return TypeTranslator.genGroupToServiceGroup(groupManager.findByPrimaryKey(groupId));
    }

    public void deleteGroup(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
        com.l7tech.identity.Group grp = groupManager.findByPrimaryKey(groupId);
        if (grp == null) throw new java.rmi.RemoteException("Group does not exist");
        groupManager.delete(grp);
    }

    public long saveGroup(long identityProviderConfigId, com.l7tech.adminws.identity.Group group) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
        return groupManager.save(TypeTranslator.serviceGroupToGenGroup(group));
    }

    public com.l7tech.adminws.identity.Header[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
        return TypeTranslator.collectionToServiceHeaders(groupManager.findAllHeaders());
    }

    public com.l7tech.adminws.identity.Header[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
        return TypeTranslator.collectionToServiceHeaders(groupManager.findAllHeaders(offset, windowSize));
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private IdentityProviderConfigManager getIdentityProviderConfigManager() {
        if (identityProviderConfigManager == null){
            // instantiate the server-side manager
            identityProviderConfigManager = (IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
        }
        return identityProviderConfigManager;
    }

    private UserManager retrieveUserManager(long identityProviderConfigId) throws java.rmi.RemoteException {
        throw new java.rmi.RemoteException("Cannot instantiate UserManager");
        // todo (it)
    }

    private GroupManager retrieveGroupManager(long identityProviderConfigId) throws java.rmi.RemoteException {
        throw new java.rmi.RemoteException("Cannot instantiate GroupManager");
        // todo (it)
    }

    IdentityProviderConfigManager identityProviderConfigManager = null;
}
