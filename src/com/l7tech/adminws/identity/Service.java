package com.l7tech.adminws.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.*;
import com.l7tech.util.Locator;
import com.l7tech.logging.LogManager;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.*;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 26, 2003
 *
 * Admin WS for identities (provider configs, users, groups)
 */
public class Service {

    public static final String VERSION = "20030529";
    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/identityAdmin";

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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "FindException in findAllIdentityProviderConfig", e);
            throw new RemoteException("FindException in findAllIdentityProviderConfig", e);
        } catch (Exception e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception in findAllIdentityProviderConfig", e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("FindException in findAllIdentityProviderConfigByOffset", e);
        } catch (Exception e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
                LogManager.getInstance().getSystemLogger().log(Level.INFO, "Updated IDProviderConfig: " + identityProviderConfig.getOid());
                return identityProviderConfig.getOid();
            }
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "Saving IDProviderConfig: " + identityProviderConfig.getOid());
            return getIdentityProviderConfigManagerAndBeginTransaction().save(identityProviderConfig);
        } catch (Exception e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("Exception in saveIdentityProviderConfig", e);
        } finally {
            endTransaction();
        }
    }
    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException {
        try {
            IdentityProviderConfigManager manager = getIdentityProviderConfigManagerAndBeginTransaction();
            manager.delete(manager.findByPrimaryKey(oid));
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "Deleted IDProviderConfig: " + oid);
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("FindException in deleteIdentityProviderConfig", e);
        }
        catch (DeleteException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("FindException in findAllUsers", e);
        } finally {
            endTransaction();
        }
    }
    public com.l7tech.identity.User findUserByPrimaryKey(long identityProviderConfigId, String userId) throws java.rmi.RemoteException {
        UserManager userManager = retrieveUserManagerAndBeginTransaction(identityProviderConfigId);
        if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
        try {
            User u = userManager.findByPrimaryKey(userId);
            Set groups = u.getGroups();

            if ( !groups.isEmpty() ) {
                Set groupHeaders = null;
                Group g;
                EntityHeader gh;
                for (Iterator i = groups.iterator(); i.hasNext();) {
                    g = (Group)i.next();
                    gh = new EntityHeader( g.getOid(), EntityType.GROUP, g.getName(), g.getDescription() );
                    if ( groupHeaders == null ) groupHeaders = new HashSet();
                    groupHeaders.add( gh );
                }
                u.setGroupHeaders( groupHeaders );
            }

            return u;
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "Deleted User: " + userId);
        } catch (DeleteException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("DeleteException in deleteUser", e);
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("FindException in deleteUser", e);
        } finally {
            endTransaction();
        }
    }

    private Map groupsToMap( Set groups ) {
        Map result = Collections.EMPTY_MAP;
        Group group;
        String oid;
        for (Iterator i = groups.iterator(); i.hasNext();) {
            group = (Group) i.next();
            oid = new Long( group.getOid() ).toString();
            if ( result == Collections.EMPTY_MAP ) result = new HashMap();
            result.put( oid, group );
        }
        return result;
    }

    private Map headersToMap(Set headers) {
        Map result = Collections.EMPTY_MAP;
        EntityHeader header;
        for (Iterator i = headers.iterator(); i.hasNext();) {
            header = (EntityHeader)i.next();
            if ( result == Collections.EMPTY_MAP ) result = new HashMap();
            result.put( header.getStrId(), header );
        }
        return result;
    }

    public long saveUser(long identityProviderConfigId, com.l7tech.identity.User user) throws java.rmi.RemoteException {
        try {
            // Let's try not to create nested transactions, since we're updating two classes of object
            IdentityProvider provider = null;
            try {
                provider = IdentityProviderFactory.makeProvider(getIdentityProviderConfigManagerAndBeginTransaction().findByPrimaryKey(identityProviderConfigId));
            } catch ( FindException fe ) {
                throw new java.rmi.RemoteException( "Couldn't get IdentityProvider!" );
            }

            UserManager userManager = provider.getUserManager();
            if (userManager == null) throw new java.rmi.RemoteException("Cannot retrieve the UserManager");
            GroupManager groupManager = provider.getGroupManager();
            if ( groupManager == null ) throw new java.rmi.RemoteException( "Cannot retrieve the GroupManager" );

            Map groupHeaderMap = headersToMap( user.getGroupHeaders() );
            Map groupMap = groupsToMap( user.getGroups() );

            EntityHeader header;
            Group group;
            String oid;

            // Add newly added headers
            for (Iterator i = groupHeaderMap.keySet().iterator(); i.hasNext();) {
                oid = (String) i.next();
                header = (EntityHeader)groupHeaderMap.get( oid );
                if ( !groupMap.containsKey( oid ) ) {
                    // This
                    groupMap.put( oid, groupManager.headerToGroup( header ) );
                }
            }

            Set groups = new HashSet();

            // Remove missing headers
            for (Iterator i = groupMap.keySet().iterator(); i.hasNext();) {
                oid = (String)i.next();
                group = (Group)groupMap.get( oid );
                if ( !groupHeaderMap.containsKey( oid ) ) {
                    // This group is no longer in the headers
                    groupMap.remove( oid );
                }
            }

            for (Iterator i = groupMap.keySet().iterator(); i.hasNext();) {
                oid = (String)i.next();
                group = (Group)groupMap.get(oid);
                groups.add( group );
            }

            user.setGroups( groups );

            if (user.getOid() > 0) {
                User originalUser = userManager.findByPrimaryKey(Long.toString(user.getOid()));
                originalUser.copyFrom(user);
                userManager.update(originalUser);

                LogManager.getInstance().getSystemLogger().log(Level.INFO, "Updated User: " + user.getOid());
                return user.getOid();
            }
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "Saving User: " + user.getOid());
            return userManager.save(user);
        } catch (Exception e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("FindException in findAllGroups", e);
        } finally {
            endTransaction();
        }
    }
    public com.l7tech.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, String groupId) throws java.rmi.RemoteException {
        try {
            return retrieveGroupManagerAndBeginTransaction(identityProviderConfigId).findByPrimaryKey(groupId);
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
                LogManager.getInstance().getSystemLogger().log(Level.INFO, "Updated Group: " + group.getOid());
                return group.getOid();
            }
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "Saving Group: " + group.getOid());
            return groupManager.save(group);
        } catch (SaveException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("SaveException in saveGroup", e);
        } catch (UpdateException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new java.rmi.RemoteException("UpdateException in saveGroup", e);
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("SQLException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        } catch (TransactionException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("TransactionException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup: "+ e.getMessage(), e);
        }
        return identityProviderConfigManager;
    }

    private void endTransaction() throws java.rmi.RemoteException {
        try {
            PersistenceContext context = PersistenceContext.getCurrent();
            context.commitTransaction();
            context.close();
        } catch (java.sql.SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("Exception commiting", e);
        } catch ( ObjectModelException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("Exception commiting", e);
        }
    }

    private UserManager retrieveUserManagerAndBeginTransaction(long identityProviderConfigId) throws java.rmi.RemoteException {
        UserManager ret = null;
        try {
            IdentityProvider provider = IdentityProviderFactory.makeProvider(getIdentityProviderConfigManagerAndBeginTransaction().findByPrimaryKey(identityProviderConfigId));
            ret = provider.getUserManager();
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new RemoteException("ClassCastException in IdentitiesSoapBindingImpl.initialiseConfigManager from Locator.getDefault().lookup", e);
        } catch (RuntimeException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
                throw new java.rmi.RemoteException("Collection contained something other than a com.l7tech.objectmodel.EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

    IdentityProviderConfigManager identityProviderConfigManager = null;
}
