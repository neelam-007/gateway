package com.l7tech.identity;

import java.security.Principal;

/**
 * Read-only interface for a User either from the internal identity provider or a ldap directory.
 * In the case of ldap, the uniqueIdentifier property contains the dn.
 * In the case of internal, it's a String representation of the oid.
 *
 * Password property is stored as HEX(MD5(login:L7SSGDigestRealm:password)). If you pass a clear text passwd in
 * setPassword, this encoding will be done for you (provided that login was set before).
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
     * Gets the OID of the {@link IdentityProviderConfig} to which this User belongs. This is only persisted for {@link com.l7tech.identity.fed.FederatedUser}s.
     * @return the OID of the {@link IdentityProviderConfig} to which this User belongs.
     *
     * For internal users, the provider ID is {@link IdentityProviderConfigManager#INTERNALPROVIDER_SPECIAL_OID}
     */
    long getProviderId();

    /**
     * Sets the OID of the {@link IdentityProviderConfig} to which this User belongs. This is only persisted for {@link com.l7tech.identity.fed.FederatedUser}s.
     * @param providerId the OID of the {@link IdentityProviderConfig} to which this User belongs.
     */
    void setProviderId(long providerId);

    /**
     * Gets the user's login ID
     * @return the user's login ID. May be null.
     */
    String getLogin();

    /**
     * Gets the User's password
     * @return the User's password. May be null.
     */
    String getPassword();

    /**
     * Gets the User's first name.
     * @return the User's first name. May be null.
     */
    String getFirstName();

    /**
     * Gets the User's last name.
     * @return the User's last name. May be null.
     */
    String getLastName();

    /**
     * Gets the User's email address.
     * @return the User's email address. May be null.
     */
    String getEmail();

    /**
     * Gets the User's department
     * @return the User's department. May be null.
     */
    String getDepartment();

    /**
     * Gets the User's X.509 subject DN (mainly used for {@link com.l7tech.identity.fed.FederatedUser}s
     * @return the User's X.509 subject DN. May be null.
     */
    String getSubjectDn();

    /**
     * Gets the mutable {@link UserBean} for this User.
     * @return the mutable {@link UserBean} for this User.
     */
    UserBean getUserBean();
}
