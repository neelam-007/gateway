package com.l7tech.identity;

/**
 * User: flascell
 * Date: Jun 23, 2003
 * Time: 9:18:20 AM
 *
 * Add the getInternalIdentityProvider to the IdentityProviderConfigManager interface
 */
public interface GlobalIdProviderConfManager extends IdentityProviderConfigManager {
    public IdentityProvider getInternalIdentityProvider();
}
