package com.l7tech.identity;

import java.security.Principal;

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

    long getProviderId();
    void setProviderId(long providerId);
    
    String getDescription();

    GroupBean getGroupBean();
}
