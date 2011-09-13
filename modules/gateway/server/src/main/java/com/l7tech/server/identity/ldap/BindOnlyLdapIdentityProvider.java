package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;

/**
 *
 */
public interface BindOnlyLdapIdentityProvider extends AuthenticatingIdentityProvider<LdapUser, LdapGroup, BindOnlyLdapUserManager, BindOnlyLdapGroupManager>, LdapUrlProvider {
}
