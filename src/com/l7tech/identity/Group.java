package com.l7tech.identity;

import java.security.Principal;

public interface Group extends Principal {
    static final String ADMIN_GROUP_NAME = "Gateway Administrators";

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
