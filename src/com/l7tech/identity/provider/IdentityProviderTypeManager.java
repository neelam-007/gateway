package com.l7tech.identity.provider;

import com.l7tech.objectmodel.StandardManager;

/**
 * @author alex
 */
public interface IdentityProviderTypeManager extends StandardManager {
    public void delete( IdentityProviderType identityProviderType );
    public IdentityProviderType create();
}
