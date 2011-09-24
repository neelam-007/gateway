package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.BindOnlyLdapUser;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;

/**
 * A non-listable identity provider that authenticates username+password credentials by constructing a DN out of the username, and then trying a bind using the password.
 */
public interface BindOnlyLdapIdentityProvider extends AuthenticatingIdentityProvider<BindOnlyLdapUser, LdapGroup, BindOnlyLdapUserManager, BindOnlyLdapGroupManager>, LdapUrlProvider {
}
