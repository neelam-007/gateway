package com.l7tech.server.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.ldap.BindOnlyLdapUser;
import com.l7tech.identity.ldap.LdapGroup;

/**
 * Group manager for bind-only provider.
 */
public interface BindOnlyLdapGroupManager extends GroupManager<BindOnlyLdapUser, LdapGroup> {
}
