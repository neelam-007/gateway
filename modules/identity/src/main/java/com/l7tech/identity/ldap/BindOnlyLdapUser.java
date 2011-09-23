package com.l7tech.identity.ldap;

/**
 * Represents a bind-only user.  Unlike a regular user, the login name is used as the unique identifier (rather than the full DN, which is expanded at runtime).
 */
public class BindOnlyLdapUser extends LdapUser {
    public BindOnlyLdapUser(long identityProviderOid, String dn, String login) {
        super(identityProviderOid, dn, login);
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
