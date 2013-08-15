package com.l7tech.identity.ldap;

import com.l7tech.objectmodel.Goid;

/**
 * Represents a bind-only user.  Unlike a regular user, the login name is used as the unique identifier (rather than the full DN, which is expanded at runtime).
 */
public class BindOnlyLdapUser extends LdapUser {
    public BindOnlyLdapUser(Goid identityProviderGoid, String dn, String login) {
        super(identityProviderGoid, dn, login);
        setLogin( login );
    }

    @Override
    public String getId() {
        return cn;
    }

    @Override
    public boolean isEquivalentId(Object thatId) {
        return cn.equals(thatId);
    }
}
