package com.l7tech.identity.ldap;

import com.l7tech.identity.*;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFormat;
import com.l7tech.logging.LogManager;

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
        if ( pc.getFormat() == CredentialFormat.CLEARTEXT ) {
            // basic authentication
            return userManager.authenticateBasic( pc.getUser().getName(), new String( pc.getCredentials() ));
        } else {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Attempt to authenticate using unsupported method" + pc.getFormat());
            throw new IllegalArgumentException( "Only cleartext credentials are currently supported!" );
        }
    }

    public boolean isReadOnly() { return true; }


    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityProviderConfig cfg = null;
    private LdapGroupManagerServer groupManager = null;
    private LdapUserManagerServer userManager = null;
}
