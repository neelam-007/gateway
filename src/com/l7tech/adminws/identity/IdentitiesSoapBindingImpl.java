package com.l7tech.adminws.identity;

import com.l7tech.adminws.translation.TypeTranslator;
import com.l7tech.identity.*;
import com.l7tech.util.Locator;
import com.l7tech.objectmodel.*;

import java.rmi.RemoteException;

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
            return TypeTranslator.collectionToServiceHeaders(getIdentityProviderConfigManagerAndBeginTransaction().findAllHeaders());
        } catch (FindException e) {
            throw new RemoteException("FindException in findAlllIdentityProviderConfig", e);
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        try {
            return TypeTranslator.collectionToServiceHeaders(getIdentityProviderConfigManagerAndBeginTransaction().findAllHeaders(offset, windowSize));
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllIdentityProviderConfigByOffset", e);
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.adminws.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException {
        try {
            return TypeTranslator.genericToServiceIdProviderConfig(getIdentityProviderConfigManagerAndBeginTransaction().findByPrimaryKey(oid));
        } catch (FindException e) {
            throw new java.rmi.RemoteException("FindException in findIdentityProviderConfigByPrimaryKey", e);
        } finally {
            endTransaction();
        }
    }

    public long saveIdentityProviderConfig(com.l7tech.adminws.identity.IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException {
        try {
            if (identityProviderConfig.getOid() > 0) {
                getIdentityProviderConfigManagerAndBeginTransaction().update(TypeTranslator.serviceIdentityProviderConfigToGenericOne(identityProviderConfig));
                return identityProviderConfig.getOid();
            }
            return getIdentityProviderConfigManagerAndBeginTransaction().save(TypeTranslator.serviceIdentityProviderConfigToGenericOne(identityProviderConfig));
        } catch (SaveException e) {
            throw new java.rmi.RemoteException("SaveException in saveIdentityProviderConfig", e);
        }
        catch (UpdateException e) {
            throw new java.rmi.RemoteException("UpdateException in saveIdentityProviderConfig", e);
        } finally {
            endTransaction();
        }
    }

    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException {
        try {
            IdentityProviderConfigManager manager = getIdentityProviderConfigManagerAndBeginTransaction();
            manager.delete(manager.findByPrimaryKey(oid));
        } catch (FindException e) {
            throw new java.rmi.RemoteException("FindException in deleteIdentityProviderConfig", e);
        }
        catch (DeleteException e) {
            throw new java.rmi.RemoteException("DeleteException in deleteIdentityProviderConfig", e);
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.adminws.identity.User findUserByPrimaryKey(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            com.l7tech.identity.User user = userManager.findByPrimaryKey(userId);
            return TypeTranslator.genUserToServiceUser(user);
        } catch (FindException e) {
            throw new java.rmi.RemoteException("FindException in findUserByPrimaryKey", e);
        } finally {
            endTransaction();
        }
    }

    public void deleteUser(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            com.l7tech.identity.User user = userManager.findByPrimaryKey(userId);
            userManager.delete(user);
        } catch (DeleteException e) {
            throw new java.rmi.RemoteException("DeleteException in deleteUser", e);
        } catch (FindException e) {
            throw new java.rmi.RemoteException("FindException in deleteUser", e);
        } finally {
            endTransaction();
        }
    }

    public long saveUser(long identityProviderConfigId, com.l7tech.adminws.identity.User user) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
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
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException {
        try {
            UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
            return TypeTranslator.collectionToServiceHeaders(userManager.findAllHeaders());
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllUsers", e);
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        try {
            return TypeTranslator.collectionToServiceHeaders(userManager.findAllHeaders(offset, windowSize));
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllUsersByOffset", e);
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.adminws.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId);
        try {
            return TypeTranslator.genGroupToServiceGroup(groupManager.findByPrimaryKey(groupId));
        } catch (FindException e) {
            throw new RemoteException("FindException in findGroupByPrimaryKey", e);
        } finally {
            endTransaction();
        }
    }

    public void deleteGroup(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId);
        try {
            com.l7tech.identity.Group grp = groupManager.findByPrimaryKey(groupId);
            if (grp == null) throw new java.rmi.RemoteException("Group does not exist");
            groupManager.delete(grp);
        } catch (ObjectModelException e) {
            throw new RemoteException("ObjectModelException in deleteGroup", e);
        } finally {
            endTransaction();
        }
    }

    public long saveGroup(long identityProviderConfigId, com.l7tech.adminws.identity.Group group) throws java.rmi.RemoteException {
        try {
            GroupManager groupManager = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId);
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
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException {
        GroupManager groupManager = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId);
        try {
            return TypeTranslator.collectionToServiceHeaders(groupManager.findAllHeaders());
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllGroups", e);
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        try {
            GroupManager groupManager = retrieveGroupManagerAndBeginTransaction(identityProviderConfigId);
            return TypeTranslator.collectionToServiceHeaders(groupManager.findAllHeaders(offset, windowSize));
        } catch (FindException e) {
            throw new RemoteException("FindException in findAllGroupsByOffset", e);
        } finally {
            endTransaction();
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private com.l7tech.identity.IdentityProviderConfigManager getIdentityProviderConfigManagerAndBeginTransaction() throws java.rmi.RemoteException {
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
        /*if (userManagersMap == null) userManagersMap = new java.util.HashMap();
        UserManager ret = (UserManager)userManagersMap.get(new Long(identityProviderConfigId));
        if (ret == null) */
        UserManager ret = null;
        try {
            IdentityProvider provider = IdentityProviderFactory.makeProvider(identityProviderConfigManager.findByPrimaryKey(identityProviderConfigId));
            ret = provider.getUserManager();
            //userManagersMap.put(new Long(identityProviderConfigId), ret);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("RemoteException in retrieveUserManager", e);
        }
        try {
            PersistenceContext.getCurrent().beginTransaction();
        } catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("Exception commiting", e);
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("Exception commiting", e);
        }
        return ret;
    }

    private GroupManager retrieveGroupManagerAndBeginTransaction(long identityProviderConfigId) throws java.rmi.RemoteException {
        /*if (groupManagersMap == null) groupManagersMap = new java.util.HashMap();
        GroupManager ret = (GroupManager)groupManagersMap.get(new Long(identityProviderConfigId));
        if (ret == null) */
        GroupManager ret = null;
        try {
            IdentityProvider provider = IdentityProviderFactory.makeProvider(identityProviderConfigManager.findByPrimaryKey(identityProviderConfigId));
            ret = provider.getGroupManager();
            //groupManagersMap.put(new Long(identityProviderConfigId), ret);
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("RemoteException in retrieveGroupManager", e);
        }
        try {
            PersistenceContext.getCurrent().beginTransaction();
        } catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("Exception commiting", e);
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("Exception commiting", e);
        }
        return ret;
    }

    private void initialiseConfigManager() throws java.rmi.RemoteException {
        try {
            // instantiate the server-side manager
            //HibernatePersistenceManager.initialize();
            identityProviderConfigManager = (com.l7tech.identity.IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
            if (identityProviderConfigManager == null) throw new java.rmi.RemoteException("Cannot instantiate the IdentityProviderConfigManager");
        } catch (ClassCastException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("ClassCastException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup", e);
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("RuntimeException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        } /*catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("SQLException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        } catch (java.io.IOException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("IOException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        } catch (javax.naming.NamingException e) {
            e.printStackTrace(System.err);
            throw new RemoteException("NamingException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        }*/
        /*
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
        */
    }

    IdentityProviderConfigManager identityProviderConfigManager = null;
    //java.util.HashMap userManagersMap = null;
    //java.util.HashMap groupManagersMap = null;
}
