package com.l7tech.identity.provider;

import com.l7tech.objectmodel.StandardManager;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends StandardManager {
    public void delete( IdentityProviderConfig identityProviderConfig );
    public IdentityProviderConfig create();
}
