package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.BindOnlyLdapUser;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;

/**
 *
 */
public interface BindOnlyLdapIdentityProvider extends AuthenticatingIdentityProvider<BindOnlyLdapUser, LdapGroup, BindOnlyLdapUserManager, BindOnlyLdapGroupManager>, LdapUrlProvider {
}
