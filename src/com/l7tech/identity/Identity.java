/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.identity;

import java.security.Principal;

/**
 * Interface from which {@link User} and {@link Group} inherit.
 */
public interface Identity extends Principal {
    /**
     * Gets the OID of the {@link com.l7tech.identity.IdentityProviderConfig} to which this User belongs. This is only persisted for {@link com.l7tech.identity.fed.FederatedUser}s.
     * @return the OID of the {@link com.l7tech.identity.IdentityProviderConfig} to which this User belongs.
     *
     * For internal users, the provider ID is {@link com.l7tech.identity.IdentityProviderConfigManager#INTERNALPROVIDER_SPECIAL_OID}
     */
    long getProviderId();

    /**
     * Sets the OID of the {@link com.l7tech.identity.IdentityProviderConfig} to which this User belongs. This is only persisted for {@link com.l7tech.identity.fed.FederatedUser}s.
     * @param providerId the OID of the {@link com.l7tech.identity.IdentityProviderConfig} to which this User belongs.
     */
    void setProviderId(long providerId);

    /**
     * Returns a String that uniquely identifies this Identity within its {@link IdentityProvider}.
     * For internal and federated identities, this will be a String representation of the OID.
     * For LDAP identities, this will be the DN.
     */
    String getUniqueIdentifier();
}
