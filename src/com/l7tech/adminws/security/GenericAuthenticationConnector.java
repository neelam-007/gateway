package com.l7tech.adminws.security;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Locator;

import java.util.Collection;
import java.util.Iterator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 3, 2003
 *
 * An additional layer of abstraction for the Tomcat specific authentication realm in case
 * we want to reuse for another servlet engine.
 */
public class GenericAuthenticationConnector {
    public boolean userIsMemberOfGroup(long userOid, String groupName) {
        com.l7tech.identity.User user = findUserByOid(userOid);
        java.util.Collection groups = user.getGroups();
        java.util.Iterator i = groups.iterator();
        while (i.hasNext()) {
            com.l7tech.identity.Group group = (com.l7tech.identity.Group)i.next();
            if (group.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    public com.l7tech.identity.User findUserByLoginAndRealm(String login, String realm) {
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
        } finally {
            endTransaction();
        }
    }

    public com.l7tech.identity.User findUserByOid(long oid) {
        try {
            UserManager manager = getInternalUserManagerAndBeginTransaction();
            return manager.findByPrimaryKey(oid);
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
            IdentityProviderConfigManager identityProviderConfigManager = (com.l7tech.identity.IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
            if (identityProviderConfigManager == null) throw new java.sql.SQLException("Cannot instantiate the IdentityProviderConfigManager");
            PersistenceContext.getCurrent().beginTransaction();
            Collection ipcCollection = identityProviderConfigManager.findAll();
            Iterator i = ipcCollection.iterator();
            while (i.hasNext()) {
                IdentityProviderConfig ipc = (IdentityProviderConfig)i.next();
                // todo, verify we have the right type of provider (once more than one type exist)
                IdentityProvider provider = IdentityProviderFactory.makeProvider(ipc);
                return provider.getUserManager();
            }
        } catch (TransactionException e) {
            e.printStackTrace(System.err);
            throw new java.sql.SQLException("TransactionException in getInternalUserManagerAndBeginTransaction "+ e.getMessage());
        } catch (FindException e) {
            e.printStackTrace(System.err);
            throw new java.sql.SQLException("TransactionException in getInternalUserManagerAndBeginTransaction "+ e.getMessage());
        }
        return null;
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

    /*
    public static void main (String[] args) throws Exception {
        // calculate the digest for a specific set of values
        java.security.MessageDigest md5Helper = java.security.MessageDigest.getInstance("MD5");
        org.apache.catalina.util.MD5Encoder md5Encoder = new org.apache.catalina.util.MD5Encoder();
        String digestValue = "ssgadmin::ssgadminpasswd";
        byte[] digest = md5Helper.digest(digestValue.getBytes());
        System.out.println(md5Encoder.encode(digest));

        result: 60189d5f68d564f9cb83e11bc2ae92e9

    }
    */
}
