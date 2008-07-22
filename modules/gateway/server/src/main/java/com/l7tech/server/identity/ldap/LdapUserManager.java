/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.ldap;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.ldap.LdapUser;

/**
 * @author alex
 */
public interface LdapUserManager extends UserManager<LdapUser> {
    void configure(LdapIdentityProvider provider);
    boolean authenticateBasic(String dn, String passwd);
}
