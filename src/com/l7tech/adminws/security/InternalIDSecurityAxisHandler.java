package com.l7tech.adminws.security;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.util.Locator;

import java.util.Collection;
import java.util.Iterator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: June 2, 2003
 *
 * Base class for authentication axis handlers
 */
public abstract class InternalIDSecurityAxisHandler extends org.apache.axis.handlers.BasicHandler {

    protected boolean userIsMemberOfGroup(long userOid, String groupName) {
        /*
        IMPLEMENTATION 1, GET USER BY ID , THEN SEE IF SSGAdmin is part of the groups he's in
        THIS DOES NOT SEEM TO WORK BECAUSE OF AN APPARENT ISSUE WITH USER.GETGROUP RETURNING NULL (ALWAYS)
        com.l7tech.identity.User user = findUserByOid(userOid);
        if (user == null) return false;
        java.util.Collection groups = user.getGroups();
        if (groups == null)
        {
            System.err.println("InternalIDSecurityAxisHandler.userIsMemberOfGroup user " + userOid + " has no group memberships.");
            return false;
        }
        java.util.Iterator i = groups.iterator();
        while (i.hasNext()) {
            com.l7tech.identity.Group group = (com.l7tech.identity.Group)i.next();
            if (group.getName().equals(groupName)) {
                return true;
            }
        }
        System.err.println("InternalIDSecurityAxisHandler.userIsMemberOfGroup: user oid " + userOid + "denied membership to group " + groupName);
        return false;
        */
        
        // IMPLEMENTATION 2, get the group, then see if the user is in there
        Group grp = findGroupByNameAndRealm(groupName, null);
        if (grp == null) {
            System.err.println("InternalIDSecurityAxisHandler.userIsMemberOfGroup group " + groupName + " cannot be found.");
            return false;
        }
        Collection members = grp.getMembers();
        if (members == null) {
            System.err.println("InternalIDSecurityAxisHandler.userIsMemberOfGroup group " + groupName + " has no members.");
            return false;
        }
        java.util.Iterator i = members.iterator();
        while (i.hasNext()) {
            com.l7tech.identity.User user = (com.l7tech.identity.User)i.next();
            if (user.getOid() == userOid) {
                return true;
            }
        }
        System.err.println("InternalIDSecurityAxisHandler.userIsMemberOfGroup: user oid " + userOid + "denied membership to group " + groupName);
        return false;
    }

    protected com.l7tech.identity.User findUserByLoginAndRealm(String login, String realm) {
        try {
            UserManager manager = getInternalUserManagerAndBeginTransaction();
            Collection users = manager.findAll();
            Iterator i = users.iterator();
            while (i.hasNext()) {
                User user = (User)i.next();
                if (user.getLogin().equals(login)) return user;
            }
            return null;
        } catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            return null;
        } catch (FindException e) {
            e.printStackTrace(System.err);
            return null;
        } catch (NullPointerException e){
            e.printStackTrace(System.err);
            return null;
        } finally {
            endTransaction();
        }
    }

    protected com.l7tech.identity.Group findGroupByNameAndRealm(String name, String realm) {
        try {
            GroupManager manager = getInternalGroupManagerAndBeginTransaction();
            Collection groups = manager.findAll();
            Iterator i = groups.iterator();
            while (i.hasNext()) {
                Group grp = (Group)i.next();
                if (grp.getName().equals(name)) return grp;
            }
            return null;
        } catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            return null;
        } catch (FindException e) {
            e.printStackTrace(System.err);
            return null;
        } finally {
            endTransaction();
        }
    }

    protected com.l7tech.identity.User findUserByOid(long oid) {
        try {
            UserManager manager = getInternalUserManagerAndBeginTransaction();
            return manager.findByPrimaryKey(Long.toString(oid));
        } catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
            return null;
        } catch (FindException e) {
            e.printStackTrace(System.err);
            return null;
        } finally {
            endTransaction();
        }
    }

    private UserManager getInternalUserManagerAndBeginTransaction() throws java.sql.SQLException {
        try {
            PersistenceContext.getCurrent().beginTransaction();
            com.l7tech.identity.GlobalIdProviderConfManager identityProviderConfigManager = (com.l7tech.identity.GlobalIdProviderConfManager)Locator.getDefault().lookup(com.l7tech.identity.GlobalIdProviderConfManager.class);
            if (identityProviderConfigManager == null) throw new java.sql.SQLException("could not instantiate the IdentityProviderConfigManager");
            return identityProviderConfigManager.getInternalIdentityProvider().getUserManager();
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
            throw new java.sql.SQLException("TransactionException in getInternalUserManagerAndBeginTransaction "+ e.getMessage());
        }
    }

    private GroupManager getInternalGroupManagerAndBeginTransaction() throws java.sql.SQLException {
        try {
            PersistenceContext.getCurrent().beginTransaction();
            com.l7tech.identity.GlobalIdProviderConfManager identityProviderConfigManager = (com.l7tech.identity.GlobalIdProviderConfManager)Locator.getDefault().lookup(com.l7tech.identity.GlobalIdProviderConfManager.class);
            if (identityProviderConfigManager == null) throw new java.sql.SQLException("could not instantiate the IdentityProviderConfigManager");
            return identityProviderConfigManager.getInternalIdentityProvider().getGroupManager();
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
            throw new java.sql.SQLException("TransactionException in getInternalGroupManagerAndBeginTransaction "+ e.getMessage());
        }
    }

    private void endTransaction() {
        try {
            PersistenceContext.getCurrent().commitTransaction();
        } catch (java.sql.SQLException e) {
            e.printStackTrace(System.err);
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * key used by the com.l7tech.adminws.security handlers to get or set the authenticated
     * user inside the axis MessageContext (org.apache.axis.MessageContext)
     *
     * ex.:
     Long authedUserId = MessageContext.getProperty(AUTHENTICATED_USER);
     */
    static final String AUTHENTICATED_USER = "Authenticated_com.l7tech.identity.User";
}
