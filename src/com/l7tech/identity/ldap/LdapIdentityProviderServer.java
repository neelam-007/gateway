package com.l7tech.identity.ldap;

import com.l7tech.identity.*;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFormat;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.directory.*;
import java.util.logging.Level;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 */
public class LdapIdentityProviderServer implements IdentityProvider {
    public void initialize(IdentityProviderConfig config) {
        if (!(config.type() == IdentityProviderType.LDAP)) throw new IllegalArgumentException("Expecting Ldap config type");
        cfg = config;
        groupManager = new LdapGroupManagerServer(cfg);
        userManager = new LdapUserManagerServer(cfg);
    }

    public IdentityProviderConfig getConfig() {
        return cfg;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public boolean authenticate( PrincipalCredentials pc ) {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "invalid id provider asked to authenticate");
            return false;
        }
        User realUser = null;
        try {
            realUser = userManager.findByLogin(pc.getUser().getLogin());
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "invalid user", e);
            return false;
        }
        if (realUser == null) {
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "invalid user");
            return false;
        }
        if ( pc.getFormat() == CredentialFormat.CLEARTEXT ) {
            // basic authentication
            boolean res = userManager.authenticateBasic(realUser.getName(), new String(pc.getCredentials()));
            if (res) pc.getUser().copyFrom(realUser);
            return res;
        } else {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Attempt to authenticate using unsupported method" + pc.getFormat());
            throw new IllegalArgumentException( "Only cleartext credentials are currently supported!" );
        }
    }

    public boolean isReadOnly() { return true; }

    public void invalidate() {
        valid = false;
        groupManager.invalidate();
        userManager.invalidate();
    }

    /**
     * searches for users and groups whose name (cn) match the pattern described in searchString
     * pattern may include wildcard such as * character
     */
    public Collection search(EntityType[] types, String searchString) throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "invalid id provider asked for search");
            throw new FindException("provider invalidated");
        }
        if (types == null || types.length < 1) throw new IllegalArgumentException("must pass at least one type");
        boolean wantUsers = false;
        boolean wantGroups = false;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == EntityType.USER) wantUsers = true;
            else if (types[i] == EntityType.GROUP) wantGroups = true;
        }
        if (!wantUsers && !wantGroups) throw new IllegalArgumentException("types must contain users and or groups");
        Collection output = new ArrayList();
        try
        {
            NamingEnumeration answer = null;
            // search string for users and or groups based on passed types wanted
            String filter = null;
            if (wantUsers && wantGroups) {
                filter = "(&(|(objectclass=" + LdapGroupManagerServer.GROUP_OBJCLASS + ")(objectclass=" + LdapUserManagerServer.USER_OBJCLASS + "))(" + LdapManager.NAME_ATTR_NAME + "=" + searchString + "))";
            } else if (wantUsers) {
                filter = "(&(objectclass=" + LdapUserManagerServer.USER_OBJCLASS + ")(" + LdapManager.NAME_ATTR_NAME + "=" + searchString + "))";
            } else if (wantGroups) {
                filter = "(&(objectclass=" + LdapGroupManagerServer.GROUP_OBJCLASS + ")(" + LdapManager.NAME_ATTR_NAME + "=" + searchString + "))";
            }
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            java.util.Hashtable env = new java.util.Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, cfg.getProperty(LdapConfigSettings.LDAP_HOST_URL));
            DirContext context = new InitialDirContext(env);
            answer = context.search(cfg.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                // get this item
                SearchResult sr = (SearchResult)answer.next();
                // set the dn (unique id)
                String dn = sr.getName() + "," + cfg.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                // is it user or group ?
                Attribute objectclasses = atts.get("objectclass");
                EntityHeader header = null;
                // construct header accordingly
                if (objectclasses.contains(LdapGroupManagerServer.GROUP_OBJCLASS)) {
                    header = new EntityHeader(dn, EntityType.GROUP, dn, null);
                } else if (objectclasses.contains(LdapUserManagerServer.USER_OBJCLASS)) {
                    Object tmp = LdapManager.extractOneAttributeValue(atts, LdapManager.LOGIN_ATTR_NAME);
                    if (tmp != null) {
                        header = new EntityHeader(dn, EntityType.USER, tmp.toString(), null);
                    }
                }
                // if we successfully constructed a header, add it to result list
                if (header != null) output.add(header);
            }
            if (answer != null) answer.close();
            context.close();
        }
        catch (NamingException e)
        {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new FindException(e.getMessage(), e);
        }
        return output;
    }


    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityProviderConfig cfg = null;
    private LdapGroupManagerServer groupManager = null;
    private LdapUserManagerServer userManager = null;
    private volatile boolean valid = true;
}
