/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.identity.ldap.LdapUser;

/**
 * @author alex
 */
public interface LdapGroupManager extends GroupManager<LdapUser, LdapGroup> {
    void configure(LdapIdentityProvider provider);
}
