package com.l7tech.adminws.identity;

import com.l7tech.adminws.translation.TypeTranslator;
import com.l7tech.identity.*;
import com.l7tech.util.Locator;
import com.l7tech.objectmodel.*;

import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.io.IOException;
import java.sql.SQLException;

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
        try {
            return TypeTranslator.collectionToServiceHeaders(getIdentityProviderConfigManager().findAllHeaders());
        } catch (FindException e) {
            throw new RemoteException("FindException in findAlllIdentityProviderConfig", e);
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        try {
            return TypeTranslator.collectionToServiceHeaders(getIdentityProviderConfigManager().findAllHeaders(offset, windowSize));
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllIdentityProviderConfigByOffset", e);
        }
    }

    public com.l7tech.adminws.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException {
        try {
            return TypeTranslator.genericToServiceIdProviderConfig(getIdentityProviderConfigManager().findByPrimaryKey(oid));
        } catch (FindException e) {
            throw new java.rmi.RemoteException("FindException in findIdentityProviderConfigByPrimaryKey", e);
        }
    }

    public long saveIdentityProviderConfig(com.l7tech.adminws.identity.IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException {
        try {
            if (identityProviderConfig.getOid() > 0) {
                getIdentityProviderConfigManager().update(TypeTranslator.serviceIdentityProviderConfigToGenericOne(identityProviderConfig));
                return identityProviderConfig.getOid();
            }
            return getIdentityProviderConfigManager().save(TypeTranslator.serviceIdentityProviderConfigToGenericOne(identityProviderConfig));
        } catch (SaveException e) {
            throw new java.rmi.RemoteException("SaveException in saveIdentityProviderConfig", e);
        }
        catch (UpdateException e) {
            throw new java.rmi.RemoteException("UpdateException in saveIdentityProviderConfig", e);
        }
    }

    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException {
        try {
            getIdentityProviderConfigManager().delete(getIdentityProviderConfigManager().findByPrimaryKey(oid));
        } catch (FindException e) {
            throw new java.rmi.RemoteException("FindException in deleteIdentityProviderConfig", e);
        }
        catch (DeleteException e) {
            throw new java.rmi.RemoteException("DeleteException in deleteIdentityProviderConfig", e);
        }
    }

    public com.l7tech.adminws.identity.User findUserByPrimaryKey(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            com.l7tech.identity.User user = userManager.findByPrimaryKey(userId);
            return TypeTranslator.genUserToServiceUser(user);
        } catch (FindException e) {
            throw new java.rmi.RemoteException("FindException in findUserByPrimaryKey", e);
        }
    }

    public void deleteUser(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            com.l7tech.identity.User user = userManager.findByPrimaryKey(userId);
            userManager.delete(user);
        } catch (DeleteException e) {
            throw new java.rmi.RemoteException("DeleteException in deleteUser", e);
        } catch (FindException e) {
            throw new java.rmi.RemoteException("FindException in deleteUser", e);
        }

    }

    public long saveUser(long identityProviderConfigId, com.l7tech.adminws.identity.User user) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            if (user.getOid() > 0) {
                userManager.update(TypeTranslator.serviceUserToGenUser(user));
                return user.getOid();
            }
            return userManager.save(TypeTranslator.serviceUserToGenUser(user));
        } catch (SaveException e) {
            throw new java.rmi.RemoteException("SaveException in saveUser", e);
        } catch (ClassNotFoundException e) {
            throw new java.rmi.RemoteException("ClassNotFoundException in TypeTranslator.serviceUserToGenUser", e);
        } catch (UpdateException e) {
            throw new java.rmi.RemoteException("UpdateException in saveUser", e);
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException {
        try {
            UserManager userManager = retrieveUserManager(identityProviderConfigId);
            return TypeTranslator.collectionToServiceHeaders(userManager.findAllHeaders());
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllUsers", e);
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManager(identityProviderConfigId);
        try {
            return TypeTranslator.collectionToServiceHeaders(userManager.findAllHeaders(offset, windowSize));
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllUsersByOffset", e);
        }
    }

    public com.l7tech.adminws.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
        try {
            return TypeTranslator.genGroupToServiceGroup(groupManager.findByPrimaryKey(groupId));
        } catch (FindException e) {
            throw new RemoteException("FindException in findGroupByPrimaryKey", e);
        }
    }

    public void deleteGroup(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
        try {
            com.l7tech.identity.Group grp = groupManager.findByPrimaryKey(groupId);
            if (grp == null) throw new java.rmi.RemoteException("Group does not exist");
            groupManager.delete(grp);
        } catch (ObjectModelException e) {
            throw new RemoteException("ObjectModelException in deleteGroup", e);
        }

    }

    public long saveGroup(long identityProviderConfigId, com.l7tech.adminws.identity.Group group) throws java.rmi.RemoteException {
        try {
            GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
            if (group.getOid() > 0) {
                groupManager.update(TypeTranslator.serviceGroupToGenGroup(group));
                return group.getOid();
            }
            return groupManager.save(TypeTranslator.serviceGroupToGenGroup(group));
        } catch (SaveException e) {
            throw new RemoteException("SaveException in saveGroup", e);
        } catch (ClassNotFoundException e) {
            throw new java.rmi.RemoteException("ClassNotFoundException in TypeTranslator.serviceGroupToGenGroup", e);
        } catch (UpdateException e) {
            throw new java.rmi.RemoteException("UpdateException in TypeTranslator.serviceGroupToGenGroup", e);
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
        try {
            return TypeTranslator.collectionToServiceHeaders(groupManager.findAllHeaders());
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllGroups", e);
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        try {
            GroupManager groupManager = retrieveGroupManager(identityProviderConfigId);
            return TypeTranslator.collectionToServiceHeaders(groupManager.findAllHeaders(offset, windowSize));
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllGroupsByOffset", e);
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private com.l7tech.identity.IdentityProviderConfigManager getIdentityProviderConfigManager() throws java.rmi.RemoteException {
        /*// todo fla, temporary fix using direcly the HibernatePersistenceManager
        if (identityProviderConfigManager == null){
            try {
                HibernatePersistenceManager.initialize();
                identityProviderConfigManager = new com.l7tech.identity.imp.IdentityProviderConfigManagerImp(com.l7tech.objectmodel.HibernatePersistenceManager.getContext());
            } catch (IOException e) {
                throw new RemoteException("IOException in IdentitiesSoapBindingImpl.getIdentityProviderConfigManager", e);
            } catch (SQLException e) {
                throw new RemoteException("SQLException in IdentitiesSoapBindingImpl.getIdentityProviderConfigManager", e);
            } catch (NamingException e) {
                throw new RemoteException("NamingException in IdentitiesSoapBindingImpl.getIdentityProviderConfigManager", e);
            } catch (NullPointerException e) {
                throw new RemoteException("NullPointerException in HibernatePersistenceManager", e);
            }
            if (identityProviderConfigManager == null) throw new java.rmi.RemoteException("Cannot instantiate the IdentityProviderConfigManager");
        }
        return identityProviderConfigManager;
        */
        if (identityProviderConfigManager == null){
            try {
                // instantiate the server-side manager
                identityProviderConfigManager = (com.l7tech.identity.IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
                if (identityProviderConfigManager == null) throw new java.rmi.RemoteException("Cannot instantiate the IdentityProviderConfigManager");
            } catch (ClassCastException e) {
                throw new RemoteException("ClassCastException in Locator.getDefault().lookup", e);
            } catch (RuntimeException e) {
                throw new RemoteException("RemoteException in Locator.getDefault().lookup: "+ e.getMessage(), e);
            }
        }
        return identityProviderConfigManager;
    }

    private UserManager retrieveUserManager(long identityProviderConfigId) throws java.rmi.RemoteException {
        if (userManagersMap == null) userManagersMap = new java.util.HashMap();
        UserManager ret = (UserManager)userManagersMap.get(new Long(identityProviderConfigId));
        if (ret != null) return ret;
        try {
            IdentityProvider provider = IdentityProviderFactory.makeProvider(identityProviderConfigManager.findByPrimaryKey(identityProviderConfigId));
            ret = provider.getUserManager();
            userManagersMap.put(new Long(identityProviderConfigId), ret);
            return ret;
        } catch (FindException e) {
            throw new RemoteException("RemoteException in retrieveUserManager", e);
        }
    }

    private GroupManager retrieveGroupManager(long identityProviderConfigId) throws java.rmi.RemoteException {
        if (groupManagersMap == null) groupManagersMap = new java.util.HashMap();
        GroupManager ret = (GroupManager)groupManagersMap.get(new Long(identityProviderConfigId));
        if (ret != null) return ret;
        try {
            IdentityProvider provider = IdentityProviderFactory.makeProvider(identityProviderConfigManager.findByPrimaryKey(identityProviderConfigId));
            ret = provider.getGroupManager();
            groupManagersMap.put(new Long(identityProviderConfigId), ret);
            return ret;
        } catch (FindException e) {
            throw new RemoteException("RemoteException in retrieveGroupManager", e);
        }
    }

    IdentityProviderConfigManager identityProviderConfigManager = null;
    java.util.HashMap userManagersMap = null;
    java.util.HashMap groupManagersMap = null;
}
