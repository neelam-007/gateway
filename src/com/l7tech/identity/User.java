package com.l7tech.identity;

import java.security.Principal;

/**
 * Read-only interface for a User either from the internal identity provider or a ldap directory.
 * In the case of ldap, the uniqueIdentifier property contains the dn.
 * In the case of internal, it's a String representation of the oid.
 *
 * Password property is stored as HEX(MD5(login:L7SSGDigestRealm:password)). If you pass a clear text passwd in
 * setPassword, this encoding will be done ofr you (provided that login was set before).
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
public interface User extends Principal {
    /**
     * This method should return an identifier for this User that is unique within its IdentityProvider.
     * For internal users, this can be a String representation of the OID.
     * For LDAP, it should be the DN.
     */
    String getUniqueIdentifier();
    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    long getProviderId();
    void setProviderId(long providerId);
    String getLogin();
    String getPassword();
    String getFirstName();
    String getLastName();
    String getEmail();
    String getDepartment();
    String getSubjectDn();
    UserBean getUserBean();
}
