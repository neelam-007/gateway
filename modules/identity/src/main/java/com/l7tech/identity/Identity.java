/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.identity;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.rbac.RbacAttribute;

import java.security.Principal;

/**
 * Interface from which {@link User} and {@link Group} inherit.
 *
 * The inherited {@link #getId()} method uniquely identifies this Identity within its {@link IdentityProvider}.
 * For internal and federated identities, this will be a String representation of the OID.
 * For LDAP identities, this will be the DN.
 */
public interface Identity extends Principal, Entity {
    public static final String ATTR_PROVIDER_OID = "providerId";

    /**
     * Gets the OID of the {@link com.l7tech.identity.IdentityProviderConfig} to which this User belongs. This is only persisted for {@link com.l7tech.identity.fed.FederatedUser}s.
     * @return the OID of the {@link com.l7tech.identity.IdentityProviderConfig} to which this User belongs.
     *
     * For internal users, the provider ID is {@link com.l7tech.identity.IdentityProviderConfigManager#INTERNALPROVIDER_SPECIAL_GOID}
     */
    Goid getProviderId();

    /**
     * Check whether the provided ID is semantically equivalent to this Identity's.
     * @param thatId the ID to compare against this Identity's ID.  Must not be null.
     * @return <code>true</code> if the IDs are semantically equivalent, <code>false</code> otherwise.
     */
    boolean isEquivalentId(Object thatId);

    @RbacAttribute
    String getName();
}
