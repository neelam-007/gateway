package com.l7tech.identity.ldap;

import com.l7tech.identity.*;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFormat;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Level;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 */
public class LdapIdentityProviderServer implements com.l7tech.identity.IdentityProvider {
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


    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityProviderConfig cfg = null;
    private LdapGroupManagerServer groupManager = null;
    private LdapUserManagerServer userManager = null;
    private volatile boolean valid = true;
}
