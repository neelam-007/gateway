package com.l7tech.identity.ldap;

import com.l7tech.identity.Identity;

import javax.naming.directory.Attributes;

/**
 * Read-only interface carrying attributes common to all LDAP-based identities,
 * including {@link LdapUser}s and {@link LdapGroup}s.
 */
public interface LdapIdentity extends Identity {
    /**
     * @return the identity's distinguished name (DN).  Never null; unlikely to be empty.
     */
    String getDn();

    /**
     * @return the identity's common name (CN).  May be null or empty.
     */
    String getCn();

    /**
     * @return the {@link Attributes} map for this identity.  May be null.
     */
    Attributes getAttributes();
}
