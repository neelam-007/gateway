package com.l7tech.identity;

import java.security.Principal;

/**
 * Represents a set of {@link User}s.
 *
 * Memberships are queried and managed using the {@link IdentityAdmin} API rather than through properties of 
 * the Group objects themselves.
 */
public interface Group extends Principal {
    /**
     * Name of the admin group. those admins can do anything.
     * This group is there at ssg install and cannot be made empty
     * nor deleted.
     */
    static final String ADMIN_GROUP_NAME = "Gateway Administrators";
    /**
     * Name of the operator group. Those admins have read only permissions.
     * This group can be made empty but cannot be deleted.
     */
    static final String OPERATOR_GROUP_NAME = "Gateway Operators";

    /**
     * Returns a String that uniquely identifies this Group within its IdentityProvider.
     * For internal groups, this will be a String representation of the OID.
     * For LDAP groups, this will be the DN.
     */
    String getUniqueIdentifier();

    /**
     * Gets the OID of the {@link IdentityProviderConfig} to which this Group belongs.
     * @return the OID of the {@link IdentityProviderConfig} to which this Group belongs.
     *
     * @see IdentityProviderConfigManager#INTERNALPROVIDER_SPECIAL_OID
     */
    long getProviderId();

    /**
     * Sets the OID of the {@link IdentityProviderConfig} to which this Group belongs.
     * @param providerId the OID of the {@link IdentityProviderConfig} to which this Group belongs.
     *
     * @see IdentityProviderConfigManager#INTERNALPROVIDER_SPECIAL_OID
     */
    void setProviderId(long providerId);

    /**
     * Gets a human-readable description for this Group
     * @return a human-readable description for this Group
     */
    String getDescription();

    /**
     * Gets the mutable {@link GroupBean} for this Group.
     * @return the mutable {@link GroupBean} for this Group.
     */
    GroupBean getGroupBean();
}
