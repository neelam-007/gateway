package com.l7tech.identity.provider;

import com.l7tech.objectmodel.Manager;

/**
 * @author alex
 */
public interface IdentityProviderConfigManager extends Manager {
    public void delete( IdentityProviderConfig identityProviderConfig );
    public IdentityProviderConfig create();
}
