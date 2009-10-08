package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.InvalidIdProviderCfgException;

/**
 * Interface for IdentityProviders that Support configuration.
 */
public interface ConfigurableIdentityProvider {

    /**
     * Set the configuration for use with the provider.
     *
     * @param configuration The configuration to use
     * @throws InvalidIdProviderCfgException If the configuration is invalid.
     */
    void setIdentityProviderConfig(final IdentityProviderConfig configuration) throws InvalidIdProviderCfgException;

    /**
     * The provider should start any maintenance tasks.
     *
     * <p>This should not be invoked before the configuration is set.</p> 
     */
    void startMaintenance();
}
