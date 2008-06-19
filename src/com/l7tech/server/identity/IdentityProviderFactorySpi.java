package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.InvalidIdProviderCfgException;

/**
 * Service provider interface for identity provider construction.
 */
public interface IdentityProviderFactorySpi {

    /**
     * Get the "classname" of the provider.
     *
     * @return The provider classname.
     */
    String getClassname();

    /**
     * Create an identity provider with the given configuration.
     *
     * @param configuration The configuration to use
     * @return The identity provider.
     * @throws InvalidIdProviderCfgException if the configuration is not valid
     */
    IdentityProvider createIdentityProvider(IdentityProviderConfig configuration) throws InvalidIdProviderCfgException;
}
