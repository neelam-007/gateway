package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.InvalidIdProviderCfgException;

/**
 * Interface for IdentityProviders that Support configuration.
 *
 * <p>Also used for any {@link IdentityProviderConfig} aware classes.</p>
 */
public interface ConfigurableIdentityProvider {
    void setIdentityProviderConfig(final IdentityProviderConfig configuration) throws InvalidIdProviderCfgException;
}
