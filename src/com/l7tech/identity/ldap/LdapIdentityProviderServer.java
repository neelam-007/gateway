package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 */
public class LdapIdentityProviderServer extends AbstractLdapIdentityProviderServer implements IdentityProvider {
    protected void doInitialize( IdentityProviderConfig config ) {
        if (config.type() != IdentityProviderType.LDAP) {
            throw new IllegalArgumentException("Expecting Ldap config type");
        }
        cfg = config;
        groupManager = new LdapGroupManagerServer(cfg);
        userManager = new LdapUserManagerServer(cfg);
    }

    protected AbstractLdapConstants getConstants() {
        return _constants;
    }

    protected LdapConstants _constants = new LdapConstants();
}
